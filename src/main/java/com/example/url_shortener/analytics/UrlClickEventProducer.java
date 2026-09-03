package com.example.url_shortener.analytics;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UrlClickEventProducer {

    private static final String TOPIC = "url-clicks";

    private final KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    public UrlClickEventProducer(
            KafkaTemplate<String, UrlClickEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(UrlClickEvent event) {
        kafkaTemplate.send(
                TOPIC,
                event.shortCode(),
                event
        );
    }
}