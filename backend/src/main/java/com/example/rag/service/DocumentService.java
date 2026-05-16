package com.example.rag.service;

import com.example.rag.model.DocumentChunkEntity;
import com.example.rag.model.DocumentEntity;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.repository.DocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles document ingestion: object storage, metadata persistence, text
 * chunking, and vector indexing.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final int CHUNK_SIZE = 1600;
    private static final int CHUNK_OVERLAP = 200;

    private final MinioClient minioClient;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<?> embeddingStore;

    @Value("${minio.bucket}")
    private String bucketName;

    public DocumentService(MinioClient minioClient,
                           DocumentRepository documentRepository,
                           DocumentChunkRepository documentChunkRepository,
                           EmbeddingModel embeddingModel,
                           EmbeddingStore<?> embeddingStore) {
        this.minioClient = minioClient;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Upload a file, persist metadata, split text into chunks, and index each
     * chunk in Milvus. Vector item IDs now point to chunk rows, not document
     * metadata rows, so retrieval can hydrate real text for the final prompt.
     */
    public DocumentEntity uploadDocument(MultipartFile file) throws IOException {
        return uploadDocument(file, null);
    }

    public DocumentEntity uploadDocument(MultipartFile file, String tenantId) throws IOException {
        ensureBucketExists();

        byte[] fileBytes = file.getBytes();
        String filename = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "uploaded-document"
                : file.getOriginalFilename();
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();
        String objectKey = UUID.randomUUID() + "-" + filename;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IOException("Failed to store uploaded document in object storage", e);
        }

        DocumentEntity entity = new DocumentEntity();
        entity.setFilename(filename);
        entity.setContentType(contentType);
        entity.setObjectKey(objectKey);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setTenantId(normalizeTenantId(tenantId));
        entity = documentRepository.save(entity);

        String content = extractText(fileBytes, filename);
        List<TextChunk> chunks = splitIntoChunks(content);
        entity.setChunkCount(chunks.size());
        entity = documentRepository.save(entity);

        int embeddedCount = 0;
        for (TextChunk chunk : chunks) {
            DocumentChunkEntity chunkEntity = new DocumentChunkEntity();
            chunkEntity.setDocument(entity);
            chunkEntity.setChunkIndex(chunk.index());
            chunkEntity.setContent(chunk.content());
            chunkEntity.setCharStart(chunk.start());
            chunkEntity.setCharEnd(chunk.end());
            chunkEntity.setCreatedAt(LocalDateTime.now());
            chunkEntity = documentChunkRepository.save(chunkEntity);

            try {
                Embedding embedding = embeddingModel.embed(chunk.content()).content();
                embeddingStore.add(String.valueOf(chunkEntity.getId()), embedding);
                embeddedCount++;
                log.info("Stored embedding for document {} chunk {}", entity.getId(), chunkEntity.getId());
            } catch (Exception e) {
                log.error("Failed to embed document {} chunk {}: {}", entity.getId(), chunkEntity.getId(), e.getMessage());
            }
        }

        if (!chunks.isEmpty() && embeddedCount == 0) {
            log.warn("Document {} has {} chunks but zero embeddings were stored", entity.getId(), chunks.size());
        }

        return entity;
    }

    private String extractText(byte[] fileBytes, String fallbackName) {
        try {
            String text = new String(fileBytes, StandardCharsets.UTF_8)
                    .replace("\u0000", " ")
                    .trim();
            if (!text.isBlank()) {
                return text;
            }
        } catch (Exception e) {
            log.warn("Failed to read file as UTF-8: {}", e.getMessage());
        }
        return fallbackName == null || fallbackName.isBlank() ? "Untitled document" : fallbackName;
    }

    private List<TextChunk> splitIntoChunks(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            normalized = "Empty document";
        }

        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            String chunkText = normalized.substring(start, end).trim();
            if (!chunkText.isBlank()) {
                chunks.add(new TextChunk(index++, start, end, chunkText));
            }
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created bucket {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error ensuring bucket existence: {}", e.getMessage());
        }
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? "default"
                : tenantId.trim().toLowerCase(Locale.ROOT);
    }

    private record TextChunk(int index, int start, int end, String content) {
    }
}
