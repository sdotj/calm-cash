create table if not exists users (
    id uuid primary key,
    email text not null unique,
    password_hash text not null,
    display_name text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token_hash text not null,
    issued_at timestamptz not null default now(),
    expires_at timestamptz not null,
    revoked_at timestamptz null,
    replaced_by_token_id uuid null,
    ip_address text null,
    user_agent text null
);

create index if not exists idx_refresh_tokens_user on refresh_tokens(user_id);
create index if not exists idx_refresh_tokens_expires on refresh_tokens(expires_at);