package com.hemo.backend.exception;

import com.hemo.backend.dto.ErrorResponseDTO;
import com.hemo.backend.dto.ValidationErrorDTO;
import com.hemo.backend.dto.ValidationErrorsResponseDTO;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException ex) {
        log.warn("app exception status={} message={}", ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationErrorDTO> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ValidationErrorDTO(err.getField(), err.getDefaultMessage()))
                .toList();
        log.warn("validation failed errors={}", errors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ValidationErrorsResponseDTO(errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason() == null || ex.getReason().isBlank() ? "Request failed" : ex.getReason();
        log.warn("response status exception status={} reason={}", ex.getStatusCode(), reason);
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponseDTO(reason));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex) {
        log.error("unexpected exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("Request failed"));
    }

    public static class AppException extends RuntimeException {
        private final HttpStatus status;

        public AppException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
