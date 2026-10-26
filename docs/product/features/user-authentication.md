# [관객/주최자] 회원가입과 로그인

**상태**: 초안
**최종 갱신**: 2026-10-14

---

## 1. 요구사항

### 1-1. 배경

관객과 주최자를 구분할 방법이 지금 전혀 없다 — 누구나 모든 API를 호출할 수 있다.
[공연 등록](performance-registration.md)(TASK-004)은 "주최자 인증/소유권"을
명시적으로 범위 밖에 둬, 아무나 공연을 등록·수정·취소할 수 있는 상태로 남아 있다.
다음에 만들 좌석 선점 기능도 "누가 선점했는가"를 기록해야 하므로, 이 기능이
선행되어야 한다.

| AS-IS | TO-BE |
|---|---|
| 관객·주최자 구분 없이 누구나 API를 호출한다 | 이메일 또는 소셜 계정(카카오/네이버/구글/애플)으로 가입·로그인해 역할(관객/주최자)을 식별한다 |
| 공연 등록 API에 소유권 개념이 없다 | 로그인한 사용자를 식별할 토큰이 발급되어, 이후 기능이 소유권을 검사할 토대가 생긴다 |
| 신규 서비스에 가입 장벽(이메일+비밀번호 입력)이 있다 | 소셜 로그인으로 가입 마찰을 줄여 초기 사용자 유입을 확보한다 |

**이번 문서의 범위**: 이메일 회원가입/로그인 + 소셜 로그인(카카오/네이버/구글/애플)
+ 인증 컨텍스트 식별까지 다룬다. 공연 등록 API에 소유권 검사(주최자가 자기 공연만
관리)를 붙이는 것은 별도 문서로 분리한다 — TASK-004/005의 "포함하지 않음"을
건드리는 작업이라 범위를 나누는 것이 안전하다(§7).

소셜 로그인 4종을 한 번에 넣기로 하면서 인증 기반 자체를 **Spring Security**로
세운다 — 각 제공자의 authorization code flow, state 검증을 직접 구현하는 대신
Spring Security의 OAuth2 Client가 표준으로 처리하게 한다(§8).

### 1-2. 태스크 분리 (필수)

| Task | 범위 | 산출물 | 포함하지 않음 | 선행 | 추정 | 태스크 ID |
|---|---|---|---|---|---|---|
| A. 백엔드 — 이메일 인증 + Spring Security 기반 | `users` 테이블, BCrypt 해싱, `SecurityFilterChain`(기존 공연 API는 `permitAll` 유지), JWT 발급·검증 필터, `AuthController`(signup/login/me) | `users` 마이그레이션, `User`/`UserRole` 도메인, `UserRepository`, `AuthService`, `JwtTokenProvider`, `SecurityConfig` | 소셜 로그인, 소유권 검사, 역할 변경 | — | 14h | TASK-009 |
| B. 프론트 — 이메일 회원가입/로그인 화면 | 회원가입/로그인 폼, 인증 상태 전역 관리, 헤더 로그인/로그아웃 전환 | `SignUpPage`, `LoginPage`, `AuthContext`, `api/auth.ts` | 소셜 버튼, 보호 라우트 가드 | A | 10h | TASK-010 |
| C. 백엔드 — 소셜 로그인(카카오/네이버/구글) | `social_accounts` 테이블, 제공자별 사용자 정보 정규화, 이메일 일치 시 자동 계정 연결, 신규 가입자는 `pendingToken` 발급 후 역할 선택 대기 | `SocialAccount` 도메인, `CustomOAuth2UserService`, `OAuth2LoginSuccessHandler`, `POST /api/auth/social/complete` | 애플 로그인, 프론트 화면 | A | 16h | TASK-011 |
| D. 백엔드 — 애플 로그인 | ES256 서명 Client Secret 생성, `form_post` 콜백 처리, 최초 1회만 내려오는 이름 캡처 | `AppleClientSecretGenerator`, `CustomOAuth2UserService`의 애플 분기 | 프론트 화면 | C | 12h | TASK-012 |
| E. 프론트 — 소셜 로그인 버튼·역할 선택·콜백 처리 | 4개 제공자 버튼, `/oauth2/redirect` 라우트(토큰/pendingToken 처리), 역할 선택 화면 | `SocialLoginButtons`, `OAuthRedirectPage`, `RoleSelectionPage` | — | C, D | 12h | TASK-013 |

[태스크 생성 2026-10-14]

**분리 이유**: 이메일 인증(A/B)이 전체 인증 체계(Security, JWT)의 토대이므로 먼저
세운다. 표준 OAuth2 제공자(카카오/네이버/구글)는 Spring Security의 OAuth2 Client로
공통 패턴 처리가 가능해 한 태스크(C)로 묶지만, 애플은 서명된 Client Secret과
`form_post` 콜백이라는 별도 구현이 필요해 분리한다(D). 프론트는 이메일 폼(B)과
소셜 버튼/역할 선택/콜백(E)으로 나뉘며, E는 모든 백엔드 제공자 엔드포인트가
준비된 뒤 착수한다.

```
흐름
  A(이메일 백엔드) ──► B(이메일 프론트)
  A ──► C(표준 소셜 백엔드) ──► D(애플 백엔드)
                    C, D ──► E(소셜 프론트)
```

### 1-3. 상세

- **비밀번호 정책**: 8자 이상 72자 이하(BCrypt 해시 입력 길이 제한). 추가 복잡도
  규칙은 두지 않는다 — 1인 개발 범위에서 검증 비용 대비 이득이 작다(product.md §6).
- **역할 불변성**: 가입 시 선택한 역할(관객/주최자)은 로그인 토큰의 클레임으로
  그대로 쓰인다. 가입 이후 역할을 바꾸는 API는 이번 범위에 없다(§7).
- **이메일 불변성**: 이메일은 가입 후 변경할 수 없다(변경 API 없음, §7).
- **계정 연결 정책**: 소셜 로그인의 이메일이 기존 `users.email`과 같고 제공자의
  이메일 검증 플래그(`email_verified` 또는 동등 필드)가 true이면 새 계정을
  만들지 않고 `social_accounts`에 연결 행만 추가한다. 검증되지 않은 이메일은
  신뢰할 수 없으므로(타인의 이메일로 계정을 가로채는 경로가 될 수 있음) 자동
  연결하지 않고 신규 가입과 동일하게 취급한다(ADR-0014). 자동 연결 시 역할은
  최초 가입 당시 값을 그대로 쓴다(소셜 로그인 쪽에서 역할을 다시 묻지 않는다).
- **역할 없는 반쪽 계정 방지**: 소셜 로그인으로 처음 들어온(이메일이 일치하는
  기존 계정이 없는) 사용자는 `users` 행을 바로 만들지 않는다. 이메일/이름/제공자
  정보를 담은 5분 만료 `pendingToken`을 발급해 역할 선택 화면으로 보내고, 역할을
  받은 뒤에야 `users` 행과 `social_accounts` 행을 함께 만든다.
- **애플 이름 캡처**: 애플은 최초 인가 응답(`form_post` 바디의 `user` 파라미터)에만
  이름을 내려주고 재로그인 시에는 보내지 않는다. 최초 1회 수신 시점에만 이름을
  저장한다.

---

## 2. 화면 설계

**UI 라이브러리**: MUI
**디자인 정본**: `docs/product/design.md` · `design-tokens.css`
**와이어프레임**: 없음 — 아래 2-4 ASCII로 대신한다

### 2-1. 이 화면에서 사용자가 하려는 일

이메일 또는 소셜 계정(카카오/네이버/구글/애플)으로 계정을 만들고, 로그인해서
관객 또는 주최자로서 서비스를 이용한다.

### 2-2. 참조

| 서비스 | 이 화면에 해당하는 부분 | 우리에게 없는 것 |
|---|---|---|
| 인터파크 티켓 | ID+비밀번호 로그인, 소셜 간편로그인 버튼 목록, 자동로그인 체크박스 | 아이디/비밀번호 찾기, 자동로그인 연장 |

**대조 결과**: 소셜 로그인은 사용자 유입 확보를 위해 이번 범위에 포함한다.
아이디/비밀번호 찾기·자동로그인 연장은 이메일 발송 인프라가 없어 여전히 §7
범위 외로 둔다.

### 2-3. 담기는 것

**회원가입 폼**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 이메일 | 로그인 식별자 | ⚠ 없음 — `users.email` 신규 |
| 2 | 비밀번호 / 비밀번호 확인 | 계정 보호, 오타 방지 | ⚠ 없음 — `users.password_hash` 신규 (평문 저장 안 함) |
| 3 | 이름 | 화면에 표시할 식별자 | ⚠ 없음 — `users.name` 신규 |
| 4 | 역할 선택 (관객/주최자) | product.md §3 역할 구분, 이후 소유권 판단의 근거 | ⚠ 없음 — `users.role` 신규 |

**로그인 폼**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 이메일 | 로그인 식별자 | `users.email` |
| 2 | 비밀번호 | 인증 | `users.password_hash` (해시 비교) |
| 3 | 소셜 로그인 버튼(카카오/네이버/구글/애플) | 가입 마찰 감소, 사용자 유입 확보 | 없음 — 고정 UI, 클릭 시 `/oauth2/authorization/{provider}` 로 이동 |

**역할 선택 화면 (신규, 소셜 최초 가입자 전용)**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 안내 문구(이메일/이름) | 어떤 계정으로 가입 중인지 확인시킨다 | `pendingToken` 클레임(email, name) |
| 2 | 역할 선택 (관객/주최자) | product.md §3 역할 구분 | ⚠ 없음 — 사용자 입력, `users.role`로 저장 |

### 2-4. 배치

가입·로그인·역할선택은 모두 입력을 순서대로 완료하는 단일 흐름이므로 세로 폼으로
그린다.

```
회원가입                          로그인                           역할 선택(소셜 최초 가입)
┌──────────────────────┐        ┌──────────────────────┐        ┌──────────────────────┐
│      회원가입          │        │       로그인           │        │  거의 다 됐어요        │
├──────────────────────┤        ├──────────────────────┤        ├──────────────────────┤
│ 이메일   [_________]  │        │ 이메일   [_________]  │        │ hong@gmail.com 으로   │
│ 비밀번호 [_________]  │        │ 비밀번호 [_________]  │        │ 가입합니다             │
│ 비밀번호 확인[______]  │        │                      │        │                      │
│ 이름     [_________]  │        │   [    로그인    ]    │        │ 역할  ( )관객 ( )주최자 │
│ 역할 ( )관객 ( )주최자 │        │ ───────── 또는 ─────  │        │                      │
│                      │        │ [카카오][네이버][구글][애플]│    │   [   계속하기   ]    │
│   [   가입하기   ]    │        │                      │        └──────────────────────┘
│ ───────── 또는 ─────  │        │ 계정이 없으신가요? 가입 │
│ [카카오][네이버][구글][애플]│    └──────────────────────┘
│                      │
│ 이미 계정이 있나요? 로그인│
└──────────────────────┘
```

**이 배치인 이유**: 입력을 순서대로 완료해야 하는 단일 작업이라 세로 폼이
자연스럽고, 소셜 버튼은 구분선("또는") 아래 한 줄로 묶어 이메일 입력과 시각적으로
분리한다.

### 2-5. 상태별로 무엇이 보이나

| 화면 | 상태 |
|---|---|
| 회원가입 | 기본 · 제출 중 · 검증 오류(필드별) · 이메일 중복(409) · 성공 |
| 로그인 | 기본 · 제출 중 · 자격 증명 오류(401) · 성공 |
| 역할 선택(소셜 최초 가입) | 기본 · 제출 중 · pendingToken 만료 오류 |
| OAuth 콜백 리다이렉트 | 처리 중(로딩) · 제공자 인증 취소/실패 |

- **기본** — 빈 폼, 포커스는 이메일 필드. 역할 선택 화면은 소셜 계정의 이메일/이름을
  안내 문구로 보여주고 역할만 고르게 한다
- **제출 중** — 버튼 비활성화 + 로딩 표시, 중복 제출 방지
- **검증 오류** — 필드 아래 인라인 메시지 (예: "비밀번호가 일치하지 않습니다")
- **이메일 중복 (회원가입)** — "이미 가입된 이메일입니다. 로그인해 주세요" + 로그인
  링크 강조
- **자격 증명 오류 (로그인)** — "이메일 또는 비밀번호가 올바르지 않습니다" — 이메일이
  없는 것인지 비밀번호가 틀린 것인지는 구분하지 않는다(계정 존재 여부 비노출)
- **pendingToken 만료 오류** — "로그인 정보가 만료되었습니다. 다시 로그인해 주세요"
  + 로그인 화면으로 이동 버튼
- **콜백 처리 중** — 스피너만, 사용자 입력 없음
- **제공자 인증 취소/실패** — 로그인 화면으로 돌아가 "로그인이 취소되었거나
  실패했습니다" 배너
- **성공** — 토큰을 저장하고 리다이렉트. 회원가입/역할선택 성공은 홈으로, 로그인
  성공은 이전 화면(또는 홈)으로

---

## 3. 데이터·API 명세

**Task A — 이메일 인증**

`backend/src/main/resources/db/migration/V3__create_users.sql` (신규)

```sql
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL, -- 'AUDIENCE' | 'ORGANIZER'
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);
```

```java
// api/dto/SignUpRequest.java
public record SignUpRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank String passwordConfirm,
    @NotBlank @Size(max = 100) String name,
    @NotNull UserRole role
) {}

// api/dto/LoginRequest.java
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

// service/UserResponse.java
public record UserResponse(Long id, String email, String name, UserRole role) {}

// service/LoginResponse.java
public record LoginResponse(
    String accessToken,
    String tokenType,       // "Bearer"
    long expiresInSeconds,  // 86400 (24h)
    UserResponse user
) {}
```

**Task C — 소셜 로그인(카카오/네이버/구글)**

`backend/src/main/resources/db/migration/V4__create_social_accounts.sql` (신규)

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL; -- 소셜 전용 계정은 비밀번호 없음

CREATE TABLE social_accounts (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id),
    provider         VARCHAR(20) NOT NULL, -- 'KAKAO' | 'NAVER' | 'GOOGLE' | 'APPLE'
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);
```

```java
// api/dto/CompleteSocialSignUpRequest.java
public record CompleteSocialSignUpRequest(@NotBlank String pendingToken, @NotNull UserRole role) {}
```

**Task D — 애플**: 별도 DDL 없음. `social_accounts.provider = 'APPLE'` 로 동일하게
기록한다.

**엔드포인트**

| 메서드 | 경로 | 요청 | 응답 | 비고 | 태스크 |
|---|---|---|---|---|---|
| POST | `/api/auth/signup` | `SignUpRequest` | 201 `UserResponse` | 이메일 중복 시 409 | A |
| POST | `/api/auth/login` | `LoginRequest` | 200 `LoginResponse` | 자격 증명 불일치 시 401 | A |
| GET | `/api/auth/me` | `Authorization: Bearer <token>` | 200 `UserResponse` | 토큰 없음/무효 시 401 | A |
| GET | `/oauth2/authorization/{provider}` | — | 302 제공자 인증 페이지로 리다이렉트 | Spring Security 기본 제공. `provider` ∈ `kakao,naver,google,apple` | C, D |
| GET | `/login/oauth2/code/{provider}` | 제공자 콜백 | 302 프론트 `/oauth2/redirect?...` | Spring Security 기본 콜백 + 커스텀 `SuccessHandler` | C, D |
| POST | `/api/auth/social/complete` | `CompleteSocialSignUpRequest` | 200 `LoginResponse` | `pendingToken` 만료/위조 시 401 | C |

**JWT 클레임**

- 일반 `accessToken`: `sub`(userId), `email`, `role`, `typ`="access", `iat`, `exp`(24h)
- `pendingToken`: `email`, `name`, `provider`, `providerUserId`, `typ`="pending", `exp`(5분).
  `JwtAuthenticationFilter`는 `typ`이 `access`가 아닌 토큰으로 일반 API를 호출하면
  거부한다.

서명 키는 환경변수 `JWT_SECRET`. 애플 Client Secret은 별도로 ES256 서명 JWT로
생성한다(Apple 개발자 포털에서 발급한 `.p8` 키 사용, `AppleClientSecretGenerator`).

---

## 4. 흐름

**이메일 회원가입/로그인**

```
사용자 ──► API(POST /auth/signup) ──► Service ──► Repository ──► DB(users INSERT, BCrypt 해싱)
            ◄──────────────────────────────── 201 UserResponse

사용자 ──► API(POST /auth/login) ──► Service ──► Repository ──► DB(users SELECT by email)
                                        │
                                        ├──► 비밀번호 해시 비교
                                        └──► JwtTokenProvider.issueAccessToken()
            ◄─────────────────────────────── 200 LoginResponse { accessToken }
```

**소셜 로그인 — 기존 이메일과 일치(자동 연결)**

```
사용자 ──► /oauth2/authorization/{provider} ──► 제공자 인증 ──► /login/oauth2/code/{provider}
                                                                    │
                                                        CustomOAuth2UserService
                                                        (email, name, providerUserId 추출)
                                                                    │
                                                        OAuth2LoginSuccessHandler
                                                        users.email 매치 AND email_verified=true? ──► Yes
                                                                    │
                                                        social_accounts 행 추가(없으면)
                                                                    │
                                                        JwtTokenProvider.issueAccessToken()
                                                                    ▼
                                            프론트 리다이렉트 /oauth2/redirect?token=...
```

**소셜 로그인 — 신규 사용자(역할 선택 필요)**

```
...CustomOAuth2UserService... ──► OAuth2LoginSuccessHandler
                                      users.email 매치 AND email_verified=true? ──► No
                                           │
                                 JwtTokenProvider.issuePendingToken(email, name, provider)
                                           ▼
                     프론트 리다이렉트 /oauth2/redirect?pendingToken=...
                                           │
                     역할 선택 화면 ──► POST /api/auth/social/complete { pendingToken, role }
                                           │
                     Service: users INSERT + social_accounts INSERT
                                           ▼
                                  200 LoginResponse { accessToken }
```

**인증된 요청 (예: GET /auth/me)**

```
사용자 ──► API(Authorization: Bearer <token>) ──► JwtAuthenticationFilter(typ=access 검증) ──► Controller
                                                        │
                                                        └── 실패 시 401, 더 들어가지 않음
```

---

## 5. 완료 조건

### Task A (백엔드 — 이메일 인증)

- [ ] 올바른 정보로 `POST /auth/signup` 호출 시 201과 함께 `UserResponse`(비밀번호
      제외)를 반환한다
- [ ] 이미 존재하는 이메일로 가입 시도 시 409를 반환한다
- [ ] 저장된 비밀번호가 평문이 아니라 BCrypt 해시임을 DB 조회로 확인한다
- [ ] 올바른 이메일/비밀번호로 `POST /auth/login` 호출 시 200과 유효한
      `accessToken`을 반환한다
- [ ] 틀린 비밀번호 또는 존재하지 않는 이메일로 로그인 시 401을 반환하며, 두 경우
      응답 메시지가 동일하다
- [ ] 유효한 토큰으로 `GET /auth/me` 호출 시 200과 해당 사용자 정보를 반환한다
- [ ] 토큰 없이 또는 만료/위조된 토큰으로 `GET /auth/me` 호출 시 401을 반환한다
- [ ] 기존 `/api/performances/**` 엔드포인트가 Security 도입 이후에도 인증 없이
      그대로 동작한다(회귀 없음)

### Task B (프론트 — 이메일 화면)

- [ ] 회원가입 폼에서 비밀번호와 비밀번호 확인이 다르면 제출 전 인라인 오류를
      표시한다
- [ ] 회원가입 성공 시 로그인 화면으로 이동한다
- [ ] 이메일 중복(409) 응답 시 전용 오류 메시지와 로그인 링크를 표시한다
- [ ] 로그인 성공 시 토큰을 저장하고 이전 화면(또는 홈)으로 이동한다
- [ ] 로그인 실패(401) 시 "이메일 또는 비밀번호가 올바르지 않습니다"를 표시한다
- [ ] 로그인 상태에서 헤더에 사용자 이름과 로그아웃 버튼이 보이고, 클릭 시 토큰이
      삭제되고 비로그인 상태로 전환된다

### Task C (백엔드 — 카카오/네이버/구글)

- [ ] 처음 로그인(기존 이메일과 매칭되지 않음)하면 `users` 행을 생성하지 않고
      `pendingToken`을 발급해 역할 선택 화면으로 보낸다
- [ ] 이미 이메일 회원가입된 계정과 같은 이메일로, **제공자의 이메일 검증
      플래그(`email_verified` 또는 동등 필드)가 true인** 소셜 로그인을 하면
      `users` 행을 새로 만들지 않고 기존 계정에 `social_accounts` 행을 추가한
      뒤 바로 `accessToken`을 발급한다(역할은 기존 값 유지)
- [ ] 이메일 검증 플래그가 false(또는 제공자가 제공하지 않음)인 경우에는 이메일이
      기존 계정과 같아도 자동 연결하지 않고, 신규 가입과 동일하게 `pendingToken`을
      발급해 역할 선택 화면으로 보낸다(계정 탈취 방지, ADR-0014)
- [ ] 같은 제공자 계정으로 재로그인해도 `social_accounts`의 유니크 제약
      (`provider`, `provider_user_id`)에 의해 중복 행이 생기지 않는다
- [ ] 카카오/네이버(중첩 응답 구조)와 구글(평면 구조) 각각에서 이메일과 이름을
      정상적으로 추출한다(제공자별 매핑 단위 테스트)
- [ ] 만료된 `pendingToken`으로 `POST /api/auth/social/complete` 호출 시 401을
      반환한다
- [ ] `pendingToken`으로 `/api/auth/social/complete` 외의 인증 필요 엔드포인트를
      호출하면 거부된다(`typ` 클레임 검증)

### Task D (백엔드 — 애플)

- [ ] 애플 Client Secret이 ES256으로 서명된 JWT로 생성되어 토큰 교환 요청에
      쓰인다
- [ ] 애플 최초 로그인 콜백에서만 전달되는 이름 정보가 `users` 행 생성 시
      반영된다
- [ ] 애플 로그인도 Task C의 자동 연결/역할 선택(`pendingToken`) 흐름을 동일하게
      따른다

### Task E (프론트 — 소셜 버튼/역할 선택/콜백)

- [ ] 로그인/회원가입 화면에 카카오/네이버/구글/애플 버튼이 보이고 각각
      `/oauth2/authorization/{provider}`로 이동한다
- [ ] 콜백 리다이렉트(`/oauth2/redirect`)에서 `token` 쿼리가 오면 즉시 로그인
      처리 후 홈으로 이동한다
- [ ] 콜백 리다이렉트에서 `pendingToken` 쿼리가 오면 역할 선택 화면으로 이동하고,
      역할 제출 후 로그인 처리된다
- [ ] `pendingToken` 만료 오류 응답을 받으면 안내 메시지와 함께 로그인 화면으로
      이동할 수 있다
- [ ] 제공자 인증을 사용자가 취소하거나 실패하면 로그인 화면으로 돌아와 오류
      메시지를 보여준다

---

## 6. 구현 참고

- `backend/src/main/resources/db/migration/V3__create_users.sql` — 신규(Task A). `users` 테이블
- `backend/src/main/resources/db/migration/V4__create_social_accounts.sql` — 신규(Task C). `social_accounts` 테이블 + `users.password_hash` nullable 전환
- `backend/src/main/java/com/example/ticket_booking/domain/User.java` — 신규. 엔티티(id, email, passwordHash(nullable),
  name, role, createdAt)
- `backend/src/main/java/com/example/ticket_booking/domain/UserRole.java` — 신규. enum `AUDIENCE`, `ORGANIZER`
- `backend/src/main/java/com/example/ticket_booking/domain/SocialAccount.java` — 신규(Task C). 엔티티(id, userId,
  provider, providerUserId, createdAt)
- `backend/src/main/java/com/example/ticket_booking/domain/SocialProvider.java` — 신규(Task C). enum `KAKAO`, `NAVER`,
  `GOOGLE`, `APPLE`
- `backend/src/main/java/com/example/ticket_booking/repository/UserRepository.java` — 신규. `findByEmail`,
  `existsByEmail`
- `backend/src/main/java/com/example/ticket_booking/repository/SocialAccountRepository.java` — 신규(Task C).
  `findByProviderAndProviderUserId`
- `backend/src/main/java/com/example/ticket_booking/service/AuthService.java` — 신규. `signUp()`, `login()`,
  `completeSocialSignUp()`(Task C)
- `backend/src/main/java/com/example/ticket_booking/service/EmailAlreadyExistsException.java` — 신규
- `backend/src/main/java/com/example/ticket_booking/service/InvalidCredentialsException.java` — 신규
- `backend/src/main/java/com/example/ticket_booking/service/PendingSignupExpiredException.java` — 신규(Task C)
- `backend/src/main/java/com/example/ticket_booking/config/SecurityConfig.java` — 신규. `SecurityFilterChain`,
  `/api/performances/**` 등 기존 엔드포인트 `permitAll` 명시
- `backend/src/main/java/com/example/ticket_booking/config/JwtTokenProvider.java` — 신규. `issueAccessToken()`,
  `issuePendingToken()`, 검증
- `backend/src/main/java/com/example/ticket_booking/config/JwtAuthenticationFilter.java` — 신규. `Authorization` 헤더
  파싱 후 `SecurityContext`에 인증 정보 주입
- `backend/src/main/java/com/example/ticket_booking/config/CustomOAuth2UserService.java` — 신규(Task C/D). 제공자별
  attribute 정규화(카카오/네이버 중첩 구조, 구글 평면 구조, 애플 `id_token` 클레임)
- `backend/src/main/java/com/example/ticket_booking/config/OAuth2LoginSuccessHandler.java` — 신규(Task C). 자동 연결
  또는 `pendingToken` 발급 분기
- `backend/src/main/java/com/example/ticket_booking/config/OAuth2LoginFailureHandler.java` — 신규(Task C)
- `backend/src/main/java/com/example/ticket_booking/config/AppleClientSecretGenerator.java` — 신규(Task D). ES256
  서명 Client Secret 생성
- `backend/src/main/java/com/example/ticket_booking/api/AuthController.java` — 신규. `POST /api/auth/signup`,
  `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/social/complete`
  (Task C)
- `backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java` — 참고 패턴. 인증 관련
  예외는 별도 `AuthExceptionHandler`로 신설
- `frontend/src/api/auth.ts` — 신규. `signUp()`, `login()`, `fetchMe()`,
  `completeSocialSignUp()`(Task E)
- `frontend/src/auth/AuthContext.tsx` — 신규. 토큰 저장(localStorage)·현재 사용자
  상태·`useAuth()` 훅
- `frontend/src/pages/SignUpPage.tsx` — 신규
- `frontend/src/pages/LoginPage.tsx` — 신규
- `frontend/src/components/SocialLoginButtons.tsx` — 신규(Task E)
- `frontend/src/pages/OAuthRedirectPage.tsx` — 신규(Task E). `/oauth2/redirect`
  라우트
- `frontend/src/pages/RoleSelectionPage.tsx` — 신규(Task E)

---

## 7. 범위 외

- 아이디/비밀번호 찾기(재설정) — 이메일 발송 인프라가 없다
- 이메일 인증(가입 확인 메일) — 위와 같은 이유
- 주최자 소유권 연동(`performance.owner_id` 추가, 공연 등록 API의 소유권 검사) —
  이 기능 다음 별도 문서로 분리한다(§1-1). TASK-004/005의 "포함하지 않음"을
  수정하는 작업이라 이번 범위에 넣지 않는다
- 리프레시 토큰/토큰 갱신 — `accessToken` 만료(24h) 시 재로그인으로 충분하다고
  판단한다(§8)
- 운영자(관리자) 계정 — product.md §4 "하지 않는 것"에 이미 범위 밖으로 명시됨.
  `UserRole` enum에도 추가하지 않는다
- 가입 후 역할 변경, 이메일 변경 — API 없음
- 소셜 계정 연결 해제(unlink), 소셜 전용 계정에 비밀번호 추가 설정 — 마이페이지
  기능 자체가 범위 밖
- 소셜 제공자 추가(당근마켓, X 등) — 이번엔 카카오·네이버·구글·애플 4종만

---

## 8. 주요 결정 사항

| 결정 | 이유 | 범위 | ADR |
|---|---|---|---|
| 인증 방식으로 세션이 아닌 JWT(Bearer 토큰)를 쓴다 | SPA·백엔드 분리 배포에서 쿠키 도메인/CORS 이슈 없이 단순하게 식별 가능 | **전체** | ADR-0012 |
| Spring Security를 전체 도입한다(OAuth2 Client + 커스텀 JWT 인증 필터) | 소셜 로그인 4종을 자체 구현하면 각 제공자의 authorization code flow·state 검증을 직접 관리해야 해 리스크가 크다. Spring Security의 OAuth2Login이 표준으로 처리한다 | **전체** | ADR-0013 |
| 기존 공연 API(`/api/performances/**`)는 이번 Security 도입에서도 `permitAll`로 유지한다 | 소유권 검사는 별도 문서로 분리돼 있어(§7), 지금 막으면 기존 기능이 깨진다 | **전체** | — |
| 이메일이 같고 `email_verified`가 true이면 기존 계정에 소셜 연결을 자동으로 추가한다(계정 연결 정책) | 같은 이메일로 여러 경로에 가입했다고 사용자가 혼란스러워하는 것을 막는다. 검증되지 않은 이메일은 신뢰하지 않아 계정 탈취 경로를 막는다 | **전체** | ADR-0014 |
| 소셜 최초 가입자는 `users` 행을 바로 만들지 않고 `pendingToken`(5분)으로 역할 선택을 받은 뒤 생성한다 | 역할 없는 "반쪽 계정"이 DB에 남는 것을 막는다 | 이 기능만 | — |
| `accessToken` 저장 위치로 localStorage를 쓴다 | 새로고침 후에도 로그인 유지가 필요. 이 단계에서 XSS 방어(CSP 등)는 범위 밖으로 수용 | **전체** | ADR-0015 |
| 소셜 로그인 콜백은 토큰을 쿼리가 아닌 URL fragment(`#token=`, `#pendingToken=`)로 프론트에 넘긴다 | 쿼리 문자열은 접근 로그·히스토리·Referer 에 남는다. fragment 는 서버로 가지 않고 서버 상태도 필요 없다. 프론트는 읽은 즉시 `history.replaceState` 로 지운다 | **전체** | ADR-0021 |
| `accessToken` 만료는 24시간, 리프레시 토큰은 두지 않는다 | 재로그인 빈도가 낮은 서비스 특성상 리프레시 로직의 복잡도가 이득보다 크다 | 이 기능만 | — |

ADR-0012(JWT 인증 방식), ADR-0013(Spring Security 전체 도입),
ADR-0014(계정 자동 연결 정책), ADR-0015(localStorage 토큰 저장)로 기록했다.
