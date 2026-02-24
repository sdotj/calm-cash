create table if not exists categories (
    id uuid primary key,
    user_id uuid not null,
    name text not null,
    created_at timestamptz not null default now(),
    unique (user_id, name)
);

create table if not exists budgets (
    id uuid primary key,
    user_id uuid not null,
    month date not null,
    category_id uuid not null references categories(id) on delete cascade,
    limit_cents bigint not null,
    created_at timestamptz not null default now(),
    unique (user_id, month, category_id)
);

create table if not exists transactions (
    id uuid primary key,
    user_id uuid not null,
    category_id uuid null references categories(id) on delete set null,
    merchant text not null,
    description text null,
    amount_cents bigint not null,
    transaction_date date not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    source text not null default 'MANUAL'
);

create index if not exists idx_transactions_user_date
    on transactions(user_id, transaction_date);

create index if not exists idx_transactions_user_category
    on transactions(user_id, category_id);

create table if not exists alerts (
    id uuid primary key,
    user_id uuid not null,
    type text not null,
    message text not null,
    created_at timestamptz not null default now(),
    read_at timestamptz null
);

create index if not exists idx_alerts_user_created
    on alerts(user_id, created_at desc);