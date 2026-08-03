package com.football.analytics.service;

import com.football.analytics.model.entity.*;
import com.football.analytics.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.analytics.client.FootballDataApiClient;
import com.football.analytics.model.dto.MatchEvent;
import com.football.analytics.producer.MatchEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Year;


import java.time.OffsetDateTime;

@Service
@Slf4j
public class EtlService {

    private final FootballDataApiClient apiClient;
    private final MatchEventProducer producer;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final StandingRepository standingRepository;
    PlayerRepository playerRepository;
    PlayerStatRepository playerStatRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EtlService(FootballDataApiClient apiClient,
                      MatchEventProducer producer,
                      LeagueRepository leagueRepository,
                      TeamRepository teamRepository,
                      StandingRepository standingRepository) {
        this.apiClient = apiClient;
        this.producer = producer;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.standingRepository = standingRepository;
    }

    public void syncLeagueTeamsAndStandings(String leagueCode) {
        try {
            String json = apiClient.getStandings(leagueCode);
            JsonNode root = objectMapper.readTree(json);

            League league = upsertLeague(root.path("competition"), leagueCode);
            int seasonYear = Year.now().getValue(); // додай імпорт java.time.Year

            JsonNode table = root.path("standings").get(0).path("table");
            for (JsonNode row : table) {
                upsertTeam(row, league, seasonYear); // передаємо весь row, не тільки team-піднод
            }

            log.info("Synced teams/standings for league {}", leagueCode);
        } catch (Exception e) {
            log.error("Failed to sync league {}: {}", leagueCode, e.getMessage());
        }
    }

    public void syncMatches(String leagueCode) {
        try {
            String json = apiClient.getMatches(leagueCode);
            JsonNode root = objectMapper.readTree(json);

            for (JsonNode matchNode : root.path("matches")) {
                MatchEvent event = toMatchEvent(matchNode, leagueCode);
                if (event != null) {
                    producer.publish(event);
                }
            }

            log.info("Synced matches for league {}", leagueCode);
        } catch (Exception e) {
            log.error("Failed to sync matches for {}: {}", leagueCode, e.getMessage());
        }
    }

    private League upsertLeague(JsonNode competitionNode, String leagueCode) {
        Integer apiId = competitionNode.path("id").asInt();
        return leagueRepository.findByApiId(apiId).orElseGet(() -> {
            League league = new League();
            league.setApiId(apiId);
            league.setName(competitionNode.path("name").asText());
            league.setCode(leagueCode);
            league.setCountry(competitionNode.path("area").path("name").asText(""));
            return leagueRepository.save(league);
        });
    }

    private void upsertTeam(JsonNode row, League league, Integer seasonYear) {
        JsonNode teamNode = row.path("team");
        Integer apiId = teamNode.path("id").asInt();

        Team team = teamRepository.findByApiId(apiId).orElseGet(Team::new);
        team.setApiId(apiId);
        team.setName(teamNode.path("name").asText());
        team.setShortName(teamNode.path("shortName").asText(""));
        team.setCrestUrl(teamNode.path("crest").asText(""));
        team.setLeague(league);
        teamRepository.save(team);

        upsertStanding(row, league, team, seasonYear);
    }

    private void upsertStanding(JsonNode row, League league, Team team, Integer seasonYear) {
        Standing standing = standingRepository
                .findByLeagueIdAndTeamIdAndSeasonYear(league.getId(), team.getId(), seasonYear)
                .orElseGet(Standing::new);

        standing.setLeague(league);
        standing.setTeam(team);
        standing.setSeasonYear(seasonYear);
        standing.setPosition(row.path("position").asInt());
        standing.setPlayed(row.path("playedGames").asInt());
        standing.setWon(row.path("won").asInt());
        standing.setDraw(row.path("draw").asInt());
        standing.setLost(row.path("lost").asInt());
        standing.setPoints(row.path("points").asInt());
        standing.setGoalsFor(row.path("goalsFor").asInt());
        standing.setGoalsAgainst(row.path("goalsAgainst").asInt());

        standingRepository.save(standing);
    }

    private MatchEvent toMatchEvent(JsonNode matchNode, String leagueCode) {
        JsonNode homeTeamNode = matchNode.path("homeTeam");
        JsonNode awayTeamNode = matchNode.path("awayTeam");
        JsonNode scoreNode = matchNode.path("score").path("fullTime");

        Integer homeApiId = homeTeamNode.path("id").asInt();
        Integer awayApiId = awayTeamNode.path("id").asInt();

        if (!teamRepository.findByApiId(homeApiId).isPresent()
                || !teamRepository.findByApiId(awayApiId).isPresent()) {
            log.warn("Skipping match — team not synced yet: {} vs {}",
                    homeTeamNode.path("name").asText(), awayTeamNode.path("name").asText());
            return null;
        }

        return new MatchEvent(
                matchNode.path("id").asInt(),
                leagueCode,
                homeTeamNode.path("name").asText(),
                awayTeamNode.path("name").asText(),
                homeApiId,
                awayApiId,
                scoreNode.path("home").isNull() ? null : scoreNode.path("home").asInt(),
                scoreNode.path("away").isNull() ? null : scoreNode.path("away").asInt(),
                matchNode.path("matchday").asInt(),
                matchNode.path("status").asText(),
                OffsetDateTime.parse(matchNode.path("utcDate").asText())
        );
    }

    public void syncScorers(String leagueCode) {
        try {
            String json = apiClient.getScorers(leagueCode);
            JsonNode root = objectMapper.readTree(json);

            League league = leagueRepository.findByCode(leagueCode)
                    .orElseThrow(() -> new IllegalStateException("League not synced yet: " + leagueCode));
            int seasonYear = Year.now().getValue();

            for (JsonNode entry : root.path("scorers")) {
                upsertPlayerStat(entry, league, seasonYear);
            }

            log.info("Synced scorers for league {}", leagueCode);
        } catch (Exception e) {
            log.error("Failed to sync scorers for {}: {}", leagueCode, e.getMessage());
        }
    }

    private void upsertPlayerStat(JsonNode entry, League league, Integer seasonYear) {
        JsonNode playerNode = entry.path("player");
        JsonNode teamNode = entry.path("team");

        Integer playerApiId = playerNode.path("id").asInt();
        Integer teamApiId = teamNode.path("id").asInt();

        Team team = teamRepository.findByApiId(teamApiId).orElse(null);
        if (team == null) {
            // Team not synced yet — skip this scorer for now
            return;
        }

        Player player = playerRepository.findByApiId(playerApiId).orElseGet(Player::new);
        player.setApiId(playerApiId);
        player.setName(playerNode.path("name").asText());
        player.setTeam(team);
        playerRepository.save(player);

        PlayerStat stat = playerStatRepository
                .findByPlayerIdAndLeagueIdAndSeasonYear(player.getId(), league.getId(), seasonYear)
                .orElseGet(PlayerStat::new);

        stat.setPlayer(player);
        stat.setLeague(league);
        stat.setSeasonYear(seasonYear);
        stat.setGoals(entry.path("goals").asInt(0));
        stat.setAssists(entry.path("assists").asInt(0));
        stat.setPlayedMatches(entry.path("playedMatches").asInt(0));

        playerStatRepository.save(stat);
    }
}