package com.football.analytics.repository;

import com.football.analytics.model.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByApiId(Integer apiId);
}