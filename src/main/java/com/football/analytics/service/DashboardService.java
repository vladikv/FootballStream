package com.football.analytics.service;

import com.football.analytics.model.dto.TeamCompareDto;
import com.football.analytics.model.dto.TeamFormDto;
import com.football.analytics.model.entity.Match;
import com.football.analytics.model.entity.PlayerStat;
import com.football.analytics.model.entity.Standing;
import com.football.analytics.model.entity.Team;
import com.football.analytics.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private final StandingRepository standingRepository;
    private final MatchRepository matchRepository;
    private final PlayerStatRepository playerStatRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;

    public List<Match> getMatchesByDate(Long leagueId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = start.plusDays(1);
        return matchRepository.findByLeagueIdAndUtcDateBetweenOrderByUtcDateAsc(leagueId, start, end);
    }

    public DashboardService(StandingRepository standingRepository,
                            MatchRepository matchRepository,
                            PlayerStatRepository playerStatRepository,
                            LeagueRepository leagueRepository,
                            TeamRepository teamRepository) {
        this.standingRepository = standingRepository;
        this.matchRepository = matchRepository;
        this.playerStatRepository = playerStatRepository;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
    }

    public List<Standing> getStandings(Long leagueId, Integer seasonYear) {
        return standingRepository.findByLeagueIdAndSeasonYearOrderByPosition(leagueId, seasonYear);
    }

    public List<Match> getRecentMatches(Long leagueId) {
        return matchRepository.findByLeagueIdOrderByUtcDateDesc(leagueId);
    }

    public List<PlayerStat> getTopScorers(Long leagueId, Integer seasonYear) {
        return playerStatRepository.findByLeagueIdAndSeasonYearOrderByGoalsDesc(leagueId, seasonYear);
    }

    // Returns teams for a league, used to populate the form/compare dropdowns
    public List<Team> getTeamsByLeague(Long leagueId) {
        return teamRepository.findByLeagueIdOrderByNameAsc(leagueId);
    }

    // Builds the last-N-matches form summary for a single team (used by the sparkline chart)
    public TeamFormDto getTeamForm(Long teamId, int lastN) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        List<Match> matches = matchRepository.findByStatus("FINISHED").stream()
                .filter(m -> m.getHomeTeam().getId().equals(teamId) || m.getAwayTeam().getId().equals(teamId))
                .sorted((a, b) -> b.getUtcDate().compareTo(a.getUtcDate()))
                .limit(lastN)
                .toList();

        List<TeamFormDto.MatchResult> results = matches.stream().map(m -> {
            boolean isHome = m.getHomeTeam().getId().equals(teamId);
            Team opponent = isHome ? m.getAwayTeam() : m.getHomeTeam();
            Integer scoreFor = isHome ? m.getHomeScore() : m.getAwayScore();
            Integer scoreAgainst = isHome ? m.getAwayScore() : m.getHomeScore();

            String outcome;
            if (scoreFor == null || scoreAgainst == null) {
                outcome = "-";
            } else if (scoreFor > scoreAgainst) {
                outcome = "W";
            } else if (scoreFor.equals(scoreAgainst)) {
                outcome = "D";
            } else {
                outcome = "L";
            }

            return new TeamFormDto.MatchResult(opponent.getName(), scoreFor, scoreAgainst, outcome, m.getUtcDate());
        }).toList();

        return new TeamFormDto(team.getId(), team.getName(), results);
    }

    // Builds a side-by-side comparison of two teams based on current-season standings
    public TeamCompareDto compareTeams(Long teamAId, Long teamBId) {
        int seasonYear = Year.now().getValue();
        return new TeamCompareDto(
                buildSummary(teamAId, seasonYear),
                buildSummary(teamBId, seasonYear)
        );
    }

    private TeamCompareDto.TeamSummary buildSummary(Long teamId, Integer seasonYear) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        Optional<Standing> standing = standingRepository
                .findByLeagueIdAndTeamIdAndSeasonYear(team.getLeague().getId(), teamId, seasonYear);

        return standing.map(s -> new TeamCompareDto.TeamSummary(
                team.getName(), s.getPosition(), s.getPlayed(), s.getWon(), s.getDraw(),
                s.getLost(), s.getPoints(), s.getGoalsFor(), s.getGoalsAgainst()
        )).orElseGet(() -> new TeamCompareDto.TeamSummary(
                team.getName(), null, 0, 0, 0, 0, 0, 0, 0
        ));
    }
}