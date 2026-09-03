# TASK-001 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route=Backend, 신규 파일 12건 |
| 2a Scenario | 🔄 재시도 중 (attempt 2) | CP-2.4 (SUPERSEDED) | 시나리오 11건은 완료했으나 Phase 4에서 REJECT — 커버리지 보강 필요 |
| 2b Red | ⬜ 재작업 대기 | CP-2.6 (완료분은 유효, 추가분 필요) | 새 시나리오에 대한 Red 테스트 추가 필요 |
| 3 Green | ⬜ 재작업 대기 | CP-3.4 (완료분은 유효, 수정 필요) | CLOSED 로직·컬럼 길이 버그 수정 필요 |
| 4 Verify | ❌ FAIL (attempt 1) | CP-4.2 | 정확성 결함 2건 — VERIFY_TASK-001.json.reject 참고 |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] `GET /api/performances` 호출 시 200과 함께 `content`/`page`/`size`/`totalElements` 형식으로 목록이 반환된다
- [ ] `status=OPEN` 필터 시 `openAt <= now <= closeAt` 이고 `availableSeats > 0` 인 공연만 반환된다
- [ ] `availableSeats == 0` 인 공연은 `status: SOLD_OUT` 으로 반환된다
- [ ] `cancelled == true` 인 공연은 시각·좌석 수와 무관하게 `status: CANCELLED` 로 반환된다
- [ ] `start_at < now` 인 공연은 상태 필터와 무관하게 목록에서 제외된다
- [ ] 존재하지 않는 id 로 `GET /api/performances/{id}` 호출 시 404와 `PERFORMANCE_NOT_FOUND` 코드가 반환된다
- [ ] `Clock` 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른 `status` 를 반환한다 (SQL `now()` 미사용을 리뷰로 확인)
- [ ] `performance` 테이블의 NOT NULL/CHECK 제약이 엔티티 애노테이션으로도 표현되어 있고, 위반 시 저장이 거부된다 (Phase 2a HITL#1에서 사용자 요청으로 추가, AC8/F7)
