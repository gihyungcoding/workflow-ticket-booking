---
status: ARCHIVED
saved_at: 2026-11-04T18:00:00Z
summary: "롤백 재작업 Green 164/164, spotless 0, 자체 리뷰 3라운드(RC-2 공통 지점 수정 확인, 결함 0)."

next_steps:
  - "Phase 4 재검증 — RC-1·RC-2 인계, V11 은 범위 밖"

decisions:
  - decision: "구현 순서: V5·엔티티 → HandleGenerator → AuthService → 대기 정보 API → JwtTokenProvider 강화"
    rationale: "Red 실패 원인 순서대로 하나씩"
  - decision: "기존 테스트 7곳 준비 코드에 아이디 인자, AuthApiTest 응답 키에 handle — 사용자 승인 2건"
    rationale: "엔티티·V5 NOT NULL 일치 유지, AC1 과 상충하는 옛 단언 갱신"
  - decision: "UNIQUE 위반 판별은 첫 줄 VALUES 앞의 제약 표시만 본다"
    rationale: "자체 리뷰 RC-1 — 메시지에 입력값·SQL 이 섞인다(H2·PG 실제 메시지로 확인)"
  - decision: "리팩토링 하지 않음"
    rationale: "로직이 각 한 곳에 모여 있고 중복 없음"

attempts:
  - round: 1
    result: "기존 테스트 3건 실패 — SC-13 동시 가입(H2 메시지의 INSERT 문 때문에 오판) 수정, AuthApiTest 2건은 승인 후 단언 갱신"
  - round: 2
    result: "자체 리뷰 1라운드 RC-1 → 테스트 먼저 실패 확인 후 수정, 2라운드 결함 없음"
---

## Phase 4 롤백 재작업 (2026-11-05)
- readPayload 공통 형식 검사(압축 JWS + 중복 키 없는 JSON 객체) — access·pending 함께 (115d0c7)
- 164/164, 자체 리뷰 결함 0, 변이 M4·M5 확인
