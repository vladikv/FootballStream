package com.football.analytics.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TeamFormDto {
    private Long teamId;
    private String teamName;
    private List<MatchResult> results;

    @Getter
    @AllArgsConstructor
    public static class MatchResult {
        private String opponent;
        private Integer scoreFor;
        private Integer scoreAgainst;
        private String outcome; // "W", "D", "L"
        private OffsetDateTime date;
    }
}