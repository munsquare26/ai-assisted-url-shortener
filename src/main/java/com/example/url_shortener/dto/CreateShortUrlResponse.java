package com.example.url_shortener.dto;

public class CreateShortUrlResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;

    public CreateShortUrlResponse(String shortCode, String shortUrl, String originalUrl) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }
}