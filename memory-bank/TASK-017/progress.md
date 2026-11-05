# TASK-017 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | flows 7 · AC 7 |
| 2a Scenario | ⚠ 게이트 예외 | CP-2.2 | 18건 · 롤백 1회 · 검증 수렴 안 됨 |
| 2b Red | ✅ 승인 | CP-2.5 | 17 Red · 1 가드 · 기존 7 교체 |
| 3 Green | ✅ 완료 | CP-3.4 | 287/287 · 자체 리뷰 correctness 0 |
| 4 Verify | ✅ 예외 승인 | CP-4.2 | 1회차 FAIL → 2회차 WARN |
| 5 Reflect | ✅ 완료 | CP-5.2 | 회고 승인 대기 |

## 완료 조건

- [x] `frontend/src/theme/tokens.ts` 의 값이 `docs/product/design-tokens.css` 와 하나도 다르지 않다 (테스트로 대조)
- [x] 테마 밖 색상 리터럴·매직 치수가 없다 (`check_architecture.py` DESIGN-001·002 통과, 003 위반 신규 0)
- [x] Pretendard 가 실제로 로드된다 (`document.fonts` 확인 또는 네트워크 요청)
- [x] 375px 폭에서 가로 스크롤이 없고, 넓은 화면에서는 가운데 한 열로 보인다
- [x] 상태 배지가 design.md §5 의 6가지 뜻·문구·클래스로만 나온다 (예매 가능·예정·매진·마감·입금 대기·예매 확정, 공연 취소는 마감과 같은 회색)
- [x] `data-stay` 에 해당하는 토스트 부품이 문구를 띄우고 스스로 사라진다
- [x] 기존 화면(목록·상세·공연 등록·수정) 테스트가 새 배지 문구로 고친 뒤 모두 통과한다
