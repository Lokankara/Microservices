package com.audio.song.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SongNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSongNotFound(SongNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", ex.getMessage());
        error.put("errorCode", "404");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateSongException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateSong(DuplicateSongException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", ex.getMessage());
        error.put("errorCode", "409");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorMessage", "Validation error");

        Map<String, String> details = new HashMap<>();
        String message = ex.getMessage();
        if (message != null && message.contains(",")) {
            String[] parts = message.split(", ");
            for (String part : parts) {
                String[] keyVal = part.split(": ", 2);
                if (keyVal.length == 2) {
                    details.put(keyVal[0], keyVal[1]);
                }
            }
        } else {
            details.put("error", message);
        }
        error.put("details", details);
        error.put("errorCode", "400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("errorMessage", "Validation error");
        body.put("details", details);
        body.put("errorCode", "400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error(ex.getMessage(), ex);
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", "Invalid ID format. ID must be a positive integer.");
        error.put("errorCode", "400");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AuthorizationDeniedException ex) {
        log.error(ex.getMessage(), ex);
        Map<String, String> error = new HashMap<>();
        error.put("errorMessage", "Access denied");
        error.put("errorCode", "403");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
