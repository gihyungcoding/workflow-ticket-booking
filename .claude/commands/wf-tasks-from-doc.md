---
description: 요구사항 문서의 §1-2 태스크 분리 표에서 태스크를 일괄 생성한다
argument-hint: "<요구사항-문서-경로>"
---

`task-authoring` 스킬의 기준으로 태스크를 만든다.

**문서 전체를 해석해 태스크를 상상하지 않는다.** `## 1-2. 태스크 분리` 표만 읽는다.
사람이 판단한 분해가 LLM 이 추론한 분해보다 안정적이고, 문서와 태스크가 1:1로 대응해
추적이 끊기지 않는다.

## 1. 표 확인

```bash
python3 -c "
import sys; sys.path.insert(0, 'scripts')
from _utils import parse_task_split_table
from pathlib import Path
rows, err = parse_task_split_table(Path('$1'))
print('ERR:', err) if err else [print(r) for r in rows]
"
```

**표가 없으면 추출하지 않는다.** 대신 이렇게 안내한다:

```
docs/product/features/xxx.md 에 '## 1-2. 태스크 분리' 표가 없습니다.

이 표가 태스크의 정의입니다 — 문서 저자가 범위·산출물·선행 관계를 판단해 적는 것이고,
그것이 tasks.json 이 됩니다. 표 없이 문서를 해석해 태스크를 만들면 실행할 때마다
다른 분해가 나옵니다.

/wf-feature 로 태스크 분리 섹션을 먼저 채우시겠어요?
```

이미 태스크 ID가 채워진 행이 있으면 **그 행은 건너뛴다** (재실행 시 중복 방지).

## 2. 나머지 문서 읽기

표에 없는 정보를 채우기 위해 문서의 다른 섹션을 읽는다.

| 태스크 필드 | 문서 어디서 |
|---|---|
| `description` | §1-1 배경 + 해당 Task 의 범위 |
| `meta.acceptance_criteria` | §4 완료 조건의 해당 Task 체크박스 |
| `meta.implementation_spec.paths` | §5 구현 참고 중 이 Task 에 해당하는 경로 |
| `meta.absorbed_steps` | §1-2 "포함하지 않음"에서 흡수한 것으로 읽히는 항목 |
| `source_refs` | 문서 경로 + 섹션 |

§5에 적힌 경로만 쓴다. 문서에 없는 경로를 추론해 넣지 않는다.

## 3. 태스크 구성

각 표 행을 태스크로 만든다. `task-authoring` 스킬 §2 스키마를 따른다.

- `primary_category` — Task 이름의 "백엔드"/"프론트" 또는 산출물로 판정
- `sub_categories` — Frontend 인 경우 `CLAUDE.md` 에 정의된 값 중에서
- `depends_on` — 표의 "선행" 열에 적힌 Task 를 **생성된 ID로 변환**해서 넣는다
- `meta.estimate.value` — 표의 추정 열
- ID — `task-authoring` §3 규칙으로 연번 채번. 표의 행 순서(BE → FE)를 따른다

WRU 판정(스킬 §1)을 각 행에 적용한다. 부적격이면 만들지 않고 사용자에게 이유를 알린다.

## 4. 병합 및 검증

```bash
jq --slurpfile new /tmp/new-tasks.json '. + $new[0]' \
   workflow_design/02_tasks/tasks.json > /tmp/merged.json && \
   mv /tmp/merged.json workflow_design/02_tasks/tasks.json

python3 scripts/validate_tasks.py
```

exit 0 이 아니면 **커밋하지 않고** 고친다.

## 5. 문서에 ID 역기록

★ 생성된 태스크 ID를 §1-2 표의 "태스크 ID" 열에 적는다.

문서와 태스크의 양방향 추적을 유지하기 위한 것이다. 원본 저장소는 이것을 손으로
관리했고, 그래서 자주 빠졌다. 여기서는 추출이 자동으로 채운다.

`## 1-2` 표 아래에 갱신 표시도 남긴다: `[태스크 생성 2026-09-02]`

## 6. 마무리

```bash
python3 scripts/rebuild_doc_index.py
git add workflow_design/02_tasks docs/product/features && \
  git commit -m "chore(tasks): <기능명> 태스크 2건 생성"
```

```
태스크 2건 생성 — docs/product/features/store-search-permission.md

  TASK-003  Backend   매장 검색에 권한 필터 추가       8h  AC 3건
  TASK-004  Frontend  검색 결과 화면 권한 표시         6h  AC 2건  ← TASK-003 선행

  검증      validate_tasks.py 통과
  역기록    §1-2 표에 ID 기입 완료

다음: /wf-start TASK-003
```

WRU 판정에서 걸러낸 행이 있으면 무엇을 왜 만들지 않았는지 함께 알린다.
