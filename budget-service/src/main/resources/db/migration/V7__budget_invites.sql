create table if not exists budget_invites (
    id uuid primary key,
    budget_id uuid not null references budget_plans(id) on delete cascade,
    invited_by_user_id uuid not null,
    token uuid not null unique,
    role text not null,
    status text not null default 'PENDING',
    expires_at timestamptz not null,
    accepted_by_user_id uuid null,
    accepted_at timestamptz null,
    created_at timestamptz not null default now(),
    constraint ck_budget_invites_role check (role in ('EDITOR', 'VIEWER')),
    constraint ck_budget_invites_status check (status in ('PENDING', 'ACCEPTED', 'REVOKED'))
);

create index if not exists idx_budget_invites_budget
    on budget_invites(budget_id, created_at desc);

create index if not exists idx_budget_invites_token
    on budget_invites(token);
