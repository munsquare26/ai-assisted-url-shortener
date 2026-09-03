package com.example.url_shortener.controller;

import com.example.url_shortener.dto.CreateShortUrlRequest;
import com.example.url_shortener.dto.CreateShortUrlResponse;
import com.example.url_shortener.dto.UrlStatsResponse;
import com.example.url_shortener.entity.ShortUrl;
import com.example.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request
    ) {
        CreateShortUrlResponse response =
                urlService.createShortUrl(
                request.getUrl(),
                request.getExpiresAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String shortCode) {
        ShortUrl shortUrl = urlService.getStats(shortCode);

        UrlStatsResponse response = new UrlStatsResponse(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getClickCount(),
                shortUrl.getCreatedAt(),
                shortUrl.getLastAccessedAt(),
                shortUrl.getExpiresAt()
        );

        return ResponseEntity.ok(response);
    }
    
}