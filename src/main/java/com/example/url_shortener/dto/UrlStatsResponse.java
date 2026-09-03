package com.example.url_shortener.dto;

import java.time.OffsetDateTime;

public class UrlStatsResponse {

    private final String shortCode;
    private final String originalUrl;
    private final long clickCount;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime lastAccessedAt;
    private final OffsetDateTime expiresAt;

    public UrlStatsResponse(
            String shortCode,
            String originalUrl,
            long clickCount,
            OffsetDateTime createdAt,
            OffsetDateTime lastAccessedAt,
            OffsetDateTime expiresAt
    ) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.expiresAt = expiresAt;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public long getClickCount() {
        return clickCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}