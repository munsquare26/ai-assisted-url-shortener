package com.example.url_shortener.exception;

public class ShortUrlExpiredException extends RuntimeException {

    public ShortUrlExpiredException(String shortCode) {
        super("Short URL has expired: " + shortCode);
    }
}