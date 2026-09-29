---
task_id: TASK-005
title: "공연 등록/수정 화면"
phase: "5"
phase_name: "Phase 5 - Reflect"
status: DONE
created_at: 2026-09-17
last_updated: 2026-09-18

primary_category: Frontend
sub_categories: ["organizer"]
target_repo: "."
branch: "feature/task-005-performance-register-edit-screen"

last_checkpoint: CP-5.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-005.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-005.md"
  test: "workflow_design/05_scenario/TEST_TASK-005.json"
  dev: "workflow_design/06_dev/DEV_TASK-005.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-005.json"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-005.json"
---

## 지금 무엇을 하고 있나

완료됐다. Phase 1~5를 모두 거쳤고 HITL 4건 전부 승인받았다. Phase 4는
2라운드(1차 FAIL → Phase 3 롤백·수정 → 2차 PASS)를 거쳤다.
verified_commit = 4fdd53b. 회고 승인, ADR 승격 후보 1건과 규칙 개선안
3건은 사용자 승인에 따라 "승인하고 종료"로 처리 — 별도 후속 작업 없이
제안으로만 기록됨.

## 다음 한 걸음

`/wf-ship` 으로 머지를 준비한다.

## 알아둬야 할 것

- 요구사항 문서: `docs/product/features/performance-registration.md`
  (Task B, §1-2·2·4·5·6)
- 인터랙티브 좌석 맵 드래그 편집기는 범위 밖(§2-2, §7)
- `sections` 는 `GET /api/performances/{id}`(단일 상세)에서만 채워진다 —
  목록/등록(POST)/수정(PUT)/취소 응답은 `sections=null`이라 키 자체가 없음
  (`@JsonInclude(NON_NULL)`). 수정 폼의 좌석 구성 요약은 반드시 별도 GET으로
  가져온다
- AC에 없는 에러 코드 4건(EMPTY_SECTIONS/INVALID_SECTION/INVALID_REQUEST/
  DUPLICATE_SEAT_RANGE)과 수정 화면의 404는 전용 UI를 만들지 않고 AC5
  실패 배너로 폴백 처리하기로 CP-1.3에서 결정함 — Phase 2a 시나리오도 이
  경계를 따른다
- 검증 오류 매핑: `INVALID_TIME_ORDER`/`SEAT_LIMIT_EXCEEDED`(400, 인라인),
  `REGISTRATION_ALREADY_OPEN`(409, 수정 전용 안내), 나머지는 일반 실패 배너
- 취소는 확인 다이얼로그 → `POST /api/performances/{id}/cancel` (멱등)
- 구현 대상 경로: `frontend/src/api/performances.ts`(확장),
  `frontend/src/pages/PerformanceRegisterPage.tsx`(신규),
  `frontend/src/pages/PerformanceEditPage.tsx`(신규),
  `frontend/src/App.tsx`(라우트 2건 추가 — PLAN에서 새로 발견, 요구사항
  문서 §6에는 없음)
- 새 npm 의존성(폼 라이브러리 등)을 도입하지 않는다 — 기존 페이지처럼
  useState로 직접 폼 상태 관리 (CP-1.3 결정)
- UI 라이브러리 MUI(ADR-0002), 디자인 정본 `docs/product/design.md` ·
  `design-tokens.css`. 재사용: PerformanceListPage의 Alert+[다시 시도]
  패턴(L53-65), PerformanceDetailPage의 상태 유니온+useEffect 패턴
- DESIGN-003(warn) 기존 위반 2건 있음(PerformanceDetailPage/ListPage의
  height={n}) — 새 코드에서 반복하지 않도록 유의
- SC-03/SC-04 오류 문구("예매 오픈·마감·공연 일시 순서가 올바르지 않습니다",
  "좌석 총수는 5,000석을 넘을 수 없습니다")는 백엔드 메시지를 그대로 쓰지
  않고 Phase 2a에서 새로 정한 사용자 지향 문구 — 프론트가 code로 분기해서
  표시해야 한다(PLAN design.outputs에 반영됨)
- App.tsx에 정적 라우트(/performances/new)와 기존 동적 라우트
  (/performances/:id)가 공존한다 — React Router v7은 정적 세그먼트를
  항상 우선 매칭하므로 선언 순서 무관하게 안전하지만, SC-10(regression)이
  이를 실제로 검증한다
- Red 단계에서 확정한 UI 계약(Phase 3에서 그대로 구현) — TextField label:
  "공연명"/"장소"/"공연일시"/"오픈"/"마감"(등록·수정 공통), 구역 필드:
  "등급"/"가격"/"시작 행"/"종료 행"/"행당 좌석수". 버튼: "구역 추가"/"등록"
  (등록 폼), "저장"/"공연 취소"(수정 폼), 다이얼로그 "확인". data-testid:
  `seat-preview`(미리보기, "총 N" + 구역별 "{grade} N" 텍스트),
  `time-fields-error`, `section-card-{index}`, `form-error`(+"다시 시도"
  버튼), `section-summary`(수정 폼 읽기 전용 좌석 요약)
- ApiError(code, message) 클래스로 register/update/cancel 실패를 code
  기반 분기 — INVALID_TIME_ORDER/SEAT_LIMIT_EXCEEDED/
  REGISTRATION_ALREADY_OPEN 외 나머지(그 외 code·일반 Error)는 전부
  form-error 배너로 폴백
- 로컬 개발 환경: node/npm이 기본 PATH에 없음 — 이 세션에서는
  `export PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"` 로 직접
  잡아서 사용함(nvm 셸 함수 자체가 안 잡혀서 `nvm use`가 안 먹음)
- Green 구현 완료: `performances.ts`(register/update/cancel + 공용
  throwIfApiError), `PerformanceRegisterPage.tsx`, `PerformanceEditPage.tsx`
  전체 구현. `frontend/src/utils/datetime.ts` 신규 추가(scope_deviation,
  DEV_TASK-005.json에 근거 기록) — 두 폼이 공유하는 datetime-local↔ISO 변환
- SEAT_LIMIT_EXCEEDED 오류는 항상 마지막 구역 카드에 표시하는 것으로 최소
  구현(다중 구역 시 "어느 구역 탓인지"는 서버가 알려주지 않음 — CP-3.2 결정)
