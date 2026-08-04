# ⚽ FootballStream

A real-time football analytics platform built with Spring Boot, Apache Kafka, and PostgreSQL. Streams live standings, match results, and top scorer data from [football-data.org](https://www.football-data.org/) across Europe's top 5 leagues, and visualizes it on an interactive dashboard.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.6-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

## Overview

FootballStream ingests match data on a schedule, publishes it to Kafka, and consumes it into PostgreSQL — decoupling data collection from data persistence via an event-driven pipeline. A Spring Boot + Thymeleaf dashboard visualizes standings, recent matches, top scorers, team form, and head-to-head comparisons with Chart.js.

## Architecture

```
football-data.org API
        │
        ▼
 MatchFetchScheduler  ──(REST)──►  FootballDataApiClient
        │
        ▼
    EtlService  (parses JSON, upserts leagues/teams/standings)
        │
        ▼
 MatchEventProducer  ──publish──►  Kafka topic: matches.raw
                                          │
                                          ▼
                                MatchEventConsumer
                                          │
                                          ▼
                                    PostgreSQL
                                          │
                                          ▼
                          DashboardService / ApiController
                                          │
                                          ▼
                        Thymeleaf Dashboard + Chart.js (REST + AJAX)
```

## Features

- **Multi-league coverage** — Premier League, La Liga, Serie A, Bundesliga, Ligue 1
- **Event-driven ETL** — match data flows through Kafka rather than being written directly to the database, decoupling ingestion from persistence
- **Scheduled sync** — standings/teams every 30 min, matches every 10 min, top scorers hourly, all rate-limited to respect the free API tier (10 req/min)
- **Interactive dashboard** — league switcher, date-based match browser, team form chart (last 5 results), two-team radar comparison
- **Fully containerized** — one `docker compose up` starts Postgres, Zookeeper, Kafka, Kafka UI, and the app

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Kafka, Thymeleaf, Validation) |
| Messaging | Apache Kafka (Confluent images) |
| Database | PostgreSQL 16 |
| Frontend | Thymeleaf, vanilla JS, Chart.js |
| Build | Maven |
| Infra | Docker Compose |

## Getting Started

### Prerequisites

- Docker + Docker Compose
- A free API key from [football-data.org](https://www.football-data.org/client/register)

## Project Structure

```
src/main/java/com/football/analytics/
├── client/       # football-data.org API client
├── scheduler/    # @Scheduled jobs driving the ETL cycle
├── service/      # EtlService (parsing/upsert), DashboardService
├── producer/     # Kafka producer
├── consumer/     # Kafka consumer
├── controller/   # DashboardController (Thymeleaf), ApiController (REST/JSON)
├── repository/   # Spring Data JPA repositories
├── model/
│   ├── dto/      # Kafka message + API response DTOs
│   └── entity/   # JPA entities (League, Team, Match, Standing, Player, PlayerStat)
└── config/       # Kafka producer/consumer, RestTemplate config
```

## API Rate Limiting

The free football-data.org tier allows 10 requests/minute. `MatchFetchScheduler` enforces a 7-second delay between requests per league to stay within that limit.

## Roadmap

- [ ] Automated tests (unit + integration)
- [ ] Resilience4j-based rate limiting instead of fixed sleep delays
- [ ] Publish only changed matches to Kafka instead of the full list each cycle
- [ ] CI pipeline (GitHub Actions)

## License

MIT
