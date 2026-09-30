# TASK-007 시나리오

## SC-01 (happy) 유효한 등록 요청은 여전히 201로 성공한다

- **Given** title/venue/startAt/openAt/closeAt가 모두 유효하다
- **And** sections에 구역 하나(grade="VIP", price=100, rowStart="A", rowEnd="A", seatsPerRow=10)가 있다
- **When** POST /api/performances 를 호출한다
- **Then** 201이 반환된다
- **And** 응답의 totalSeats는 10이다

covers: 유효한 등록/수정 요청은 마이그레이션 이전과 동일하게 201/200으로 성공한다 (기존 23개 시나리오 회귀 없음)
flow: F1

## SC-02 (regression) title이 비어 있으면 400 INVALID_REQUEST가 반환된다

- **Given** 등록 요청의 title이 빈 문자열이다
- **And** 나머지 필드(venue/시각/sections)는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST이다

covers: title/venue가 없거나 200자를 넘으면 400과 INVALID_REQUEST가 반환된다 (Bean Validation 경로)
flow: F2

## SC-03 (regression) title이 200자를 넘으면 400 INVALID_REQUEST가 반환된다

- **Given** 등록 요청의 title이 201자이다
- **And** 나머지 필드(venue/시각/sections)는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST이다

covers: title/venue가 없거나 200자를 넘으면 400과 INVALID_REQUEST가 반환된다 (Bean Validation 경로)
flow: F2

## SC-04 (regression) 구역의 grade가 20자를 넘으면 400 INVALID_SECTION이 반환된다

- **Given** 등록 요청의 sections에 구역 하나가 있고 grade가 21자이다
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION이다 (INVALID_REQUEST가 아니다)

covers: section의 grade/rowStart/rowEnd/seatsPerRow가 없거나 형식에 맞지 않으면 400과 INVALID_SECTION이 반환된다 (Bean Validation 경로)
flow: F3

## SC-05 (regression) 구역의 rowStart가 형식에 맞지 않으면 400 INVALID_SECTION이 반환된다

- **Given** 등록 요청의 sections에 구역 하나가 있고 rowStart가 빈 문자열이다
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION이다

covers: section의 grade/rowStart/rowEnd/seatsPerRow가 없거나 형식에 맞지 않으면 400과 INVALID_SECTION이 반환된다 (Bean Validation 경로)
flow: F3

## SC-06 (regression) 구역의 seatsPerRow 필드 자체가 없으면 400 INVALID_SECTION이 반환된다

- **Given** 등록 요청의 sections에 구역 하나가 있고 seatsPerRow 필드가 JSON에 없다(null)
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION이다

covers: section의 grade/rowStart/rowEnd/seatsPerRow가 없거나 형식에 맞지 않으면 400과 INVALID_SECTION이 반환된다 (Bean Validation 경로)
flow: F3

## SC-07 (error) 요청 본문이 파싱 불가능한 JSON이면 400 INVALID_REQUEST가 반환된다

- **Given** 요청 본문이 올바른 JSON 구문이 아니다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST이다
- **And** 응답 본문에 code와 message 필드가 있다

covers: 요청 본문이 파싱 불가능한 JSON이거나 필드 타입이 맞지 않으면(HttpMessageNotReadableException) 빈 body가 아니라 400과 INVALID_REQUEST(ErrorResponse{code,message})가 반환된다
flow: F4

## SC-08 (regression) 구역의 seatsPerRow가 0이면 이관 이후에도 여전히 400 INVALID_SECTION으로 거부된다

- **Given** 등록 요청의 sections에 구역 하나가 있고 seatsPerRow가 0이다(필드는 존재하지만 1 미만)
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION이다

covers: 유효한 등록/수정 요청은 마이그레이션 이전과 동일하게 201/200으로 성공한다 (기존 23개 시나리오 회귀 없음)
flow: F1

## SC-09 (regression) 수정 요청의 title이 200자를 넘으면 400 INVALID_REQUEST가 반환된다

- **Given** now보다 openAt이 늦은(수정 가능한) 공연이 등록되어 있다
- **And** 수정 요청의 title이 201자이다
- **When** PUT /api/performances/{id} 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST이다

covers: title/venue가 없거나 200자를 넘으면 400과 INVALID_REQUEST가 반환된다 (Bean Validation 경로)
flow: F2

## SC-10 (regression) 구역 배열에 null 원소가 있으면 이관 이후에도 여전히 400 INVALID_SECTION으로 거부된다

- **Given** 등록 요청의 sections 배열에 null 원소가 하나 있다([null])
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION이다

covers: 유효한 등록/수정 요청은 마이그레이션 이전과 동일하게 201/200으로 성공한다 (기존 23개 시나리오 회귀 없음)
flow: F1

## SC-11 (regression) 최상위 필드와 section 필드를 동시에 위반하면 INVALID_REQUEST가 우선한다

- **Given** 등록 요청의 title이 빈 문자열이다
- **And** 동시에 sections의 구역 하나가 grade 21자로 형식도 어긴다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST이다 (INVALID_SECTION이 아니다)

covers: title/venue가 없거나 200자를 넘으면 400과 INVALID_REQUEST가 반환된다 (Bean Validation 경로)
flow: F2

---

## 커버리지 결정 노트

### 동시 위반 시 에러 코드 우선순위 (3차 검증 V10 → 사람 확인)

3차 독립검증(V10)이 발견한 것: title이 비어있으면서 동시에 section의 grade도
너무 긴 것처럼 **최상위 필드와 section 필드를 함께 위반하는 요청**의 에러
코드가 이 마이그레이션으로 실제로 바뀔 수 있었다 — 현재 코드는 Service의
검증 순서(`validateRequired`가 `validateSection`보다 먼저 실행) 때문에 항상
INVALID_REQUEST를 반환하지만, Plan이 애초에 정한 "section 경로 FieldError가
하나라도 있으면 INVALID_SECTION" 규칙을 그대로 구현하면 이 경우 INVALID_SECTION으로
뒤집힌다. 기존 23개 테스트는 전부 단일 위반만 다뤄 이 뒤집힘을 잡을 수 없었다.

사람에게 직접 확인한 결과 **최상위 필드 오류를 우선**하기로 했다(기존 동작
유지). PLAN_TASK-007.json의 F2/F3 steps를 "FieldError 중 'sections'로 시작하지
않는 것이 하나라도 있으면 INVALID_REQUEST, 전부 'sections' 시작일 때만
INVALID_SECTION"으로 명시적으로 갱신했고, 이를 SC-11로 회귀 검증한다.

### 타입 재분류 — 이 태스크에는 "새로 실패하는" 시나리오가 거의 없다

2차 독립검증(V9)에서 지적받고 재확인한 사실: **이 태스크는 행위를 바꾸지 않는
순수 리팩토링에 가깝다.** SC-02~06/08/09/10이 검증하는 모든 입력(title 누락/길이,
grade 길이, rowStart 형식, seatsPerRow 누락/0, sections 배열 null 원소)은
`PerformanceService.validateRequired`/`validateSection`의 **현재 코드가 이미**
정확히 같은 응답(같은 HTTP 상태·같은 에러 코드)을 만든다. Bean Validation을
붙여도 관찰되는 외부 동작은 바뀌지 않는다 — 검증이 일어나는 **위치**만 Service
메서드 내부에서 DTO 애노테이션(+ 잔존한 일부 Service 검증)으로 옮겨질 뿐이다.

그래서 이 시나리오들의 type을 최초에 `error`로 잘못 붙였던 것을 `regression`으로
정정했다. `error`/`happy`라는 type은 "이 태스크로 새로 생기는 동작"을 뜻하지 않고
시나리오의 기능적 성격(거부 경로/정상 경로)을 뜻하지만, Phase 2b가 "실제로 실패하는"
Red 테스트를 요구하는 이상 — 현재 코드에서 이미 통과하는 입력을 새 실패 테스트로
쓸 수는 없다. 이 태스크에서 **진짜로 지금 실패하는 것은 SC-07(HttpMessageNotReadableException
핸들러 부재 — 현재는 ErrorResponse 형식이 아닌 Spring 기본 오류 응답이 반환된다)
하나뿐이다.** Phase 2b는 이 사실을 명시적으로 다뤄야 한다 — SC-07만 신규 Red 테스트로
작성하고, 나머지 regression 시나리오들은 "현재도 통과 + Phase 3 이후에도 계속
통과해야 하는" 캐릭터라이제이션 테스트로 취급하는 것을 제안한다(Phase 2b 스킬의
실제 지침을 따른다).

### 왜 기존 23개를 그대로 옮겨 적지 않고 일부만 새로 썼는가

`PerformanceRegistrationApiTest.java`의 기존 23개(sc01~sc24, sc14 결번)는 코드
변경 없이 그대로 재실행하는 것으로 AC1의 회귀 조항을 검증한다. 이 문서에 새로
추가한 regression 시나리오(SC-02/03/04/05/06/08/09/10)는 "새로운 입력을 찾는 것"이
아니라 — Plan(CP-1.3)이 확정한 **검증 로직이 실제로 이관/잔존하는 경계선**을 각각
정확히 겨냥한 것이다:

- SC-03/04/05 — 각각 title(`@Size`)·grade(`@Size`)·rowStart(`@NotBlank`+`@Pattern`)가
  이번에 새로 Bean Validation으로 옮겨지는 필드다. 이관 후에도 같은 입력이 같은
  결과를 내는지 확인해야 한다
- SC-02/06 — title 빈 문자열, seatsPerRow 누락은 **기존 23개 테스트에 아예 없던
  입력**이다(기존 sc16은 title이 아니라 sections 필드 누락을 테스트한다). 새로
  Bean Validation으로 옮겨지는 코드 경로인데 자동화된 회귀 테스트가 전혀 없었던
  공백을 메운다
- SC-08 — price/seatsPerRow의 "존재 여부"만 이관하고 "하한값"은 Service에 남기는
  split(Plan CP-1.3의 핵심 결정)이 잘못되면 가장 먼저 깨지는 지점이다
  (seatsPerRow=0, 기존 sc18과 동일 입력)
- SC-09 — PUT 경로(UpdatePerformanceRequest, Controller.update)도 대상 파일에
  포함되는데 POST만 검증하면 update()의 `@Valid` 추가가 실제로 동작하는지 아무
  시나리오도 확인하지 못한다(기존 sc24와 동일 입력)
- SC-10 — Plan(PLAN_TASK-007.json `codebase_analysis.unresolved`)이 스스로
  "가장 큰 미확정 위험"으로 지목한 지점이다: `List<@Valid SectionRequest> sections`에
  `@Valid` cascade를 걸어도 **배열 안의 null 원소 자체는 cascade 검증 대상이 아니라서**
  Bean Validation이 절대 잡아주지 않는다 — `PerformanceService.validateSection`의
  `section == null` 체크가 계속 이 역할을 해야 한다. 이관 범위에 포함되지 않는
  코드라 실수로 지워질 위험이 가장 큰 지점(기존 sc23과 동일 입력)이라 2차 검증에서
  누락이 지적된 뒤 추가했다

나머지(sc01~sc24 중 위에 대응하지 않는 항목 — 좌석 생성 로직, 시각 순서,
좌석 상한, 행 범위 겹침, rowStart>rowEnd 역방향 등 이번 태스크가 건드리지 않는
코드)는 재기술 없이 기존 테스트 재실행에 전적으로 위임한다.

### 병합 근거

- **rowStart/rowEnd는 동일한 `@Pattern` 애노테이션을 양쪽에 적용하므로 rowStart
  하나만 대표로 검증한다(SC-05).** rowEnd 전용 시나리오는 같은 코드 경로라
  추가하지 않는다. (2차 검증에서 SC-05가 실제로는 `@NotBlank`(빈 문자열)를
  때리고 `@Pattern` 고유 위반(예: 소문자·2자 이상)은 어느 시나리오도 다루지
  않는다는 지적을 받았다 — 기존 sc19와 동일 입력을 유지하는 것을 우선해 이번에는
  추가하지 않되, Phase 3 구현 시 `@Pattern` 케이스도 함께 통과하는지 눈으로
  확인한다)
- **price의 "존재 여부"(SC-06과 동일한 성격의 @NotNull)는 별도 시나리오로 추가하지
  않는다.** seatsPerRow의 @NotNull(SC-06)과 완전히 동일한 애노테이션 종류·동일한
  FieldError 라우팅 코드 경로를 타므로, 두 필드를 각각 검증해도 서로 독립적으로
  실패할 수 있는 결함이 없다

### AC 커버리지 관련 caveat (2차 검증 V3에서 지적)

- AC2가 언급하는 "venue" 필드는 이 문서의 어느 시나리오도 직접 검증하지 않는다
  (SC-02/03/09 모두 title). venue는 title과 동일한 `@NotBlank`+`@Size(max=200)`를
  받는 동일 코드 경로라 별도 시나리오를 추가하지 않기로 했다 — title에서 실패하는
  결함이 venue만 비껴갈 개연성이 낮다고 판단했다
- AC1의 "200 성공"(수정 성공 경로)은 이 문서에서 직접 단언하지 않는다 — 기존
  sc06(오픈 전 공연 수정 성공)이 코드 변경 없이 재실행되는 것으로 커버된다
