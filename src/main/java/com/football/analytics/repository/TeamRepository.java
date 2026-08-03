package com.football.analytics.repository;

import com.football.analytics.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByApiId(Integer apiId);
    List<Team> findByLeagueIdOrderByNameAsc(Long leagueId);
}