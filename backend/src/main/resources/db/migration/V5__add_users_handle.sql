-- 티켓온 아이디 (TASK-016). 일행에게 공유하는 값이라 이메일·이름과 무관하게 「단어 + 숫자 4자리」로 무작위로 만든다
-- (booking-ux-01-auth.md §3-2). 단어 목록은 HandleGenerator.WORDS 와 같아야 한다 — UsersHandleMigrationTest 가 대조한다.
ALTER TABLE users ADD COLUMN handle VARCHAR(30);

DO $$
DECLARE
    words TEXT[] := ARRAY['wave', 'star', 'stage', 'encore', 'aria', 'beat', 'chord', 'drum',
                          'echo', 'flute', 'harp', 'jazz', 'lyric', 'melody', 'note', 'opera',
                          'piano', 'rhythm', 'solo', 'tempo', 'tune', 'verse', 'viola', 'waltz',
                          'cello', 'choir', 'disco', 'folk', 'hymn', 'oboe', 'lute', 'gig'];
    target RECORD;
    candidate TEXT;
BEGIN
    FOR target IN SELECT id FROM users WHERE handle IS NULL ORDER BY id LOOP
        LOOP
            candidate := words[1 + floor(random() * array_length(words, 1))::int]
                         || lpad(floor(random() * 10000)::int::text, 4, '0');
            EXIT WHEN NOT EXISTS (SELECT 1 FROM users WHERE handle = candidate);
        END LOOP;
        UPDATE users SET handle = candidate WHERE id = target.id;
    END LOOP;
END $$;

ALTER TABLE users ALTER COLUMN handle SET NOT NULL;
CREATE UNIQUE INDEX ux_users_handle ON users (handle);
