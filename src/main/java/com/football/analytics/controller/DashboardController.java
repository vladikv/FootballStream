package com.football.analytics.controller;

import com.football.analytics.repository.LeagueRepository;
import com.football.analytics.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Year;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final LeagueRepository leagueRepository;

    public DashboardController(DashboardService dashboardService, LeagueRepository leagueRepository) {
        this.dashboardService = dashboardService;
        this.leagueRepository = leagueRepository;
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) Long leagueId, Model model) {
        // Default to the first available league if none is selected
        Long resolvedLeagueId = leagueId != null
                ? leagueId
                : leagueRepository.findAll().stream().findFirst().map(l -> l.getId()).orElse(null);

        model.addAttribute("leagues", leagueRepository.findAll());
        model.addAttribute("selectedLeagueId", resolvedLeagueId);

        if (resolvedLeagueId != null) {
            int currentYear = Year.now().getValue();
            model.addAttribute("standings", dashboardService.getStandings(resolvedLeagueId, currentYear));
            model.addAttribute("recentMatches", dashboardService.getRecentMatches(resolvedLeagueId));
            model.addAttribute("topScorers", dashboardService.getTopScorers(resolvedLeagueId, currentYear));
        }

        return "dashboard";
    }
}