---
status: ACTIVE
saved_at: 2026-10-14T04:00:00Z
summary: "시나리오 23건 전부 테스트 함수로 옮김 — 19건 Red(실제 실행 확인), 4건 already_passing(SC-03/04/08/11). 기존 테스트 66건 전부 통과. HITL#2 승인됨."

next_steps:
  - "HITL#2 승인 완료 (23건: 19 red + 4 already_passing)"
  - "Phase 3(Green) — wf-develop 로 구현"

decisions:
  - decision: "CorsFilter/WriteGuardFilter 를 Spring 빈으로 등록하지 않고 순수 자바 클래스로만 둠(Red 단계)"
    rationale: "빈으로 등록하면 doFilter() 의 무조건 throw 가 전체 요청 경로를 막아 기존 테스트 66건이 모두 깨진다. 필터 와이어링(CorsConfig/FilterRegistrationBean)은 Phase 3 로 미룬다"
  - decision: "CorsFilter.fromCommaSeparated(String)/WriteGuardFilter.fromEnv(String,String) 정적 팩토리를 스켈레톤에 추가"
    rationale: "쉼표 목록 파싱(SC-14/19/20)과 bool 파싱(SC-21/24)이 필터 생성자 바깥의 책임이라, 이를 단위 테스트하려면 별도 진입점이 필요했다"
  - decision: "SC-06(메서드×키상태 8조합), SC-07(키 미설정/공백 2종)을 각각 단일 테스트 함수 내 반복/표로 구현"
    rationale: "coverage-policy §2-1 표 방식 — 시나리오 수를 불리지 않으면서 전수 확인"
  - decision: "SC-04/08/03/11을 already_passing으로 분류"
    rationale: "필터 미등록 상태에서 쓰기가 가드 없이 성공하는 것(SC-04/08)과, 아직 작성되지 않은 코드가 제약을 위반할 수 없는 것(SC-03 와일드카드, SC-11 DB 하드코딩)이 구조적으로 이미 참이다. 실제 실행으로 확인했다(WriteGuardIntegrationTest 2/2, ApplicationPropertiesFileTest 1/1, ArchitectureCheckTest 1/1 모두 green)"
  - decision: "SC-04 테스트 작성 중 openAt 을 과거로 잘못 설정해 409(REGISTRATION_ALREADY_OPEN)로 실패한 버그를 발견·수정"
    rationale: "Red 가 아니라 테스트 코드 결함이었다(Step 3 판정 기준) — openAt 을 미래로 고쳐 재실행해 의도한 already_passing 결과를 확인했다"

attempts: []
---
