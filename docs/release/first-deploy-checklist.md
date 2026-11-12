# 첫 배포 체크리스트 — 연습 · 무료 인프라

**상태**: 부분 완료 (2026-10-20) — 배포·스모크 완료, 롤백·알림 확인은 남음. 틀린 키 빌드 검증은 TASK-022 로 쓰기 키가 없어져 해당 없음 — 다음 배포는 §5-1 순서를 따른다
**근거**: ADR-0016 · TASK-014 · TASK-015
**목적**: 서비스를 내는 것이 아니라 **배포 절차에서 겪는 마찰을 기록하는 것.**
여기서 겪은 것이 `docs/release/` 의 릴리스 문서·런북의 원료가 된다 — 써보지 않은 절차를
문서로 굳히지 않기 위해, 이 체크리스트를 먼저 손으로 한 번 돌린다.

```
브라우저 ──► Cloudflare Pages (정적 React)
   │
   └─ fetch ─► Render 무료 웹 서비스 (Spring Boot, Docker) ──► Neon 무료 PostgreSQL
```

**담당 표기** — `사람`: 계정·결제수단·비밀값·승인 화면은 사람이 직접 한다. `에이전트`: 코드·문서·검증 명령.

---

## 1. 시작 조건 (하나라도 아니면 시작하지 않는다)

- [ ] TASK-014(백엔드), TASK-015(프론트)가 develop 에 머지됨
- [ ] `cd backend && ./gradlew test` 통과
- [ ] `cd frontend && npm test && npm run lint && npm run build` 통과
- [ ] `python3 scripts/check_architecture.py` 에 error 없음 (특히 `DEPLOY-001`·`DEPLOY-002`)
- [ ] `JWT_SECRET`(32바이트 이상)을 만들어 비밀번호 관리자에 보관했다 — `openssl rand -hex 32`. **저장소에 커밋하지 않는다**
- [ ] 이 체크리스트를 기록하며 진행할 준비 (아래 §9 표)

## 2. 과금 가드 — 이 중 하나라도 보이면 멈춘다

목표는 **카드 없이, 과금 없이**다. 아래 화면이 나오면 진행하지 말고 다른 호스트를 본다.

| 어디서 | 멈출 신호 |
|---|---|
| 모든 서비스 가입 | **카드 입력을 요구하는 화면** (Cloudflare 는 공식 한도 문서에 카드 언급이 없어 확인하지 못했다 — 이 지점이 확인 지점이다) |
| Render | ~~결제수단 등록~~ → **첫 실행에서 Free 배포에도 카드 등록(Stripe, $1 임시 승인)을 요구했다.** 카드를 등록하고 진행하기로 정했다 (§10). 안전장치는 **Free 인스턴스만 쓰는 것**이다 — 유료 유형을 고르거나 서비스를 더 만들지 않는다 |
| Render | 인스턴스 유형을 Free 가 아닌 것으로 선택하는 화면 |
| Neon | 업그레이드·플랜 변경 유도. 무료 한도를 넘으면 데이터가 지워지는 게 아니라 연산이 멈춘다 (공식 가격 페이지) |
| Cloudflare | Workers·Functions 유료 전환 안내 |

## 3. 이름을 먼저 정한다 — 서로의 주소를 미리 알기 위해

백엔드는 프론트 주소(CORS)가, 프론트는 백엔드 주소(API 주소)가 필요해서 순환한다.
**서비스 이름을 먼저 정하면 주소가 정해져** 이 순환이 풀린다.

| 항목 | 값 (직접 채운다) | 예상 주소 |
|---|---|---|
| Render 서비스 이름 | `br-ticket-booking` | `https://br-ticket-booking.onrender.com` |
| Pages 프로젝트 이름 | `br-ticket-booking` | `https://br-ticket-booking.pages.dev` |

- [ ] 이름이 이미 쓰이고 있으면 접미사가 붙을 수 있다. **만든 뒤 실제 주소를 확인하고** 달라졌으면 §6 의 환경변수를 고친다

## 4. 단계별 진행

### 4-1. DB — Neon  `사람`

- [ ] 계정 생성, **카드를 요구하지 않는지** 확인
- [ ] 프로젝트 생성. 리전은 **백엔드와 가까운 곳** — 어긋나면 모든 쿼리에 왕복 지연이 붙는다 (한국에서 가까운 리전이 있는지는 확인하지 못했다)
- [ ] 연결 정보를 확인하고 **직접 연결(non-pooled)** 값을 쓴다. 풀러 경유 주소는 Flyway 의 마이그레이션 잠금과 맞지 않을 수 있다 (Neon 문서 확인 필요)
- [ ] 연결 문자열을 JDBC 형식으로 바꾼다:

```
Neon 이 주는 값    postgresql://사용자:비밀번호@호스트/DB명?sslmode=require
Spring 에 줄 값    SPRING_DATASOURCE_URL      = jdbc:postgresql://호스트/DB명?sslmode=require
                  SPRING_DATASOURCE_USERNAME = 사용자
                  SPRING_DATASOURCE_PASSWORD = 비밀번호
```

`postgresql://` 주소를 통째로 `SPRING_DATASOURCE_URL` 에 넣으면 기동에 실패한다.
사용자·비밀번호는 URL 에서 분리해 따로 넣는다.

- [ ] 마이그레이션은 `backend/src/main/resources/db/migration` 의 V1~V7 이다. 기동 시 Flyway 가 아직 적용되지 않은 것만 적용한다 — 빈 DB 면 V1~V7 전부, 2026-10-20 첫 배포의 DB(V1·V2 까지)면 V3~V7. V7 의 운영 영향은 §5-2

### 4-2. 백엔드 — Render  `사람`(계정·연동) + `에이전트`(설정값)

- [ ] 계정 생성. Free 배포에도 **카드 등록이 필요했다** (§10). 등록 후 Billing 화면에서 **현재 요금이 $0 인지, 유료 인스턴스가 하나도 없는지** 확인한다
- [ ] GitHub 저장소 연동 (연동 승인 화면은 사람이 한다)
- [ ] 웹 서비스 생성: 런타임 **Docker**, 인스턴스 유형 **Free**, `backend/Dockerfile` 경로를 TASK-014 가 만든 위치에 맞춘다
- [ ] 환경변수 입력 (§6 표)
- [ ] **헬스체크 경로는 비워둔다.** 지금 백엔드에는 DB 를 타지 않는 헬스 엔드포인트가 없다 (§8)
- [ ] 배포 로그에서 확인: 이미지 빌드 성공 → JVM 기동 → Flyway 가 §4-1 의 남은 마이그레이션을 적용(마지막이 `V7`) → `Started` 로그

### 4-3. 프론트 — Cloudflare Pages  `사람`

- [ ] 계정 생성, **카드를 요구하지 않는지** 확인
- [ ] GitHub 저장소 연동, 프로젝트 이름은 §3 에서 정한 값
- [ ] 빌드 설정: 루트 디렉터리 `frontend`, 빌드 명령 `npm run build`, 출력 `dist`
- [ ] 환경변수 `NODE_VERSION=22` 를 **명시한다.** Vite 8 을 쓰는데 `engines` 도 `.nvmrc` 도 없어서 호스트 기본 Node 가 낮으면 빌드가 실패할 수 있다 (로컬은 22 에서 확인함)
- [ ] 환경변수 입력 (§6 표)
- [ ] `404.html` 이 없는지 확인 — 있으면 Pages 의 SPA 모드가 꺼진다

### 4-4. 연결 확인  `에이전트`

- [ ] 실제 발급된 두 주소가 §3 의 예상과 같은가. 다르면 `CORS_ALLOWED_ORIGINS`·`VITE_API_BASE_URL` 을 고치고 둘 다 재배포
- [ ] **Pages 미리보기 배포(`<해시>.<프로젝트>.pages.dev`)는 허용 출처가 아니다.** 미리보기에서 API 를 부르면 CORS 로 막히는 것이 정상이다

## 5. 공연 쓰기는 주최자 토큰으로 보호된다

예전의 공유 쓰기 키(`WRITE_GUARD_ENABLED`·`WRITE_API_KEY`·`X-Write-Key`)는 없어졌다 (ADR-0029, TASK-022).
공연 등록·수정·취소는 `ORGANIZER` 역할의 로그인 토큰이 있어야 하고, 수정·취소는 자기 공연만 된다.
조회(`GET`)는 지금처럼 누구나 쓴다. 백엔드에 남아 있는 옛 변수는 읽지 않으므로 지워도 된다.

### 5-1. 배포 순서 — 프론트에서 `VITE_WRITE_KEY` 를 먼저 지운다  `사람`

TASK-024 전까지 프론트 코드(`frontend/src/api/http.ts`)는 `VITE_WRITE_KEY` 가 있으면 **모든 쓰기 요청**(가입·로그인 포함)에
`X-Write-Key` 헤더를 붙인다. 이 백엔드는 CORS 허용 헤더에서 그 헤더를 뺐으므로(`Content-Type, Authorization` 만),
브라우저가 사전 요청(OPTIONS) 단계에서 그 요청을 **보내지 않는다** — 서버가 무시하는 것이 아니라 브라우저가 막는다.

그래서 이 백엔드를 배포하기 전에:

- [ ] Pages 빌드 환경변수에서 `VITE_WRITE_KEY` 를 **지우고** 프론트를 다시 빌드·배포했다
- [ ] 다시 배포한 프론트에서 로그인 요청의 사전 요청 헤더(`Access-Control-Request-Headers`)에 `x-write-key` 가 없다 (Network 탭)
- [ ] 그 다음에 백엔드를 배포했다

순서를 거꾸로 하면 프론트를 다시 배포할 때까지 **로그인·가입이 브라우저에서 실패**한다 (콘솔에 CORS 오류).

이 순서에도 짧은 공백이 있다. 프론트를 다시 배포한 뒤 백엔드 배포가 끝날 때까지는 옛 백엔드의 쓰기 가드가 살아 있어서
키 없는 쓰기(로그인·가입 포함)를 `401 WRITE_KEY_INVALID` 로 거부한다. 점검 구간으로 보고 두 배포를 이어서 한다.
`VITE_WRITE_KEY` 를 읽는 코드 자체는 TASK-024 에서 없앤다.

TASK-024 전까지는 프론트가 로그인 토큰을 쓰기 요청에 싣지 않는다. 그래서 이 백엔드가 나간 뒤에는
**브라우저에서 공연을 등록·수정·취소할 수 없다**(401). 의도된 공백이다 — 쓰기 확인은 §7 의 `curl` 스모크로 한다.

### 5-2. 이미 있는 공연은 소유자가 없다  `사람`

V7 은 공연에 소유자(`owner_id`)를 더하고 **기존 행은 비워 둔다**(NULL). 소유자를 지정하는 화면이나 API 는 없다.
그래서 이 백엔드를 배포하면 그 전에 등록된 공연(2026-10-20 스모크로 만든 것 등)은 **누구도 수정·취소할 수 없다**
(남의 공연과 같은 404). 조회는 그대로 된다.

- [ ] 연습 데이터라면 그대로 둔다
- [ ] 계속 관리해야 할 공연이 있으면, 그 주최자 계정으로 가입한 뒤 DB 에서 직접 소유자를 넣는다

```sql
UPDATE performance
   SET owner_id = (SELECT id FROM users WHERE email = '<주최자 이메일>')
 WHERE id IN (<공연 id 목록>) AND owner_id IS NULL;
```

그 주최자의 `role` 이 `ORGANIZER` 여야 수정·취소가 된다 (`users.role`).

## 6. 환경변수 한눈에

**백엔드 (Render)**

| 이름 | 값 | 비고 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://…?sslmode=require` | §4-1 |
| `SPRING_DATASOURCE_USERNAME` | Neon 사용자 | |
| `SPRING_DATASOURCE_PASSWORD` | Neon 비밀번호 | 비밀 |
| `CORS_ALLOWED_ORIGINS` | `https://<Pages 이름>.pages.dev` | 쉼표로 여러 개. 와일드카드 금지 |
| `JWT_SECRET` | §1 에서 만든 값 | 비밀. 비었거나 32바이트 미만이면 기동 실패 (`SecurityConfig`) |
| `PORT` | **직접 넣지 않는다** | Render 가 주입한다 |

**프론트 (Pages 빌드 환경변수)**

| 이름 | 값 | 비고 |
|---|---|---|
| `VITE_API_BASE_URL` | `https://<Render 이름>.onrender.com` | **끝에 `/` 없이** |
| `NODE_VERSION` | `22` | §4-3 |

### 6-1. 소셜 로그인 (선택)  `사람`

**이 절은 소셜 로그인을 켤 때만 한다.** 제공자 환경변수가 하나도 없으면 서버는 이메일 인증만으로 기동하고,
위 §6 표만으로 §7 스모크가 성립한다. 근거는 `SocialLoginConfig.java`(ADR-0013) — 이 절의 이름·규칙은 거기서 읽었다.

**백엔드 (Render) — 공통**

| 이름 | 값 | 비고 |
|---|---|---|
| `FRONTEND_BASE_URL` | `https://<Pages 이름>.pages.dev` | 제공자를 **하나라도** 켜면 필수 — 없거나 형식이 틀리면 **기동 실패**. `http`/`https` 만, 사용자 정보·쿼리·`#` 불가, 끝 `/` 는 제거됨 |
| `JWT_SECRET` | 32바이트 이상 | 비었거나 짧으면 기동 실패 (`SecurityConfig`) |

**백엔드 (Render) — 제공자별.** 제공자는 아래 값이 **전부** 있을 때만 등록된다. 하나라도 비면 그 제공자는 조용히 빠지고(기동은 된다) 버튼을 누르면 404 가 난다.

| 제공자 | 환경변수 | 콜백 URI (콘솔에 등록) | 요청 범위 |
|---|---|---|---|
| 카카오 | `KAKAO_CLIENT_ID` · `KAKAO_CLIENT_SECRET` | `https://<Render 이름>.onrender.com/login/oauth2/code/kakao` | 닉네임, 이메일 |
| 네이버 | `NAVER_CLIENT_ID` · `NAVER_CLIENT_SECRET` | `https://<Render 이름>.onrender.com/login/oauth2/code/naver` | 이름, 이메일 |
| 구글 | `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` | `https://<Render 이름>.onrender.com/login/oauth2/code/google` | profile, email |
| 애플 | `APPLE_CLIENT_ID`(Services ID) · `APPLE_TEAM_ID` · `APPLE_KEY_ID` · `APPLE_PRIVATE_KEY` | `https://<Render 이름>.onrender.com/login/oauth2/code/apple` | name, email |

`*_CLIENT_SECRET`·`APPLE_PRIVATE_KEY` 는 비밀이다 — 저장소에 커밋하지 않는다. 애플은 고정 비밀번호가 아니라 `.p8` 키로 서명한 JWT 를 쓰므로 `APPLE_PRIVATE_KEY` 에 그 키를 넣는다.

**프론트 (Pages)** — 추가할 변수는 없다. 소셜 버튼은 §6 의 `VITE_API_BASE_URL` 로 이동한다.

**제공자 콘솔에서 할 일 (사람)** — 메뉴 이름은 확인한 것이 아니라 바뀌었을 수 있다.

- [ ] 카카오: 카카오 로그인 활성화, Client Secret 발급·사용 설정, Redirect URI 등록, 동의항목(닉네임·이메일) 설정. 이메일은 비즈 앱 전환이 필요할 수 있다
- [ ] 네이버: 네이버 로그인 API 추가, 제공 정보에 이름·이메일, Callback URL 등록
- [ ] 구글: OAuth 클라이언트 ID(웹 애플리케이션) 생성, 승인된 리디렉션 URI 등록, 동의 화면 설정
- [ ] 애플: Services ID 와 Return URL 등록, `.p8` 키 발급. **Apple Developer Program(유료)과 공개 HTTPS 도메인이 필요하다** — §2 의 "카드 없이, 과금 없이" 와 맞지 않으므로 애플은 나중으로 미뤄도 된다

**확인하지 못한 것**

- [ ] **프록시 헤더.** 콜백 주소는 요청에서 `{baseUrl}/login/oauth2/code/{id}` 로 만들어지는데 `server.forward-headers-strategy` 설정이 코드에 없다. Render 는 TLS 를 앞단에서 끝내므로 `http://…` 로 만들어져 제공자가 `redirect_uri_mismatch` 로 거부할 수 있다. 그러면 `SERVER_FORWARD_HEADERS_STRATEGY=framework` 를 시험한다 — **Render 에서 돌려 본 적 없다.** 처음에는 구글이나 카카오 하나만 연결해 이것부터 확인한다
- [ ] 실제 제공자 왕복(인가 → 콜백 → 로그인)은 어떤 제공자로도 끝까지 확인하지 못했다. 카카오는 더미 클라이언트로 인가 주소 이동(302)까지만 확인했다 (TASK-013 R-1)
- [ ] 애플 `form_post` 콜백에서 세션 쿠키가 전달되는지 확인하지 못했다 (TASK-013 R-1)
- [ ] 로그인 CSRF — 콜백이 URL 의 토큰을 로그인을 시작한 브라우저에 묶지 않는다. 공개 서비스로 열기 전에 처리한다 (TASK-013 S-1, 후속 태스크)
- [ ] 설정하지 않은 제공자의 버튼도 화면에는 보이고 누르면 404 가 난다. 쓰지 않을 제공자를 숨기는 일은 아직 없다

## 7. 스모크 테스트  `에이전트`

주소를 변수로 두고 순서대로 실행한다. 각 줄의 **기대값과 다르면 거기서 멈추고 §9 에 적는다.**

```bash
BACKEND="https://<Render 이름>.onrender.com"
FRONT="https://<Pages 이름>.pages.dev"
JSON='Content-Type: application/json'

# 1. 조회 — 첫 요청이라면 콜드스타트 시간이 여기서 측정된다. 기대: 200
time curl -s -o /dev/null -w "%{http_code}\n" "$BACKEND/api/performances"

# 2. 허용된 출처의 사전 요청. 기대: 2xx + access-control-allow-origin: $FRONT
curl -s -i -X OPTIONS "$BACKEND/api/performances" \
  -H "Origin: $FRONT" -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,authorization" | grep -iE '^HTTP|access-control'

# 3. 허용되지 않은 출처. 기대: access-control-allow-origin 이 출력되지 않음
curl -s -i -X OPTIONS "$BACKEND/api/performances" \
  -H "Origin: https://evil.example" -H "Access-Control-Request-Method: POST" | grep -i 'access-control-allow-origin' \
  || echo "(헤더 없음 — 정상)"

# 4. 토큰 없는 쓰기. 기대: 401
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BACKEND/api/performances" -H "$JSON" -d '{}'

# 5. 스모크용 주최자·관객 계정을 만들고 토큰을 받는다 (이메일은 매번 새 값으로)
SUFFIX=$(date +%s)
login() { # $1=email $2=role — 가입 후 로그인해 accessToken 을 출력한다
  curl -s -o /dev/null -X POST "$BACKEND/api/auth/signup" -H "$JSON" \
    -d "{\"email\":\"$1\",\"password\":\"smoke-pass-1\",\"passwordConfirm\":\"smoke-pass-1\",\"name\":\"스모크\",\"role\":\"$2\"}"
  curl -s -X POST "$BACKEND/api/auth/login" -H "$JSON" \
    -d "{\"email\":\"$1\",\"password\":\"smoke-pass-1\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}
ORG=$(login "smoke-org-$SUFFIX@example.com" ORGANIZER)
AUD=$(login "smoke-aud-$SUFFIX@example.com" AUDIENCE)

# 6. 주최자 토큰, 일부러 잘못된 본문. 기대: 400 — 인가는 통과했고 본문 검증에서 걸렸다는 뜻
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BACKEND/api/performances" -H "$JSON" -H "Authorization: Bearer $ORG" -d '{}'

# 7. 관객 토큰. 기대: 403 (code FORBIDDEN_ROLE)
curl -s -w "\n%{http_code}\n" -X POST "$BACKEND/api/performances" -H "$JSON" -H "Authorization: Bearer $AUD" -d '{}'

# 8. 조회는 토큰 없이 통과. 기대: 200
curl -s -o /dev/null -w "%{http_code}\n" "$BACKEND/api/performances"
```

4번이 401, 6번이 400, 7번이 403 이면 역할 인가가 의도대로다. 6번이 401 이면 로그인 토큰을 못 받았다
(5번의 응답과 `JWT_SECRET` 을 확인한다). 4번이 401 이 아니면 **쓰기가 열려 있다** — 배포된 버전을 확인한다.
스모크 계정은 연습용 데이터에 남는다.

**브라우저에서** (개발자 도구 Network 탭을 열고)

- [ ] 목록 화면이 뜨고, 요청이 `$BACKEND` 로 나간다 (상대 경로가 아니다)
- [ ] 콘솔에 CORS 오류가 없다
- [ ] 상세 화면에서 **새로고침** — `/performances/1` 에서 404 가 아니라 화면이 열린다
- [ ] 로그인을 해본다 — 사전 요청(OPTIONS)이 2xx 이고 그 요청 헤더에 `x-write-key` 가 없다, 로그인이 성공한다 (§5-1)
- [ ] 공연 등록·수정·취소의 브라우저 확인은 **TASK-024 뒤에** 한다 — 그 전에는 프론트가 토큰을 싣지 않아 401 이 정상이다 (§5-1). 지금은 위 `curl` 6·7번으로 확인한다

## 8. 확인하지 못한 가설 세 가지 (ADR-0016)

공식 문서로 확인한 것이 아니다. **결과를 이 칸에 적는다.**

| # | 가설 | 어떻게 보나 | 결과 |
|---|---|---|---|
| 1 | 콜드스타트 중 응답에 CORS 헤더가 없어 **첫 요청이 CORS 오류처럼** 보인다 | 15분 이상 아무 요청도 보내지 않은 뒤 프론트를 열고 콘솔과 Network 탭을 본다 | **부분 관찰 (2026-10-20)**: 슬립 후 첫 요청이 `pending` 으로 오래 머물렀고 CORS 오류는 보이지 않았다. 새로고침하자 정상 응답(빈 목록). pending 지속 시간은 재지 못했다 — 가설 1번은 **확정하지 못했다** |
| 2 | Render 무료 인스턴스(블로그 기준 512MB)에서 Spring Boot 가 **기동된다** | 배포 로그에서 `Started`, 또는 메모리 초과로 종료됐는지 | **기동됨** (2026-10-20). `Started ... in 116.5 seconds`, Flyway V1·V2 적용, 메모리 종료 없음. 기동에 약 2분 — 콜드스타트 대기가 길다 |
| 3 | Cloudflare 가입에 **카드가 필요 없다** | 가입 과정 | **가입·Pages 프로젝트 생성·배포까지 카드 요구 없음** (2026-10-20). 단 Create app 기본 경로는 Workers 흐름이라 Pages 경로를 따로 찾아야 했다 |

## 9. 알려진 공백 — 체크리스트를 쓰며 발견한 것

- [ ] **DB 를 타지 않는 헬스 엔드포인트가 없다.** `actuator` 의존성도 없다. Render 의 헬스체크를 `/api/performances` 로 걸면 **주기적으로 DB 를 깨워** Neon 의 무료 연산 시간(월 100 CU-hours)을 소모할 수 있다. 첫 배포에서는 헬스체크를 비우고, 필요성이 확인되면 별도 태스크로 만든다
- [ ] **배포 순서 규칙이 없다.** 프론트와 백엔드가 따로 배포되므로, API 를 **추가**하는 변경은 백엔드를 먼저, **제거**하는 변경은 프론트를 먼저 배포해야 안전하다 (일반적인 관행이고 이 프로젝트에서 검증한 것은 아니다). 첫 구체 사례: TASK-022 의 `X-Write-Key` 허용 헤더 제거 — §5-1
- [ ] **롤백을 해본 적이 없다.** 이전 배포로 되돌리는 기능이 Render 에 있는지, 어디서 하는지 이번에 확인한다 — **2026-10-20: 첫 배포라 이전 배포가 없어 롤백 메뉴를 확인하지 못했다.** 두 번째 배포 뒤 다시 확인한다 (이전 배포가 있어야 보이는지는 확인 못함)
- [ ] **로그를 어디서 보는지, 알림이 오는지** 확인한다. 지금 운영 단계 자산은 비어 있다 — **확인함(2026-10-20)**: Render 는 Monitor → Logs. Cloudflare Pages 는 Workers & Pages → br-ticket-booking → Deployment details 에서 **빌드·배포 로그만** 보이고 웹 서비스(런타임) 로그는 확인되지 않았다 (정적 호스팅이라 서버 로그가 없는 것으로 보이나 확인한 사실은 아니다). 알림 수신 여부는 아직 확인하지 않았다
- [ ] **비밀값 보관 방식이 정해져 있지 않다.** 지금은 사람이 대시보드에 직접 입력한다

## 10. 기록 — 그 자리에서 적는다

끝난 뒤에 기억으로 쓰지 않는다. 한 줄이면 된다.

| 단계 | 소요 | 막힌 곳 · 예상과 다른 것 | 스타터 공백 후보 |
|---|---|---|---|
| 4-1 Neon | 5분 | Neon 의 기본 브랜치 이름이 `production` 이다 (사용자 보고) | 없음 |
| 4-2 Render | 기록 안 함 | Free($0/month, 0.1 CPU, 512MB) 선택 후 Deploy 시 카드 등록 팝업(Stripe, $1 임시 승인). §2 멈춤 신호 — 가정(카드 없이 Free 가능)이 어긋남. 대안 호스트를 조사했으나 카드 없이 쓸 수 있다고 확인된 곳이 없어 Render 에 카드를 등록하고 진행하기로 정함 | 호스트 선정 시 "카드 없이 가입·배포 가능"을 가입 화면에서 확인한 뒤 문서화 |
| 4-3 Pages | 기록 안 함 | Create app 이 Workers 흐름(`wrangler deploy`)으로 열려 Pages 경로를 따로 찾아야 했다. 주소는 예상과 같음 | Pages 진입 경로(Create 화면의 Pages 링크) 안내 |
| 4-4 연결 | 기록 안 함 | 두 주소가 §3 예상과 일치해 환경변수 수정 불필요. 슬립 후 첫 요청이 `pending` 으로 오래 머묾 (§8-1) | 콜드스타트 시 화면 안내 부재 |
| 7 스모크 | 기록 안 함 | curl 1·2·3·4·6 통과, 5번은 브라우저 등록 성공으로 대체 확인. 브라우저: 목록·CORS·등록 성공. 상세 화면 새로고침 정상(SPA 폴백 확인). 틀린 키 빌드는 미확인. **목록 화면에 "공연 등록" 진입 버튼이 없어 `/performances/new` 를 주소로 직접 입력해야 했다** — 어느 태스크·범위 외 목록에도 없는 공백 (TASK-005 는 라우트만 만들었다). **상세 화면에도 수정·취소 버튼이 없다** (같은 종류의 공백). 보류, 인증(TASK-009~) 계획 때 역할별 노출과 함께 판단 | 화면 태스크의 완료 조건에 "다른 화면에서 이 화면으로 가는 진입점" 을 넣는다 |

끝나면 위 내용을 `WORKFLOW-NOTES.md` 로 옮기고, **같은 마찰이 두 번 나온 것부터** 릴리스 문서와
`/wf-ship` 이후 단계로 올린다. 한 번 나온 것은 아직 절차가 아니라 사건이다.
