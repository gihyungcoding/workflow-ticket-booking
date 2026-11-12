# TASK-024 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | flows 4 · AC 6 |
| 2a Scenario | ✅ 완료 | CP-2.2 | 16건 · 경계 34 · 검증 2회차 PASS |
| 2b Red | ✅ 완료 | CP-2.5 | 61 실패 · already_passing 3 · HITL#1·2 승인 |
| 3 Green | ✅ 완료 | CP-3.4 | 767/767 · 자체 리뷰 1R 결함 0 |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
- [ ] 비로그인으로 `/performances/new`·`/performances/:id/edit` 에 들어가면 `/login` 으로 가고, 로그인 후 원래 경로로 돌아온다
- [ ] 관객으로 같은 경로에 들어가면 폼 대신 「주최자만 이용할 수 있어요」와 「공연 둘러보기」가 보인다
- [ ] 토큰 복원 중에는 가드가 안내·이동 없이 아무것도 그리지 않는다 (복원 후 주최자면 폼이 보인다)
- [ ] 쓰기 API 가 401 이면 로그아웃되고 `/login` 으로 간다. 403 이면 「주최자만 이용할 수 있어요」가 보인다
- [ ] `VITE_WRITE_KEY`·`WRITE_REJECTED_MESSAGE`·`isWriteKeyRejected` 가 `frontend/src` 와 `.env.example` 에 없다
