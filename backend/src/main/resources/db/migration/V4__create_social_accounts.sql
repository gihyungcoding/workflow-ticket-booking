-- 소셜 전용 계정은 비밀번호가 없다 (ADR-0013, TASK-011)
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE social_accounts (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider          VARCHAR(20) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_social_accounts_provider_user UNIQUE (provider, provider_user_id)
);
