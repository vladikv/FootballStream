package com.football.analytics.service;

import com.football.analytics.model.entity.Match;
import com.football.analytics.model.entity.PlayerStat;
import com.football.analytics.model.entity.Standing;
import com.football.analytics.repository.LeagueRepository;
import com.football.analytics.repository.MatchRepository;
import com.football.analytics.repository.PlayerStatRepository;
import com.football.analytics.repository.StandingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final StandingRepository standingRepository;
    private final MatchRepository matchRepository;
    private final PlayerStatRepository playerStatRepository;
    private final LeagueRepository leagueRepository;

    public DashboardService(StandingRepository standingRepository,
                            MatchRepository matchRepository,
                            PlayerStatRepository playerStatRepository,
                            LeagueRepository leagueRepository) {
        this.standingRepository = standingRepository;
        this.matchRepository = matchRepository;
        this.playerStatRepository = playerStatRepository;
        this.leagueRepository = leagueRepository;
    }

    // Returns the league table ordered by position
    public List<Standing> getStandings(Long leagueId, Integer seasonYear) {
        return standingRepository.findByLeagueIdAndSeasonYearOrderByPosition(leagueId, seasonYear);
    }

    // Returns the most recent matches for a league
    public List<Match> getRecentMatches(Long leagueId) {
        return matchRepository.findByLeagueIdOrderByUtcDateDesc(leagueId);
    }

    // Returns the top scorers list for a league/season
    public List<PlayerStat> getTopScorers(Long leagueId, Integer seasonYear) {
        return playerStatRepository.findByLeagueIdAndSeasonYearOrderByGoalsDesc(leagueId, seasonYear);
    }

    // Returns the last N matches for a single team, used for the "form" chart
    public List<Match> getTeamForm(Long teamId, int lastN) {
        return matchRepository.findByStatus("FINISHED").stream()
                .filter(m -> m.getHomeTeam().getId().equals(teamId) || m.getAwayTeam().getId().equals(teamId))
                .sorted((a, b) -> b.getUtcDate().compareTo(a.getUtcDate()))
                .limit(lastN)
                .toList();
    }
}