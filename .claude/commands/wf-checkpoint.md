---
description: 현재 작업 상태를 체크포인트로 저장하고 커밋한다
argument-hint: "[CP 번호, 예: CP-3.2]"
---

지금까지의 진행을 체크포인트로 남긴다. 세션을 끝내기 전이나 긴 작업 중간에 쓴다.

## 1. 대상 결정

```bash
REPO_ROOT="$(git rev-parse --show-toplevel)"
cat "$REPO_ROOT/memory-bank/<활성 TASK-ID>/activeContext.md"
```

인자로 CP 번호가 주어지면 그것을 쓴다. 없으면 현재 Phase에서 **아직 저장하지 않은
가장 이른 CP**를 고른다.

CP 번호와 파일명은 `docs/workflow/artifact-paths.md` §3의 **고정 목록**에서 고른다.
목록에 없는 번호나 슬러그를 만들지 않는다.

## 2. 작성

`docs/workflow/checkpoint-template.md` 의 스키마를 따른다.

이번 세션에서 실제로 일어난 것만 적는다. 특히:

- `progress.completed` — **완료한 것만.** 하려던 것을 적지 않는다
- `progress.in_progress` — 지금 하던 것 (단일 문자열)
- `decisions` — 이번에 내린 결정과 **이유**. 없으면 `[]`
- `next_steps` — 다음 세션이 무엇부터 할지, 우선순위 순
- `integrity.source_files` — 참조하는 산출물 경로

HITL 체크포인트라면 `approval` 블록을 포함한다 (승인을 실제로 받았을 때만).

## 3. activeContext 갱신

체크포인트만 저장하고 `activeContext.md` 를 두면 복구 시 어느 쪽이 최신인지 알 수 없다.
함께 갱신한다:

- `phase` / `phase_name`
- `last_checkpoint`
- `last_updated`
- `artifacts` (새 산출물이 생겼으면)
- 본문의 "지금 무엇을 하고 있나" / "다음 한 걸음"

## 4. 커밋

```bash
REPO_ROOT="$(git rev-parse --show-toplevel)"
git add "$REPO_ROOT/memory-bank/<TASK-ID>" workflow_design/
git commit -m "chore(memory-bank): <TASK-ID> <CP 번호> 체크포인트"
```

**커밋까지가 이 명령의 범위다.** untracked 체크포인트는 세션이 끊기면 복구되지 않으므로
저장만 하고 끝내지 않는다.

## 5. 보고

```
CP-3.2_tests-green 저장

  완료   최소 구현 2파일, 대상 테스트 6/6 통과
  진행   리팩토링 검토 중
  다음   쿼리 로직을 리포지토리 계층으로 이동
  커밋   a3f21c9
```
