# TASK-014 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | route: Backend, flows 8건 |
| 2a Scenario | 🔄 진행 중 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] `CORS_ALLOWED_ORIGINS`에 있는 출처의 요청에는 `Access-Control-Allow-Origin`이
      그 출처로 응답되고, PUT·DELETE의 OPTIONS 사전 요청이 Content-Type·Authorization·
      X-Write-Key 헤더를 허용하며 2xx로 통과한다
- [ ] 오류: 허용 목록에 없는 출처의 요청에는 `Access-Control-Allow-Origin` 헤더가
      붙지 않는다
- [ ] `WRITE_GUARD_ENABLED=true`이고 `X-Write-Key`가 맞으면 쓰기 요청이 기존 동작
      그대로 처리된다. 가드가 켜져 있어도 GET 조회와 CORS 사전 요청은 키 없이 통과한다
- [ ] 오류: 가드가 켜진 상태에서 `X-Write-Key`가 없거나 틀리면 POST·PUT·DELETE가
      401로 거부되고 DB가 변경되지 않는다
- [ ] 오류: `WRITE_GUARD_ENABLED=true`인데 `WRITE_API_KEY`가 비어 있으면 애플리케이션이
      기동에 실패한다
- [ ] `WRITE_GUARD_ENABLED`가 없으면 가드는 꺼져 있고 기존 백엔드 테스트가 모두
      통과한다 — `./gradlew test`
- [ ] `PORT` 환경변수 값이 `server.port`로 반영된다. 설정 파일에 DB 접속 정보가
      하드코딩되어 있지 않다
- [ ] `python3 scripts/check_architecture.py --id DEPLOY-002`가 위반 0건이다
      (CORS 와일드카드 없음)
- [ ] `backend/Dockerfile`이 bootJar 산출물을 실행하고 JVM 힙 상한(-Xmx)을 지정한다.
      `docker build`로 이미지가 빌드된다 (Docker 데몬이 꺼져 있으면 WARN으로 남기고
      첫 배포에서 확인)
