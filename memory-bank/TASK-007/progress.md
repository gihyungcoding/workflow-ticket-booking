# TASK-007 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route: Backend, inputs 8/outputs 3/flows 4 |
| 2a Scenario | ✅ 완료 | CP-2.4 | 시나리오 11건(happy1/error1/regression9), 검증 3회 pass, HITL#1 승인 |
| 2b Red | ✅ 완료 | CP-2.6 | 테스트 11개(SC-07만 Red, 10개 already_passing), HITL#2 승인 |
| 3 Green | ✅ 완료 | CP-3.4 | 58/58 통과, 린트 error 0, 범위 이탈 0 |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 유효한 등록/수정 요청은 마이그레이션 이전과 동일하게 201/200으로 성공한다 (기존 23개 시나리오 회귀 없음)
- [ ] title/venue가 없거나 200자를 넘으면 400과 INVALID_REQUEST가 반환된다 (Bean Validation 경로)
- [ ] section의 grade/rowStart/rowEnd/seatsPerRow가 없거나 형식에 맞지 않으면 400과 INVALID_SECTION이 반환된다 (Bean Validation 경로)
- [ ] 요청 본문이 파싱 불가능한 JSON이거나 필드 타입이 맞지 않으면(HttpMessageNotReadableException) 빈 body가 아니라 400과 INVALID_REQUEST(ErrorResponse{code,message})가 반환된다
