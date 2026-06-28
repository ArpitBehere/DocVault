package com.docvault.controller;

import com.docvault.dto.DocumentDto;
import com.docvault.model.Document;
import com.docvault.model.User;
import com.docvault.service.DocumentService;
import com.docvault.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    // ─── Upload ─────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto.Response> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "tags", required = false) String tags,
            @RequestPart(value = "documentCategory", required = false) String documentCategory,
            Principal principal,
            HttpServletRequest request) throws IOException {

        User owner = userService.getByUsername(principal.getName());
        DocumentDto.Response response = documentService.uploadDocument(
                file, description, tags, documentCategory, owner, getClientIp(request));
        return ResponseEntity.ok(response);
    }

    // ─── List / Search ───────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<DocumentDto.PageResponse> list(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        User owner = userService.getByUsername(principal.getName());
        return ResponseEntity.ok(
                documentService.listDocuments(owner, query, type, category, page, size));
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<DocumentDto.StatsResponse> stats(Principal principal) {
        User owner = userService.getByUsername(principal.getName());
        return ResponseEntity.ok(documentService.getStats(owner));
    }

    // ─── Download ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long id,
            Principal principal,
            HttpServletRequest request) {

        User owner = userService.getByUsername(principal.getName());
        Document doc = documentService.downloadDocument(id, owner, getClientIp(request));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(doc.getOriginalFilename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentLength(doc.getFileSize())
                .body(new ByteArrayResource(doc.getFileData()));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Principal principal,
            HttpServletRequest request) {

        User owner = userService.getByUsername(principal.getName());
        documentService.deleteDocument(id, owner, getClientIp(request));
        return ResponseEntity.noContent().build();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
