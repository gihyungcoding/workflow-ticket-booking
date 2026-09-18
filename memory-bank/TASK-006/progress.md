# TASK-006 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route: Backend, target_files 3건 |
| 2a Scenario | ⬜ 대기 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] section.price가 정수가 아닌 값(예: -0.5)이면 400과 INVALID_SECTION이 반환되고 좌석이 생성되지 않는다
- [ ] section.seatsPerRow가 정수가 아닌 값(예: 1.9)이면 400과 INVALID_SECTION이 반환되고 좌석이 생성되지 않는다
- [ ] 정수 price/seatsPerRow를 보내는 기존 등록 요청은 이전과 동일하게 201로 성공한다
