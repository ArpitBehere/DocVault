package com.docvault.repository;

import com.docvault.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // All docs for a user (excludes blob data for performance in listings)
    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId ORDER BY d.createdAt DESC")
    Page<Document> findByOwnerId(@Param("userId") Long userId, Pageable pageable);

    // Full-text search by filename, description, or tags
    @Query("""
            SELECT d FROM Document d
            WHERE d.owner.id = :userId
              AND (LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(d.description)       LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(d.tags)              LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(d.documentCategory)  LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY d.createdAt DESC
            """)
    Page<Document> searchByOwner(@Param("userId") Long userId,
                                 @Param("query") String query,
                                 Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE d.owner.id = :userId
              AND (:category = 'all' OR d.documentCategory = :category)
              AND (:typeFilter = 'all' OR d.contentType LIKE CONCAT('%', :typeFilter, '%'))
              AND (LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(d.description)       LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(d.tags)              LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(d.documentCategory)  LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY d.createdAt DESC
            """)
    Page<Document> searchByOwnerWithFilters(@Param("userId") Long userId,
                                            @Param("query") String query,
                                            @Param("category") String category,
                                            @Param("typeFilter") String typeFilter,
                                            Pageable pageable);

    // Filter by content type category
    @Query("""
            SELECT d FROM Document d
            WHERE d.owner.id = :userId
              AND d.contentType LIKE CONCAT('%', :typeFilter, '%')
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByOwnerAndType(@Param("userId") Long userId,
                                      @Param("typeFilter") String typeFilter,
                                      Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE d.owner.id = :userId
              AND d.documentCategory = :category
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByOwnerAndCategory(@Param("userId") Long userId,
                                          @Param("category") String category,
                                          Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE d.owner.id = :userId
              AND d.documentCategory = :category
              AND d.contentType LIKE CONCAT('%', :typeFilter, '%')
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByOwnerAndCategoryAndType(@Param("userId") Long userId,
                                                 @Param("category") String category,
                                                 @Param("typeFilter") String typeFilter,
                                                 Pageable pageable);

    // Fetch with file data (for download)
    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.owner.id = :userId")
    Optional<Document> findByIdAndOwnerId(@Param("id") Long id, @Param("userId") Long userId);

    // Storage stats per user
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.owner.id = :userId")
    Long sumFileSizeByOwner(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d")
    Long sumFileSize();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.owner.id = :userId")
    long countByOwnerId(@Param("userId") Long userId);

    // Distinct content types for a user
    @Query("SELECT DISTINCT d.contentType FROM Document d WHERE d.owner.id = :userId")
    List<String> findDistinctContentTypesByOwner(@Param("userId") Long userId);
}
