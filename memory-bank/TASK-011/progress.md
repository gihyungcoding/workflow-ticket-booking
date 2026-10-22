# TASK-011 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | 🔄 진행 중 | — | |
| 2a Scenario | ⬜ 대기 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
- [ ] 이미 이메일 회원가입된 계정과 같은 이메일로, email_verified가 true인 소셜 로그인을 하면 users 행을 새로 만들지 않고 기존 계정에 social_accounts 행을 추가한 뒤 바로 accessToken을 발급한다(역할은 기존 값 유지)
- [ ] email_verified가 false(또는 미제공)인 경우에는 이메일이 같아도 자동 연결하지 않고 신규 가입과 동일하게 pendingToken을 발급한다
- [ ] 같은 제공자 계정으로 재로그인해도 social_accounts의 유니크 제약(provider, provider_user_id)에 의해 중복 행이 생기지 않는다
- [ ] 카카오/네이버(중첩 응답 구조)와 구글(평면 구조) 각각에서 이메일과 이름을 정상적으로 추출한다(제공자별 매핑 단위 테스트)
- [ ] 만료된 pendingToken으로 POST /api/auth/social/complete 호출 시 401을 반환한다
- [ ] pendingToken으로 /api/auth/social/complete 외의 인증 필요 엔드포인트를 호출하면 거부된다(typ 클레임 검증)
