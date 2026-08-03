package com.football.analytics.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FootballDataApiClient {

    private final RestTemplate restTemplate;

    @Value("${football-data.api.base-url}")
    private String baseUrl;

    @Value("${football-data.api.token}")
    private String apiToken;

    public FootballDataApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getStandings(String leagueCode) {
        return getRaw("/competitions/" + leagueCode + "/standings");
    }

    public String getMatches(String leagueCode) {
        return getRaw("/competitions/" + leagueCode + "/matches");
    }

    public String getScorers(String leagueCode) {
        return getRaw("/competitions/" + leagueCode + "/scorers");
    }

    private String getRaw(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", apiToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + path,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }
}