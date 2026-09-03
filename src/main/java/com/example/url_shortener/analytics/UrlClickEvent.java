package com.example.url_shortener.analytics;

import java.time.OffsetDateTime;

public record UrlClickEvent(
        String shortCode,
        OffsetDateTime clickedAt
) {
}