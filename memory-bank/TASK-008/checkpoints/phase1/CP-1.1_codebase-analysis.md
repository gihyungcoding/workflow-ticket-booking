---
checkpoint_id: CP-1.1
checkpoint_name: "코드베이스 조사 완료"
task_id: TASK-008
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-17T01:00:00Z
status: ACTIVE

work_summary: "PerformanceResponse/PerformanceService/SeatRepository/Seat 엔티티와 기존 API 테스트 2건을 조사해 좌석 집계 쿼리와 응답 DTO 확장 지점을 확정"

progress:
  completed:
    - "PerformanceService.toResponse()가 PerformanceResponse를 생성하는 유일한 지점이며, getPerformances(목록)/getPerformance(상세)/register/update/cancel 5곳에서 호출됨을 확인"
    - "Seat 엔티티가 이미 grade/price를 컬럼으로 갖고 있어 새 마이그레이션 없이 그룹핑 집계가 가능함을 확인"
    - "SeatRepository가 빈 JpaRepository 인터페이스임을 확인 — @Query 그룹핑 메서드를 바로 추가 가능"
    - "PerformanceApiTest.java, PerformanceRegistrationApiTest.java 의 SpringBootTest+MockMvc+H2 풀스택 테스트 패턴 확인 — Phase 2b가 따를 관례"
    - "architecture.md의 DTO 소유 원칙(응답 DTO는 그 값을 만드는 계층이 소유) 재확인 — Repository는 자신의 프로젝션 인터페이스만, Service는 API 응답 DTO를 소유하도록 설계"
  in_progress: "PLAN_TASK-008.json 작성"
  blocked: []

next_steps:
  - priority: 1
    task: "route 결정 및 CP-1.2 저장"
  - priority: 2
    task: "PLAN_TASK-008.json 작성 및 CP-1.3 저장"

decisions:
  - decision: "register/update/cancel 응답도 sections를 포함한다(목록만 제외)"
    rationale: "이 4개 메서드(상세 포함)는 모두 '단일 공연'을 반환하는 동일한 의미론을 공유하고, feature 문서 §3이 등록 응답을 '공연 상세와 동일한 구조'라고 명시한다. AC는 상세/목록만 명시하지만 등록·수정·취소를 상세와 다르게 만들 근거가 없다"
    alternatives_considered: ["상세 조회(GET /{id})에서만 sections 포함, 나머지는 제외"]
    impact: "PerformanceService.toResponse를 목록용/단일용으로 분기"
  - decision: "sections 필드는 nullable + @JsonInclude(NON_NULL)로 목록 응답에서 키 자체를 생략한다"
    rationale: "AC3 '포함하지 않는다'를 '빈 배열([])'이 아니라 '키 자체 부재'로 해석 — 더 보수적이고 명확한 해석"
    alternatives_considered: ["목록에서 sections: [] 로 빈 배열 반환"]
    impact: "PerformanceResponse 클래스에 Jackson 애노테이션 추가 필요"
  - decision: "집계 프로젝션 인터페이스(SeatSectionCount)는 repository 패키지에, API 응답 DTO(SectionSummaryResponse)는 service 패키지에 분리한다"
    rationale: "architecture.md DTO 소유 원칙과 대칭 — Repository가 Service 패키지 타입을 참조하면 역방향 의존이 된다"
    alternatives_considered: ["SeatRepository의 @Query가 service.SectionSummaryResponse를 직접 생성"]
    impact: "신규 파일 2개(repository/SeatSectionCount.java, service/SectionSummaryResponse.java)"

recovery_prerequisites: []

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend build"
  env_required: []
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceResponse.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/repository/SeatRepository.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - path: "backend/src/main/java/com/example/ticket_booking/domain/Seat.java"
---

## 무엇을 했나

TASK-004가 이미 구현한 PerformanceService/PerformanceResponse/SeatRepository/Seat를
읽고, 구역별 좌석 요약을 추가하는 데 새 마이그레이션이 필요 없음을 확인했다. DTO 소유
원칙에 따라 Repository 프로젝션과 Service 응답 DTO를 분리하기로 했고, 목록 응답에서는
sections를 완전히 생략하는 방향(NON_NULL)으로 정했다.

## 산출물

| 파일 | 역할 |
|---|---|
| (다음 CP에서 생성) `workflow_design/04_plan/PLAN_TASK-008.json` | Phase 1 최종 산출물 |

## 재개 방법

1. 위 decisions를 다시 논의하지 않는다
2. route(Backend)를 CP-1.2로 남긴다
3. PLAN_TASK-008.json을 작성한다
