---
status: ACTIVE
saved_at: 2026-10-15T00:00:00Z
summary: "PLAN_TASK-015.json 작성 완료 — route Frontend, flows 5건이 AC 8건을 전부 덮음. EXIT GATE 통과."

next_steps:
  - "Phase 2a — wf-scenario 로 시나리오 설계 (쓰기 거부 안내 문구·재시도 버튼 유무, PATCH 포함 여부 확정)"

decisions:
  - decision: "공통 http.ts(apiUrl·request)를 신규로 만들고 performances.ts 의 fetch 5곳을 교체"
    rationale: "주소 접두·헤더 부착 로직이 5곳에 흩어지면 누락 위험. 환경변수는 호출 시점에 읽어 vi.stubEnv 로 검증 가능하게 한다"
  - decision: "401 안내는 새 오류 클래스 없이 기존 ApiError(code=WRITE_KEY_INVALID)로 판별"
    rationale: "throwIfApiError 가 이미 바디의 code 를 ApiError 로 변환한다"
  - decision: "쓰기 메서드 집합은 서버와 동일하게 POST·PUT·PATCH·DELETE"
    rationale: "AC 는 PATCH 를 적지 않았으나 서버 가드(ADR-0016)와 어긋나면 안 된다. 범위 확대가 아니라 정합"

attempts: []
---
