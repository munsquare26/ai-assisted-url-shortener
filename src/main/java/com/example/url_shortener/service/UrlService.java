package com.example.url_shortener.service;

import org.springframework.beans.factory.annotation.Value;
import com.example.url_shortener.exception.ShortUrlNotFoundException;
import com.example.url_shortener.dto.CreateShortUrlResponse;
import com.example.url_shortener.entity.ShortUrl;
import com.example.url_shortener.repository.ShortUrlRepository;
import com.example.url_shortener.util.ShortCodeGenerator;
import org.springframework.stereotype.Service;
import com.example.url_shortener.exception.InvalidUrlException;
import com.example.url_shortener.exception.ShortUrlExpiredException;
import com.example.url_shortener.analytics.UrlClickEvent;
import com.example.url_shortener.analytics.UrlClickEventProducer;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.OffsetDateTime;

@Service
public class UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final String baseUrl;
    private final UrlClickEventProducer urlClickEventProducer;
    
    public UrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            @Value("${app.base-url}") String baseUrl,
            UrlClickEventProducer urlClickEventProducer
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.baseUrl = baseUrl;
        this.urlClickEventProducer = urlClickEventProducer;
    }

    public CreateShortUrlResponse createShortUrl(String originalUrl, OffsetDateTime expiresAt) {
        validateUrl(originalUrl);
        String shortCode = generateUniqueShortCode();
        
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode(shortCode);
        shortUrl.setOriginalUrl(originalUrl);
        shortUrl.setCreatedAt(OffsetDateTime.now());
        shortUrl.setExpiresAt(expiresAt);
        shortUrl.setClickCount(0L);

        ShortUrl saved = shortUrlRepository.save(shortUrl);
        return new CreateShortUrlResponse(
                saved.getShortCode(),
                baseUrl + "/" + saved.getShortCode(),
                saved.getOriginalUrl()
        );
    }
    public String getOriginalUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        if (shortUrl.getExpiresAt() != null
            && shortUrl.getExpiresAt().isBefore(OffsetDateTime.now())) {
        throw new ShortUrlExpiredException(shortCode);
    }
    urlClickEventProducer.publish(
            new UrlClickEvent(shortCode, OffsetDateTime.now())
    );
        return shortUrl.getOriginalUrl();
    }

    public ShortUrl getStats(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));
    }
    
    private String generateUniqueShortCode() {
        String shortCode;

        do {
            shortCode = shortCodeGenerator.generate();
        } while (shortUrlRepository.existsByShortCode(shortCode));

        return shortCode;
    }
    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);

            String scheme = uri.getScheme();

            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http")
                            && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {

                throw new InvalidUrlException(
                        "URL must be a valid HTTP or HTTPS URL"
                );
            }

        } catch (URISyntaxException e) {
            throw new InvalidUrlException(
                    "URL must be a valid HTTP or HTTPS URL"
            );
        }
    }
}
