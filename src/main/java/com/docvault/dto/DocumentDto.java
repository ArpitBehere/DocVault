package com.docvault.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentDto {

    /** Lightweight listing response — no file bytes */
    @Data @Builder
    public static class Response {
        private Long id;
        private String originalFilename;
        private String contentType;
        private Long fileSize;
        private String fileSizeFormatted;
        private String description;
        private String tags;
        private String category;
        private String documentCategory;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /** Upload request metadata */
    @Data
    public static class UploadRequest {
        private String description;
        private String tags;
        private String documentCategory;
    }

    /** Stats summary for dashboard */
    @Data @Builder
    public static class StatsResponse {
        private long totalDocuments;
        private long totalSizeBytes;
        private String totalSizeFormatted;
        private int distinctFileTypes;
        private List<String> contentTypes;
    }

    /** Paginated list wrapper */
    @Data @Builder
    public static class PageResponse {
        private List<Response> documents;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
