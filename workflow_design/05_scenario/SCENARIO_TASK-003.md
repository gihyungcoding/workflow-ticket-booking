# TASK-003 시나리오

총 7건 — happy 5 / error 1 / regression 1. 이 태스크의 complexity level은
"단순"(tasks.json meta.complexity, 기준선 2~3건)이지만, `acceptance_criteria`
7건이 이미 서로 독립적으로 깨질 수 있는 코드 경로다(테마 팔레트 값, 목록/상세
각각의 서체 적용, 상태 문구, `index.html` 메타, 서체 폴백 체인, 기존 회귀) —
하나를 고쳐도 나머지가 통과하면서 그것만 실패할 수 있어 인위적으로 줄이지
않았다.

## SC-01 (happy) 테마가 토큰 값으로 초기화된다

- **Given** `frontend/src/theme/index.ts` 가 `docs/product/design-tokens.css` 의
  값으로 MUI 테마를 만든다
- **When** 이 테마 객체의 `palette` 를 확인한다
- **Then** `palette.success.main` 이 `'#0E6E52'` 다
- **And** `palette.background.default` 가 `'#F7F8F9'` 다
- **And** `palette.background.paper` 가 `'#FFFFFF'` 다
- **And** `palette.text.primary` 가 `'#1A2027'` 다
- **And** `shape.borderRadius` 가 `2` 다
- **And** `shadows` 배열의 모든 값이 `'none'` 이다

covers: 테마가 design-tokens.css 의 값으로 초기화된다 — python3 scripts/check_architecture.py --id DESIGN-001 이 위반 0건
flow: F1

## SC-02 (happy) 목록 카드의 공연명에 Noto Serif KR이 적용된다

- **Given** `PerformanceListPage` 가 공연 1건("재즈의 밤")을 렌더한다
- **When** 카드 안의 공연명 텍스트 요소의 스타일을 확인한다
- **Then** 그 요소의 `font-family` 에 `'Noto Serif KR'` 이 포함된다
- **And** 같은 카드의 장소 텍스트 요소의 `font-family` 는 `'IBM Plex Sans KR'`
  이다(`'Noto Serif KR'` 이 아니다)

covers: 공연명이 Noto Serif KR, 나머지 텍스트가 IBM Plex Sans KR 로 렌더링되고 두 서체가 실제로 로드된다
flow: F2

## SC-03 (happy) 상세 화면의 공연명에 Noto Serif KR이 적용된다

- **Given** `PerformanceDetailPage` 가 공연 1건("재즈의 밤")을 렌더한다
- **When** 공연명 텍스트 요소의 스타일을 확인한다
- **Then** 그 요소의 `font-family` 에 `'Noto Serif KR'` 이 포함된다
- **And** 같은 화면의 장소 텍스트 요소의 `font-family` 는 `'IBM Plex Sans KR'`
  이다(`'Noto Serif KR'` 이 아니다)

covers: 공연명이 Noto Serif KR, 나머지 텍스트가 IBM Plex Sans KR 로 렌더링되고 두 서체가 실제로 로드된다
flow: F2

## SC-04 (happy) 상태 문구가 의미 매핑대로 표시된다

- **Given** `StatusBadge` 에 `UPCOMING`·`OPEN`·`SOLD_OUT`·`CLOSED`·`CANCELLED`
  각각을 준다
- **When** 다섯 개를 각각 렌더한다
- **Then** `UPCOMING` 은 "예매예정"으로 보인다
- **And** `OPEN` 은 "예매가능"으로 보인다
- **And** `SOLD_OUT` 은 "매진"으로 보인다
- **And** `CLOSED` 는 "예매마감"으로 보인다
- **And** `CANCELLED` 는 "공연취소"로 보인다

covers: 상태 문구가 예매가능·예매예정·매진·예매마감·공연취소로 표시된다 (design.md §5 의미 매핑)
flow: F3

## SC-05 (happy) index.html이 한국어 lang·실제 title·웹폰트 로드 지시를 갖는다

- **Given** `frontend/index.html` 파일이 있다
- **When** 그 문서의 `<html>` 태그·`<title>`·`<link>` 목록을 확인한다
- **Then** `lang` 속성이 `"ko"` 다
- **And** `<title>` 내용이 `"frontend"` 가 아니다
- **And** `fonts.googleapis.com` 을 가리키는 `<link rel="stylesheet">` 가 있다
- **And** 그 `<link>` 의 `href` 에 `display=swap` 이 포함된다

covers: index.html 이 lang="ko" 이고 title 이 템플릿 기본값(frontend)이 아니다
flow: F4

## SC-06 (error) 서체 로드가 실패해도 한글이 시스템 폰트로 표시된다

- **Given** Noto Serif KR·IBM Plex Sans KR 웹폰트 요청이 전부 실패한다(네트워크 차단)
- **When** 관객이 공연 목록 화면을 연다
- **Then** 공연명과 본문 텍스트의 한글 음절이 정상적인 글자로 보인다(빈
  사각형(tofu) `□` 이나 대체 불가 문자로 보이지 않는다) — 시스템 폰트로의
  폴백을 브라우저 개발자도구에서 눈으로 판정한다

covers: 오류: 서체 로드가 실패해도 한글이 시스템 폰트로 읽힌다 (font-family 폴백 체인)
flow: F5

## SC-07 (regression) 기존 화면 동작이 그대로 유지된다

- **Given** TASK-002가 만든 기존 테스트 6건(`PerformanceListPage.test.tsx` 4건,
  `PerformanceDetailPage.test.tsx` 2건)이 있다 — 그중 하나는 목록 API 실패 시
  오류 메시지와 [다시 시도] 버튼을 검증한다
- **When** 이 태스크의 변경(테마 주입·서체 적용·상태 문구·index.html 메타)을
  포함한 상태로 `npm test` 를 실행한다
- **Then** 6건 모두 통과한다(목록 오류 시나리오 포함)

covers: 오류: 목록 API 호출이 실패하면 기존과 동일하게 오류 안내와 다시 시도 버튼이 표시된다; 기존 프론트 테스트 6건이 모두 통과한다 — npm test
flow: F6, F7

---

## Red 처리 방식에 대한 메모

**SC-06** — "서체 로드 실패"는 jsdom에서 재현할 수 없다(폰트 네트워크 요청
자체가 jsdom 환경에는 없다). 새 vitest 테스트를 만들지 않는다. Phase 4에서
브라우저 devtools로 웹폰트 요청을 실제로 차단한 뒤 화면을 열어 한글이
깨지지 않는지 확인한다(TASK-002 Phase 4의 실브라우저 검증과 동일한 방식).
같은 이유로 완료 조건(2)의 "두 서체가 실제로 로드된다"(로드 **성공** 여부)도
Phase 4의 실브라우저 확인 몫이다 — SC-02·SC-03·SC-05는 "올바른 서체가
지정돼 있다/로드 지시가 있다"까지만 자동 테스트로 확인한다.

**SC-07** — 이미 통과하고 있는 기존 테스트가 계속 통과하는가를 묻는 순수
회귀 시나리오라, 이 태스크만으로는 애초에 Red 상태를 만들 수 없다(다른
시나리오를 구현하다 실수로 깨뜨리지 않는 한). Phase 2b는 SC-07에 대해 **새
테스트 함수를 추가하지 않고**, 기존 6개 함수를 그대로 재실행해 통과를
확인하는 방식으로 처리한다.

정리하면 이번 태스크에서 **새로 작성하는 Red 테스트는 SC-01~05 다섯 건**
이고, SC-06·SC-07은 각각 Phase 4 실브라우저 확인·기존 테스트 재실행으로
처리한다 — `TEST_TASK-003.json` 에 이 구분을 명시한다.

## 독립검증 반영 (VALIDATION_TASK-003.json attempt 2, V4/V5/V8)

- SC-01: `shape.borderRadius`·`shadows` 단언을 추가했다(V8 — Plan outputs가
  선언한 테마 표면 중 palette만 검증되고 있었음)
- SC-02/SC-03: 장소 텍스트의 서체를 "포함되지 않는다"는 부정 단언에서
  "IBM Plex Sans KR 이다"는 긍정 단언으로 바꿨다(V5 — 부정 단언만으로는
  아무 서체나 통과했음)
- SC-05: Google Fonts `<link>` 의 `href` 에 `display=swap` 단언을 추가했다
  (V8 — Plan inputs의 font-display=swap 제약이 검증되지 않고 있었음)
- SC-06: "깨지지 않는다"는 표현에 tofu(`□`) 용어와 "브라우저 개발자도구에서
  눈으로 판정"이라는 구체적 판정 방법을 덧붙였다(V5 — 완전히 객관적인
  단언으로 바꿀 수는 없는 성격이지만 판정 기준을 최대한 구체화함)
- flow 필드 타입을 SC-01~07 전부 배열로 통일했다(V4 — SC-07만 배열이라
  파서가 타입을 하나로 가정하면 깨질 위험이 있었음)
- 반영하지 않은 경고: (V6) SC-01/02/03/05의 When이 관찰 표현인 것 — 테마
  객체·정적 파일처럼 사용자 행위가 없는 대상을 테스트할 때는 "확인한다"가
  자연스러운 When이라고 판단해 유지. (V7) SC-01이 MUI 객체 내부 경로를
  단언하는 것 — Plan이 "MUI 테마 객체"를 산출물로 직접 명시해 계약
  표면이라고 보고 유지

## 생략 이유

- STATUS_COLOR(배지 색 매핑)·`--color-ink-faint`(물러난 상태 텍스트)에 대한
  시나리오: `PLAN_TASK-003.json` 의 unresolved에 이미 기록된 대로 이번 태스크
  범위 밖(색 구조 재설계는 후속 태스크)
- DESIGN-003(스켈레톤 매직 넘버) 해소 시나리오: tasks.json 설명이 명시적으로
  범위 밖으로 뺐다(product.md §7 데이터 모델 반영 후 목록이 재설계되면 버려짐)
