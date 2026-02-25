# Security Overview

This document describes the security model for `auth-service`, current protections, and known gaps.

## Threat Model

The service protects:
- User credentials (`password_hash` only, never plaintext storage).
- Access tokens (short-lived JWT).
- Refresh tokens (long-lived, stored as hashes, rotation-based).

Primary threats considered:
- Credential stuffing/brute-force login attempts.
- Token theft and replay.
- Misconfigured JWT validation.
- Duplicate-account and race-condition edge cases.
- Information leakage through error messages.

## Security Controls In Place

### Authentication and Tokens
- Password hashing uses BCrypt.
- Access token algorithm is explicitly `HS256`.
- JWT includes `iss` and `aud`, and both are validated on decode.
- Access token lifetime is short (15 minutes by default).
- Refresh tokens are high-entropy random values, stored as SHA-256 hashes.
- Refresh token rotation is implemented; old tokens are revoked when rotated.

### API and Transport Layer
- Stateless API (`SessionCreationPolicy.STATELESS`).
- Only explicit auth endpoints are public (`/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`).
- All other endpoints require authentication.
- Validation is applied to incoming DTOs (`@Valid`, email and size constraints).

### Abuse Protection
- Login attempt throttling blocks repeated failures per `email + clientIp` window.

### Data Integrity
- Unique user email constraint.
- Refresh token hash uniqueness/index and token lookup indexes.
- Transactional auth flows to prevent partial success (for example, persisted user without tokens).

### Error Handling
- Centralized API exception handling with controlled error payloads.
- Generic 500 response body to avoid leaking internals.

## Operational Requirements

For non-local environments:
- Set `JWT_SECRET` to a random secret of at least 32 bytes.
- Set strong values for `app.jwt.issuer` and `app.jwt.audience`.
- Enable HTTPS at the edge/load balancer.
- Run behind a trusted reverse proxy and standardize client IP extraction.
- Keep dependencies and JDK updated.

## Known Gaps (Planned)

- Login throttling is in-memory and not distributed (should move to Redis for multi-instance deployments).
- No MFA/email verification/account recovery workflow yet.
- No key rotation strategy for JWT secrets yet.
- Additional security tests should be expanded (negative and abuse-case matrix).

## Testing Strategy

`AuthIntegrationTests` cover:
- register success and duplicate handling
- refresh token rotation and reuse rejection
- login throttling behavior

This suite is intended as a baseline and should grow with new auth features.
