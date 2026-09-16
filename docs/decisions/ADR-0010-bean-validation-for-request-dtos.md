# ADR-0010: 요청 DTO의 필드 형태 검증에 Bean Validation을 쓴다

- **상태**: 채택됨
- **날짜**: 2026-09-16
- **관련**: `TASK-004`, `TASK-006`, `docs/architecture/architecture.md` §2·§3, ADR-0003

---

## 맥락

TASK-004(공연 등록/수정/취소 API)는 Phase 4 검증에서 세 라운드 연속 FAIL/WARN을
받았다. 세 결함 모두 근본 원인이 같았다 — 서비스 계층에 검증 지점(`validateRequired`,
`validateSection`)은 있었지만 새 필드나 새 케이스가 추가될 때 그 지점에 반영되지
않았다.

- attempt 1: 필수 필드(title/venue/startAt/openAt/closeAt) 누락 시 500
- attempt 2: title/venue 200자 초과, `sections` 배열의 null 원소 시 500
- attempt 3(WARN, TASK-006으로 추적 중): `price`/`seatsPerRow`에 소수를 보내면
  Jackson이 정수로 절삭해 "0 이상" 가드를 우회

`backend/build.gradle`에는 `spring-boot-starter-validation` 의존성이 이미 선언돼
있었지만 프로젝트 전체에 `jakarta.validation`/`@Valid` 사용처는 0건이었다 — 검증
수단은 갖춰져 있었는데 쓰이지 않은 상태로 세 번의 결함이 났다.

한편 ADR-0003(계층형 아키텍처)의 `ARCH-002` 제약은 Service 계층이 `api.*`
패키지를 import하지 못하게 막는다. 검증 방식을 바꾸더라도 이 경계는 유지해야 한다.

## 검토한 선택지

### A. 수동 검증(예외-당-규칙 패턴)을 유지하고 프로세스로 누락을 막는다

- 장점: 기존 코드(`EmptySectionsException` 등)와 스타일이 일관된다. 추가
  의존성이나 애노테이션 학습이 필요 없다.
- 단점: "새 필드를 추가할 때마다 검증 지점에 반영한다"는 것이 사람의 기억에
  의존하는 절차다. 이미 같은 방식으로 세 번 실패했다 — 코드 리뷰나 체크리스트를
  더 촘촘히 하는 것으로는 구조적 해법이 되지 않는다는 것이 실증됐다.

### B. Bean Validation(`jakarta.validation`)을 DTO에 도입한다

- 장점: `@NotBlank`, `@Size(max=200)`, `@Min(0)` 같은 제약이 필드 옆에 선언으로
  남는다. 새 필드를 추가하면서 제약을 깜빡해도 "이 필드엔 왜 애노테이션이
  없지?"가 리뷰에서 바로 보인다 — 지금까지처럼 검증 자체가 없어도 티가 안 나는
  구조와 다르다. 이미 의존성이 있어 추가 비용이 없다. `MethodArgumentNotValidException`
  핸들러 하나로 모든 DTO의 형태 오류를 동일한 `ErrorResponse{code,message}`로
  응답할 수 있어, TASK-004 attempt 2에서 지적된 "역직렬화 실패 시 빈 body" 문제도
  같은 자리에서 해소된다.
- 단점: `List<SectionRequest>` 원소별 검증(`@Valid` 전파), 배열 원소 null 처리
  등 컬렉션 검증은 별도로 익혀야 한다. 여러 필드·DB 상태를 엮는 규칙(시각 순서,
  좌석 상한, 행 범위 겹침)은 애노테이션으로 표현할 수 없어 이 부분은 계속
  Service 계층 수동 검증으로 남는다 — 검증 방식이 두 갈래로 나뉜다.

### C. 자체 검증 프레임워크를 만든다

- 장점: 프로젝트 관례에 완전히 맞출 수 있다.
- 단점: 이미 검증된 표준 라이브러리가 의존성으로도 존재하는데 그걸 두고 새로
  만드는 것은 1인 개발(product.md §6)이 피해야 할 유지보수 부담이다. 바로 기각.

## 결정

**B — Bean Validation을 요청 DTO(`api.dto` 패키지)의 필드 형태 검증에 도입한다.**
여러 필드·DB 상태를 엮는 도메인 규칙(시각 순서, 좌석 상한, 행 범위 겹침 등)은
계속 Service 계층 수동 검증으로 남긴다.

### 이유

A(수동 검증 유지)는 이미 같은 실패를 세 번 반복해 프로세스 강화만으로는 막히지
않는다는 것이 증명됐다. C(자체 프레임워크)는 이미 있는 표준 도구를 두고 새로
만드는 것이라 1인 개발 제약과 맞지 않는다.

B를 선택한 결정적 이유는 "검증 누락"의 성격이 달라진다는 점이다 — 수동 검증에서는
필드를 추가하면서 검증을 빠뜨려도 컴파일도 되고 리뷰에서도 눈에 잘 안 띈다(코드가
그냥 없을 뿐이다). Bean Validation에서는 제약이 필드 옆 애노테이션으로 "있어야
할 자리"가 생기므로, 빠뜨리면 그 필드만 애노테이션이 없는 것이 시각적으로
도드라진다. 세 라운드 모두 code-reviewer의 실제 실행 검증이 아니었다면 못 잡았을
결함이었는데, 이 결정은 애초에 그런 결함이 나기 어려운 구조를 만드는 것이다.

`ARCH-002`와는 충돌하지 않는다 — 애노테이션은 `api.dto` 패키지의 DTO 클래스
자체에 선언되고, Spring이 Controller 경계(`@Valid @RequestBody`)에서 검증을
수행하므로 Service가 `api.dto`를 알 필요가 없다. 지금처럼 Controller가
`SectionSpec` 같은 service 전용 타입으로 변환해 넘기는 흐름은 그대로 유지된다.

## 결과

- 좋아지는 것: 필드 형태 제약(필수/길이/범위/형식)이 선언적으로 코드에 남아
  새 필드 추가 시 빠뜨리기 어려워진다. `HttpMessageNotReadableException`류
  역직렬화 실패와 Bean Validation 실패를 한 핸들러(`INVALID_REQUEST`)로 묶을
  수 있어 TASK-004에서 지적된 "빈 body 400" 문제도 같이 정리된다.
- 감수하는 것: 검증 방식이 두 갈래(DTO 형태 = Bean Validation, 도메인 규칙 =
  수동)로 나뉜다. 어떤 제약이 어느 쪽에 속하는지 팀(1인이어도 미래의 자신 포함)이
  판단 기준을 알아야 한다 — 기준은 "이 필드 하나만 보고 판단 가능한가"(Bean
  Validation) vs "다른 필드나 DB 상태를 함께 봐야 하는가"(수동). 기존 DTO
  (`RegisterPerformanceRequest`, `SectionRequest`, `UpdatePerformanceRequest` 등)에
  애노테이션을 붙이고 대응하는 수동 검증 코드를 걷어내는 마이그레이션 작업이
  별도로 필요하다 — 이 ADR은 방향만 정하며, 마이그레이션은 후속 태스크에서 한다.
- 되돌리려면: DTO의 애노테이션과 Controller의 `@Valid`를 제거하고 대응하는
  수동 검증을 Service에 다시 쓴다. 애노테이션이 필드 단위로 국소화돼 있어
  DTO별로 점진적으로 되돌릴 수 있다.

## 검사 가능한 제약

- 후보 있음, 지금은 추가하지 않음 — "`@RequestBody` 파라미터에는 `@Valid`가
  있어야 한다"는 규칙을 `constraints.yaml`에 넣을 수 있지만, 아직 어떤 DTO도
  마이그레이션되지 않아 지금 추가하면(error) 기존 컨트롤러 전부가 즉시 위반으로
  잡힌다. 마이그레이션 태스크가 생기면 그 안에서 `architecture-doc` 스킬로
  추가하고 이 ADR에 제약 ID를 채운다.
