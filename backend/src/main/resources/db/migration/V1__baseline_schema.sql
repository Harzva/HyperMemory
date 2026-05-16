CREATE TABLE IF NOT EXISTS documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    chunk_count INT NOT NULL,
    tenant_id VARCHAR(128) NOT NULL DEFAULT 'default',
    PRIMARY KEY (id),
    CONSTRAINT uk_documents_object_key UNIQUE (object_key),
    KEY idx_documents_tenant_id (tenant_id)
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    char_start INT NOT NULL,
    char_end INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_document_chunks_document_id (document_id),
    KEY idx_document_chunks_created_at (created_at),
    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE TABLE IF NOT EXISTS wiki_pages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(128) NOT NULL,
    document_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wiki_pages_tenant_document UNIQUE (tenant_id, document_id),
    KEY idx_wiki_pages_tenant (tenant_id),
    KEY idx_wiki_pages_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS gbrain_skill_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(128) NOT NULL,
    skill_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    details TEXT,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_gbrain_runs_tenant (tenant_id),
    KEY idx_gbrain_runs_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS hyper_memory_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_hyper_memory_tenant (tenant_id),
    KEY idx_hyper_memory_created_at (created_at)
);
