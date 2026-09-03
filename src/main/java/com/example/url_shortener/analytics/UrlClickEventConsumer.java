package com.example.url_shortener.analytics;

import com.example.url_shortener.entity.ShortUrl;
import com.example.url_shortener.repository.ShortUrlRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UrlClickEventConsumer {

    private final ShortUrlRepository shortUrlRepository;

    public UrlClickEventConsumer(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @KafkaListener(topics = "url-clicks", groupId = "url-shortener-analytics")
    @Transactional
    public void consume(UrlClickEvent event) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(event.shortCode())
                .orElseThrow();

        shortUrl.setClickCount(shortUrl.getClickCount() + 1);
        shortUrl.setLastAccessedAt(event.clickedAt());

        shortUrlRepository.save(shortUrl);
    }
}