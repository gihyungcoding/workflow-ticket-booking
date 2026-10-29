---
status: ARCHIVED
saved_at: 2026-10-27T00:00:00Z
summary: "[Phase 4 2라운드 WARN, HITL#3 예외 승인] RC-1 해소(재현 변종 3개를 운영 경로 테스트로 Red→Green, 2회차 리뷰어 결함 없음). H2 146/146 · PostgreSQL 114/114 · 린트·아키텍처 error 0 · 실제 앱+PostgreSQL HTTP/이름 경계 프로브 ALL OK. 남은 경고: R-1(교차 사이트 form_post 세션 쿠키, 실제 브라우저 미검증 — 사용자가 수용·후속 태스크로 결정)."

next_steps:
  - "Phase 5 (wf-reflect) — KPT 회고 + 규칙 개선안, HITL#4. 입력: Phase 4 FAIL 1회(RC-1)와 attempts, 후보 6건(아래)"
  - "Phase 5 회고·후속 후보: ① 프레임워크가 입력을 정규화하는 계층이 있으면 그 계층 이후 값 기준의 통합 테스트를 같이 쓴다(wf-scenario/wf-red 지침 후보) ② Phase 2b 하네스 프로브에 프레임워크 변환기를 포함한다 ③ architecture.md 계층 표에 config 행이 없다(미인코딩) ④ R-1 후속 태스크: SameSite=None;Secure 또는 쿠키 기반 저장소+ADR, 실제 브라우저 검증 ⑤ ADR-0021(fragment) 리다이렉트 형식 후속 태스크 ⑥ user-authentication.md §1-3 계정 연결 정책 문장(ADR-0020 이전 내용) 갱신"
  - "Docker Desktop 은 검증을 위해 켠 것이다(원래 꺼져 있었다). 검증용 컨테이너 tb-verify-012 는 정리한다"

decisions:
  - decision: "리뷰어 소견을 믿지 않고 실제 필터 체인으로 독립 재현했다"
    rationale: "리뷰어는 변환기를 단독 실행했고 \"True\" 는 추정만 했다. 승인된 SC-06 과 충돌하는 주장이라 판정(FAIL)을 내리기 전에 증거가 필요했다. 가짜 애플 서버로 서명한 id_token 의 email_verified 15종을 전체 체인에 통과시켜 \"TRUE\"/\"True\"/\"tRue\" 가 자동 연결, \" true\"·\"yes\"·\"1\"·정수 1 등은 가입 대기임을 확인. 프로브 파일은 삭제"
  - decision: "내 Phase 3 프로브가 이 결함을 놓친 이유: 변환기가 없는 순수 NimbusJwtDecoder 로 확인했다"
    rationale: "Phase 2b 의 하네스 프로브는 JWKS 서명 검증 가정만 확인했고 Spring 의 OidcIdTokenDecoderFactory 클레임 변환은 포함하지 않았다. Phase 5 회고 후보: '입력을 정규화하는 프레임워크 계층이 있으면 그 계층 이후의 값을 기준으로 시나리오·테스트를 쓴다 — attributes 를 직접 주입하는 핸들러 테스트는 변환 이전 값을 가정할 수 있다'"
  - decision: "PostgreSQL 전체 스위트 실행에서 10건이 실패한 것은 구현 결함이 아니라 환경변수 오버라이드가 만든 인공물로 판단하고, 제외 사유를 VERIFY 에 그대로 기록했다"
    rationale: "자체 H2 URL 을 넘기는 SpringApplicationBuilder 테스트 4개와 @DataJpaTest 슬라이스(PostgreSQL 전용 TIMESTAMPTZ 마이그레이션이 H2 에서 실행됨)가 원인임을 실패 메시지와 소스로 확인했다. 이 태스크는 domain·repository·resources 를 건드리지 않았다. 해당 클래스는 H2 전체 실행에서 통과"
  - decision: "R-1(세션 쿠키)은 결함이 아니라 사용자 결정 사항으로 둔다"
    rationale: "리뷰어 의견: 보안 결함이 아니라 가용성 위험(중간). 실제 HTTP 로 JSESSIONID 에 SameSite·Secure 가 없음을 확인했다. 선택지 (a) SameSite=None;Secure 설정 (b) 쿠키 기반 저장소+ADR 은 AC 밖의 배포 설정 결정이라 HITL#3 에서 묻는다"

  - decision: "롤백 대상은 구현(Phase 3), R-1 은 수용 — 둘 다 사용자 선택"
    rationale: "구현 롤백은 승인된 시나리오·테스트를 그대로 지키고 공통 지점 1곳을 고친다. 시나리오 롤백은 코드는 단순해지지만 HITL#1·#2 를 다시 받고 테스트를 고쳐야 한다. R-1 은 AC 밖의 배포 설정 결정이라 후속 태스크로 분리"

  - decision: "2라운드: 1라운드의 일회성 체인 프로브를 영구 회귀 테스트로 승격했고 PostgreSQL·HTTP 프로브를 수정 커밋 기준으로 다시 실행했다"
    rationale: "재검증은 이전 증거를 믿지 않고 수정 커밋 3f4f382 로 bootJar 를 다시 빌드해 같은 프로브를 돌린다. 결과가 1라운드와 같다(ALL OK)는 것 자체가 수정이 다른 경로를 건드리지 않았다는 증거"
  - decision: "상태는 PASS 가 아니라 WARN"
    rationale: "R-1 은 사용자가 수용했지만 '교차 사이트 form_post 에서 세션 쿠키가 전송되는가'는 실제 브라우저로 확인하지 못한 가정이다. 측정하지 않은 것을 PASS 로 쓰지 않는다. 결함이 아니라 미검증 위험이라 FAIL 도 아니다"

  - decision: "HITL#3 2라운드: EXCEPTION_APPROVE (R-1 예외 기록), verified_commit 기록"
    rationale: "WARN 사유는 사용자가 이미 수용·후속 태스크로 결정한 R-1 하나다. 일반 승인이 아니라 예외 승인으로 미검증 가정(실제 브라우저)을 VERIFY 에 남겨 /wf-ship 과 회고가 읽게 한다"

attempts:
  - phase: "4"
    round: 1
    outcome: "Phase 4 FAIL — RC-1: 운영 경로의 Spring 클레임 변환 때문에 email_verified 대소문자 변형이 검증됨으로 읽힘(승인된 SC-06 과 불일치)"
    back_to: "3"
  - phase: "4"
    round: 2
    outcome: "WARN — RC-1 해소, R-1 미검증(사용자 수용) → EXCEPTION_APPROVE"
---
