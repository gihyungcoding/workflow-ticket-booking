# TASK-001 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route=Backend, 신규 파일 12건 |
| 2a Scenario | ✅ 완료 (attempt 2) | CP-2.4_retry1 | 시나리오 18건, AC 11/11, HITL#1 재승인 |
| 2b Red | ✅ 완료 (attempt 2) | CP-2.6_retry1 | 20개 중 3개 Red(SC-15/16, F9 단위 테스트), 17개는 기존 구현으로 이미 Green. HITL#2 재승인 |
| 3 Green | ⬜ 재작업 대기 | CP-3.4 (attempt 1 완료분은 유효, 수정 필요) | F8(CHECK 제약)·컬럼 길이·PerformanceStatusRules 반영 필요 |
| 4 Verify | ❌ FAIL (attempt 1) | CP-4.2 | 정확성 결함 2건 — VERIFY_TASK-001.json.reject 참고. attempt 2는 Phase 2b/3 재작업 후 재검증 |
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
- [ ] `openAt` 가 `closeAt` 보다 늦은 공연은 저장이 거부된다 (Phase 4에서 발견한 결함의 근본 해결, AC9/F8)
- [ ] `performance` 테이블의 컬럼 길이 제약(title/venue VARCHAR(200))이 엔티티에도 표현되어 있고, 위반 시 저장이 거부된다 (Phase 4에서 발견, AC10/F7)
- [ ] `now < openAt` 인 공연은 `status: UPCOMING` 으로 반환된다 (원래 기능 문서 §4 누락분, 재시도 중 발견, AC11/F3)
- [ ] `PerformanceStatusRules.of()`/`matches()` 가 5개 상태에 대해 상호배타적이다 (단위 테스트, GWT 시나리오 대응 없음, F9 — HITL#1 재승인 시 V10 대응으로 추가)
