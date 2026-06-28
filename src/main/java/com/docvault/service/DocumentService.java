package com.docvault.service;

import com.docvault.dto.DocumentDto;
import com.docvault.exception.DocVaultExceptions;
import com.docvault.model.Document;
import com.docvault.model.DocumentAudit;
import com.docvault.model.User;
import com.docvault.repository.DocumentAuditRepository;
import com.docvault.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAuditRepository auditRepository;
    private final Tika tika = new Tika();

    @Value("${app.document.max-file-size-bytes:52428800}")
    private long maxFileSizeBytes;

    @Value("${app.document.allowed-content-types}")
    private String allowedContentTypes;

    // ─── Upload ─────────────────────────────────────────────────────────────

    @Transactional
    public DocumentDto.Response uploadDocument(MultipartFile file,
                                               String description,
                                               String tags,
                                               String documentCategory,
                                               User owner,
                                               String ipAddress) throws IOException {
        validateFile(file);

        String detectedType = tika.detect(file.getInputStream(), file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Document doc = Document.builder()
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .contentType(detectedType)
                .fileSize(file.getSize())
                .fileData(file.getBytes())
                .description(description)
                .tags(tags)
                .documentCategory(normalizeCategory(documentCategory))
                .owner(owner)
                .build();

        Document saved = documentRepository.save(doc);
        audit(saved.getId(), owner.getId(), DocumentAudit.Action.UPLOAD, saved.getOriginalFilename(), ipAddress);

        log.info("User {} uploaded document '{}' ({} bytes)", owner.getUsername(), saved.getOriginalFilename(), saved.getFileSize());
        return toResponse(saved);
    }

    // ─── Download ────────────────────────────────────────────────────────────

    @Transactional
    public Document downloadDocument(Long id, User owner, String ipAddress) {
        Document doc = documentRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new DocVaultExceptions.DocumentNotFoundException(id));
        audit(doc.getId(), owner.getId(), DocumentAudit.Action.DOWNLOAD, doc.getOriginalFilename(), ipAddress);
        return doc;
    }

    // ─── List / Search ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DocumentDto.PageResponse listDocuments(User owner, String query, String typeFilter,
                                                  String documentCategory,
                                                  int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Document> result;
        String normalizedCategory = normalizeCategory(documentCategory);
        boolean filterByCategory = !normalizedCategory.equals("all");
        boolean filterByType = typeFilter != null && !typeFilter.isBlank() && !typeFilter.equals("all");

        if (query != null && !query.isBlank()) {
            result = documentRepository.searchByOwnerWithFilters(
                    owner.getId(),
                    query.trim(),
                    normalizedCategory,
                    filterByType ? typeFilter : "all",
                    pageable);
        } else if (filterByCategory && filterByType) {
            result = documentRepository.findByOwnerAndCategoryAndType(owner.getId(), normalizedCategory, typeFilter, pageable);
        } else if (filterByCategory) {
            result = documentRepository.findByOwnerAndCategory(owner.getId(), normalizedCategory, pageable);
        } else if (filterByType) {
            result = documentRepository.findByOwnerAndType(owner.getId(), typeFilter, pageable);
        } else {
            result = documentRepository.findByOwnerId(owner.getId(), pageable);
        }

        return DocumentDto.PageResponse.builder()
                .documents(result.getContent().stream().map(this::toResponse).toList())
                .currentPage(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .build();
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DocumentDto.StatsResponse getStats(User owner) {
        long total = documentRepository.countByOwnerId(owner.getId());
        long totalSize = documentRepository.sumFileSizeByOwner(owner.getId());
        List<String> types = documentRepository.findDistinctContentTypesByOwner(owner.getId());

        return DocumentDto.StatsResponse.builder()
                .totalDocuments(total)
                .totalSizeBytes(totalSize)
                .totalSizeFormatted(formatSize(totalSize))
                .distinctFileTypes(types.size())
                .contentTypes(types)
                .build();
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    public void deleteDocument(Long id, User owner, String ipAddress) {
        Document doc = documentRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new DocVaultExceptions.DocumentNotFoundException(id));
        audit(null, owner.getId(), DocumentAudit.Action.DELETE, doc.getOriginalFilename(), ipAddress);
        documentRepository.delete(doc);
        log.info("User {} deleted document '{}'", owner.getUsername(), doc.getOriginalFilename());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new DocVaultExceptions.InvalidFileException("No file provided");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new DocVaultExceptions.FileTooLargeException(maxFileSizeBytes);
        }
        String detectedType = tika.detect(file.getInputStream(), file.getOriginalFilename());
        List<String> allowed = Arrays.asList(allowedContentTypes.split(","));
        if (!allowed.contains(detectedType)) {
            throw new DocVaultExceptions.InvalidFileException(
                    "File type not allowed: " + detectedType);
        }
    }

    private void audit(Long docId, Long userId, DocumentAudit.Action action,
                       String filename, String ip) {
        auditRepository.save(DocumentAudit.builder()
                .documentId(docId)
                .userId(userId)
                .action(action)
                .filename(filename)
                .ipAddress(ip)
                .build());
    }

    private DocumentDto.Response toResponse(Document d) {
        return DocumentDto.Response.builder()
                .id(d.getId())
                .originalFilename(d.getOriginalFilename())
                .contentType(d.getContentType())
                .fileSize(d.getFileSize())
                .fileSizeFormatted(formatSize(d.getFileSize()))
                .description(d.getDescription())
                .tags(d.getTags())
                .category(d.getCategory())
                .documentCategory(d.getDocumentCategory())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return "general";
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_-]{1,50}") ? normalized : "general";
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1_048_576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1_073_741_824) return String.format("%.1f MB", bytes / 1_048_576.0);
        return String.format("%.2f GB", bytes / 1_073_741_824.0);
    }
}
