package com.example.url_shortener.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateShortUrlRequest {

    @NotBlank
    @Size(max = 2048)
    private String url;
    private OffsetDateTime expiresAt;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}