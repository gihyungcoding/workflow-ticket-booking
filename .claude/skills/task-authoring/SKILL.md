---
name: task-authoring
description: >-
  태스크를 만드는 단일 기준 — WRU 적격성 판정, 스키마 채우기, ID 채번, tasks.json 병합.
  Use when 태스크를 새로 만들거나, 요구사항 문서에서 태스크를 추출하거나, 기존 태스크를
  고칠 때. /wf-task-new 와 /wf-tasks-from-doc 이 모두 이 스킬을 쓴다.
---

# 태스크 작성

태스크를 만드는 경로는 둘(대화형·문서 추출)이지만 **판정 기준은 하나**여야 한다.
두 경로가 각자 기준을 들면 어느 쪽으로 만들었느냐에 따라 태스크 품질이 갈린다.

## MUST

1. WRU 필수 조건 5개를 **하나씩 확인**한다 (아래 §1). 통과하지 못하면 만들지 않는다
2. `acceptance_criteria` 를 최소 2건 쓴다 — 정상 1, 오류 1
3. 추정치를 4~16시간 범위로 맞춘다. 벗어나면 합치거나 나눈다
4. Backend/Frontend 가 갈리면 **태스크를 나누고** FE에 `depends_on` 을 건다
5. `tasks.json` 에 쓰기 전 `validate_tasks.py` 를 통과시킨다

## FORBIDDEN

1. ❌ **경로를 지어내기** — 입력에 없는 파일 경로를 `implementation_spec.paths` 에 넣지 않는다
2. ❌ 추정치를 범위에 맞추려고 임의로 조정하기 — 실제로 합치거나 나눈다
3. ❌ WRU 부적격인데 "일단 만들어 두기"
4. ❌ 기존 태스크 ID 재사용 또는 자릿수 변경 (`TASK-121` ≠ `TASK-0121`)
5. ❌ `tasks.json` 전체를 다시 쓰기 — 기존 배열에 **추가**한다

---

## 1. WRU 판정

`docs/workflow/task-schema.md` §1 이 정본이다. 다섯 조건을 하나씩 묻는다.

| # | 질문 | 아니라면 |
|---|---|---|
| 1 | 입력·출력이 명확해 설계를 쓸 수 있나 | 요구사항을 더 확인한다 |
| 2 | Given/When/Then 을 2개 이상 쓸 수 있나 (정상 1 + 오류 1) | 너무 작다 — 인접 작업과 합친다 |
| 3 | 실패하는 자동 테스트를 쓸 수 있나 | 수동 QA 항목이다 — 구현 태스크에 흡수시킨다 |
| 4 | **프로덕션 코드가 1개 파일 이상 바뀌나** | 분석·조사 태스크다 — 후속 태스크의 `description` 으로 흡수 |
| 5 | 빌드·테스트·린트로 완료를 판정할 수 있나 | 완료 기준을 측정 가능하게 고친다 |

조건 4가 가장 많이 걸린다. "원인 분석", "스펙 정의", "키 수집"은 단독 태스크로 만들지 않는다.

### 크기

| 추정 | 판정 |
|---|---|
| < 4h | 인접 태스크와 합칠 후보 — 워크플로우 오버헤드가 작업보다 크다 |
| 4~16h | 정상 |
| > 16h | 둘로 나눈다 |

### Backend/Frontend 분리

양쪽에 **구체적 증거**가 있을 때만 나눈다. 한쪽이 추측이면 나누지 않는다.

- 나눈다: 입력에 API 엔드포인트·에러코드·서버 로그와, 화면 요소·문구·상태 전이가 **둘 다** 명시됨
- 나누지 않는다: "서버 쪽도 손봐야 할 것 같다" 수준의 추측

나눌 때 Frontend 태스크는 `depends_on: ["<Backend 태스크 ID>"]` 를 반드시 갖는다.

---

## 2. 스키마 채우기

전체 스키마는 `docs/workflow/task-schema.md` §2 를 본다. 채울 때 주의할 곳만 여기 적는다.

```jsonc
{
  "id": "TASK-003",                    // §3 채번 규칙
  "title": "매장 검색에 권한 필터 추가",   // 무엇을 하는지. 50자 이내
  "description": "...",                // 흡수한 단계가 있으면 여기 명시
  "primary_category": "Backend",
  "sub_categories": [],                // Frontend 일 때만. 값은 CLAUDE.md 에 정의됨
  "reason_primary": "권한 판정과 쿼리 필터가 서버 범위",
  "priority": "High",
  "depends_on": [],
  "status": "todo",
  "meta": {
    "estimate": { "unit": "hour", "value": 8 },
    "risk": "medium",
    "acceptance_criteria": [           // ★ 최소 2건, 정상 1 + 오류 1
      "담당하지 않는 매장은 검색 결과에 나오지 않는다",
      "권한 없는 직접 조회는 403을 반환한다"
    ],
    "absorbed_steps": [],              // 흡수한 분석·QA·스펙 단계
    "implementation_spec": { "paths": [] },   // ★ 입력에 적힌 경로만
    "complexity": { "size_score": 3, "risk_score": 2, "total_score": 42.0, "level": "보통" }
  },
  "source_refs": {                     // 문서에서 추출한 경우
    "source_file": "docs/product/features/store-search-permission.md",
    "sections": ["1-2"],
    "sections_with_ranges": [{ "section": "1-2", "range": "Task A" }]
  }
}
```

### `acceptance_criteria` 쓰는 법

**관찰 가능한 문장**으로 쓴다. 그대로 Phase 2a 시나리오가 되고 Phase 4 가 하나씩 대조한다.

```
❌ 권한이 올바르게 처리된다
✅ 담당하지 않는 매장을 조회하면 403이 반환된다

❌ 성능이 개선된다
✅ 검색 응답 시간이 기존 대비 20% 이내로 증가한다
```

측정 수단이 없는 기준은 Phase 4 에서 WARN 으로 남는다. 쓸 때 "무슨 명령으로 확인하나"를
한 번 생각하고, 답이 없으면 기준을 고친다.

### `implementation_spec.paths` — 지어내지 않는다

입력(요구사항 문서·사용자 설명)에 **명시적으로 적힌 경로만** 넣는다. 없으면 `[]` 로 둔다.

지어낸 경로는 Phase 1이 사실로 받아들이고, 없는 파일을 전제로 설계가 진행되어
Phase 3에서야 발견된다. 경로 탐색은 Phase 1 코드베이스 분석의 일이다.

---

## 3. ID 채번

```bash
jq -r '[.[].id] | max' workflow_design/02_tasks/tasks.json
```

기존 최대값 + 1, **자릿수는 기존 태스크에 맞춘다**. 삭제된 번호를 재사용하지 않는다.

여러 건을 한 번에 만들 때는 연번으로 부여하고, 문서 안 순서(BE → FE)를 따른다.

---

## 4. tasks.json 병합

`tasks.json` 은 여러 사람·세션이 함께 쓴다. **전체를 다시 쓰지 않고 추가한다.**

```bash
# 최신 상태를 먼저 읽는다 (head/sed 로 자르지 않는다 — data-antipatterns.md §1)
jq '. | length' workflow_design/02_tasks/tasks.json

# 새 태스크를 배열에 추가
jq --slurpfile new /tmp/new-tasks.json '. + $new[0]' \
   workflow_design/02_tasks/tasks.json > /tmp/merged.json
mv /tmp/merged.json workflow_design/02_tasks/tasks.json
```

병합 후 반드시:

```bash
python3 scripts/validate_tasks.py
```

exit 0 이 아니면 **커밋하지 않고** 지적된 항목을 고친다.

---

## 5. 보고

만든 태스크를 사용자에게 이 형식으로 보여준다.

```
태스크 2건 생성

  TASK-003  Backend   매장 검색에 권한 필터 추가        8h   AC 3건
  TASK-004  Frontend  매장 검색 결과 화면 권한 표시     6h   AC 2건  ← TASK-003 선행

  검증: validate_tasks.py 통과
  다음: /wf-start TASK-003
```

WRU 판정에서 걸러낸 것이 있으면 함께 알린다 — 무엇을 왜 태스크로 만들지 않았는지가
사용자에게 필요한 정보다.
