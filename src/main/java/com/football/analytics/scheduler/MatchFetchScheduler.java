package com.football.analytics.scheduler;

import com.football.analytics.service.EtlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MatchFetchScheduler {

    private final EtlService etlService;

    @Value("${football-data.api.leagues}")
    private String leaguesCsv;

    public MatchFetchScheduler(EtlService etlService) {
        this.etlService = etlService;
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 5000)
    public void fetchStandingsAndTeams() {
        for (String league : leaguesCsv.split(",")) {
            etlService.syncLeagueTeamsAndStandings(league.trim());
            sleep(7000);
        }
    }

    // once per hour — sync top scorers
    @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 120000)
    public void fetchScorers() {
        for (String league : leaguesCsv.split(",")) {
            etlService.syncScorers(league.trim());
            sleep(7000);
        }
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 60000)
    public void fetchMatches() {
        for (String league : leaguesCsv.split(",")) {
            etlService.syncMatches(league.trim());
            sleep(7000);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}