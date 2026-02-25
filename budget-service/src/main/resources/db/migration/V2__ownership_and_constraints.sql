-- Enforce tenant-safe ownership for category references.
alter table categories
    add constraint uq_categories_id_user unique (id, user_id);

-- Replace category foreign keys with ownership-safe composite foreign keys.
alter table budgets
    drop constraint if exists budgets_category_id_fkey;
alter table budgets
    add constraint fk_budgets_category_owner
    foreign key (category_id, user_id)
    references categories(id, user_id)
    on delete cascade;

alter table transactions
    drop constraint if exists transactions_category_id_fkey;
alter table transactions
    add constraint fk_transactions_category_owner
    foreign key (category_id, user_id)
    references categories(id, user_id)
    on delete set null;

-- Financial and enum-like guardrails.
alter table budgets
    add constraint ck_budgets_limit_positive check (limit_cents > 0);

alter table budgets
    add constraint ck_budgets_month_first_day check (month = date_trunc('month', month)::date);

alter table transactions
    add constraint ck_transactions_non_zero_amount check (amount_cents <> 0);

alter table transactions
    add constraint ck_transactions_source check (source in ('MANUAL', 'PLAID', 'IMPORT'));

alter table alerts
    add constraint ck_alerts_type check (type in ('BUDGET_80', 'BUDGET_100', 'SYSTEM'));
