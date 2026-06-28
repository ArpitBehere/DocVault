package com.docvault.repository;

import com.docvault.model.DocumentAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentAuditRepository extends JpaRepository<DocumentAudit, Long> {
    Page<DocumentAudit> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);
    Page<DocumentAudit> findByDocumentIdOrderByOccurredAtDesc(Long documentId, Pageable pageable);
}
