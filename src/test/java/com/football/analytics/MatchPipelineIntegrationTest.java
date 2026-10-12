package com.football.analytics;

import com.football.analytics.model.dto.MatchEvent;
import com.football.analytics.model.entity.League;
import com.football.analytics.model.entity.Match;
import com.football.analytics.model.entity.Team;
import com.football.analytics.producer.MatchEventProducer;
import com.football.analytics.repository.LeagueRepository;
import com.football.analytics.repository.MatchRepository;
import com.football.analytics.repository.TeamRepository;
import com.football.analytics.scheduler.MatchFetchScheduler;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// Full pipeline against REAL Postgres and Kafka: producer -> Kafka -> consumer -> Postgres.
// disabledWithoutDocker: the test is skipped (not failed) on machines without a Docker daemon.
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MatchPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    // Point Spring at the containers (random ports are only known at runtime)
    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    // Create the topic up front so the consumer can get its partition assignment deterministically
    @TestConfiguration
    static class TopicConfig {
        @Bean
        NewTopic matchesRawTopic() {
            return TopicBuilder.name("matches.raw").partitions(1).replicas(1).build();
        }
    }

    // Keep the real scheduler from calling football-data.org during the test
    @MockBean
    MatchFetchScheduler matchFetchScheduler;

    @Autowired MatchEventProducer producer;
    @Autowired MatchRepository matchRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired LeagueRepository leagueRepository;
    @Autowired KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        leagueRepository.deleteAll();

        League league = new League();
        league.setApiId(2021);
        league.setName("Premier League");
        league.setCode("PL");
        league = leagueRepository.save(league);

        saveTeam(57, "Arsenal FC", league);
        saveTeam(61, "Chelsea FC", league);

        // consumer uses auto.offset.reset=latest, so publish only after it owns the partition
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @Test
    void publishedMatchEvent_isPersistedByConsumer() {
        producer.publish(event(999, 3, 1, "FINISHED"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Optional<Match> saved = matchRepository.findByApiId(999);
            assertThat(saved).isPresent();
            assertThat(saved.get().getHomeScore()).isEqualTo(3);
            assertThat(saved.get().getAwayScore()).isEqualTo(1);
            assertThat(saved.get().getStatus()).isEqualTo("FINISHED");
        });
    }

    @Test
    void secondEventForSameMatch_updatesRowInsteadOfCreatingDuplicate() {
        producer.publish(event(999, 0, 0, "IN_PLAY"));
        await().atMost(Duration.ofSeconds(30))
                .until(() -> matchRepository.findByApiId(999).isPresent());

        producer.publish(event(999, 2, 1, "FINISHED"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(matchRepository.findByApiId(999).orElseThrow().getHomeScore()).isEqualTo(2);
            assertThat(matchRepository.count()).isEqualTo(1);
        });
    }

    private MatchEvent event(int apiId, int homeScore, int awayScore, String status) {
        return new MatchEvent(apiId, "PL", "Arsenal FC", "Chelsea FC",
                57, 61, homeScore, awayScore, 5, status, OffsetDateTime.now());
    }

    private void saveTeam(int apiId, String name, League league) {
        Team team = new Team();
        team.setApiId(apiId);
        team.setName(name);
        team.setLeague(league);
        teamRepository.save(team);
    }
}