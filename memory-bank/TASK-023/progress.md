# TASK-023 진행

| Phase | 상태 | 체크포인트 | 비고 |
|---|---|---|---|
| 1 Plan | ✅ 완료 | CP-1.3 | flows 11 · AC 12 · input_fields 7 |
| 2a Scenario | ⬜ 대기 | — | |
| 2b Red | ⬜ 대기 | — | |
| 3 Green | ⬜ 대기 | — | |
| 4 Verify | ⬜ 대기 | — | |
| 5 Reflect | ⬜ 대기 | — | |

## 완료 조건

- [ ] 마이그레이션 후 기존 행은 `published = true` 이고, 새로 등록한 공연은 `published = false` 다
- [ ] 비공개 공연은 `GET /api/performances` 목록·`/next-opening` 에 나오지 않고 `GET /api/performances/{id}` 는 404 다
- [ ] `GET /api/organizer/performances` 는 요청자 소유 공연만 `start_at DESC` 로 돌려주고, 다른 주최자 공연·소유자 NULL 공연은 없다
- [ ] `filter=DRAFT` 는 취소되지 않은 비공개 공연만, `filter=UPCOMING|OPEN|SOLD_OUT|CLOSED|CANCELLED` 는 공개 여부와 무관하게 해당 상태만 돌려준다
- [ ] `GET /api/organizer/performances/{id}` 는 자기 공연이면 `sections`·`published`·`editable`·`visibilityChangeable` 을 담고, 남의 공연·없는 공연은 404 다
- [ ] `now < openAt` 인 자기 공연에 `publish` 를 보내면 `published = true` 로 DB 에 반영되고 관객 목록에 나타난다. `unpublish` 는 반대
- [ ] `now >= openAt` 이면 `publish`·`unpublish` 가 409 `VISIBILITY_LOCKED` 이고 DB 가 바뀌지 않는다
- [ ] 취소된 공연에 `publish`·`unpublish`·수정을 보내면 409 이고 DB 가 바뀌지 않는다
- [ ] 이미 공개된 공연에 `publish` 를 보내면 200 이고 값이 그대로다
- [ ] 비공개 공연은 `now >= openAt` 이어도 수정이 200 이고 반영된다. 공개 공연은 기존처럼 409
- [ ] `editable`·`visibilityChangeable` 값이 같은 시각에 실제 수정·전환 요청의 성공 여부와 일치한다 (고정 `Clock` 으로 경계 시각 전후 확인)
- [ ] 관객 응답(`PerformanceResponse`) JSON 에 `published`·`editable`·`visibilityChangeable`·`ownerId` 키가 없다
