# Step 1 Implementation Checklist

## Decisions (Locked)
- Replace-first strategy: old endpoint backward compatibility is not required.
- Budget model: multiple budget containers per period window are allowed.
- Budget category presentation: each category limit can include a UI color (`colorHex`).
- Phase A delivery: schema + entities + repositories only.

## Phase A (In Progress)
- [x] Add new schema foundation migration for budget-centric model.
- [x] Add `budget_id` support on `transactions` and `alerts`.
- [x] Add current enums/entities (`Budget`, `BudgetCategoryLimit`).
- [x] Add current repositories for budget plans and category limits.
- [ ] Remove old budget entities/services/controllers (deferred to Phase B+ implementation).

## Phase B (API + Service)
- [x] Create current DTOs for budget create/get/list/patch.
- [x] Implement `POST /api/budgets`.
- [x] Implement `GET /api/budgets` and `GET /api/budgets/{id}`.
- [x] Implement `PATCH /api/budgets/{id}`.
- [x] Implement category limit upsert/delete endpoints.
- [ ] Add budget validation helpers:
  - `MONTHLY` start/end rules.
  - `WEEKLY` start/end rules.
  - date-in-range validation for transactions.

## Phase C (Transactions + Summary + Alerts)
- [x] Add current transaction DTOs/endpoints with required `budgetId`.
- [x] Add budget-scoped transaction listing endpoint.
- [x] Add budget summary endpoint.
- [x] Make budget threshold alert dedupe budget-aware (`budget_id + category_id + threshold`).
- [x] Include category color in summary payloads.

## Phase D (Cleanup + Cutover)
- [x] Remove v1 budget endpoints/controllers/services/repositories/entities.
- [x] Remove no-longer-needed old budget migration assumptions from app code.
- [x] Update tests to target current surface only.
- [x] Refresh API docs examples for weekly + monthly budgets.

## Notes
- Initial Flyway migrations (`V1` through `V3`) remain intact by design.
- We can drop legacy tables/columns later, once current services are fully live and tested.
