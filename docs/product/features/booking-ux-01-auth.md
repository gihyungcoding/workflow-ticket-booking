# [관객] 흐름 1 — 로그인·가입을 시안대로 (booking-ux-01)

**상태**: 확정
**최종 갱신**: 2026-11-04 (아이디 생성 규칙 변경)

---

## 1. 요구사항

### 1-1. 배경

로그인·가입·소셜 로그인은 TASK-009~013 에서 이미 동작한다 (`user-authentication.md`).
이번에 디자인 정본을 `out/` 시안으로 바꿨고(ADR-0022), 시안은 지금 화면과 세 가지가 다르다.

| AS-IS | TO-BE (시안) |
|---|---|
| 잉크 + 별색 1도, 명조, 그림자 없음 | 브랜드 블루·Pretendard·의미 배지 6종 — 모든 화면의 바탕 |
| 이메일 가입 → 로그인 화면 | 가입 → 자동 로그인 → **가입 완료 화면**(내 티켓온 아이디 + 복사) → 홈 |
| 역할 선택: "거의 다 됐어요" + 라디오 | 어느 소셜·어느 이메일로 가입 중인지 보이고, 5분이 지나면 그 자리에서 만료 상태로 바뀐다 |

이 흐름은 다섯 흐름(booking-ux-01~05) 중 첫 번째다. 테마·공통 부품을 여기서 만들고
뒤 흐름이 그대로 쓴다. 「티켓온 아이디」는 흐름 5(양도)에서 받는 사람을 찾는 키가 된다.

### 1-2. 태스크 분리 (필수)

| Task | 범위 | 산출물 | 포함하지 않음 | 선행 | 추정 | 태스크 ID |
|---|---|---|---|---|---|---|
| A. 백엔드 — 티켓온 아이디·소셜 가입 대기 정보 | `users.handle` 추가·자동 생성(기존 회원 채우기 포함), 사용자 응답에 `handle`, pendingToken 의 제공자·이메일·이름·만료 시각을 돌려주는 조회 API | `V5__add_users_handle.sql`, `HandleGenerator`, `UserResponse.handle`, `POST /api/auth/social/pending` | 아이디 변경, 아이디로 회원 찾기(흐름 5), 휴대폰 번호 | — | 8h | TASK-016 |
| B. 프론트 — 디자인 토큰 교체·공통 부품 | `design-tokens.css` 값으로 MUI 테마 재작성, Pretendard 로드, 모바일 한 열 레이아웃, 시안 부품의 React 컴포넌트, 배지 문구를 design.md §5 로 | `theme/tokens.ts`·`theme/index.ts`, `components/ui/*`(AppBar·Page·Block·Row·Badge·CtaBar·Field·Toast·Sheet) | 화면별 재배치(목록·상세는 흐름 2), 다크 모드, 주최자 화면 재디자인 | — | 12h | TASK-017 |
| C. 프론트 — 인증 화면 5종을 시안대로 | 로그인·가입·콜백·역할 선택·가입 완료(신규) 화면과 그 오류 상태 3종, 화면 정책(5분 만료·아이디 복사·실패 안내·자동 로그인) | `LoginPage`·`SignUpPage`·`OAuthRedirectPage`·`RoleSelectionPage` 재작성, `JoinedPage`(신규), `/joined` 라우트, `AUTH_MESSAGES` 해요체 | 아이디·비밀번호 찾기, 홈 화면(흐름 2) | A, B | 12h | TASK-018 |

[태스크 생성 2026-11-04]

**분리 이유**: 테마·공통 부품(B)은 다섯 흐름이 모두 쓰는 바탕이라 인증 화면(C)과 따로 검증한다.
아이디 필드(A)는 마이그레이션이 있는 백엔드 변경이라 화면과 다른 행이다.

```
흐름
  A(백엔드: 아이디·대기 정보) ──┐
                                ├──► C(프론트: 인증 화면 5종)
  B(프론트: 토큰·공통 부품) ────┘
```

### 1-3. 상세 — 화면에 적힌 규칙을 기능으로

시안(`out/screens.json` 의 `policies`, 각 HTML 의 정책 주석)에 적힌 규칙은 표시 문구가 아니라
동작이다.

| 화면 | 규칙 | 기능 | Task |
|---|---|---|---|
| login | 틀리면 어느 쪽이 틀렸는지 알려 주지 않는다 | 401 은 두 칸을 함께 오류 표시 + 한 문구. 서버는 이미 계정 존재를 숨긴다 | C |
| login | 소셜 로그인이 취소·실패하면 이 화면 맨 위에 안내 | `/login?error=social` 이면 맨 위 안내 블록 | C |
| signup | 비밀번호 8자 이상 72자 이하 | 칸 아래 안내가 기본, 벗어나면 같은 자리가 오류로 바뀜 (서버도 검증 — ADR-0019) | C |
| signup | 비밀번호 확인이 다르면 그 칸 아래 | 클라이언트 검증 | C |
| signup | 이미 가입된 이메일이면 이메일 칸에서 알리고 로그인 안내 | 409 `EMAIL_ALREADY_EXISTS` → 이메일 칸 오류 + 칸 안 로그인 링크, 아래 "이미 계정이 있나요" 줄은 숨김 | C |
| signup | 이메일·역할은 가입 뒤 바꿀 수 없다 | 바꾸는 API·화면을 만들지 않는다. 안내 문구만 | — |
| signup → joined | 가입하면 바로 시작 | 가입 201 직후 같은 자격 증명으로 로그인 → `/joined` | C |
| oauth-callback | 어느 계정을 확인 중인지 | 소셜 버튼을 누를 때 제공자를 `sessionStorage` 에 남기고 콜백에서 읽음 | C |
| role-select | 5분 안에 고르지 않으면 만료 — 다시 로그인 | `expiresAt` 까지 남은 시간이 0 이 되면 화면이 만료 상태로 바뀜. 서버도 만료 토큰을 거부(기존) | A, C |
| role-select | 가입 뒤 역할은 바꿀 수 없다 | 안내 문구 | — |
| joined | 아이디는 눌러서 복사 — 일행에게 보내 표를 받는다 | 클립보드 복사 + 토스트 "아이디를 복사했어요 — 일행에게 붙여넣어 보내세요" | C |

---

## 2. 화면 설계

**UI 라이브러리**: MUI — ADR-0002
**디자인 정본**: `docs/product/design.md` · `design-tokens.css` (값의 출처 `out/tokens.css`)
**시안**: `out/screens/login.html` · `login--error.html` · `signup.html` · `signup--error.html` ·
`oauth-callback.html` · `role-select.html` · `role-select--error.html` · `joined.html` — 흐름은 `out/index.html` 「처음 들어올 때」

### 2-1. 이 화면에서 사용자가 하려는 일

가입·로그인이 예매의 걸림돌이 되지 않게, **쓰던 소셜 계정이면 버튼 한 번으로 들어와
바로 공연을 찾는다** — 처음 온 사람(김하린)은 표를 받을 내 아이디까지 손에 쥔다.

### 2-2. 참조

`out/market/` 에 모은 스토어 화면·리뷰 (2026-11-04 수집, `out/conventions.json`).

| 서비스 | 이 화면에 해당하는 부분 | 우리에게 없는 것 |
|---|---|---|
| NOL(인터파크 티켓) | 로그인 — 소셜 버튼이 위, 이메일 폼이 아래 | 아이디·비밀번호 찾기, 본인인증 |
| 티켓링크 | 로그인 유지, 예매 중 로그인 요구 | 아이디·비밀번호 찾기 |
| 예스24 티켓 | 계정 연동(통합 계정) | 본인인증, 통합 계정 |

리뷰 불만 A1 (예스24·티켓링크·NOL·타임티켓): "로그인·본인인증·계정 연동이 실패하거나
풀려서 예매 화면에 못 들어간다."

**대조 결과**: 아이디·비밀번호 찾기, 본인인증은 넣지 않는다(§7). A1 은 기존 동작 —
로그인 뒤 원래 화면으로 돌아가기(`safeDestination`), 24시간 토큰 유지 — 을 그대로 지켜 대응한다.

### 2-3. 담기는 것

**로그인** (`login`)

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 머리글 "티켓온 쓰던 계정으로 바로 들어와요" + 보조 문구 | 소셜이면 처음이어도 버튼 한 번이라는 것 | 고정 문구 |
| 2 | 이메일·비밀번호 칸 + 로그인하기 | 이메일 회원 | 입력 |
| 3 | 소셜 버튼 4개(카카오·네이버·구글·애플) | 처음 오는 사람의 길 | 고정 (`SocialLoginButtons`) |
| 4 | "계정이 없나요? 이메일로 가입" | | 고정 |
| 0 | (실패 시) 맨 위 안내 "로그인이 취소됐거나 실패했어요" | | `?error=social` |

**가입** (`signup`)

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 안내 "이메일과 역할은 가입한 뒤에 바꿀 수 없어요" | 되돌릴 수 없는 것을 먼저 | 고정 |
| 2 | 이메일 · 비밀번호(안내 "8자 이상 72자 이하로 정해요") · 비밀번호 확인 · 이름 | | 입력 |
| 3 | 역할(관객·주최자) | | 입력 |
| 4 | "이미 계정이 있나요? 로그인" + 하단 CTA 가입하기 | | 고정 |

**콜백** (`oauth-callback`): 진행 표시 + "카카오 계정을 확인하고 있어요" — 제공자 이름은 `sessionStorage`.

**역할 선택** (`role-select`)

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | "카카오로 처음 가입" + "harin.kim@kakao.com (김하린)으로 가입하고 있어요" | 내 어느 계정으로 가입되는지 확신 | pendingToken → `POST /api/auth/social/pending` (A) |
| 2 | 역할 행 2개 — 관객 "공연을 찾고 예매해요" / 주최자 "공연을 등록하고 관리해요" | | 고정 |
| 3 | "5분 안에 골라 주세요. 가입한 뒤에는 역할을 바꿀 수 없어요" + 계속하기 | | `expiresAt` (A) |

**가입 완료** (`joined`, 신규)

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | "가입이 끝났어요" + 가입 경로·역할 ("카카오 · 관객") | | 제공자(`sessionStorage`, 이메일이면 "이메일"), `user.role` |
| 2 | **내 티켓온 아이디** `wave7302` (시안의 `harin.kim` 자리 — 형식은 §3-2) + 아이디 복사 | 일행에게 보내 표를 받는다 (흐름 5) | ⚠ 없음 — **`users.handle` 신규** (A) |
| 3 | "일행에게 아이디를 보내면 표를 받을 수 있어요" + 시작하기 → 홈 | | 고정 |

### 2-4. 배치

시안 HTML 이 배치의 정본이다. 요약만 둔다 (375px 한 열).

```
login                         signup                      role-select
┌───────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
│ (실패 안내 — 있을 때)  │    │ ← 이메일로 가입      │    │ ←                    │
│ 티켓온 쓰던 계정으로…  │    │ 안내: 바꿀 수 없어요 │    │ 카카오로 처음 가입   │
│ 보조 문구              │    │ [이메일]             │    │ harin.kim@… (김하린) │
│ [이메일]               │    │ [비밀번호] 8~72자    │    ├──────────────────────┤
│ [비밀번호]             │    │ [비밀번호 확인]      │    │ ○ 관객  공연을 찾고… │
│ [ 로그인하기 ]         │    │ [이름]               │    │ ○ 주최자 공연을 등록…│
│ ── 또는 ──             │    │ ( 관객 | 주최자 )    │    │ 5분 안에 골라 주세요 │
│ [카카오][네이버]       │    │ 이미 계정이…? 로그인 │    │                      │
│ [구글][애플]           │    ├──────────────────────┤    ├──────────────────────┤
│ 계정이 없나요? 가입    │    │ [     가입하기     ] │    │ [     계속하기     ] │
└───────────────────────┘    └──────────────────────┘    └──────────────────────┘
joined
┌──────────────────────┐
│  (일러스트)           │
│ 카카오 · 관객         │
│ 가입이 끝났어요       │
│ 내 티켓온 아이디      │
│ wave7302   [복사]     │
│ 일행에게 아이디를…    │
├──────────────────────┤
│ [     시작하기     ] │
└──────────────────────┘
```

**이 배치인 이유**: 폼 화면은 세로 한 열 + 하단 고정 CTA(시안 `.cta`) — 엄지 닿는 곳에
주 버튼 하나. 로그인은 CTA 를 고정하지 않는다 — 소셜 버튼이 같은 무게의 대안이라서.

### 2-5. 상태별로 무엇이 보이나

| 화면 | 상태 |
|---|---|
| login | 기본 · 제출 중 · 자격 증명 오류(`login--error`) · 소셜 실패 안내 · 서버 오류 |
| signup | 기본 · 칸 오류(길이·불일치) · 이메일 중복(`signup--error`) · 제출 중 · 서버 오류 |
| oauth-callback | 확인 중 · 실패 → login 실패 안내 |
| role-select | 기본 · 대기 정보 로딩 · **만료(`role-select--error`)** · 제출 중 |
| joined | 기본 · 복사함(토스트) · 복사 실패 |

- **login 자격 증명 오류** — 두 칸 모두 오류 테두리, 비밀번호 칸 아래 "이메일 또는 비밀번호가 올바르지 않아요". 어느 칸인지 구분하지 않는다
- **login 소셜 실패** — 맨 위 "로그인이 취소됐거나 실패했어요"
- **signup 칸 오류** — 비밀번호 칸의 안내 문구 자리가 오류로 바뀜 / 확인 칸 아래 "비밀번호가 일치하지 않아요"
- **signup 이메일 중복** — 이메일 칸 아래 "이미 가입된 이메일이에요" + 칸 안 "로그인하기" 링크, 아래 "이미 계정이 있나요?" 줄 숨김
- **role-select 만료** — 역할 행 대신 "가입 시간이 지났어요 / 5분이 지나 역할을 고를 수 없어요. 다시 로그인해 주세요", CTA "다시 로그인하기" → `/login`. 화면에 머물러 있다가 5분이 되는 순간에도 바뀐다. 계속하기를 눌렀는데 서버가 만료(`PENDING_SIGNUP_EXPIRED`)로 답해도 같은 상태
- **joined 복사** — 토스트 "아이디를 복사했어요 — 일행에게 붙여넣어 보내세요". 클립보드가 막혀 있으면 아이디 글자를 선택 상태로 두고 "길게 눌러 복사해 주세요"
- **제출 중** — 주 버튼 비활성 + 진행 표시, 두 번 누를 수 없다
- **서버 오류** — 폼 맨 위 "가입하지 못했어요. 잠시 후 다시 시도해 주세요" / "로그인하지 못했어요. 잠시 후 다시 시도해 주세요"

---

## 3. 데이터·API 명세

### 3-1. `users.handle` (신규)

```sql
-- V5__add_users_handle.sql
ALTER TABLE users ADD COLUMN handle VARCHAR(30);
-- 기존 회원: 규칙(3-2)과 같은 단어 목록 + 숫자 4자리로 무작위 생성, 겹치면 다시 뽑는다 (PL/pgSQL 루프)
DO $$ ... $$;
ALTER TABLE users ALTER COLUMN handle SET NOT NULL;
CREATE UNIQUE INDEX ux_users_handle ON users (handle);
```

### 3-2. 아이디 생성 규칙 (`HandleGenerator`, service)

**이메일·이름과 무관하게 만든다.** 아이디는 남에게 보내라고 만든 값이라, 이메일에서 파생하면
이메일 주소를 추측할 단서가 공개된다 (2026-11-04 결정, §8).

- 형식: `<단어><숫자 4자리>` — 정규식 `^[a-z]{3,6}[0-9]{4}$` (예: `wave7302`, `aria0042`)
- 단어 목록(32개, 공연 어휘): `wave star stage encore aria beat chord drum echo flute harp jazz lyric melody note opera piano rhythm solo tempo tune verse viola waltz cello choir disco folk hymn oboe lute gig` — 32 × 10,000 = 32만 개
- 단어와 숫자는 난수원(`RandomGenerator`, 운영은 `SecureRandom`)에서 뽑는다. 테스트는 난수원을 주입해 결정적으로 만든다
- 이미 있으면 다시 뽑는다. **10번 연속 겹치면** 가입을 실패시키고(503 `HANDLE_GENERATION_FAILED`) 아무 행도 만들지 않는다
- 동시 가입으로 UNIQUE(`ux_users_handle`) 위반이 나면 이메일 중복(409)으로 오보하지 않는다
- 마이그레이션의 단어 목록은 Java 목록과 같아야 한다 (마이그레이션 테스트가 대조)
- 가입 뒤 바꾸지 않는다

### 3-3. 응답 변경

```java
// UserResponse — handle 추가 (GET /api/auth/me, POST /api/auth/signup)
public record UserResponse(Long id, String email, String name, UserRole role, String handle) {}
```

### 3-4. 소셜 가입 대기 정보 조회 (신규)

```
POST /api/auth/social/pending
요청  { "pendingToken": "<5분 토큰>" }
200   { "provider": "KAKAO", "email": "harin.kim@kakao.com", "name": "김하린",
        "expiresAt": "2026-11-11T08:40:00Z" }
401   { "code": "PENDING_SIGNUP_EXPIRED", "message": "..." }   -- 만료·위조 (기존 코드 재사용)
```

- POST 인 이유: 토큰이 쿼리 문자열로 접근 로그에 남지 않게 (ADR-0021 과 같은 이유)
- `JwtTokenProvider.verifyPendingToken()` 을 그대로 쓰고, 만료 시각을 함께 돌려준다
- 쓰기 가드(`WriteGuardFilter`)는 모든 POST 에 걸린다. 프론트 `api/http.ts` 가 이미 모든 쓰기 요청에
  `X-Write-Key` 를 붙이므로 따로 예외를 두지 않는다 (`/social/complete` 와 같은 취급)

---

## 4. 흐름

```
이메일 가입
 signup ──POST /signup(201, handle)──► POST /login ──► token 저장 ──► /joined ──► 시작하기 ──► /
                │409 EMAIL_ALREADY_EXISTS
                └──► 이메일 칸 오류 + 로그인 링크

소셜 로그인
 login ──[카카오] sessionStorage.provider=kakao ──► /oauth2/authorization/kakao
   ▲                                                   │
   │ ?error=social                                     ▼
   └────────────── 실패 ──── /oauth2/redirect (#token | #pendingToken)
                                │#token(기존 회원) ──► 원래 화면
                                │#pendingToken(처음)
                                ▼
                     /oauth2/role ──POST /social/pending──► 제공자·이메일·이름·expiresAt
                                │  남은 시간 0 또는 401 ──► 만료 상태 ──► /login
                                │계속하기 POST /social/complete
                                ▼
                             /joined ──► 시작하기 ──► /
```

---

## 5. 완료 조건

### Task A (백엔드)

- [ ] 이메일 가입 응답과 `GET /api/auth/me` 응답에 `handle` 이 있고, 단어 목록의 단어 하나 + 숫자 4자리(`^[a-z]{3,6}[0-9]{4}$`) 형식이다
- [ ] 아이디는 단어 목록과 숫자로만 만들어지고 이메일·이름 값이 생성에 쓰이지 않는다 (같은 난수 순서면 이메일이 달라도 같은 아이디가 나온다)
- [ ] 뽑은 아이디가 이미 있으면 다시 뽑아 겹치지 않는 아이디를 주고, 10번 연속 겹치면 가입이 503 `HANDLE_GENERATION_FAILED` 로 실패하며 `users` 행이 생기지 않는다
- [ ] 소셜 최초 가입(`/social/complete`)으로 만든 회원도 `handle` 을 받는다
- [ ] 마이그레이션 뒤 기존 회원 전원이 같은 형식의 겹치지 않는 `handle` 을 갖는다 (NOT NULL·UNIQUE, 단어는 Java 목록 안)
- [ ] `POST /api/auth/social/pending` 이 유효한 토큰에 제공자·이메일·이름·만료 시각을 돌려준다
- [ ] 만료·위조 토큰이면 401 `PENDING_SIGNUP_EXPIRED` 이고 DB 변화가 없다
- [ ] accessToken·pendingToken 은 JWS 압축 형식(base64url 세 마디) 밖의 문자가 끝에 붙거나 마디 안에 섞이면 거부한다 — 같은 토큰의 다른 문자열 표기로 인증되지 않는다 (2026-11-04 Phase 4 검증에서 추가)

### Task B (프론트 — 토큰·공통 부품)

- [ ] `frontend/src/theme/tokens.ts` 의 값이 `docs/product/design-tokens.css` 와 하나도 다르지 않다 (테스트로 대조)
- [ ] 테마 밖 색상 리터럴·매직 치수가 없다 (`check_architecture.py` DESIGN-001·002 통과, 003 위반 신규 0)
- [ ] Pretendard 가 실제로 로드된다 (`document.fonts` 확인 또는 네트워크 요청)
- [ ] 375px 폭에서 가로 스크롤이 없고, 넓은 화면에서는 가운데 한 열로 보인다
- [ ] 상태 배지가 design.md §5 의 6가지 뜻·문구·클래스로만 나온다 (예매 가능·예정·매진·마감·입금 대기·예매 확정, 공연 취소는 마감과 같은 회색)
- [ ] `data-stay` 에 해당하는 토스트 부품이 문구를 띄우고 스스로 사라진다
- [ ] 기존 화면(목록·상세·공연 등록·수정) 테스트가 새 배지 문구로 고친 뒤 모두 통과한다

### Task C (프론트 — 인증 화면)

- [ ] 로그인 401 이면 두 칸이 함께 오류 표시되고 "이메일 또는 비밀번호가 올바르지 않아요" 가 나온다
- [ ] `/login?error=social` 이면 맨 위에 "로그인이 취소됐거나 실패했어요" 가 나온다
- [ ] 로그인 성공 시 원래 가려던 화면으로 돌아간다 (기존 동작 유지)
- [ ] 비밀번호가 8자 미만이거나 72자 초과면 비밀번호 칸 아래 안내가 오류로 바뀌고 제출되지 않는다
- [ ] 비밀번호 확인이 다르면 "비밀번호가 일치하지 않아요" 가 그 칸 아래 나온다
- [ ] 가입 409 이면 이메일 칸 아래 "이미 가입된 이메일이에요" 와 로그인 링크가 나오고, 아래 "이미 계정이 있나요?" 줄이 사라진다
- [ ] 가입에 성공하면 자동으로 로그인되어 `/joined` 로 가고 "이메일 · 관객" 과 내 아이디가 보인다
- [ ] 소셜 버튼을 누른 뒤 콜백 화면에 "카카오 계정을 확인하고 있어요" 처럼 그 제공자 이름이 나온다
- [ ] 역할 선택 화면에 "카카오로 처음 가입" 과 "<이메일> (<이름>)으로 가입하고 있어요" 가 나온다
- [ ] 역할 선택 화면을 연 채 `expiresAt` 이 지나면 만료 상태(가입 시간이 지났어요 + 다시 로그인하기)로 바뀐다 (가짜 타이머로 확인)
- [ ] 대기 정보 조회나 계속하기가 401 `PENDING_SIGNUP_EXPIRED` 면 같은 만료 상태가 된다
- [ ] 역할을 고르고 계속하기를 누르면 `/joined` 로 가고 "카카오 · 관객" 과 내 아이디가 보인다
- [ ] 가입 완료 화면의 아이디 복사를 누르면 클립보드에 아이디가 들어가고 토스트 "아이디를 복사했어요 — 일행에게 붙여넣어 보내세요" 가 나온다
- [ ] 시작하기를 누르면 홈(`/`)으로 간다
- [ ] 제출 중에는 주 버튼을 다시 누를 수 없다
- [ ] 다섯 화면과 오류 상태 3종을 375px 로 띄워 `out/screens/` 의 같은 화면과 나란히 비교한 캡처가 Phase 4 증거에 있다

---

## 6. 구현 참고

**백엔드**
- `backend/src/main/resources/db/migration/V5__add_users_handle.sql` — 신규
- `backend/src/main/java/com/example/ticket_booking/domain/User.java` — `handle` 필드 추가
- `backend/src/main/java/com/example/ticket_booking/service/HandleGenerator.java` — 신규
- `backend/src/main/java/com/example/ticket_booking/service/AuthService.java` — `signUp()`·`completeSocialSignUp()` 에서 handle 생성, 대기 정보 조회 메서드 신규
- `backend/src/main/java/com/example/ticket_booking/service/UserResponse.java` — `handle` 추가
- `backend/src/main/java/com/example/ticket_booking/config/JwtTokenProvider.java:verifyPendingToken()` — `PendingSignup` 에 만료 시각 추가
- `backend/src/main/java/com/example/ticket_booking/api/AuthController.java` — `POST /social/pending` 신규
- `backend/src/main/java/com/example/ticket_booking/repository/UserRepository.java` — `existsByHandle()` 신규

**프론트**
- `frontend/src/theme/tokens.ts`·`index.ts` — 전면 재작성 (값은 `docs/product/design-tokens.css`)
- `frontend/src/components/ui/` — 신규 (시안 부품: `out/components.css`·`out/components.html`)
- `frontend/src/components/StatusBadge.tsx` — 6가지 뜻으로 교체
- `frontend/src/components/AppHeader.tsx` — 시안 `.appbar`(뒤로·제목) 로 교체
- `frontend/src/components/SocialLoginButtons.tsx` — 누를 때 제공자를 `sessionStorage` 에 기록
- `frontend/src/pages/LoginPage.tsx`·`SignUpPage.tsx`·`OAuthRedirectPage.tsx`·`RoleSelectionPage.tsx` — 시안대로 재작성
- `frontend/src/pages/JoinedPage.tsx` — 신규, `App.tsx` 에 `/joined`
- `frontend/src/auth/messages.ts` — 해요체 (시안 문구가 정본)
- `frontend/src/api/auth.ts` — `fetchPendingSignup()` 신규, `UserResponse.handle`

---

## 7. 범위 외

- 아이디·비밀번호 찾기 — 시안 정책 "이번 범위 밖"
- 본인인증·통합 계정 — 참조 서비스에 있지만 PRD 에 없다
- 아이디 직접 정하기·바꾸기 — 자동 생성으로 정했다 (§8)
- 아이디로 회원 찾기, 휴대폰 번호로 찾기 — 흐름 5(양도)
- 주최자를 고른 뒤 주최자 전용 첫 화면 — 시안 범위 밖, 지금처럼 홈으로
- 홈·공연 목록의 재디자인 — 흐름 2 (이 흐름에서는 새 테마·배지 문구만 적용)
- 다크 모드 — 시안에 없다

---

## 8. 주요 결정 사항

| 결정 | 이유 | 범위 | ADR |
|---|---|---|---|
| 디자인 정본을 `out/` 시안으로, 토큰은 `out/tokens.css` 값 그대로 | 화면을 시안과 나란히 대조하기 위해 | **전체** | ADR-0022 |
| 티켓온 아이디는 이메일과 무관한 「단어 + 숫자 4자리」로 자동 생성, 바꿀 수 없다 (2026-11-04 변경 — 처음엔 이메일 앞부분이었다) | 아이디는 단톡방 등에 공유하는 값이다. 이메일 앞부분이면 흔한 도메인을 붙여 이메일을 추측할 수 있고, 흐름 5 의 아이디 검색이 그 경로가 된다. 가입 폼에 칸을 늘리지 않는다(시안 유지). 양도에서 받는 사람을 찾는 키가 바뀌면 안 된다 | 이 기능 + 흐름 5 | — |
| 이메일 가입 뒤 프론트가 같은 자격 증명으로 바로 로그인한다 | 가입 API 응답 형태를 바꾸지 않고 시안 흐름(가입 완료 → 홈)을 만든다 | 이 기능만 | — |
| 소셜 가입 대기 정보는 POST 로 조회한다 | 토큰을 쿼리에 싣지 않는다 | 이 기능만 | ADR-0021 과 같은 이유 |
| 오류 문구는 해요체(시안 문구) | 시안 말투 | **전체** | ADR-0022 |
