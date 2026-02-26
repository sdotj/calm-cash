create table if not exists budget_members (
    id uuid primary key,
    budget_id uuid not null references budget_plans(id) on delete cascade,
    user_id uuid not null,
    role text not null,
    created_at timestamptz not null default now(),
    unique (budget_id, user_id),
    constraint ck_budget_members_role check (role in ('EDITOR', 'VIEWER'))
);

create index if not exists idx_budget_members_user
    on budget_members(user_id);

create index if not exists idx_budget_members_budget
    on budget_members(budget_id);
