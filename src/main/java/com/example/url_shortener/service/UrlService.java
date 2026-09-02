package com.example.url_shortener.service;

import com.example.url_shortener.exception.ShortUrlNotFoundException;
import com.example.url_shortener.dto.CreateShortUrlResponse;
import com.example.url_shortener.entity.ShortUrl;
import com.example.url_shortener.repository.ShortUrlRepository;
import com.example.url_shortener.util.ShortCodeGenerator;
import org.springframework.stereotype.Service;
import com.example.url_shortener.exception.InvalidUrlException;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.OffsetDateTime;

@Service
public class UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    public UrlService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    public CreateShortUrlResponse createShortUrl(String originalUrl) {
        validateUrl(originalUrl);
        String shortCode = generateUniqueShortCode();

        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode(shortCode);
        shortUrl.setOriginalUrl(originalUrl);
        shortUrl.setCreatedAt(OffsetDateTime.now());
        shortUrl.setClickCount(0L);

        ShortUrl saved = shortUrlRepository.save(shortUrl);
        return new CreateShortUrlResponse(
                saved.getShortCode(),
                "http://localhost:8080/" + saved.getShortCode(),
                saved.getOriginalUrl()
        );
    }
    public String getOriginalUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        return shortUrl.getOriginalUrl();
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
