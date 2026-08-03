package com.football.analytics.repository;

import com.football.analytics.model.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findByApiId(Integer apiId);
    Optional<League> findByCode(String code);
}