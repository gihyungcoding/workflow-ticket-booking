# TASK-022 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | flows 8 · AC 8 |
| 2a Scenario | ✅ 완료 | CP-2.2 | 15건 · 경계 57 · 검증 2회차 PASS |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 마이그레이션 후 기존 `performance` 행의 `owner_id` 가 NULL 이고, 기존 관객 조회 API 응답이 마이그레이션 전과 같다
- [ ] 주최자 토큰으로 `POST /api/performances` 를 보내면 201 이고, 저장된 행의 `owner_id` 가 그 사용자 id 다 (비트랜잭션 조회로 확인)
- [ ] 토큰 없이 `POST`·`PUT /api/performances/**` 를 보내면 401, 관객 토큰이면 403 `FORBIDDEN_ROLE` 이고 DB 가 바뀌지 않는다
- [ ] 다른 주최자의 공연, 소유자가 NULL 인 공연, 없는 id 에 수정·취소를 보내면 모두 같은 형태의 404 이고 DB 가 바뀌지 않는다
- [ ] 자기 공연의 수정·취소는 지금 규칙(수정은 `now < openAt`) 그대로 동작하고 DB 에 반영된다
- [ ] `X-Write-Key` 없이 가입·로그인·소셜 가입 완료가 성공한다. `WRITE_GUARD_ENABLED=true` 를 줘도 쓰기 요청이 키 때문에 거부되지 않는다
- [ ] `WriteGuardFilter`·`WriteGuardConfig` 와 그 테스트가 저장소에 없고, CORS 허용 헤더에 `X-Write-Key` 가 없다
- [ ] `GET /api/performances/**` 는 토큰 없이 지금처럼 200 이다
