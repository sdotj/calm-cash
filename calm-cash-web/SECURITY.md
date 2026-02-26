# Calm Cash Web Security Notes

This frontend is a portfolio baseline and includes practical browser-facing controls.

## Implemented controls

- Runtime API URL validation in `src/config/env.ts`:
  - requires valid absolute URLs
  - blocks non-HTTPS URLs outside localhost
  - blocks non-HTTPS URLs in production builds
- CSP and browser policy meta tags in `index.html`
- Typed API error handling to avoid leaking backend internals in UI

## Current tradeoff

- Access and refresh tokens are persisted in `localStorage` to keep setup simple.
- This is acceptable for portfolio demo speed, but not ideal for enterprise browser security because XSS could exfiltrate tokens.

## Enterprise recommendation

For production:

1. Move auth to secure, `HttpOnly`, `Secure`, `SameSite` cookies with a BFF/session model.
2. Enforce security headers at edge/proxy (not only meta tags):
   - `Content-Security-Policy`
   - `Strict-Transport-Security`
   - `X-Content-Type-Options: nosniff`
   - `Referrer-Policy`
   - `Permissions-Policy`
3. Pin production `connect-src` to exact API origins.
4. Add dependency and SAST scanning in CI.
5. Add security tests for auth/session lifecycle and CORS/cookie behavior.
