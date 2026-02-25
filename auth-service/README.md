# Auth Service (Calm Cash)

Authentication service for the Calm Cash project, built with Java 21 and Spring Boot.

## Current Capabilities

- User registration and login
- Access token issuance (JWT, HS256)
- Refresh token issuance, storage as hash, and rotation
- Logout via refresh token revocation
- Input validation and centralized API error handling
- Flyway database migrations
- Integration tests for core auth and security behavior

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Gradle

## Architecture

Layered structure:

- `controller`: HTTP endpoints and request/response handling
- `service`: auth business logic, token workflows, transactional boundaries
- `repository`: persistence abstractions
- `entity`: JPA domain models
- `config`: security and JWT wiring

High-level request flow:

```text
Client
  -> AuthController
    -> AuthService (transaction boundary)
      -> UserRepository / RefreshTokenRepository
      -> JwtService (access token)
      -> RefreshTokenService (issue/rotate/revoke refresh token)
  <- AuthResponse (accessToken, refreshToken)
```

## Running Locally

1. Set environment variables:
   - `SPRING_PROFILES_ACTIVE=local`
   - `JWT_SECRET=<at least 32-byte random string>`
2. Start the app:
   - `./gradlew bootRun`

## Testing

- Run all tests:
  - `./gradlew test`

## Security Notes

See [SECURITY.md](SECURITY.md) for threat model, implemented controls, and known gaps.

## Next Iterations: Production Readiness

The project is intentionally scoped for portfolio clarity. If moving to full production, the next upgrades would be:

1. Distributed rate limiting
   - Replace in-memory login throttling with Redis-backed counters/Lua for multi-instance deployments.
2. Account lifecycle hardening
   - Add email verification, password reset, and optional MFA.
3. Key management and rotation
   - Introduce JWT key rotation strategy and secret management via a vault/KMS.
4. CI/CD quality gates
   - Add pipeline checks for tests, static analysis, dependency vulnerability scans, and migration verification.
5. Observability
   - Add structured security events, metrics (failed logins, refresh failures), and alerting dashboards.
6. API/contract maturity
   - Expand OpenAPI examples and document stable error codes for client teams.
