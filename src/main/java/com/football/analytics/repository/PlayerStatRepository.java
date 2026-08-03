package com.football.analytics.repository;

import com.football.analytics.model.entity.PlayerStat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerStatRepository extends JpaRepository<PlayerStat, Long> {
    List<PlayerStat> findByLeagueIdAndSeasonYearOrderByGoalsDesc(Long leagueId, Integer seasonYear);
    Optional<PlayerStat> findByPlayerIdAndLeagueIdAndSeasonYear(Long playerId, Long leagueId, Integer seasonYear);
}