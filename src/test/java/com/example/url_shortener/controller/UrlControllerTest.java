package com.example.url_shortener.controller;

import com.example.url_shortener.dto.CreateShortUrlResponse;
import com.example.url_shortener.entity.ShortUrl;
import com.example.url_shortener.exception.InvalidUrlException;
import com.example.url_shortener.exception.ShortUrlExpiredException;
import com.example.url_shortener.exception.ShortUrlNotFoundException;
import com.example.url_shortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

@WebMvcTest({UrlController.class, RedirectController.class})
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Test
    void shouldCreateShortUrl() throws Exception {
        CreateShortUrlResponse response = new CreateShortUrlResponse(
                "abc1234",
                "http://localhost:8080/abc1234",
                "https://example.com"
        );

        when(urlService.createShortUrl(eq("https://example.com"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));
    }

    @Test
    void shouldRedirectToOriginalUrl() throws Exception {
        when(urlService.getOriginalUrl("abc1234"))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void shouldReturnNotFoundForUnknownShortCode() throws Exception {
        when(urlService.getOriginalUrl("missing"))
                .thenThrow(new ShortUrlNotFoundException("Short URL not found"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SHORT_URL_NOT_FOUND"));
    }

    @Test
    void shouldReturnGoneForExpiredShortUrl() throws Exception {
        when(urlService.getOriginalUrl("expired"))
                .thenThrow(new ShortUrlExpiredException("Short URL has expired"));

        mockMvc.perform(get("/expired"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("SHORT_URL_EXPIRED"));
    }

    @Test
    void shouldReturnBadRequestForInvalidUrl() throws Exception {
        when(urlService.createShortUrl(eq("abc"), any()))
                .thenThrow(new InvalidUrlException("Only HTTP and HTTPS URLs are supported"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_URL"));
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
    @Test
    void shouldReturnUrlStats() throws Exception {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setShortCode("abc1234");
        shortUrl.setOriginalUrl("https://example.com");
        shortUrl.setClickCount(3L);
        shortUrl.setCreatedAt(OffsetDateTime.parse("2026-09-03T05:00:00Z"));
        shortUrl.setLastAccessedAt(OffsetDateTime.parse("2026-09-03T05:10:00Z"));

        when(urlService.getStats("abc1234"))
                .thenReturn(shortUrl);

        mockMvc.perform(get("/api/v1/urls/abc1234/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.clickCount").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-09-03T05:00:00Z"))
                .andExpect(jsonPath("$.lastAccessedAt").value("2026-09-03T05:10:00Z"));
    }
}