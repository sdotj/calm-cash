create table if not exists budget_plans (
    id uuid primary key,
    owner_user_id uuid not null,
    name text not null,
    period_type text not null,
    start_date date not null,
    end_date date not null,
    currency varchar(3) not null default 'USD',
    status text not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_budget_plans_period_type check (period_type in ('WEEKLY', 'MONTHLY')),
    constraint ck_budget_plans_status check (status in ('ACTIVE', 'ARCHIVED')),
    constraint ck_budget_plans_date_range check (start_date <= end_date),
    constraint ck_budget_plans_currency_uppercase check (currency = upper(currency))
);

alter table budget_plans
    add constraint uq_budget_plans_id_owner unique (id, owner_user_id);

create index if not exists idx_budget_plans_period_window
    on budget_plans(owner_user_id, period_type, start_date, end_date)
    where status = 'ACTIVE';

create index if not exists idx_budget_plans_owner_window
    on budget_plans(owner_user_id, start_date, end_date);

create table if not exists budget_category_limits (
    id uuid primary key,
    budget_id uuid not null,
    user_id uuid not null,
    category_id uuid not null,
    limit_cents bigint not null,
    color_hex varchar(7) null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_budget_category_limits_positive check (limit_cents > 0),
    constraint ck_budget_category_limits_color_hex check (
        color_hex is null or color_hex ~ '^#[0-9A-Fa-f]{6}$'
    ),
    constraint uq_budget_category_limits_budget_category unique (budget_id, category_id),
    constraint fk_budget_category_limits_budget_owner
        foreign key (budget_id, user_id)
        references budget_plans(id, owner_user_id)
        on delete cascade,
    constraint fk_budget_category_limits_category_owner
        foreign key (category_id, user_id)
        references categories(id, user_id)
        on delete cascade
);

create index if not exists idx_budget_category_limits_budget
    on budget_category_limits(budget_id);

create index if not exists idx_budget_category_limits_user
    on budget_category_limits(user_id);

alter table transactions
    add column if not exists budget_id uuid null;

alter table transactions
    add constraint fk_transactions_budget_plan
        foreign key (budget_id)
        references budget_plans(id)
        on delete set null;

create index if not exists idx_transactions_user_budget_date
    on transactions(user_id, budget_id, transaction_date);

alter table alerts
    add column if not exists budget_id uuid null;

alter table alerts
    add constraint fk_alerts_budget_plan
        foreign key (budget_id)
        references budget_plans(id)
        on delete set null;

create unique index if not exists ux_alerts_budget_dedupe_v2
    on alerts(user_id, budget_id, category_id, threshold_pct)
    where type in ('BUDGET_80', 'BUDGET_100') and budget_id is not null;
