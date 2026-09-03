package com.example.url_shortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleShortUrlNotFound(
            ShortUrlNotFoundException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "SHORT_URL_NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }
    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<Map<String, String>> handleInvalidUrl(
            InvalidUrlException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "INVALID_URL",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ShortUrlExpiredException.class)
    public ResponseEntity<Map<String, String>> handleShortUrlExpired(
            ShortUrlExpiredException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(Map.of(
                        "error", "SHORT_URL_EXPIRED",
                        "message", ex.getMessage()
                ));
    }
}