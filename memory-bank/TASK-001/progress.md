# TASK-001 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route=Backend, 신규 파일 12건 |
| 2a Scenario | ⬜ 대기 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] `GET /api/performances` 호출 시 200과 함께 `content`/`page`/`size`/`totalElements` 형식으로 목록이 반환된다
- [ ] `status=OPEN` 필터 시 `openAt <= now <= closeAt` 이고 `availableSeats > 0` 인 공연만 반환된다
- [ ] `availableSeats == 0` 인 공연은 `status: SOLD_OUT` 으로 반환된다
- [ ] `cancelled == true` 인 공연은 시각·좌석 수와 무관하게 `status: CANCELLED` 로 반환된다
- [ ] `start_at < now` 인 공연은 상태 필터와 무관하게 목록에서 제외된다
- [ ] 존재하지 않는 id 로 `GET /api/performances/{id}` 호출 시 404와 `PERFORMANCE_NOT_FOUND` 코드가 반환된다
- [ ] `Clock` 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른 `status` 를 반환한다 (SQL `now()` 미사용을 리뷰로 확인)
