# TASK-015 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | 🔄 진행 중 | — | |
| 2a Scenario | ⬜ 대기 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] `VITE_API_BASE_URL` 이 설정되면 모든 API 호출이 그 주소를 앞에 붙여 나가고, 설정이 없으면 기존처럼 상대 경로(`/api/...`)로 나간다
- [ ] 오류: `VITE_API_BASE_URL` 끝에 슬래시가 있어도 요청 주소에 `//` 가 생기지 않는다
- [ ] `VITE_WRITE_KEY` 가 설정되면 쓰기 요청(POST·PUT·DELETE)에 `X-Write-Key` 헤더가 붙고 조회(GET)에는 붙지 않는다
- [ ] 오류: `VITE_WRITE_KEY` 가 없으면 `X-Write-Key` 헤더를 아예 보내지 않는다 (빈 값을 보내지 않는다)
- [ ] 오류: 쓰기 요청이 401 로 거부되면 일반 오류와 구분되는 안내가 화면에 표시된다
- [ ] `npm run build` 산출물에 `index.html` 이 있고 `404.html` 은 없다 (Cloudflare Pages 가 SPA 모드로 동작하는 조건)
- [ ] `python3 scripts/check_architecture.py --id DEPLOY-001` 이 위반 0건이다 (소스에 배포 주소 하드코딩 없음)
- [ ] 기존 프론트 테스트가 모두 통과한다 — `npm test`, `npm run lint`
