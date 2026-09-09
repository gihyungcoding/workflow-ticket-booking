# TASK-003 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route: Frontend, flows 7건(AC 7건 전부 커버) |
| 2a Scenario | ✅ 완료 | CP-2.4 | 시나리오 7건, 검증 PASS(attempt 2), HITL#1 승인 |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | DESIGN-001~003 warn→error 승격 검토 포함 |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 테마가 design-tokens.css 의 값으로 초기화된다 — check_architecture.py --id DESIGN-001 위반 0건
- [ ] 공연명이 Noto Serif KR, 나머지 텍스트가 IBM Plex Sans KR 로 렌더링되고 두 서체가 실제로 로드된다
- [ ] 상태 문구가 예매가능·예매예정·매진·예매마감·공연취소로 표시된다
- [ ] index.html 이 lang="ko" 이고 title 이 템플릿 기본값(frontend)이 아니다
- [ ] 서체 로드 실패해도 한글이 시스템 폰트로 읽힌다 (font-family 폴백 체인)
- [ ] 목록 API 호출 실패 시 기존과 동일하게 오류 안내와 다시 시도 버튼이 표시된다
- [ ] 기존 프론트 테스트 6건이 모두 통과한다
