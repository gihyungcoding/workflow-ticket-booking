# TASK-019 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | flows 7 · AC 7 · 칸 82 |
| 2a Scenario | ✅ 통과 | CP-2.2 | 16건 · 칸 40 · 검증 1회차 PASS |
| 2b Red | ✅ 승인 | CP-2.5 | 16건 (red 16) |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] `GET /api/time` 이 인증 없이 200 과 주입된 `Clock` 의 시각을 `now`(ISO-8601 UTC)로 돌려주고, 응답에 `Cache-Control: no-store` 가 있다
- [ ] `posterUrl`·`zones` 없이 등록하면 지금과 같이 201 이고, 상세 `sections` 는 등록 순서대로 `stageOrder` 1, 2, … 와 `area` = 등급 이름을 갖는다
- [ ] `zones` 를 주고 등록하면 상세 `sections` 가 `zones` 를 펼친 순서의 `stageOrder` 로 정렬되고 `area`·`areaNote` 를 갖는다. `zones` 에 없는 등급은 그 뒤에 붙는다
- [ ] `posterUrl` 이 `http://`·상대 경로·501자면 400 `INVALID_POSTER_URL`, `zones` 에 없는 등급이면 400 `ZONE_GRADE_UNKNOWN`, 중복 등급이면 400 `ZONE_GRADE_DUPLICATED` 이고 공연·좌석·구역 행이 생기지 않는다
- [ ] 목록·상세 응답에 `posterUrl` 이 있고, 없는 공연은 키가 없다
- [ ] `next-opening` 이 아직 열리지 않은 공연 중 `openAt` 이 가장 이른 1건을 돌려준다. 취소된 공연·이미 열린 공연·지난 공연은 고르지 않고, 후보가 없으면 204
- [ ] V6 마이그레이션이 기존 데이터가 있는 DB 에 적용되고 기존 공연의 목록·상세 응답이 바뀌지 않는다(`posterUrl` 없음, `sections` 에 기본 구역값만 추가)
