# TASK-004 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route: Backend, flows 9건, AC 9건 전부 커버 |
| 2a Scenario | ✅ 완료 | CP-2.4 | 시나리오 14건(SC-14 Red 진행 중 철회), AC 9/9, HITL#1 승인 |
| 2b Red | ✅ 완료 | CP-2.6 | 테스트 14개, 14/14 실패(UnsupportedOperationException), 기존 20개 통과, HITL#2 승인 |
| 3 Green | ✅ 완료 | CP-3.4 | 테스트 34/34 통과, 린트 error 0, 리팩토링 불필요 |
| 4 Verify | ⛔ FAIL | CP-4.2 | code-reviewer 재현 — AC4 우회(역방향 범위/정수 오버플로), DoS 가능, 입력검증 부재. 롤백 대기 |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 구역 1개 이상으로 POST /api/performances 호출 시 201과 함께 각 구역의 rowStart~rowEnd × seatsPerRow 만큼 seat 행이 생성된다
- [ ] 생성된 performance.total_seats/available_seats 가 생성된 좌석 총수와 같다
- [ ] openAt > closeAt 또는 closeAt > startAt 이면 400과 INVALID_TIME_ORDER 가 반환된다
- [ ] 좌석 총수가 5,000을 넘으면 400과 SEAT_LIMIT_EXCEEDED 가 반환된다
- [ ] 두 구역의 행 범위가 겹치면 409와 DUPLICATE_SEAT_RANGE 가 반환된다
- [ ] now < openAt 인 공연을 PUT /api/performances/{id} 로 수정하면 200과 변경된 필드가 반영된 상세가 반환된다
- [ ] now >= openAt 인 공연을 수정하려 하면 409와 REGISTRATION_ALREADY_OPEN 이 반환된다
- [ ] 존재하지 않는 id 로 수정/취소를 호출하면 404와 PERFORMANCE_NOT_FOUND 가 반환된다
- [ ] POST /api/performances/{id}/cancel 호출 시 cancelled 가 true 로 바뀌고, 이미 취소된 공연을 다시 호출해도 200이 반환된다(멱등)
