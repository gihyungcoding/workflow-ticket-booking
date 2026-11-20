-- 일회용 이메일 링크 토큰(ADR-0032, TASK-027). 원문은 저장하지 않고 SHA-256 해시만 둔다(DB 가 새도 링크를 만들 수 없다)
CREATE TABLE email_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(20) NOT NULL,          -- 'VERIFY_EMAIL' | 'RESET_PASSWORD'
    token_hash  CHAR(64)    NOT NULL UNIQUE,   -- SHA-256(원문) 16진 소문자
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,                   -- 사용한 시각. NULL 이면 아직 쓸 수 있다
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_token_user_purpose ON email_token (user_id, purpose);

-- 계정으로 나가는 메일(인증·재설정·소셜 안내)의 마지막 발송 시각. 60초 간격 제한의 기준이다. 기존 행은 NULL
ALTER TABLE users ADD COLUMN account_mail_sent_at TIMESTAMPTZ;
