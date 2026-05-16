package com.example.rag.dto;

public class SourceCitation {
    private int sourceNumber;
    private Long documentId;
    private String filename;
    private int chunkIndex;
    private String preview;

    public SourceCitation() {
    }

    public SourceCitation(int sourceNumber, Long documentId, String filename, int chunkIndex, String preview) {
        this.sourceNumber = sourceNumber;
        this.documentId = documentId;
        this.filename = filename;
        this.chunkIndex = chunkIndex;
        this.preview = preview;
    }

    public int getSourceNumber() {
        return sourceNumber;
    }

    public void setSourceNumber(int sourceNumber) {
        this.sourceNumber = sourceNumber;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}
