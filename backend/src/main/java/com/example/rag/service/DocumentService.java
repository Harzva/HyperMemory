package com.example.rag.service;

import com.example.rag.model.DocumentEntity;
import com.example.rag.repository.DocumentRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.messages.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for handling document ingestion: uploading files to
 * object storage, recording metadata in the relational database, and
 * generating and persisting vector embeddings into Milvus.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final MinioClient minioClient;
    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<String> embeddingStore;

    @Value("${minio.bucket}")
    private String bucketName;

    public DocumentService(MinioClient minioClient,
                           DocumentRepository documentRepository,
                           EmbeddingModel embeddingModel,
                           EmbeddingStore<String> embeddingStore) {
        this.minioClient = minioClient;
        this.documentRepository = documentRepository;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * Upload a file to MinIO, save metadata to MySQL and generate an embedding.
     * This method is intentionally simple: it treats the entire file as a
     * single text chunk. In a real application you would parse PDFs/Word
     * documents into smaller segments using LangChain4j's document parsers
     * (Apache Tika, POI) and call embeddingModel.embedAll().
     *
     * @param file Multipart file uploaded by the client
     * @return persisted DocumentEntity with generated ID
     */
    public DocumentEntity uploadDocument(MultipartFile file) throws IOException {
        // Ensure bucket exists; create if not present
        ensureBucketExists();

        // Generate a unique object key to avoid collisions
        String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
        // Upload file to MinIO
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        // Persist metadata to MySQL
        DocumentEntity entity = new DocumentEntity();
        entity.setFilename(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setObjectKey(objectKey);
        entity.setCreatedAt(LocalDateTime.now());
        entity = documentRepository.save(entity);

        // Derive basic text from the file. For demonstration we only handle
        // plain text files. Binary documents are skipped.
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to read file as UTF-8: {}", e.getMessage());
            content = file.getOriginalFilename();
        }

        // Generate vector for the content and store in Milvus
        try {
            Embedding embedding = embeddingModel.embed(content).content();
            // We associate the vector with the document ID as the item key. This
            // allows us to retrieve the document later during semantic search.
            embeddingStore.add(String.valueOf(entity.getId()), embedding.vector());
            log.info("Stored embedding for document {}", entity.getId());
        } catch (Exception e) {
            log.error("Failed to embed and store vector for document {}: {}", entity.getId(), e.getMessage());
        }

        return entity;
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
}