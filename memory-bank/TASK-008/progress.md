# TASK-008 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | 설계 라우팅: Backend |
| 2a Scenario | 🔄 진행 중 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 구역 2개(등급·가격이 다른)로 등록된 공연을 GET /api/performances/{id} 로 조회하면 응답의 sections 필드에 구역별 grade·price·좌석수가 각각 포함된다
- [ ] 존재하지 않는 공연 id로 GET /api/performances/{id} 를 호출하면 404와 PERFORMANCE_NOT_FOUND 가 반환된다 (기존 동작 유지)
- [ ] GET /api/performances (목록) 응답은 sections 필드를 포함하지 않는다 — 상세 조회에만 추가한다
