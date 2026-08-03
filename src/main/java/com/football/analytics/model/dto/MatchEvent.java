package com.football.analytics.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvent {

    private Integer apiId;
    private String leagueCode;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeTeamApiId;
    private Integer awayTeamApiId;
    private Integer homeScore;
    private Integer awayScore;
    private Integer matchday;
    private String status;
    private OffsetDateTime utcDate;
}