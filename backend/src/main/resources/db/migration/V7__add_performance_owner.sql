-- 공연 소유자(ADR-0029). 기존 공연은 소유자를 모르므로 NULL 로 남긴다 — 소유자 없는 공연은 아무도 수정·취소할 수 없다(404).
-- 사용자 삭제 기능은 없지만 사용자 행이 지워져도 공연은 남도록 SET NULL 로 둔다.
ALTER TABLE performance ADD COLUMN owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_performance_owner_id ON performance (owner_id);
