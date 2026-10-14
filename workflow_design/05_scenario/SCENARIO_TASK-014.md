# TASK-014 시나리오

## SC-01 (happy) 허용된 출처의 요청은 CORS를 통과한다

- **Given** `CORS_ALLOWED_ORIGINS` 가 `https://app.example.com,https://admin.example.com` 으로 설정되어 있다
- **When** 브라우저가 `Origin: https://admin.example.com` 으로 PUT 요청 전에 OPTIONS 사전 요청을 보낸다
- **Then** 응답에 `Access-Control-Allow-Origin: https://admin.example.com` 이 포함된다
- **And** `Access-Control-Allow-Headers` 에 `Content-Type`, `Authorization`, `X-Write-Key` 가 포함된다
- **And** 응답 상태가 2xx다

covers: CORS_ALLOWED_ORIGINS 에 있는 출처의 요청에는 Access-Control-Allow-Origin 이 그 출처로 응답되고, PUT·DELETE 의 OPTIONS 사전 요청이 Content-Type·Authorization·X-Write-Key 헤더를 허용하며 2xx 로 통과한다
flow: F1

## SC-02 (error) 허용되지 않은 출처는 CORS 헤더를 받지 못한다

- **Given** `CORS_ALLOWED_ORIGINS` 가 `https://app.example.com` 으로 설정되어 있다
- **When** `Origin: https://evil.example.com` 으로 요청한다
- **Then** 응답에 `Access-Control-Allow-Origin` 헤더가 없다

covers: 오류: 허용 목록에 없는 출처의 요청에는 Access-Control-Allow-Origin 헤더가 붙지 않는다
flow: F2

## SC-03 (boundary) CORS 구현에 와일드카드 패턴이 없다

- **Given** `CorsConfig` 구현이 작성되어 있다
- **When** `python3 scripts/check_architecture.py --id DEPLOY-002` 를 실행한다
- **Then** 위반이 0건으로 보고된다

covers: python3 scripts/check_architecture.py --id DEPLOY-002 가 위반 0건이다 (CORS 와일드카드 없음)
flow: F2

## SC-04 (happy) 가드가 켜져 있고 키가 맞으면 쓰기 요청이 정상 처리되고 변경이 반영된다

- **Given** `WRITE_GUARD_ENABLED=true`, `WRITE_API_KEY=secret123` 로 기동했고, venue 가 "서울올림픽공원"인 공연이 등록되어 있다
- **When** `X-Write-Key: secret123` 헤더와 함께 `PUT /api/performances/{id}` 로 venue 를 "부산 BEXCO" 로 바꿔 호출한다
- **Then** 200이 반환되고 응답 바디의 `venue` 가 "부산 BEXCO" 다
- **And** 같은 공연을 다시 조회(`GET /api/performances/{id}`)하면 `venue` 가 "부산 BEXCO" 로 반영되어 있다

covers: WRITE_GUARD_ENABLED=true 이고 X-Write-Key 가 맞으면 쓰기 요청이 기존 동작 그대로 처리된다. 가드가 켜져 있어도 GET 조회와 CORS 사전 요청은 키 없이 통과한다
flow: F3

## SC-05 (boundary) GET 조회는 가드가 켜져 있어도 키 없이 통과한다

- **Given** `WRITE_GUARD_ENABLED=true`, `WRITE_API_KEY=secret123` 로 기동했다
- **When** `X-Write-Key` 헤더 없이 `GET /api/performances` 를 호출한다
- **Then** 200이 반환된다

covers: WRITE_GUARD_ENABLED=true 이고 X-Write-Key 가 맞으면 쓰기 요청이 기존 동작 그대로 처리된다. 가드가 켜져 있어도 GET 조회와 CORS 사전 요청은 키 없이 통과한다
flow: F3

## SC-06 (error) 쓰기 메서드는 키가 없거나 틀리면 401로 거부된다

- **Given** `WRITE_GUARD_ENABLED=true`, `WRITE_API_KEY=secret123` 로 기동했다
- **When** 다음 조합으로 요청한다 (메서드와 키 상태가 독립적으로 깨질 수 있는 분기라 각 칸을 모두 확인한다)

  | 메서드 | X-Write-Key |
  |---|---|
  | POST | (헤더 없음) |
  | POST | `wrong` |
  | PUT | (헤더 없음) |
  | PUT | `wrong` |
  | PATCH | (헤더 없음) |
  | PATCH | `wrong` |
  | DELETE | (헤더 없음) |
  | DELETE | `wrong` |

- **Then** 표의 모든 조합에서 401이 반환된다
- **And** 각 요청 모두 DB에 어떤 변경도 생기지 않는다

covers: 오류: 가드가 켜진 상태에서 X-Write-Key 가 없거나 틀리면 POST·PUT·DELETE 가 401 로 거부되고 DB 가 변경되지 않는다
flow: F4

## SC-07 (error) 키가 비어 있으면 애플리케이션이 기동에 실패한다

- **Given** `WRITE_GUARD_ENABLED=true` 이고 `WRITE_API_KEY` 가 다음 중 하나다 (null 체크만 하는 구현과 isBlank 로 하는 구현이 서로 다르게 반응할 수 있어 둘 다 확인한다)

  | WRITE_API_KEY |
  |---|
  | (미설정, null) |
  | `"   "` (공백 문자열만) |

- **When** 각 경우로 애플리케이션을 기동한다
- **Then** 두 경우 모두 애플리케이션 컨텍스트 로딩이 실패한다

covers: 오류: WRITE_GUARD_ENABLED=true 인데 WRITE_API_KEY 가 비어 있으면 애플리케이션이 기동에 실패한다
flow: F5

## SC-08 (regression) 가드가 꺼져 있으면 기존 테스트가 모두 통과한다

- **Given** `WRITE_GUARD_ENABLED` 를 설정하지 않았다(기본값)
- **When** 기존 백엔드 테스트 스위트를 실행한다 (`./gradlew test`)
- **Then** 모든 기존 테스트가 통과한다

(기존 테스트에 이미 가드 없는 상태의 공연 등록/수정 성공 경로가 포함돼 있으므로, 가드가 꺼진 채 쓰기 요청이 정상 처리되는 것은 이 시나리오로 충분히 검증된다 — 별도의 "가드 꺼짐 + 특정 요청 성공" 시나리오를 중복으로 두지 않는다)

covers: WRITE_GUARD_ENABLED 가 없으면 가드는 꺼져 있고 기존 백엔드 테스트가 모두 통과한다 — ./gradlew test
flow: F6

## SC-09 (happy) PORT 환경변수가 server.port 로 반영된다

- **Given** `PORT=10000` 환경변수가 설정되어 있다
- **When** 애플리케이션 설정을 로딩한다
- **Then** `server.port` 값이 10000이다

covers: PORT 환경변수 값이 server.port 로 반영된다 (설정 로딩 테스트). 설정 파일에 DB 접속 정보가 하드코딩되어 있지 않다
flow: F7

## SC-10 (boundary) PORT 가 없으면 기본 포트(8080)를 쓴다

- **Given** `PORT` 환경변수가 설정되어 있지 않다
- **When** 애플리케이션 설정을 로딩한다
- **Then** `server.port` 값이 8080이다

covers: PORT 환경변수 값이 server.port 로 반영된다 (설정 로딩 테스트). 설정 파일에 DB 접속 정보가 하드코딩되어 있지 않다
flow: F7

## SC-11 (boundary) 설정 파일에 DB 접속 정보가 하드코딩돼 있지 않다

- **Given** `application.properties` 파일이 작성되어 있다
- **When** 파일 내용을 확인한다
- **Then** `spring.datasource.url`/`username`/`password` 가 파일에 리터럴 값으로 없다 (Spring 표준 환경변수 바인딩에 맡겨 아예 선언하지 않는다)

covers: PORT 환경변수 값이 server.port 로 반영된다 (설정 로딩 테스트). 설정 파일에 DB 접속 정보가 하드코딩되어 있지 않다
flow: F7

## SC-12 (boundary) Dockerfile이 bootJar 산출물을 JVM 힙 상한과 함께 실행하도록 작성되어 있다

- **Given** `backend/Dockerfile` 이 작성되어 있다
- **When** 파일 내용을 확인한다
- **Then** `ENTRYPOINT` 또는 `CMD` 라인이 bootJar 산출물을 `java -jar` 로 실행하며 `-Xmx` 옵션을 포함한다

covers: backend/Dockerfile 이 bootJar 산출물을 실행하고 JVM 힙 상한(-Xmx)을 지정한다. docker build 로 이미지가 빌드된다 — Docker 데몬이 꺼져 있으면 검증하지 못한 것으로 WARN 에 기록하고 첫 배포에서 확인한다
flow: F8

## SC-13 (boundary) Origin 헤더가 없는 요청은 CORS 로직 없이 그대로 처리된다

- **Given** `CORS_ALLOWED_ORIGINS` 가 `https://app.example.com` 으로 설정되어 있다
- **When** `Origin` 헤더 없이 `GET /api/performances` 를 호출한다 (동일 출처 또는 서버 간 호출)
- **Then** 200이 반환된다
- **And** 응답에 `Access-Control-Allow-Origin` 헤더가 없다

covers: 오류: 허용 목록에 없는 출처의 요청에는 Access-Control-Allow-Origin 헤더가 붙지 않는다
flow: F2

## SC-14 (error) CORS_ALLOWED_ORIGINS 가 비어 있으면 모든 출처가 거부된다

- **Given** `CORS_ALLOWED_ORIGINS` 가 미설정(또는 빈 문자열)이다
- **When** `Origin: https://app.example.com` 으로 요청한다
- **Then** 응답에 `Access-Control-Allow-Origin` 헤더가 없다

covers: 오류: 허용 목록에 없는 출처의 요청에는 Access-Control-Allow-Origin 헤더가 붙지 않는다
flow: F2

## SC-15 (error) 허용 출처를 부분 문자열로 포함하는 다른 출처는 거부된다

- **Given** `CORS_ALLOWED_ORIGINS` 가 `https://app.example.com` 으로 설정되어 있다
- **When** `Origin: https://app.example.com.evil.com` 으로 요청한다 (허용 출처를 접두사로 포함하지만 완전히 같지는 않음)
- **Then** 응답에 `Access-Control-Allow-Origin` 헤더가 없다

covers: 오류: 허용 목록에 없는 출처의 요청에는 Access-Control-Allow-Origin 헤더가 붙지 않는다
flow: F2

## SC-16 (boundary) OPTIONS 사전 요청은 가드가 켜져 있어도 키 없이 통과한다

- **Given** `WRITE_GUARD_ENABLED=true`, `WRITE_API_KEY=secret123` 로 기동했다
- **When** `PUT /api/performances/{id}` 앞에 `X-Write-Key` 헤더 없이 `OPTIONS` 사전 요청을 보낸다
- **Then** 2xx로 통과한다

covers: WRITE_GUARD_ENABLED=true 이고 X-Write-Key 가 맞으면 쓰기 요청이 기존 동작 그대로 처리된다. 가드가 켜져 있어도 GET 조회와 CORS 사전 요청은 키 없이 통과한다
flow: F3

## SC-18 (boundary) HEAD 요청은 가드가 켜져 있어도 키 없이 통과한다

- **Given** `WRITE_GUARD_ENABLED=true`, `WRITE_API_KEY=secret123` 로 기동했다
- **When** `X-Write-Key` 헤더 없이 `HEAD /api/performances` 를 호출한다
- **Then** 2xx로 통과한다

covers: WRITE_GUARD_ENABLED=true 이고 X-Write-Key 가 맞으면 쓰기 요청이 기존 동작 그대로 처리된다. 가드가 켜져 있어도 GET 조회와 CORS 사전 요청은 키 없이 통과한다
flow: F3

## SC-19 (error) 쉼표 목록의 빈 원소가 모든 출처를 허용하는 결함을 만들지 않는다

- **Given** `CORS_ALLOWED_ORIGINS` 가 `https://app.example.com,,https://admin.example.com` 으로 설정되어 있다 (연속 콤마로 빈 원소 포함)
- **When** `Origin: https://unrelated.example.com` 으로 요청한다 (목록의 빈 원소와 그대로 "일치"해버리는 결함이 있다면 통과함)
- **Then** 응답에 `Access-Control-Allow-Origin` 헤더가 없다

covers: 오류: 허용 목록에 없는 출처의 요청에는 Access-Control-Allow-Origin 헤더가 붙지 않는다
flow: F2

## SC-20 (boundary) 쉼표 뒤 공백이 있어도 출처가 올바르게 매칭된다

- **Given** `CORS_ALLOWED_ORIGINS` 가 `https://app.example.com, https://admin.example.com` 으로 설정되어 있다 (두 번째 값 앞에 공백)
- **When** `Origin: https://admin.example.com` 으로 요청한다
- **Then** 응답에 `Access-Control-Allow-Origin: https://admin.example.com` 이 포함된다

covers: CORS_ALLOWED_ORIGINS 에 있는 출처의 요청에는 Access-Control-Allow-Origin 이 그 출처로 응답되고, PUT·DELETE 의 OPTIONS 사전 요청이 Content-Type·Authorization·X-Write-Key 헤더를 허용하며 2xx 로 통과한다
flow: F1

## SC-21 (boundary) "true"가 아닌 값은 가드 비활성으로 처리된다

- **Given** `WRITE_GUARD_ENABLED=1` 로 기동했다 (Spring Boot 표준 boolean 바인딩에서 "true"가 아닌 값은 false)
- **When** `X-Write-Key` 헤더 없이 `POST /api/performances` 를 호출한다
- **Then** 가드가 꺼진 것과 동일하게 요청이 정상 처리된다(401 로 거부되지 않는다)

covers: WRITE_GUARD_ENABLED 가 없으면 가드는 꺼져 있고 기존 백엔드 테스트가 모두 통과한다 — ./gradlew test
flow: F6

## SC-22 (happy) Dockerfile로 이미지가 빌드된다

- **Given** `backend/Dockerfile` 이 작성되어 있다 (SC-12 에서 내용 확인됨)
- **When** `docker build` 로 이미지를 빌드한다
- **Then** 빌드가 성공한다

(Docker 데몬이 없으면 이 시나리오는 검증 불가로 WARN 처리하고 첫 배포에서 확인한다 — AC 원문에 이미 명시됨)

covers: backend/Dockerfile 이 bootJar 산출물을 실행하고 JVM 힙 상한(-Xmx)을 지정한다. docker build 로 이미지가 빌드된다 — Docker 데몬이 꺼져 있으면 검증하지 못한 것으로 WARN 에 기록하고 첫 배포에서 확인한다
flow: F8
