package com.football.analytics.controller;

import com.football.analytics.model.dto.TeamCompareDto;
import com.football.analytics.model.dto.TeamFormDto;
import com.football.analytics.model.entity.Team;
import com.football.analytics.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import com.football.analytics.model.entity.Match;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DashboardService dashboardService;

    public ApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/leagues/{leagueId}/teams")
    public List<Map<String, Object>> getTeams(@PathVariable Long leagueId) {
        List<Team> teams = dashboardService.getTeamsByLeague(leagueId);
        return teams.stream()
                .map(t -> Map.<String, Object>of("id", t.getId(), "name", t.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/teams/{teamId}/form")
    public TeamFormDto getTeamForm(@PathVariable Long teamId,
                                   @RequestParam(defaultValue = "5") int lastN) {
        return dashboardService.getTeamForm(teamId, lastN);
    }

    @GetMapping("/teams/compare")
    public TeamCompareDto compareTeams(@RequestParam Long teamA, @RequestParam Long teamB) {
        return dashboardService.compareTeams(teamA, teamB);
    }

    @GetMapping("/leagues/{leagueId}/matches")
    public List<Map<String, Object>> getMatchesByDate(@PathVariable Long leagueId,
                                                      @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<Match> matches = dashboardService.getMatchesByDate(leagueId, localDate);

        return matches.stream().map(m -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("home", m.getHomeTeam().getName());
            map.put("away", m.getAwayTeam().getName());
            map.put("homeScore", m.getHomeScore());
            map.put("awayScore", m.getAwayScore());
            map.put("status", m.getStatus());
            map.put("homeCrest", m.getHomeTeam().getCrestUrl());
            map.put("awayCrest", m.getAwayTeam().getCrestUrl());
            return map;
        }).collect(Collectors.toList());
    }
}