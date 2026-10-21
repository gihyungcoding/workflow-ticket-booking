---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "[4차 재수정] LoginRequest.password 에 NoLoneSurrogate @Pattern 한 줄 추가 — SC-23 Red 2행 Green, 전체 112/112, spotlessCheck 통과. 리팩토링 없음(name 정규식은 정책이 달라 유지). [이전 요약] [3차 재수정] SC-21 Green — email·password·로그인 email 에 짝 없는 서로게이트 거부(@Pattern, NoLoneSurrogate 상수). 전체 111/111, spotlessCheck·아키텍처 통과. [이전] [2차 재진입] 수정 Green — 전체 109/109(SC-20 포함), 변이 3건 사망, spotlessCheck·아키텍처 error 0. name 의 제어 문자·서로게이트 거부(@Pattern), catch 를 UNIQUE(SQLState 23505)로 한정. 테스트 결함 하나 수정(서로게이트 행의 입력이 UTF-8 인코딩에서 '?' 로 바뀌어 서버에 닿지 않음). [이전 재진입 요약] 수정 Green — 전체 108/108(테스트 19 + 기존 89), spotlessCheck·아키텍처 error 0. SC-13 정각·SC-17·SC-18 Red → Green, 변이 3건 사망(exp 없는 토큰 허용 변이는 시나리오가 없어 생존 — 알려진 갭)."

next_steps:
  - "Phase 4 다섯 번째 — wf-verify. PostgreSQL 에서 로그인 password 서로게이트 400·한글 로컬 파트 확인, code-reviewer 에 이전 root_causes 전달. 또 FAIL 이면 Phase 1 상승"
  - "Phase 4 4차(wf-verify) — PostgreSQL 에서 email·password 서로게이트 400, 한글 로컬 파트·U+0085 201 저장값 일치, name·동시 가입 회귀, develop 비교 프로브, code-reviewer 4차(이전 root_causes 지목). 다음 FAIL 은 Phase 1 로 상승"
  - "Phase 4 — wf-verify. docker postgres 로 V3 실제 마이그레이션·가입·로그인 확인(H2 에서는 V3 가 실행되지 않음). code-reviewer 호출 시 JPA 지시 포함"
  - "Ship 에서 JWT_SECRET 이 Render 에 먼저 들어가야 한다는 점을 사람에게 알린다"

decisions:
  - decision: "[2차 재진입] name 에 @Pattern(\\p{Cc}·\\p{Cs} 거부)을 두고 DataIntegrityViolationException catch 는 근본 원인의 SQLState 23505 로 한정"
    rationale: "PostgreSQL 이 NUL 을 거부(22021)해 409 로 오보되던 결함의 뿌리가 'catch 가 모든 무결성 예외를 중복으로 취급' 이었다. 입력은 앞단에서 막고, 그래도 새는 무결성 위반은 409 로 위장하지 않는다. java.sql.SQLException 은 ARCH-003 의 대상이 아니다(hibernate 타입을 서비스가 import 하지 않는다)"
  - decision: "[2차 재진입] SC-20 서로게이트 행의 테스트 결함을 Phase 3 에서 고치고 기록했다"
    rationale: "Java 문자열의 짝 없는 서로게이트는 UTF-8 로 인코딩될 때 '?' 로 바뀌어 서버에 닿지 않는다 — 구현이 맞아도 통과할 수 없던 행이었다. 단언을 약화한 것이 아니라 시나리오가 정한 입력을 실제로 보내게 했고, 변이(서로게이트 거부만 제거 → 그 행만 실패)로 검증했다. 테스트 수정이므로 HITL#3 요약에서 사람에게 알린다"
  - decision: "[재진입] 만료 비교를 라이브러리 기본 검증기 대신 직접 구현(now < exp)"
    rationale: "SC-13 이 정각을 만료로 확정(RFC 7519 §4.1.4). Spring 의 JwtTimestampValidator 는 정각을 유효로 본다. exp 없는 토큰은 만료를 알 수 없어 거부"
  - decision: "[재진입] ERROR 디스패치를 permitAll 로 — REQUEST 디스패치의 인가는 그대로"
    rationale: "sendError 오류가 /error 로 재디스패치될 때 anyRequest().authenticated() 에 걸려 모든 프레임워크 오류가 401 이 됐다. 원래 요청은 REQUEST 디스패치에서 이미 인가를 통과했거나(공연 API) 401 이 났다(허용 목록 밖). 허용 목록 밖 경로는 여전히 401"
  - decision: "[재진입] 이메일 정규화 후 길이를 서비스에서 400 으로 막고, DataIntegrityViolationException catch 는 UNIQUE 경합 전용으로 둔다"
    rationale: "예외 종류만으로 UNIQUE 위반을 구분하려면 hibernate 타입을 서비스가 import 해야 해 ARCH-003 의 취지(영속성 프레임워크 타입 직접 사용 금지)와 어긋난다. 길이·NOT NULL 위반이 앞단에서 막히므로 catch 에 도달하는 것은 UNIQUE 경합뿐"
  - decision: "Jackson 3 에서 enum 숫자 거부는 spring.jackson.datatype.enum.fail-on-numbers-for-enums=true"
    rationale: "처음 쓴 spring.jackson.deserialization.* 는 Jackson 3 에서 EnumFeature 로 옮겨져 컨텍스트가 뜨지 않았다. SC-04 의 role=1 이 201 이던 위험을 설정으로 해결"
  - decision: "이메일 trim 은 DTO 컴팩트 생성자, 소문자 정규화와 저장은 AuthService"
    rationale: "@Email 이 앞뒤 공백을 먼저 거부해 SC-01/03/07 의 공백 행이 400 이 됐다. 검증 전에 걷어내야 한다"
  - decision: "JwtAuthenticationFilter 를 빈으로 등록하지 않고 SecurityConfig 가 체인에 직접 끼운다"
    rationale: "서블릿 필터 빈은 Boot 가 보안 체인과 별도로 한 번 더 등록한다"
  - decision: "JWT 검증 실패는 인증을 비워 두고 체인을 계속한다 — 401 은 EntryPoint 가 낸다"
    rationale: "공연 API(permitAll)는 만료·위조 토큰이 실려 와도 통과해야 한다(SC-15)"
  - decision: "리팩토링은 하지 않았다"
    rationale: "중복처럼 보이는 이메일 trim 의 두 곳은 역할이 다르다(검증 전 공백 제거 vs 저장 표기 통일)"
  - decision: "변이 검증 첫 시도의 사고 — 구현을 먼저 커밋한 뒤 변이"
    rationale: "미커밋 파일에는 git checkout 이 통하지 않고, 커밋된 스켈레톤은 스켈레톤으로 되돌려졌다. 복원 후 105건 통과 확인"

attempts:
  - phase: "3"
    round: "재진입 2 (Phase 4 2차 FAIL 롤백)"
    result: "통과 — 첫 시도에서 AuthService 치환이 포맷된 주석과 어긋나 적용되지 않았고, 서로게이트 행이 테스트 결함으로 실패(1건). 정정 후 109/109"
  - phase: "3"
    round: "재진입 1 (Phase 4 FAIL 롤백)"
    result: "통과 — 첫 시도에서 포맷 변경으로 문자열 치환이 어긋나 적용되지 않았고(테스트 변화 없음 확인) 정정 후 108/108"
  - phase: "3"
    round: 1
    result: "통과"
    reason: "1차 실행 101/105 — 이메일 공백 400, role 숫자 201. 수정 후 컨텍스트 로드 실패(Jackson 속성 이름) → 정정 후 105/105"
---
