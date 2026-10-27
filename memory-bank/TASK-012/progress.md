# TASK-012 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | flows F1~F7, 변경 대상 5건, AuthService 무변경 |
| 2a Scenario | ✅ 완료 | CP-2.2 | 시나리오 12건, 검증 2회차 PASS, HITL#1 승인 |
| 2b Red | 🔄 HITL#2 대기 | CP-2.5 | 테스트 12건, Red 11 + already_passing 1 |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 애플 Client Secret이 ES256으로 서명된 JWT로 생성되어 토큰 교환 요청에 쓰인다
- [ ] 애플 최초 로그인 콜백에서만 전달되는 이름 정보가 users 행 생성 시 반영된다
- [ ] 애플 로그인도 TASK-011의 자동 연결/역할 선택(pendingToken) 흐름을 동일하게 따른다
