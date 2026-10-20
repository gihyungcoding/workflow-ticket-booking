# TASK-015 시나리오

대상: `frontend/src/api/http.ts`(신규), `performances.ts`, 등록·수정 화면.
"호출 5곳" = getPerformances · getPerformance · registerPerformance · updatePerformance · cancelPerformance.

## SC-01 (happy) API 주소가 설정되면 모든 호출이 그 주소를 앞에 붙여 나간다

- **Given** `VITE_API_BASE_URL` 이 `https://api.example.com` 으로 설정돼 있다
- **When** 호출 5곳을 각각 실행한다
- **Then** 5곳의 요청 URL 이 모두 `https://api.example.com/api/...` 로 시작한다
- **And** 요청 URL 중 상대 경로(`/api/...`)로만 나간 것이 없다

| 호출 | 기대 URL |
|---|---|
| getPerformances | `https://api.example.com/api/performances` |
| getPerformance("7") | `https://api.example.com/api/performances/7` |
| registerPerformance | `https://api.example.com/api/performances` |
| updatePerformance("7") | `https://api.example.com/api/performances/7` |
| cancelPerformance("7") | `https://api.example.com/api/performances/7/cancel` |

covers: VITE_API_BASE_URL 이 설정되면 모든 API 호출이 그 주소를 앞에 붙여 나가고, 설정이 없으면 기존처럼 상대 경로(/api/...)로 나간다
flow: F1

## SC-02 (regression) 주소 설정이 없으면 기존처럼 상대 경로로 나간다

- **Given** `VITE_API_BASE_URL` 이 설정되지 않았다 (미설정, 빈 문자열, 공백만 세 경우 — 공백만은 미설정으로 취급한다)
- **When** 호출 5곳을 각각 실행한다
- **Then** 요청 URL 이 정확히 `/api/performances`, `/api/performances/7`, `/api/performances/7/cancel` 형태이다
- **And** 등록은 `POST`, 수정은 `PUT`, 취소는 `POST` 이고 조회 둘은 메서드를 지정하지 않는다(GET)
- **And** 등록·수정의 헤더는 정확히 `{"Content-Type": "application/json"}` 이고 본문은 입력 객체를 `JSON.stringify` 한 문자열이다
- **And** 취소·조회 요청에는 본문이 없다

covers: VITE_API_BASE_URL 이 설정되면 …설정이 없으면 기존처럼 상대 경로(/api/...)로 나간다; 기존 프론트 테스트가 모두 통과한다 — npm test, npm run lint
flow: F1, F5

## SC-03 (boundary) 주소 끝에 슬래시가 있어도 // 가 생기지 않는다

- **Given** `VITE_API_BASE_URL` 이 `https://api.example.com/` 이다
- **And** 다른 경우로 `https://api.example.com//` (슬래시 둘) 도 확인한다
- **When** getPerformances 를 실행한다
- **Then** 요청 URL 은 `https://api.example.com/api/performances` 이다
- **And** 스킴(`https://`) 뒤를 제외한 URL 에 `//` 가 없다

covers: 오류: VITE_API_BASE_URL 끝에 슬래시가 있어도 요청 주소에 // 가 생기지 않는다
flow: F1

## SC-04 (happy) 쓰기 키가 있으면 쓰기 요청에만 X-Write-Key 가 붙는다

- **Given** `VITE_WRITE_KEY` 가 `k-123` 으로 설정돼 있다
- **When** 요청을 메서드별로 보낸다
- **Then** POST·PUT·PATCH·DELETE 요청에는 `X-Write-Key: k-123` 헤더가 있다
- **And** GET·HEAD 요청에는 `X-Write-Key` 헤더가 없다
- **And** 등록·수정 요청의 `Content-Type: application/json` 은 유지된다
- **And** 화면 API 로 보면 registerPerformance(POST)·updatePerformance(PUT)·cancelPerformance(POST) 에 붙고 getPerformances·getPerformance 에는 붙지 않는다

covers: VITE_WRITE_KEY 가 설정되면 쓰기 요청(POST·PUT·DELETE)에 X-Write-Key 헤더가 붙고 조회(GET)에는 붙지 않는다
flow: F2

## SC-05 (error) 쓰기 키가 없으면 헤더를 아예 보내지 않는다

- **Given** `VITE_WRITE_KEY` 가 미설정이다 (미설정, 빈 문자열, 공백만 세 경우)
- **When** 쓰기 요청(POST·PUT·DELETE)을 보낸다
- **Then** 요청 헤더에 `X-Write-Key` 항목이 존재하지 않는다 (빈 값으로 보내지도 않는다)

covers: 오류: VITE_WRITE_KEY 가 없으면 X-Write-Key 헤더를 아예 보내지 않는다 (빈 값을 보내지 않는다)
flow: F2

## SC-06 (error) 등록이 쓰기 거부(401)되면 구분된 안내가 보인다

- **Given** 공연 등록 화면에 올바른 값이 입력돼 있다
- **And** 서버가 등록 요청에 401 `{code: "WRITE_KEY_INVALID"}` 로 응답한다
- **When** 등록 버튼을 누른다
- **Then** `쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요` 안내가 표시된다
- **And** 일반 오류 문구 `등록하지 못했습니다. 다시 시도해 주세요` 는 표시되지 않는다
- **And** 안내에 `다시 시도` 버튼이 없다 (키가 틀린 상태에서는 재시도가 소용없다)
- **And** 화면은 등록 화면에 머물고 공연명 입력란의 값이 제출 전과 같다

covers: 오류: 쓰기 요청이 401 로 거부되면 일반 오류와 구분되는 안내가 화면에 표시된다
flow: F3

## SC-07 (error) 수정 저장이 쓰기 거부(401)되면 구분된 안내가 보인다

- **Given** 공연 수정 화면이 열려 있다
- **And** 서버가 수정(PUT) 요청에 401 `{code: "WRITE_KEY_INVALID"}` 로 응답한다
- **When** 저장 버튼을 누른다
- **Then** `쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요` 안내가 표시된다
- **And** `저장하지 못했습니다. 다시 시도해 주세요` 는 표시되지 않는다

covers: 오류: 쓰기 요청이 401 로 거부되면 일반 오류와 구분되는 안내가 화면에 표시된다
flow: F3

## SC-08 (error) 취소가 쓰기 거부(401)되면 구분된 안내가 보인다

- **Given** 공연 수정 화면에서 공연 취소 확인 대화상자가 열려 있다
- **And** 서버가 취소(POST) 요청에 401 `{code: "WRITE_KEY_INVALID"}` 로 응답한다
- **When** 취소를 확정한다
- **Then** 대화상자가 닫히고 `쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요` 안내가 표시된다
- **And** `취소하지 못했습니다. 다시 시도해 주세요` 는 표시되지 않는다

covers: 오류: 쓰기 요청이 401 로 거부되면 일반 오류와 구분되는 안내가 화면에 표시된다
flow: F3

## SC-09 (regression) 401 이 아닌 일반 실패는 기존 문구를 유지한다

- **Given** 서버가 등록·저장·취소 요청에 각각 500(코드 없는 바디)으로 응답한다
- **When** 각 화면에서 쓰기 동작을 시도한다
- **Then** 각각 기존 문구 `등록하지 못했습니다. 다시 시도해 주세요` / `저장하지 못했습니다. 다시 시도해 주세요` / `취소하지 못했습니다. 다시 시도해 주세요` 가 표시된다
- **And** `쓰기 권한 확인에 실패했습니다` 는 표시되지 않는다

covers: 오류: 쓰기 요청이 401 로 거부되면 일반 오류와 구분되는 안내가 화면에 표시된다 (구분의 반대편); 기존 프론트 테스트가 모두 통과한다
flow: F3, F5

## SC-12 (error) 401 이어도 쓰기 가드 코드가 아니면 일반 오류로 처리한다

- **Given** 공연 등록 화면에 올바른 값이 입력돼 있다
- **And** 서버가 등록 요청에 401 로 응답하되 바디가 아래 중 하나다

| 바디 | 의미 |
|---|---|
| `{"code": "UNAUTHORIZED", "message": "..."}` | 코드가 가드 코드가 아님 (향후 인증) |
| 빈 바디 | 코드 판별 불가 |
| JSON 이 아닌 텍스트 | 파싱 불가 |

- **When** 등록 버튼을 누른다
- **Then** 일반 오류 문구 `등록하지 못했습니다. 다시 시도해 주세요` 가 표시된다
- **And** `쓰기 권한 확인에 실패했습니다` 는 표시되지 않는다

covers: 오류: 쓰기 요청이 401 로 거부되면 일반 오류와 구분되는 안내가 화면에 표시된다 (판별은 상태가 아니라 코드 기준)
flow: F3

## SC-10 (happy) 빌드 산출물이 SPA 모드 조건을 만족한다

- **Given** 프론트 정적 자산 폴더(`frontend/public`)와 진입 문서(`frontend/index.html`)가 있다
- **When** SPA 조건을 확인한다
- **Then** `frontend/index.html` 이 존재한다
- **And** `frontend/public/404.html` 이 존재하지 않는다

(주) 자동 테스트 단언은 위 Then 두 가지까지다. 실제 `npm run build` 산출물 `dist/` 의 index.html 존재·404.html 부재는 Phase 4 에서 빌드를 실행해 증거로 남긴다. 이 시나리오의 자동 테스트는 빌드가 404.html 을 만들어낼 수 있는 입력(public/)을 본다.

covers: npm run build 산출물에 index.html 이 있고 404.html 은 없다 (Cloudflare Pages 가 SPA 모드로 동작하는 조건)
flow: F4

## SC-11 (regression) 소스에 배포 주소가 하드코딩돼 있지 않다

- **Given** `frontend/src` 의 `.ts`·`.tsx` 파일 전체
- **When** DEPLOY-001 의 금지 패턴(`https?://*.onrender.com|pages.dev|up.railway.app|fly.dev|vercel.app|netlify.app`)으로 검색한다
- **Then** 일치하는 파일이 0개이다

(주) 자동 테스트 단언은 위 Then 까지다. `check_architecture.py --id DEPLOY-001` 위반 0건은 Phase 4 증거로 남긴다.

covers: python3 scripts/check_architecture.py --id DEPLOY-001 이 위반 0건이다 (소스에 배포 주소 하드코딩 없음)
flow: F5

---

## 커버리지·판단 기록

- V11 대응: 401 이면서 코드가 다른 경우(SC-12), base 공백만(SC-02)
- 보류한 경고: SC-01·04·09 의 다중 When 은 같은 코드 경로의 표 입력이라 묶음 유지, 함수명은 프론트 API 모듈의 공개 인터페이스라 유지
- 경계값(§2-1): 문자열 env 의 "미설정·빈 문자열·공백만"(SC-02, SC-05), 끝 슬래시 1개·2개(SC-03)
- 묶은 것: 호출 5곳의 URL 검사는 같은 `request()` 경로라 SC-01 한 건의 표로 묶음. 등록·저장·취소 화면은 catch 블록이 각각 달라 SC-06/07/08 로 분리(같은 코드 경로가 아님)
- AC "기존 프론트 테스트가 모두 통과" 는 새 테스트 함수가 아니라 전체 스위트 실행 명령이 증거다. SC-02·SC-09 가 기존 동작 보존을 시나리오로 덮는다
- PATCH 는 서버 가드와 같은 집합이라 SC-04 에 포함 (PLAN unresolved 2)
