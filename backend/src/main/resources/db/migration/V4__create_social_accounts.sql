-- 소셜 전용 계정은 비밀번호가 없다 (ADR-0013, TASK-011)
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- 이메일 소유가 확인된 계정만 소셜 자동 연결 대상이다 (ADR-0020). 기존 이메일 가입자는 확인된 적이 없으므로 false
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE social_accounts (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider          VARCHAR(20) NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_social_accounts_provider_user UNIQUE (provider, provider_user_id)
);
