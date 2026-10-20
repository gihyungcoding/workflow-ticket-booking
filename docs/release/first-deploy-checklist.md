# 첫 배포 체크리스트 — 연습 · 무료 인프라

**상태**: 부분 완료 (2026-10-20) — 배포·스모크 완료, 롤백·알림 확인과 틀린 키 빌드 검증은 남음
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
- [ ] 쓰기 키를 만들어 비밀번호 관리자에 보관했다 — `openssl rand -hex 24`. **저장소에 커밋하지 않는다**
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

- [ ] 마이그레이션은 V1·V2 두 개다 (`V1__create_performance`, `V2__create_seat`). 빈 DB 라 기동 시 Flyway 가 둘 다 적용해야 한다

### 4-2. 백엔드 — Render  `사람`(계정·연동) + `에이전트`(설정값)

- [ ] 계정 생성. Free 배포에도 **카드 등록이 필요했다** (§10). 등록 후 Billing 화면에서 **현재 요금이 $0 인지, 유료 인스턴스가 하나도 없는지** 확인한다
- [ ] GitHub 저장소 연동 (연동 승인 화면은 사람이 한다)
- [ ] 웹 서비스 생성: 런타임 **Docker**, 인스턴스 유형 **Free**, `backend/Dockerfile` 경로를 TASK-014 가 만든 위치에 맞춘다
- [ ] 환경변수 입력 (§6 표)
- [ ] **헬스체크 경로는 비워둔다.** 지금 백엔드에는 DB 를 타지 않는 헬스 엔드포인트가 없다 (§8)
- [ ] 첫 배포 로그에서 확인: 이미지 빌드 성공 → JVM 기동 → Flyway 가 `V1`·`V2` 적용 → `Started` 로그

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

## 5. 쓰기 키는 비밀이 아니다

`VITE_WRITE_KEY` 는 빌드 시 번들에 들어가서 **누구에게나 보인다.** 이 가드는 보안이 아니라
"주소를 우연히 알게 된 요청"을 거르는 수준이다 (ADR-0016). 연습용 데이터라는 전제에서만
성립하므로, **실제 개인정보나 의미 있는 데이터를 넣지 않는다.**

## 6. 환경변수 한눈에

**백엔드 (Render)**

| 이름 | 값 | 비고 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://…?sslmode=require` | §4-1 |
| `SPRING_DATASOURCE_USERNAME` | Neon 사용자 | |
| `SPRING_DATASOURCE_PASSWORD` | Neon 비밀번호 | 비밀 |
| `CORS_ALLOWED_ORIGINS` | `https://<Pages 이름>.pages.dev` | 쉼표로 여러 개. 와일드카드 금지 |
| `WRITE_GUARD_ENABLED` | `true` | 켜고 키가 비면 기동 실패 |
| `WRITE_API_KEY` | §1 에서 만든 키 | 비밀 |
| `PORT` | **직접 넣지 않는다** | Render 가 주입한다 |

**프론트 (Pages 빌드 환경변수)**

| 이름 | 값 | 비고 |
|---|---|---|
| `VITE_API_BASE_URL` | `https://<Render 이름>.onrender.com` | **끝에 `/` 없이** |
| `VITE_WRITE_KEY` | 위 `WRITE_API_KEY` 와 같은 값 | 번들에 노출됨 (§5) |
| `NODE_VERSION` | `22` | §4-3 |

## 7. 스모크 테스트  `에이전트`

주소를 변수로 두고 순서대로 실행한다. 각 줄의 **기대값과 다르면 거기서 멈추고 §9 에 적는다.**

```bash
BACKEND="https://<Render 이름>.onrender.com"
FRONT="https://<Pages 이름>.pages.dev"
KEY="<WRITE_API_KEY>"
JSON='Content-Type: application/json'

# 1. 조회 — 첫 요청이라면 콜드스타트 시간이 여기서 측정된다. 기대: 200
time curl -s -o /dev/null -w "%{http_code}\n" "$BACKEND/api/performances"

# 2. 허용된 출처의 사전 요청. 기대: 2xx + access-control-allow-origin: $FRONT
curl -s -i -X OPTIONS "$BACKEND/api/performances" \
  -H "Origin: $FRONT" -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,x-write-key" | grep -iE '^HTTP|access-control'

# 3. 허용되지 않은 출처. 기대: access-control-allow-origin 이 출력되지 않음
curl -s -i -X OPTIONS "$BACKEND/api/performances" \
  -H "Origin: https://evil.example" -H "Access-Control-Request-Method: POST" | grep -i 'access-control-allow-origin' \
  || echo "(헤더 없음 — 정상)"

# 4. 키 없는 쓰기. 기대: 401
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BACKEND/api/performances" -H "$JSON" -d '{}'

# 5. 키가 있는 쓰기, 일부러 잘못된 본문. 기대: 400 — 가드는 통과했고 본문 검증에서 걸렸다는 뜻
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BACKEND/api/performances" -H "$JSON" -H "X-Write-Key: $KEY" -d '{}'

# 6. 조회는 키 없이 통과. 기대: 200
curl -s -o /dev/null -w "%{http_code}\n" "$BACKEND/api/performances"
```

4번이 401, 5번이 400 이면 가드가 의도대로다. 5번이 401 이면 키가 어긋났고(`VITE_WRITE_KEY` 와 `WRITE_API_KEY`), 4번이 400 이면 **가드가 꺼져 있다** — 환경변수를 확인한다.

**브라우저에서** (개발자 도구 Network 탭을 열고)

- [ ] 목록 화면이 뜨고, 요청이 `$BACKEND` 로 나간다 (상대 경로가 아니다)
- [ ] 콘솔에 CORS 오류가 없다
- [ ] 상세 화면에서 **새로고침** — `/performances/1` 에서 404 가 아니라 화면이 열린다
- [ ] 공연 등록을 하나 해본다 — 사전 요청(OPTIONS)이 2xx, 등록이 성공한다
- [ ] 키를 일부러 틀리게 빌드해보면 "쓰기 거부" 안내가 일반 오류와 다르게 보인다 (TASK-015 의 완료 조건)

## 8. 확인하지 못한 가설 세 가지 (ADR-0016)

공식 문서로 확인한 것이 아니다. **결과를 이 칸에 적는다.**

| # | 가설 | 어떻게 보나 | 결과 |
|---|---|---|---|
| 1 | 콜드스타트 중 응답에 CORS 헤더가 없어 **첫 요청이 CORS 오류처럼** 보인다 | 15분 이상 아무 요청도 보내지 않은 뒤 프론트를 열고 콘솔과 Network 탭을 본다 | **부분 관찰 (2026-10-20)**: 슬립 후 첫 요청이 `pending` 으로 오래 머물렀고 CORS 오류는 보이지 않았다. 새로고침하자 정상 응답(빈 목록). pending 지속 시간은 재지 못했다 — 가설 1번은 **확정하지 못했다** |
| 2 | Render 무료 인스턴스(블로그 기준 512MB)에서 Spring Boot 가 **기동된다** | 배포 로그에서 `Started`, 또는 메모리 초과로 종료됐는지 | **기동됨** (2026-10-20). `Started ... in 116.5 seconds`, Flyway V1·V2 적용, 메모리 종료 없음. 기동에 약 2분 — 콜드스타트 대기가 길다 |
| 3 | Cloudflare 가입에 **카드가 필요 없다** | 가입 과정 | **가입·Pages 프로젝트 생성·배포까지 카드 요구 없음** (2026-10-20). 단 Create app 기본 경로는 Workers 흐름이라 Pages 경로를 따로 찾아야 했다 |

## 9. 알려진 공백 — 체크리스트를 쓰며 발견한 것

- [ ] **DB 를 타지 않는 헬스 엔드포인트가 없다.** `actuator` 의존성도 없다. Render 의 헬스체크를 `/api/performances` 로 걸면 **주기적으로 DB 를 깨워** Neon 의 무료 연산 시간(월 100 CU-hours)을 소모할 수 있다. 첫 배포에서는 헬스체크를 비우고, 필요성이 확인되면 별도 태스크로 만든다
- [ ] **배포 순서 규칙이 없다.** 프론트와 백엔드가 따로 배포되므로, API 를 **추가**하는 변경은 백엔드를 먼저, **제거**하는 변경은 프론트를 먼저 배포해야 안전하다 (일반적인 관행이고 이 프로젝트에서 검증한 것은 아니다)
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
