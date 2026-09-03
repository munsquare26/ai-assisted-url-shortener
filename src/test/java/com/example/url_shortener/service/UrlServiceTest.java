package com.example.url_shortener.service;

import com.example.url_shortener.dto.CreateShortUrlResponse;
import com.example.url_shortener.entity.ShortUrl;
import com.example.url_shortener.repository.ShortUrlRepository;
import com.example.url_shortener.util.ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(
                shortUrlRepository,
                shortCodeGenerator,
                "http://localhost:8080"
        );
    }

    @Test
    void shouldCreateShortUrl() {
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(shortUrlRepository.existsByShortCode("abc1234")).thenReturn(false);

        when(shortUrlRepository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateShortUrlResponse response =
                urlService.createShortUrl("https://www.google.com", null);

        assertEquals("abc1234", response.getShortCode());
        assertEquals("http://localhost:8080/abc1234", response.getShortUrl());
        assertEquals("https://www.google.com", response.getOriginalUrl());

        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    void shouldResolveOriginalUrl() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode("abc1234");
        shortUrl.setOriginalUrl("https://www.google.com");

        when(shortUrlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        String result = urlService.getOriginalUrl("abc1234");

        assertEquals("https://www.google.com", result);
    }
        @Test
    void shouldRejectInvalidUrl() {
        assertThrows(
                com.example.url_shortener.exception.InvalidUrlException.class,
                () -> urlService.createShortUrl("abc", null)
        );

        verifyNoInteractions(shortUrlRepository);
    }

    @Test
    void shouldThrowWhenShortCodeDoesNotExist() {
        when(shortUrlRepository.findByShortCode("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                com.example.url_shortener.exception.ShortUrlNotFoundException.class,
                () -> urlService.getOriginalUrl("missing")
        );
    }

    @Test
    void shouldCreateShortUrlWithExpiration() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        when(shortCodeGenerator.generate()).thenReturn("exp1234");
        when(shortUrlRepository.existsByShortCode("exp1234")).thenReturn(false);

        when(shortUrlRepository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        urlService.createShortUrl(
                "https://www.google.com",
                expiresAt
        );

        verify(shortUrlRepository).save(argThat(shortUrl ->
                expiresAt.equals(shortUrl.getExpiresAt())
        ));
    }
    @Test
    void shouldRejectExpiredShortUrl() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode("expired1");
        shortUrl.setOriginalUrl("https://www.google.com");
        shortUrl.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(shortUrlRepository.findByShortCode("expired1"))
                .thenReturn(Optional.of(shortUrl));

        assertThrows(
                com.example.url_shortener.exception.ShortUrlExpiredException.class,
                () -> urlService.getOriginalUrl("expired1")
        );
    }
}