package com.docvault;

import com.docvault.dto.DocumentDto;
import com.docvault.model.Document;
import com.docvault.model.User;
import com.docvault.repository.DocumentAuditRepository;
import com.docvault.repository.DocumentRepository;
import com.docvault.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock DocumentAuditRepository auditRepository;
    @InjectMocks DocumentService documentService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "maxFileSizeBytes", 52428800L);
        ReflectionTestUtils.setField(documentService, "allowedContentTypes",
            "application/pdf,image/jpeg,image/png,text/plain,text/csv," +
            "application/msword," +
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document," +
            "application/vnd.ms-excel," +
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet," +
            "image/gif,image/webp");

        testUser = User.builder().id(1L).username("alice").email("alice@example.com").build();
    }

    @Test
    void uploadDocument_validFile_returnsResponse() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "Hello DocVault".getBytes());

        Document saved = Document.builder()
            .id(1L).originalFilename("test.txt").storedFilename("uuid_test.txt")
            .contentType("text/plain").fileSize(14L).fileData("Hello DocVault".getBytes())
            .documentCategory("study").owner(testUser).build();

        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        DocumentDto.Response result = documentService.uploadDocument(file, "My note", "test", "study", testUser, "127.0.0.1");

        assertThat(result.getOriginalFilename()).isEqualTo("test.txt");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        assertThat(result.getDocumentCategory()).isEqualTo("study");
        verify(documentRepository).save(any(Document.class));
        verify(auditRepository).save(any());
    }

    @Test
    void uploadDocument_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThatThrownBy(() -> documentService.uploadDocument(emptyFile, null, null, "general", testUser, "127.0.0.1"))
            .hasMessageContaining("No file provided");
    }

    @Test
    void deleteDocument_ownerMismatch_throwsNotFoundException() {
        when(documentRepository.findByIdAndOwnerId(99L, testUser.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentService.deleteDocument(99L, testUser, "127.0.0.1"))
            .hasMessageContaining("99");
    }

    @Test
    void formatSize_correctlyFormats() {
        assertThat(DocumentService.formatSize(512)).isEqualTo("512 B");
        assertThat(DocumentService.formatSize(2048)).isEqualTo("2.0 KB");
        assertThat(DocumentService.formatSize(1_572_864)).isEqualTo("1.5 MB");
    }
}
