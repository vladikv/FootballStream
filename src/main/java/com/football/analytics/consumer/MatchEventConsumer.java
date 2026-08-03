package com.football.analytics.consumer;

import com.football.analytics.model.dto.MatchEvent;
import com.football.analytics.model.entity.League;
import com.football.analytics.model.entity.Match;
import com.football.analytics.model.entity.Team;
import com.football.analytics.repository.LeagueRepository;
import com.football.analytics.repository.MatchRepository;
import com.football.analytics.repository.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MatchEventConsumer {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;

    public MatchEventConsumer(MatchRepository matchRepository,
                              TeamRepository teamRepository,
                              LeagueRepository leagueRepository) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
    }

    @KafkaListener(topics = "matches.raw", groupId = "football-stream-group")
    public void consume(MatchEvent event) {
        League league = leagueRepository.findByCode(event.getLeagueCode())
                .orElseThrow(() -> new IllegalStateException("Unknown league: " + event.getLeagueCode()));

        Team homeTeam = teamRepository.findByApiId(event.getHomeTeamApiId())
                .orElseThrow(() -> new IllegalStateException("Unknown home team: " + event.getHomeTeamApiId()));

        Team awayTeam = teamRepository.findByApiId(event.getAwayTeamApiId())
                .orElseThrow(() -> new IllegalStateException("Unknown away team: " + event.getAwayTeamApiId()));

        Match match = matchRepository.findByApiId(event.getApiId())
                .orElse(new Match());

        match.setApiId(event.getApiId());
        match.setLeague(league);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setHomeScore(event.getHomeScore());
        match.setAwayScore(event.getAwayScore());
        match.setMatchday(event.getMatchday());
        match.setStatus(event.getStatus());
        match.setUtcDate(event.getUtcDate());

        matchRepository.save(match);
        log.info("Saved match: {} vs {} ({})", homeTeam.getName(), awayTeam.getName(), event.getStatus());
    }
}