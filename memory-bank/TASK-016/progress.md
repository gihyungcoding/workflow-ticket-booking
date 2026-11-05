# TASK-016 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 (롤백 1회 반영) | CP-1.3 | flows 6 · AC 7 · claim_schema |
| 2a Scenario | ✅ 완료 | CP-2.2 | 16건 · 롤백 2회 · Phase 4 롤백 1회 |
| 2b Red | ✅ 재승인 | CP-2.5 | 변이 M1~M5 |
| 3 Green | ✅ 완료 | CP-3.4 | 164/164 · 자체 리뷰 4R |
| 4 Verify | ✅ 예외 승인 | CP-4.2 | 1회차 FAIL(RC-2) → 2회차 WARN |
| 5 Reflect | ✅ 완료 | CP-5.2 | 회고 승인 대기 |

## 완료 조건

- [ ] 이메일 가입 응답과 `GET /api/auth/me` 응답에 `handle` 이 있고, 이메일 `harin.kim@kakao.com` 이면 `harin.kim` 이다
- [ ] 같은 앞부분의 두 번째 가입자는 `harin.kim2` 를 받는다
- [ ] 허용 밖 문자는 지워지고 20자를 넘지 않으며, 3자 미만이면 `user` 기반으로 만든다
- [ ] 소셜 최초 가입(`/social/complete`)으로 만든 회원도 `handle` 을 받는다
- [ ] 마이그레이션 뒤 기존 회원 전원이 겹치지 않는 `handle` 을 갖는다 (NOT NULL·UNIQUE)
- [ ] `POST /api/auth/social/pending` 이 유효한 토큰에 제공자·이메일·이름·만료 시각을 돌려준다
- [ ] 만료·위조 토큰이면 401 `PENDING_SIGNUP_EXPIRED` 이고 DB 변화가 없다
