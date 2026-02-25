create unique index if not exists uq_refresh_tokens_token_hash on refresh_tokens(token_hash);
create index if not exists idx_refresh_tokens_user_active on refresh_tokens(user_id, revoked_at, expires_at);
