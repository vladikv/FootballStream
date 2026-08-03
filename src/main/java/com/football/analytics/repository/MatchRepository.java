package com.football.analytics.repository;

import com.football.analytics.model.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByApiId(Integer apiId);
    List<Match> findByLeagueIdOrderByUtcDateDesc(Long leagueId);
    List<Match> findByStatus(String status);

    // Used by the date-browser filter on the dashboard
    List<Match> findByLeagueIdAndUtcDateBetweenOrderByUtcDateAsc(
            Long leagueId, OffsetDateTime start, OffsetDateTime end);
}