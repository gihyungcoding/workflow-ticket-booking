---
status: ARCHIVED
saved_at: 2026-11-11T00:00:00Z
summary: "Green 180/180, spotless 0. 자체 리뷰 2R — RC-1 변종 5건 수정(cd2df9f), 같은 뿌리 [correctness] 2건 미해결(호스트 ':' 여럿·비 ASCII 포트 숫자)로 Phase 4 에 넘김."

next_steps:
  - "Phase 4 — wf-verify. 이전 라운드 root_causes(RC-1)와 pre_review.unresolved 2건을 리뷰어에 넘긴다"

decisions:
  - decision: "구현 순서: V6 → 엔티티 제약 → TimeController·SecurityConfig → DTO(ZoneRequest·StrictStringDeserializer) → 예외·핸들러 → placeZones·sectionsOf·getNextOpening"
    rationale: "스키마부터 바깥으로"
  - decision: "V6 와 PerformanceZone 에 ON DELETE CASCADE — feature §3-1 SQL 과 다름 (scope_deviation)"
    rationale: "등록이 구역 행을 만들면서 승인 테스트 AuthRegressionApiTest 의 정리(seat → performance deleteAll)가 FK 위반. 구역은 공연에 딸린 값. 리뷰어: 좌석 FK 는 cascade 가 없어 좌석 있는 공연 삭제는 여전히 막힌다 — 위험 낮음"
  - decision: "승인 테스트 수정 1건 — UsersHandleMigrationTest SC-13 flyway(null) → flyway(\"5\"), 단언 그대로. 사용자 승인(AskUserQuestion 2026-11-11)"
    rationale: "최신 버전 == 5 단언은 V6 가 생기면 반드시 깨진다. PLAN 이 놓친 파급 — 회고 대상"
  - decision: "리팩토링 — 하지 않음(1차). placeZones·sectionsOf 는 이미 한 일만 한다"
    rationale: "Step 4"
  - decision: "자체 리뷰 RC-1 은 시나리오에 없는 동작이라 Phase 3 에서 테스트를 만들지 않고 2a 로 — 사용자 결정 '관대하게': 밑줄·비 ASCII 호스트 허용, 호스트 %XX·빈 userinfo·빈 포트 거부, IPv6·userinfo+포트 허용"
    rationale: "wf-develop Step 5.5 표 — 변종 전부 한 번에"

  - decision: "자체 리뷰 2R 뒤 남은 [correctness] 2건은 고치지 않고 pre_review.unresolved — 2라운드 상한(wf-develop Step 5.5)"
    rationale: "판정은 Phase 4"

attempts:
  - round: "pre_review 1"
    result: "[correctness] 1 (RC-1, 변종 5 reproduced) · [test] 1 — 같은 뿌리"
  - round: "pre_review 2"
    result: "RC-1 변종 5 수정 확인 · 새 [correctness] 2 (unresolved) · 판단 요청 1 (deferred)"
---
