package com.istrazidomace.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Greška validacije", errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (msg != null) {
            if (msg.contains("nije pronađen") || msg.contains("nije pronađena"))
                status = HttpStatus.NOT_FOUND;
            else if (msg.contains("zauzet") || msg.contains("već"))
                status = HttpStatus.CONFLICT;
            else if (msg.contains("dozvolu") || msg.contains("Nemate"))
                status = HttpStatus.FORBIDDEN;
            else if (msg.contains("Maksimalan"))
                status = HttpStatus.BAD_REQUEST;
        }

        return error(status, msg != null ? msg : "Došlo je do greške", null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String message, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("greška", message);
        if (details != null) body.put("detalji", details);
        return ResponseEntity.status(status).body(body);
    }
}