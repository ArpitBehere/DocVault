package com.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class DocVaultExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(Long id) {
            super("Document not found with id: " + id);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidFileException extends RuntimeException {
        public InvalidFileException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public static class FileTooLargeException extends RuntimeException {
        public FileTooLargeException(long maxBytes) {
            super("File exceeds the maximum allowed size of " + (maxBytes / 1024 / 1024) + " MB");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String field, String value) {
            super(field + " '" + value + "' is already taken");
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException() {
            super("You don't have permission to access this resource");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidAccountPasswordException extends RuntimeException {
        public InvalidAccountPasswordException() {
            super("Incorrect password. Account was not deleted.");
        }
    }
}
