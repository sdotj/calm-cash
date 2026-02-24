# Calm Cash

Calm Cash is a personal finance backend platform built with a microservice architecture.  
This repository is the parent project containing shared infrastructure and two Spring Boot services:

- `auth-service`
- `budget-service`

## Project Goals

- Build a production-minded backend foundation for personal budgeting workflows.
- Practice service boundaries, API security, database migrations, and local containerized development.
- Showcase architecture and engineering process as a portfolio project.

## Current Architecture

- **Auth Service** (`auth-service`): authentication and token-related concerns.
- **Budget Service** (`budget-service`): budgeting domain logic and data operations.
- **PostgreSQL (Docker Compose)**: local development database shared by services.

Each service manages its own schema and Flyway migrations:

- `auth-service` -> `auth` schema
- `budget-service` -> `budget` schema

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- Flyway
- PostgreSQL 16
- Docker Compose
- Gradle

## Getting Started

### 1. Start database

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with default local credentials from `docker-compose.yml`.

### 2. Run services (separate terminals)

```bash
cd auth-service
./gradlew bootRun --args='--spring.profiles.active=local'
```

```bash
cd budget-service
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. Service ports

- Auth Service: `http://localhost:8081`
- Budget Service: `http://localhost:8082`

## Repository Layout

```text
calm-cash/
  docker-compose.yml
  auth-service/
  budget-service/
```

## Development Notes

- Local profile config lives in each service under `src/main/resources/application-local.yml`.
- Flyway migration scripts are in `src/main/resources/db/migration`.
- JWT settings currently use development-safe defaults and should be hardened for non-local environments.

## Roadmap

- API gateway and centralized routing
- Service-to-service auth hardening
- CI pipeline with test + quality checks
- Observability (structured logging + metrics dashboards)
- Frontend client integration

## Portfolio Context

This project is intentionally structured as a growing backend platform.  
The current state demonstrates foundation work (infrastructure, service setup, persistence, migrations) with additional features added iteratively.

## License

MIT License (see `LICENSE`).

