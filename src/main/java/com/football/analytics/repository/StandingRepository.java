package com.football.analytics.repository;

import com.football.analytics.model.entity.Standing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Long> {
    List<Standing> findByLeagueIdAndSeasonYearOrderByPosition(Long leagueId, Integer seasonYear);
    Optional<Standing> findByLeagueIdAndTeamIdAndSeasonYear(Long leagueId, Long teamId, Integer seasonYear);
}