alter table alerts
    add column if not exists category_id uuid null,
    add column if not exists month date null,
    add column if not exists threshold_pct integer null;

alter table alerts
    add constraint ck_alerts_threshold_range check (threshold_pct is null or (threshold_pct >= 0 and threshold_pct <= 100));

alter table alerts
    add constraint ck_alerts_budget_metadata_required check (
        (type not in ('BUDGET_80', 'BUDGET_100'))
        or (category_id is not null and month is not null and threshold_pct is not null)
    );

alter table alerts
    add constraint ck_alerts_month_first_day check (month is null or month = date_trunc('month', month)::date);

create unique index if not exists ux_alerts_budget_dedupe
    on alerts(user_id, type, category_id, month, threshold_pct)
    where type in ('BUDGET_80', 'BUDGET_100');
