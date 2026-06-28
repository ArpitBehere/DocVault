package com.docvault.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // Stored directly in MySQL as LONGBLOB
    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(length = 500)
    private String description;

    // Comma-separated tags e.g. "invoice,2024,tax"
    @Column(length = 500)
    private String tags;

    @Column(name = "document_category", nullable = false, length = 50)
    @Builder.Default
    private String documentCategory = "general";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Convenience: derive category from content type
    public String getCategory() {
        if (contentType == null) return "other";
        if (contentType.equals("application/pdf")) return "pdf";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.contains("word") || contentType.contains("text")) return "document";
        if (contentType.contains("excel") || contentType.contains("spreadsheet") || contentType.equals("text/csv")) return "spreadsheet";
        return "other";
    }
}
