---
description: 태스크를 머지 준비 상태로 만든다 — 최종 검사 후 PR 생성
argument-hint: "[TASK-ID]"
---

Phase 5까지 끝난 태스크를 기준 브랜치로 보낼 준비를 한다.
**머지를 실행하지는 않는다** — PR을 만들고 사람이 병합한다.

## 1. 대상 결정

인자가 없으면 현재 브랜치에서 태스크 ID를 추출한다 (`feature/task-001-...` → `TASK-001`).
그래도 모르면 `python3 scripts/wf_status.py --active` 로 후보를 보여주고 고르게 한다.

## 2. 사전 검사

```bash
python3 scripts/ship_preflight.py --task-id TASK-001
```

일곱 항목을 확인한다. **exit 0이 아니면 PR을 만들지 않는다.**

| 검사 | 실패하면 |
|---|---|
| Phase 5 완료 | 회고까지 마치고 온다 |
| 산출물 완결성 | 빠진 산출물을 만들거나 커밋한다 |
| **신선도** | **Phase 4 재검증** (아래 §3) |
| 아키텍처 제약 | Phase 3으로 돌아가 고친다 |
| 작업 트리 | 커밋하거나 되돌린다 |
| 브랜치 | feature 브랜치로 옮긴다 |
| 병합 가능 | 기준 브랜치를 rebase 한다 |

## 3. 신선도 실패 처리

가장 중요한 실패 유형이다. **사람이 승인한 코드가 아닌 것이 머지되려는 상황**이다.

```
✗ 신선도 — 승인한 코드 그대로인가
    승인 이후 소스 파일 2개가 바뀌었다
      src/services/store.py
      src/api/store_search.py
      → Phase 4 재검증이 필요하다
```

이때 사용자에게 이렇게 알린다:

```
Phase 4에서 승인받은 뒤 소스가 2개 바뀌었습니다.

  승인 커밋  a3f21c9
  현재       7d1e4b2
  변경       src/services/store.py, src/api/store_search.py

지금 머지하면 사람이 검토하지 않은 코드가 나갑니다.
Phase 4를 다시 수행해 재승인을 받아야 합니다.
```

그리고 `wf-verify` 스킬로 넘긴다. **신선도 실패를 우회하는 옵션을 제시하지 않는다** —
그게 이 게이트의 존재 이유다.

## 4. PR 본문 조립

손으로 쓰지 않는다. Phase 산출물에서 만든다. 리뷰어가 여섯 단계를 되짚지 않아도 되게 하는 것이 목적이다.

```bash
jq -r '.route, .route_reason' workflow_design/04_plan/PLAN_TASK-001.json
jq -r '.scenarios | length' workflow_design/05_scenario/SCENARIO_TASK-001.json
jq -r '.test_result.summary // .test_status' workflow_design/06_dev/DEV_TASK-001.json
jq -r '.status, .human_review.decision' workflow_design/07_verify/VERIFY_TASK-001.json
jq -r '.keep[].what' workflow_design/08_reflect/REFLECT_TASK-001.json
```

본문 형식:

```markdown
## TASK-001 매장 검색에 슈퍼바이저 권한 필터 적용

담당하지 않는 매장을 검색 결과에서 제외하고, 직접 조회 시 403을 반환합니다.

### 완료 조건

- [x] 담당하지 않는 매장은 검색 결과에 나오지 않는다
- [x] 권한 없는 매장을 직접 조회하면 403을 반환한다
- [x] 관리자의 전체 조회는 기존대로 동작한다

### 설계

route: Backend — 권한 판정과 쿼리 필터가 서버 범위
계층: Service (architecture.md §2) · 참조 ADR: ADR-0002
제약: ARCH-001 적용

### 검증

| 항목 | 결과 |
|---|---|
| 시나리오 | 6건 (happy 3 / error 2 / boundary 1), 독립검증 PASS |
| 테스트 | 18/18 통과 |
| 린트 | error 0 |
| 아키텍처 | 제약 2건 통과 |
| Phase 4 | WARN — 성능 기준 미측정 (예외 승인) |

### 회고

- Keep: 재사용 자산을 Phase 1에서 미리 식별
- Try: acceptance_criteria 에 검증 명령을 함께 적기

<details><summary>산출물</summary>

- `workflow_design/04_plan/PLAN_TASK-001.json`
- `workflow_design/05_scenario/SCENARIO_TASK-001.md`
- `workflow_design/07_verify/VERIFY_TASK-001.json`
- `memory-bank/TASK-001/checkpoints/`

</details>

Refs: TASK-001
```

WARN이나 예외 승인이 있으면 **반드시 표에 남긴다.** 리뷰어가 알아야 할 것을
접어두지 않는다.

## 5. PR 생성

```bash
BASE="$(git config workflow.baseBranch || echo develop)"
git push -u origin "$(git branch --show-current)"
gh pr create --base "$BASE" --title "TASK-001 매장 검색에 슈퍼바이저 권한 필터 적용" --body-file /tmp/pr-body.md
```

- 원격이 없거나 `gh` 가 없으면 **본문을 파일로 남기고 명령을 안내**한다. 실패로 처리하지 않는다
- 이미 PR이 있으면 새로 만들지 않고 본문만 갱신할지 묻는다 (`gh pr edit`)

푸시는 되돌리기 쉽지만 외부에 보이는 동작이다. 원격이 처음이면 사용자에게 확인을 받는다.

## 6. 기록

`activeContext.md` 에 PR 정보를 남긴다.

```yaml
pull_request:
  number: 42
  url: "https://github.com/org/repo/pull/42"
  base: develop
  opened_at: 2026-09-02
```

커밋 후 보고한다.

```
Ship 완료 — TASK-001

  검사    7/7 통과 (아키텍처 제약 없음 — 건너뜀)
  브랜치  feature/task-001-store-search-permission → develop
  PR      #42  https://github.com/org/repo/pull/42

머지는 리뷰 후 직접 하세요. 머지된 뒤 릴리스에 포함됩니다.
```

## 하지 않는 것

- ❌ **머지 실행** — 리뷰를 거쳐 사람이 한다
- ❌ 테스트 재실행 — Phase 3·4에서 이미 했고 결과가 산출물에 있다
- ❌ 신선도 실패 우회
- ❌ `--force` 푸시
