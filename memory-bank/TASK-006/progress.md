# TASK-006 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route: Backend, target_files 3건 |
| 2a Scenario | ✅ 완료 | CP-2.4 | 시나리오 3건, 독립검증 PASS(warn 3), HITL#1 승인 |
| 2b Red | ✅ 완료 | CP-2.6 | SC-01/02 Red, SC-03 already_passing, HITL#2 승인 |
| 3 Green | ✅ 완료 | CP-3.4 | 전체 50/50 통과, lint error 0 |
| 4 Verify | ✅ 완료 | CP-4.3 | 1·2차 FAIL(결함 3건 수정) → 3차 WARN, EXCEPTION_APPROVE |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] section.price가 정수가 아닌 값(예: -0.5)이면 400과 INVALID_SECTION이 반환되고 좌석이 생성되지 않는다
- [ ] section.seatsPerRow가 정수가 아닌 값(예: 1.9)이면 400과 INVALID_SECTION이 반환되고 좌석이 생성되지 않는다
- [ ] 정수 price/seatsPerRow를 보내는 기존 등록 요청은 이전과 동일하게 201로 성공한다
