# Calm Cash Web

React + TypeScript frontend for the Calm Cash portfolio project.

## Features

- Auth flow wired to `auth-service` (`register`, `login`, `refresh`, `logout`, `me`)
- Month-based budgeting dashboard wired to `budget-service`
- Category creation
- Budget setup by month and category
- Transaction entry + recent transaction list
- Monthly summary cards (income, expenses, net)
- Alerts list with mark-as-read

## Project structure

- `src/components`: reusable UI components
- `src/hooks`: stateful orchestration (`useAuth`, `useDashboard`)
- `src/api`: backend API client modules
- `src/config`: runtime config/environment validation
- `src/utils`: formatting and error helpers
- `src/types.ts`: shared app DTO/types

## Local setup

1. Install dependencies:

```bash
npm install
```

2. Copy env file:

```bash
cp .env.example .env
```

3. Run dev server:

```bash
npm run dev
```

Default local API URLs:

- Auth API: `http://localhost:8081`
- Budget API: `http://localhost:8082`

Override via:

- `VITE_AUTH_API_URL`
- `VITE_BUDGET_API_URL`

## Security posture

- Runtime URL validation blocks insecure non-local API URLs.
- Browser policies and CSP are defined in `index.html`.
- Detailed notes and production recommendations are in [SECURITY.md](./SECURITY.md).

## Commands

Build:

```bash
npm run build
```

Lint:

```bash
npm run lint
```
