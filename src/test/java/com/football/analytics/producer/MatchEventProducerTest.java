package com.football.analytics.producer;

import com.football.analytics.model.dto.MatchEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MatchEventProducerTest {

    private KafkaTemplate<String, MatchEvent> kafkaTemplate;
    private MatchEventProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new MatchEventProducer(kafkaTemplate);
    }

    @Test
    void publish_sendsEventToMatchesRawTopic_withApiIdAsKey() {
        MatchEvent event = new MatchEvent(
                123, "PL", "Arsenal FC", "Chelsea FC",
                1, 2, 2, 1, 5, "FINISHED", OffsetDateTime.now()
        );

        when(kafkaTemplate.send(anyString(), anyString(), any(MatchEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        producer.publish(event);

        // Verifies the topic name, the key (apiId as string), and the exact payload
        verify(kafkaTemplate, times(1)).send(eq("matches.raw"), eq("123"), eq(event));
    }
}