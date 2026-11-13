-- 공연 공개 여부(organizer-flow Task B). 기존 공연은 지금처럼 관객에게 보이도록 TRUE 로 채우고,
-- 이후 새 행의 기본값은 FALSE 로 바꾼다 — 등록은 비공개(초안)로 시작하고 주최자가 공개해야 보인다.
ALTER TABLE performance ADD COLUMN published BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE performance ALTER COLUMN published SET DEFAULT FALSE;
