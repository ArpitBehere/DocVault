-- V1__init_schema.sql
-- DocVault initial schema

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS documents (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename  VARCHAR(255)  NOT NULL,
    content_type     VARCHAR(100)  NOT NULL,
    file_size        BIGINT        NOT NULL,
    file_data        LONGBLOB      NOT NULL,
    description      VARCHAR(500),
    tags             VARCHAR(500),
    owner_id         BIGINT        NOT NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_documents_owner    (owner_id),
    INDEX idx_documents_filename (original_filename),
    INDEX idx_documents_created  (created_at),
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit log table
CREATE TABLE IF NOT EXISTS document_audit (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    document_id BIGINT,
    user_id     BIGINT       NOT NULL,
    action      VARCHAR(50)  NOT NULL,   -- UPLOAD, DOWNLOAD, DELETE, VIEW
    filename    VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(45),
    occurred_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_document (document_id),
    INDEX idx_audit_user     (user_id),
    INDEX idx_audit_occurred (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
