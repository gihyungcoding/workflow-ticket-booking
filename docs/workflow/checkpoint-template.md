# 체크포인트 템플릿

세션이 끊겨도 작업을 이어갈 수 있게 하는 상태 스냅샷의 형식이다.

> **원본에서 바뀐 점**
> 이 템플릿은 원본 저장소에서 6개 규칙 파일에 걸쳐 **21개 블록으로 중복**되어 있었다(약 1,200줄).
> Phase 문서가 각자 자기 완결적이려 했기 때문인데, 결과적으로 한쪽을 고치면 나머지 5곳이
> 뒤처졌다. 여기서는 이 파일 하나가 정본이고, 각 Phase 스킬은 이 문서를 참조만 한다.

---

## 1. 원칙: 포인터 + 요약만 저장

체크포인트는 **데이터를 복사하지 않는다.** 실제 산출물은 `workflow_design/` 에 있고,
CP 파일은 그곳을 가리키는 포인터와 사람이 30초 안에 상황을 파악할 요약만 담는다.

```
CP 파일이 담는 것          CP 파일이 담지 않는 것
├── 메타데이터              ├── 시나리오 본문
├── 한 줄 작업 요약          ├── 테스트 코드
├── 완료/진행/차단 목록       ├── 검증 리포트 전문
├── 의사결정 + 이유          └── JSON 산출물 사본
├── 다음 단계 (우선순위)
└── 산출물 파일 경로
```

CP 파일이 300줄을 넘으면 요약이 아니라 사본을 만들고 있는 것이다.

---

## 2. Frontmatter 스키마 (v1.1)

```yaml
---
# === 기본 메타데이터 (필수) ===
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-001                    # 반드시 폴더명과 일치
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-01T10:30:00Z
status: ACTIVE                       # ACTIVE | SUPERSEDED | ARCHIVED

# === 작업 요약 (필수) ===
work_summary: "매장 검색 API의 GWT 시나리오 6건 작성"

# === 진행 상황 (필수) ===
progress:
  completed:
    - "happy path 시나리오 3건 작성"
    - "error 시나리오 2건 작성 (권한 없음, 빈 결과)"
    - "boundary 시나리오 1건 작성 (페이지 경계)"
  in_progress: "독립검증자 호출 준비"       # 단일 문자열
  blocked: []

# === 다음 단계 (필수) ===
next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-001.json 에 원본 그대로 저장"

# === 의사결정 기록 (필수 — 비어 있으면 [] 명시) ===
decisions:
  - decision: "페이지네이션 경계 시나리오를 1건으로 통합"
    rationale: "첫 페이지/마지막 페이지가 같은 코드 경로를 타므로 분리 이득 없음"
    alternatives_considered: ["첫/마지막 각각 1건", "생략"]
    impact: "시나리오 수 7 → 6"

# === 복구 정보 (필수) ===
recovery_prerequisites:              # 이 CP로 복구할 때 필요한 선행 CP
  - CP-1.3

# === 실행 컨텍스트 (권장) ===
execution_context:
  test_command: "pytest tests/test_store_search.py"
  build_command: "npm run build"
  env_required: []
  main_files:
    - "src/api/store_search.py"

# === 무결성 (필수) ===
integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-001.md"

# === 승인 트레이스 (HITL 체크포인트만) ===
approval:
  approved_at: 2026-09-01T11:02:00Z
  decision: APPROVE                  # hitl-protocol.md 의 decision 값
  comment: "edge case 2건 추가 후 승인"
---
```

### 필드별 복구 가치

| 필드 | 필수 | 복구 시 무엇을 해결하나 |
|---|---|---|
| `checkpoint_id` | O | 어디서 재개할지 식별 |
| `work_summary` | O | 상황 파악 (긴 본문을 안 읽어도 됨) |
| `progress.completed` | O | **같은 작업 반복 방지** |
| `progress.in_progress` | O | 재개 지점 명확화 |
| `decisions` | O | **같은 결정 재논의 방지** |
| `next_steps` | O | 우선순위 있는 재개 순서 |
| `execution_context` | 권장 | 테스트/빌드 명령 재탐색 불필요 |
| `approval` | HITL만 | **재승인 불필요** — 사람을 두 번 부르지 않는다 |
| `integrity.source_files` | O | 참조 산출물이 실제로 있는지 확인 |

> **`checkpoint_hash` 는 두지 않는다.**
> 원본 스키마에는 `integrity.checkpoint_hash` 필드가 있었으나 실제로는 전부
> `"sha256:pending"` placeholder로 남아 한 번도 계산되지 않았다. 계산하지 않을 필드를
> 스키마에 두면 "채워져 있다"는 착각만 만든다. 필요해지면 그때 스크립트와 함께 추가한다.

---

## 3. 본문 구조

Frontmatter 아래에는 다음 3개 섹션만 둔다.

```markdown
## 무엇을 했나

<work_summary 를 2~4문장으로 풀어 쓴다. 왜 이렇게 했는지 포함.>

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-001.md` | 시나리오 SoT |
| `workflow_design/05_scenario/SCENARIO_TASK-001.json` | 파생 JSON |

## 재개 방법

1. 위 산출물 파일을 읽는다
2. `scenario-validator` 서브에이전트를 호출한다
3. 결과를 `validator/VALIDATION_TASK-001.json` 에 저장한다
```

---

## 4. 저장 절차

```bash
REPO_ROOT="$(git rev-parse --show-toplevel)"
CP_DIR="$REPO_ROOT/memory-bank/$TASK_ID/checkpoints/phase2a"

mkdir -p "$CP_DIR"
# CP 파일 작성 (Write 도구)
# artifact-paths.md 의 고정 슬러그 목록에서 파일명을 고른다 — 자유 작명 금지

git add "$REPO_ROOT/memory-bank/$TASK_ID"
git commit -m "chore(memory-bank): $TASK_ID CP-2.2 체크포인트"
```

체크포인트를 저장하면 **`activeContext.md` 의 `last_checkpoint` 와 `phase` 도 함께 갱신**한다
(→ `memory-bank-spec.md`). 둘이 어긋나면 복구 시 어느 쪽을 믿을지 알 수 없다.

---

## 5. status 값의 의미

| 값 | 언제 |
|---|---|
| `ACTIVE` | 현재 유효한 체크포인트 |
| `SUPERSEDED` | 같은 CP를 재작성함 (reject 후 재시도 등). 이전 것을 지우지 말고 이 값으로 바꾼다 |
| `ARCHIVED` | 태스크 완료 후 |

재시도로 CP를 다시 쓸 때 이전 파일을 **삭제하지 않는 이유**는, 왜 첫 시도가 거부됐는지가
회고(Phase 5)의 입력이기 때문이다. 같은 파일명이면 `_retry1` 접미사를 붙인다.
