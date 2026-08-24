package com.football.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.analytics.client.FootballDataApiClient;
import com.football.analytics.model.entity.League;
import com.football.analytics.model.entity.Team;
import com.football.analytics.producer.MatchEventProducer;
import com.football.analytics.repository.LeagueRepository;
import com.football.analytics.repository.StandingRepository;
import com.football.analytics.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EtlServiceTest {

    private TeamRepository teamRepository;
    private EtlService etlService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        etlService = new EtlService(
                mock(FootballDataApiClient.class),
                mock(MatchEventProducer.class),
                mock(LeagueRepository.class),
                teamRepository,
                mock(StandingRepository.class)
        );
    }

    @Test
    void resolveOrCreateTeam_createsNewTeam_whenTeamNotFoundByApiId() throws Exception {
        League league = new League();
        league.setId(1L);

        JsonNode teamNode = objectMapper.readTree(
                "{\"id\": 57, \"name\": \"Arsenal FC\", \"crest\": \"https://crests.example.com/57.png\"}"
        );

        when(teamRepository.findByApiId(57)).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Team result = invokeResolveOrCreateTeam(teamNode, league);

        assertEquals(57, result.getApiId());
        assertEquals("Arsenal FC", result.getName());
        assertEquals(league, result.getLeague());
        verify(teamRepository, times(1)).save(any(Team.class));
    }

    @Test
    void resolveOrCreateTeam_returnsExistingTeam_withoutCreatingDuplicate() throws Exception {
        League league = new League();
        league.setId(1L);

        Team existing = new Team();
        existing.setId(10L);
        existing.setApiId(57);
        existing.setName("Arsenal FC");

        JsonNode teamNode = objectMapper.readTree(
                "{\"id\": 57, \"name\": \"Arsenal FC\", \"crest\": \"https://crests.example.com/57.png\"}"
        );

        when(teamRepository.findByApiId(57)).thenReturn(Optional.of(existing));

        Team result = invokeResolveOrCreateTeam(teamNode, league);

        assertEquals(existing.getId(), result.getId());
        verify(teamRepository, never()).save(any(Team.class));
    }

    // resolveOrCreateTeam is private — reflection lets us test it in isolation
    // without going through the full syncMatches() flow (which needs a live API response).
    private Team invokeResolveOrCreateTeam(JsonNode teamNode, League league) throws Exception {
        Method method = EtlService.class.getDeclaredMethod("resolveOrCreateTeam", JsonNode.class, League.class);
        method.setAccessible(true);
        return (Team) method.invoke(etlService, teamNode, league);
    }
}