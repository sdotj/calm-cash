# Step 1 Spec: Budget-Centric Redesign (Weekly + Monthly)

## 1) Objective
Shift from the current model (`budget per {month, category}`) to a simpler, user-facing model:

- A user creates one or more budgets for a period (`WEEKLY` or `MONTHLY`)
- That budget contains category limits
- Transactions are tracked against that budget and category
- Users can adjust limits and transactions throughout the period

This keeps contracts simple while preparing for:
- shared budgets (step 2)
- AI-driven transaction/budget actions (step 3)

## 2) Product Principles
- One clear object users reason about: `Budget`
- Predictable periods: explicit start/end dates
- No hidden math: all money fields use cents
- Strong tenant boundaries (same as current service)
- Backward-compatible rollout (no hard break on day 1)

## 3) Scope (Step 1)
### In Scope
- New budget container model (`WEEKLY` and `MONTHLY`)
- Category limits as children of a budget
- Transactions attached to a budget
- Budget-level summary endpoint
- Clone/template endpoint for rapid setup
- Unified API surface on `/api/...`

### Out of Scope
- Shared membership and permissions (step 2)
- AI chat and statement parsing (step 3)
- Multi-currency conversion

## 4) Domain Model (Current)
### `budgets`
- `id UUID PK`
- `owner_user_id UUID NOT NULL`
- `name TEXT NOT NULL` (ex: "March 2026 Budget", "Week of 2026-03-02")
- `period_type TEXT NOT NULL` (`WEEKLY`, `MONTHLY`)
- `start_date DATE NOT NULL`
- `end_date DATE NOT NULL`
- `currency CHAR(3) NOT NULL DEFAULT 'USD'`
- `status TEXT NOT NULL DEFAULT 'ACTIVE'` (`ACTIVE`, `ARCHIVED`)
- `created_at timestamptz NOT NULL default now()`
- `updated_at timestamptz NOT NULL default now()`

Constraints:
- `start_date <= end_date`
- multiple budgets per owner + period window are allowed

### `budget_category_limits`
- `id UUID PK`
- `budget_id UUID NOT NULL FK -> budgets(id) ON DELETE CASCADE`
- `category_id UUID NOT NULL` (owned by same user in step 1)
- `limit_cents BIGINT NOT NULL` (`> 0`)
- `color_hex VARCHAR(7) NULL` (hex format `#RRGGBB`, UI display helper)
- `created_at timestamptz NOT NULL default now()`
- `updated_at timestamptz NOT NULL default now()`

Constraints:
- unique (`budget_id`, `category_id`)

### `transactions` (evolution)
Add:
- `budget_id UUID NULL FK -> budgets(id) ON DELETE SET NULL`

Keep existing fields for compatibility:
- `user_id`, `category_id`, `merchant`, `amount_cents`, `transaction_date`, `source`, etc.

Step 1 rule:
- `budget_id` is required for create/update transaction APIs

## 5) Period Rules
### Monthly
- `start_date` must be the first day of month
- `end_date` must be the last day of the same month

### Weekly
- Use user-configured week start day (default Monday for now)
- `end_date = start_date + 6 days`
- start day must align with configured week start

Timezone:
- Keep current date-only storage
- Use a single user timezone setting in app logic (default `America/New_York` until user profile settings are added)

## 6) API Contract (Current)
Base path: `/api`

## 6.1 Budgets
### `POST /api/budgets`
Create budget container.

Request:
```json
{
  "name": "March 2026 Budget",
  "periodType": "MONTHLY",
  "startDate": "2026-03-01",
  "currency": "USD",
  "categoryLimits": [
    { "categoryId": "uuid", "limitCents": 50000, "colorHex": "#34A853" },
    { "categoryId": "uuid", "limitCents": 15000, "colorHex": "#FBBC05" }
  ]
}
```

Behavior:
- Derive `endDate` from period rules
- Validate categories belong to current user
- Create budget + category limits atomically

Response `201`:
```json
{
  "id": "uuid",
  "name": "March 2026 Budget",
  "periodType": "MONTHLY",
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "currency": "USD",
  "status": "ACTIVE",
  "totalLimitCents": 65000,
  "createdAt": "2026-02-26T20:00:00Z",
  "updatedAt": "2026-02-26T20:00:00Z",
  "categoryLimits": [
    { "id": "uuid", "categoryId": "uuid", "limitCents": 50000, "colorHex": "#34A853", "spentCents": 0, "remainingCents": 50000, "utilizationPct": 0.0 },
    { "id": "uuid", "categoryId": "uuid", "limitCents": 15000, "colorHex": "#FBBC05", "spentCents": 0, "remainingCents": 15000, "utilizationPct": 0.0 }
  ]
}
```

### `GET /api/budgets`
Query params:
- `periodType` optional
- `status` optional (default `ACTIVE`)
- `startDateFrom` optional
- `startDateTo` optional

Returns budget list (without full transaction list).

### `GET /api/budgets/{budgetId}`
Returns one budget with category limits + computed spend/remaining.

### `PATCH /api/budgets/{budgetId}`
Patch mutable fields:
- `name`
- `status` (`ARCHIVED` allowed)

### `POST /api/budgets/{budgetId}/clone`
Create a new budget from an existing one.

Request:
```json
{
  "startDate": "2026-04-01",
  "name": "April 2026 Budget"
}
```

Behavior:
- Carry forward category limits only
- No transactions copied

## 6.2 Budget Category Limits
### `PUT /api/budgets/{budgetId}/categories/{categoryId}`
Upsert category limit.

Request:
```json
{ "limitCents": 20000 }
```

### `DELETE /api/budgets/{budgetId}/categories/{categoryId}`
Remove category from budget limits.

## 6.3 Transactions (Budget-scoped)
### `POST /api/transactions`
Request:
```json
{
  "budgetId": "uuid",
  "categoryId": "uuid",
  "merchant": "Trader Joe's",
  "description": "Weekly groceries",
  "amountCents": -12500,
  "transactionDate": "2026-03-05",
  "source": "MANUAL"
}
```

Validation:
- `budgetId` belongs to current user
- `transactionDate` must be within budget range
- if `categoryId` provided, it must exist and belong to user

### `PATCH /api/transactions/{transactionId}`
Allow updating:
- `budgetId`, `categoryId`, `merchant`, `description`, `amountCents`, `transactionDate`, `source`

Re-validate period ownership on every affected field.

### `GET /api/budgets/{budgetId}/transactions`
Query params:
- `categoryId` optional
- `minDate`, `maxDate` optional (bounded by budget window)
- `limit` optional (default 50, max 200)
- `cursor` optional (future-ready; can start with offset pagination if needed)

## 6.4 Summary
### `GET /api/budgets/{budgetId}/summary`
Returns:
- budget totals (`totalLimitCents`, `totalSpentCents`, `totalRemainingCents`, `utilizationPct`)
- income/expense/net for budget window
- category-level summary (`limit/spent/remaining/utilization`)
- uncategorized expense row when applicable

## 7) Alerts (Step 1 adjustment)
Keep current alert logic, but make it budget-aware:
- `BUDGET_80` and `BUDGET_100` should dedupe per:
  - `user_id + budget_id + category_id + threshold_pct`

Schema updates:
- Add `budget_id` to `alerts` metadata
- Keep existing fields until v1 is retired

## 8) Rollout Status
- Budget-centric endpoints are now the default on `/api`.
- Legacy v1 budget/monthly-summary code paths are removed.

## 9) Migration Notes
Flyway migration set should include:
- create `budgets`
- create `budget_category_limits`
- add `budget_id` to `transactions`
- add `budget_id` to `alerts`
- dedupe/constraint indexes for alert threshold uniqueness in current model
- indexes:
  - `budgets(owner_user_id, start_date, end_date, status)`
  - `budget_category_limits(budget_id, category_id)`
  - `transactions(user_id, budget_id, transaction_date)`

Data backfill strategy (best effort):
- for each existing `budgets` row (`month + category`), create/find monthly container by `user + month`
- move category limit into `budget_category_limits`
- attach matching monthly transactions to the new `budget_id` when date fits

## 10) Error/Validation Rules
Use current `ErrorResponse` pattern and status conventions:
- `400` validation and bad period semantics
- `404` unknown budget/category/transaction for current user
- `409` unique conflicts (duplicate active budget window, duplicate category in budget)

New validation examples:
- `startDate must align with WEEKLY start day`
- `transactionDate must fall within budget period`
- `category already exists in this budget`

## 11) Test Plan (Minimum)
- Unit:
  - period boundary calculations weekly/monthly
  - summary math and utilization
- Integration:
  - create/list/get budget current
  - add/update/remove category limits
  - create/update transaction with in/out-of-range dates
  - summary endpoint with mixed income/expense
  - alert dedupe for 80/100 thresholds by budget+category
  - tenant isolation across all new endpoints

## 12) Open Questions (Decide Before Build Starts)
1. Should we add optional tags or labels to help users distinguish multiple budgets in the same period?
2. Should transactions without category be allowed in current? (recommended: yes)
3. Should current transaction listing be cursor-based now or after shared budgets?
4. Week start source: global config now vs user preference table now?

## 13) Recommended Build Order
1. Introduce schema + entities (`budgets`, `budget_category_limits`, `transactions.budget_id`)
2. Add current budget create/get/list + category-limit upsert/delete
3. Add current transaction create/patch/list budget-scoped
4. Add current summary endpoint
5. Wire budget-aware alerts and dedupe
6. Add bridge logic and deprecation markers for v1
