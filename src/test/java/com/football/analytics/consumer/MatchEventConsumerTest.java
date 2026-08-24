package com.football.analytics.consumer;

import com.football.analytics.model.dto.MatchEvent;
import com.football.analytics.model.entity.League;
import com.football.analytics.model.entity.Match;
import com.football.analytics.model.entity.Team;
import com.football.analytics.repository.LeagueRepository;
import com.football.analytics.repository.MatchRepository;
import com.football.analytics.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchEventConsumerTest {

    private MatchRepository matchRepository;
    private TeamRepository teamRepository;
    private LeagueRepository leagueRepository;
    private MatchEventConsumer consumer;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        teamRepository = mock(TeamRepository.class);
        leagueRepository = mock(LeagueRepository.class);
        consumer = new MatchEventConsumer(matchRepository, teamRepository, leagueRepository);
    }

    @Test
    void consume_savesNewMatch_whenMatchDoesNotExistYet() {
        League league = new League();
        league.setId(1L);
        league.setCode("PL");

        Team home = new Team();
        home.setId(10L);
        home.setApiId(100);

        Team away = new Team();
        away.setId(20L);
        away.setApiId(200);

        MatchEvent event = new MatchEvent(
                999, "PL", "Arsenal FC", "Chelsea FC",
                100, 200, 3, 1, 5, "FINISHED", OffsetDateTime.now()
        );

        when(leagueRepository.findByCode("PL")).thenReturn(Optional.of(league));
        when(teamRepository.findByApiId(100)).thenReturn(Optional.of(home));
        when(teamRepository.findByApiId(200)).thenReturn(Optional.of(away));
        when(matchRepository.findByApiId(999)).thenReturn(Optional.empty());

        consumer.consume(event);

        // Verifies a brand-new Match was built and saved with the correct score
        verify(matchRepository, times(1)).save(argThat(match ->
                match.getApiId().equals(999)
                        && match.getHomeScore().equals(3)
                        && match.getAwayScore().equals(1)
                        && match.getStatus().equals("FINISHED")
        ));
    }

    @Test
    void consume_updatesExistingMatch_insteadOfCreatingDuplicate() {
        League league = new League();
        league.setId(1L);

        Team home = new Team();
        home.setId(10L);
        home.setApiId(100);

        Team away = new Team();
        away.setId(20L);
        away.setApiId(200);

        Match existingMatch = new Match();
        existingMatch.setId(555L);
        existingMatch.setApiId(999);

        MatchEvent event = new MatchEvent(
                999, "PL", "Arsenal FC", "Chelsea FC",
                100, 200, 2, 2, 5, "FINISHED", OffsetDateTime.now()
        );

        when(leagueRepository.findByCode("PL")).thenReturn(Optional.of(league));
        when(teamRepository.findByApiId(100)).thenReturn(Optional.of(home));
        when(teamRepository.findByApiId(200)).thenReturn(Optional.of(away));
        when(matchRepository.findByApiId(999)).thenReturn(Optional.of(existingMatch));

        consumer.consume(event);

        // Verifies the SAME entity (id=555) was updated, not a new one created — this is the upsert guarantee
        verify(matchRepository, times(1)).save(argThat(match ->
                match.getId().equals(555L) && match.getHomeScore().equals(2)
        ));
    }
}
