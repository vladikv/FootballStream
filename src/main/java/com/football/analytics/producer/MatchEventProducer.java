package com.football.analytics.producer;

import com.football.analytics.model.dto.MatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MatchEventProducer {

    private static final String TOPIC = "matches.raw";

    private final KafkaTemplate<String, MatchEvent> kafkaTemplate;

    public MatchEventProducer(KafkaTemplate<String, MatchEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MatchEvent event) {
        kafkaTemplate.send(TOPIC, event.getApiId().toString(), event);
        log.info("Published match event: apiId={}, {} vs {}",
                event.getApiId(), event.getHomeTeamName(), event.getAwayTeamName());
    }
}