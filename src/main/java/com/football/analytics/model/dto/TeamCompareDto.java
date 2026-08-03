package com.football.analytics.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeamCompareDto {
    private TeamSummary teamA;
    private TeamSummary teamB;

    @Getter
    @AllArgsConstructor
    public static class TeamSummary {
        private String name;
        private Integer position;
        private Integer played;
        private Integer won;
        private Integer draw;
        private Integer lost;
        private Integer points;
        private Integer goalsFor;
        private Integer goalsAgainst;
    }
}