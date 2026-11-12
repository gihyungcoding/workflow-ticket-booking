---
status: ARCHIVED
saved_at: 2026-11-12T00:00:00Z
summary: "Green 184/184, spotless 0, 제약 error 0. RETRY_SCENARIO 2회(SC-06 k·l 400 · sub 정규형). 자체 리뷰 2R — RC-1 공통 지점 수정(7f812e0), unresolved 0, deferred 3."

next_steps:
  - "Phase 4 — wf-verify. 이전 라운드 root_causes(RC-1, not_probed 변종 포함)와 deferred(HEAD) 를 리뷰어에 넘긴다"

decisions:
  - decision: "구현 순서: V7 → Performance.ownerId → AccessClaims·verifyAccessClaims(role·sub 원문) → JwtAuthenticationFilter ROLE_ → SecurityConfig(GET 만 공개, 403 FORBIDDEN_ROLE) → Service findOwned(404) → WriteGuard 삭제·CORS 헤더 → 배포 체크리스트"
    rationale: "스키마부터 바깥으로. 구현 커밋 46af97b, 문서 ebcb3e5"
  - decision: "재사용 — PerformanceNotFoundException(소유권 위반 404), unauthorizedEntryPoint 와 같은 ErrorResponse 직렬화(writeError 로 묶음), AuthTestHelper signUp/login, AuthController.me 의 principal 관례"
    rationale: "PLAN reusable 4건 모두 사용"
  - decision: "verifyAccessClaims 가 sub 를 페이로드 원문에서 JSON 문자열인지 본다"
    rationale: "디코더(MappedJwtClaimSetConverter)가 숫자 sub 7 을 \"7\" 로 바꿔 읽어 SC-05 q 가 통과하던 것. exp 를 원문에서 보는 hasWholeSecondsExp 관례"
  - decision: "SC-12 a~h 는 ERROR 디스패치 permitAll(기존)이 있어 토큰을 실어도 같은 숫자 — 실제 서버 테스트로 확인"
    rationale: "Red 때 harness_probe.not_verified 항목 해소"
  - decision: "Green 1차 실패 3건 → RETRY_SCENARIO 1/3(SC-06 k·l 400) + 하네스 2건(SC-05·13), 재승인 e40db64"
    rationale: "사용자 선택"
  - decision: "리팩토링 — 추가로 하지 않음. writeError·findOwned 는 구현하며 이미 뽑았다. principal 꺼내기는 PLAN 이 정한 AuthController.me 관례"
    rationale: "Step 4"
  - decision: "자체 리뷰 1R RC-1 은 시나리오 밖 변종 — Phase 3 에서 테스트를 만들지 않고 2a 로(사용자 결정, RETRY_SCENARIO 2/3). [design] HEAD 401/403 은 시나리오 결정('GET 외 모든 메서드는 ORGANIZER')과 같아 deferred. [정보] 프론트 X-Write-Key 는 TASK-024 와 같은 배포로"
    rationale: "wf-develop Step 5.5 표"

  - decision: "RC-1 수정 — verifyAccessClaims 에 CANONICAL_USER_ID([1-9][0-9]{0,18}) 를 parseLong 앞에. 공통 지점 1곳(verifyAccessToken 은 위임)"
    rationale: "자체 리뷰 2R: 결함 없음, 같은 카테고리 전수 확인"

attempts:
  - round: "green 1"
    result: "184 중 3 실패 — SC-06 k·l 400, SC-05 FK(정리 순서), SC-13 401(토큰 만료) → 2a·2b 왕복"
  - round: "green 2"
    result: "184/184"
  - round: "pre_review 1"
    result: "[correctness] 1 (RC-1, 변종 4 reproduced) · [design] 1 deferred · 정보 1"
  - round: "green 3"
    result: "RETRY 2/3 재승인 뒤 sub 정규형 검사 — 184/184"
  - round: "pre_review 2"
    result: "결함 없음 — RC-1 reproduced 4건 해소, not_probed 변종은 Phase 4 로"
---

# CP-3.4 Dev — TASK-022

자체 리뷰 1R root_causes (원문 — 2R 원문은 DEV_TASK-022.json 의 pre_review.root_causes):

```json
{ "root_causes": [
  { "id": "RC-1", "summary": "sub 의 정수 판정을 Long.parseLong 에 위임해 정규형이 아닌 표기(부호·앞자리 0·유니코드 숫자)도 같은 사용자로 받아들인다",
    "found_in_round": 1,
    "variants": [
      { "case": "sub \"+7\"", "origin": "found", "probe": "reproduced" },
      { "case": "sub \"07\"", "origin": "predicted", "probe": "reproduced" },
      { "case": "sub \"٧\" (유니코드 숫자)", "origin": "predicted", "probe": "reproduced" },
      { "case": "sub \"-7\" (음수 userId)", "origin": "predicted", "probe": "reproduced" },
      { "case": "sub \" 7\" (공백)", "origin": "predicted", "probe": "not_reproduced" },
      { "case": "sub \"9223372036854775808\" (long 초과)", "origin": "predicted", "probe": "not_reproduced" },
      { "case": "sub 숫자 7 / 이스케이프 \"\\u0037\"", "origin": "predicted", "probe": "not_reproduced" },
      { "case": "role 대소문자·공백·배열·null·중복 키·이스케이프", "origin": "predicted", "probe": "not_reproduced" }
    ] } ] }
```
