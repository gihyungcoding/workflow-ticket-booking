---
description: 새 개발 태스크의 워크플로우를 시작한다 (memory-bank 생성 → 브랜치 → Phase 1)
argument-hint: "<TASK-ID>"
---

인자로 받은 태스크의 워크플로우를 시작한다.

`wf-orchestrator` 스킬의 "신규 태스크 시작" 절차를 따른다.

## 절차

### 1. 태스크 확인

```bash
jq '.[] | select(.id == "$1")' workflow_design/02_tasks/tasks.json
```

- 없으면 중단하고 사용자에게 알린다. 태스크를 지어내지 않는다
- ID 형식이 `^TASK-\d{3,}$` 가 아니면 중단한다

### 2. 이미 시작됐는지 확인

```bash
REPO_ROOT="$(git rev-parse --show-toplevel)"
ls "$REPO_ROOT/memory-bank/$1/" 2>/dev/null
```

디렉터리가 이미 있으면 **새로 만들지 않고** `/wf-resume` 을 안내한다.

### 3. 출처 문서 로드

태스크에 `source_refs.source_file` 이 있으면 그 요구사항 문서를 읽는다.

```bash
jq -r '.[] | select(.id == "$1") | .source_refs.source_file // empty' \
   workflow_design/02_tasks/tasks.json
```

태스크의 `description` 보다 문서에 맥락이 훨씬 많다. 특히:

- `## 1-1. 배경` — 왜 이 작업을 하는가
- `## 5. 구현 참고` — Phase 1이 코드베이스를 찾을 때의 출발점
- `## 6. 범위 외` — 하지 말아야 할 것

Foundation 문서도 확인해 둔다 (없으면 `wf-orchestrator` 의 안내를 따른다).

```bash
ls docs/architecture/architecture.md docs/decisions/README.md 2>/dev/null
```

### 4. WRU 적격성 확인

`docs/workflow/task-schema.md` §1 의 필수 조건 5개를 확인한다.
특히 **Green 단계에서 바뀌는 프로덕션 코드가 있는지**를 본다.

부적격이면 진행하기 전에 사용자에게 알리고 확인을 받는다.

### 5. Memory Bank 생성

```bash
REPO_ROOT="$(git rev-parse --show-toplevel)"
mkdir -p "$REPO_ROOT/memory-bank/$1/checkpoints"/{phase1,phase2a,phase2b,phase3,phase4,phase5}
```

`activeContext.md` 와 `progress.md` 를 작성한다 (→ `docs/workflow/memory-bank-spec.md` §1, §2).
태스크의 `title`, `primary_category`, `acceptance_criteria` 를 옮겨 담는다.

### 6. 브랜치

```bash
git checkout -b "feature/task-<번호>-<짧은-slug>"
```

slug는 태스크 제목에서 뽑되 영문 소문자·하이픈으로 3~5단어.

### 7. 커밋

```bash
git add "$REPO_ROOT/memory-bank/$1"
git commit -m "chore(memory-bank): $1 워크플로우 시작"
```

### 8. Phase 1 진입

`wf-plan` 스킬로 넘어간다.

---

## 인자가 없으면

`workflow_design/02_tasks/tasks.json` 에서 `status: "todo"` 이고 `depends_on` 이 모두 완료된
태스크를 우선순위 순으로 최대 5건 보여주고, `AskUserQuestion` 으로 고르게 한다.

태스크가 하나도 없으면 만드는 방법을 안내한다:

```
아직 태스크가 없습니다.

  요구사항 문서부터    /wf-feature <기능이름>
  문서에서 일괄 생성    /wf-tasks-from-doc <경로>
  한 건만 바로         /wf-task-new "설명"
```
