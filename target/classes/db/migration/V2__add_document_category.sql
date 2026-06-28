-- Add user-facing document categories such as study, personal, finance, and work.

ALTER TABLE documents
    ADD COLUMN document_category VARCHAR(50) NOT NULL DEFAULT 'general';

CREATE INDEX idx_documents_category ON documents (owner_id, document_category);
