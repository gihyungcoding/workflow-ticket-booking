# Claude Code 개발 워크플로우 스타터

아이디어에서 코드까지 이어지는 개발 워크플로우를 Claude Code 위에서 돌리기 위한 스캐폴드다.
Skills / Subagents / Hooks / Slash Commands 로 구성했고, 스택에 종속되지 않는다.

```
Foundation (프로젝트 수준)        Workflow (태스크마다)
  product.md                       Plan → Scenario → Red → Green → Verify → Reflect → Ship
  architecture.md ────참조────►            HITL#1    HITL#2       HITL#3   HITL#4     │
  constraints.yaml ───검증────►                                      │        │        ▼
  ADR-*.md         ◄──────────────── ADR 제안 ─────────────────────────────────┘      PR
  features/*.md ──── §1-2 표 ────► tasks.json
```

**두 계층**이다. Foundation 은 간헐적으로 갱신되고, Workflow 는 태스크마다 돈다.
Phase 5 회고가 Foundation 을 되먹이는 고리가 있어서 문서가 코드와 함께 늙지 않는다.

---

## 무엇이 들어 있나

| 구성요소 | 위치 | 역할 |
|---|---|---|
| **상시 컨텍스트** | `CLAUDE.md` | 매 요청 주입. 짧게 유지한다 (128줄) |
| **Foundation 문서** | `docs/product/` `docs/architecture/` `docs/decisions/` | 프로덕트 정의, 아키텍처, 기술 결정 |
| **Foundation 스킬 4개** | `.claude/skills/{product-definition,architecture-doc,adr,task-authoring}/` | 문서 작성과 태스크 생성 기준 |
| **Phase 스킬 6개 + 오케스트레이터** | `.claude/skills/wf-*/` | 각 단계의 MUST/FORBIDDEN/EXIT GATE + 절차 |
| **서브에이전트 2개** | `.claude/agents/` | 시나리오 독립검증, 코드 리뷰 |
| **슬래시 커맨드 10개** | `.claude/commands/` | `/wf-init` `/wf-feature` `/wf-tasks-from-doc` `/wf-task-new` `/wf-adr` `/wf-start` `/wf-resume` `/wf-ship` `/wf-status` `/wf-checkpoint` |
| **훅 5개** | `.claude/settings.json` + `scripts/hooks/` | 세션 복구 자동화, 경로·파일명 규약 강제 |
| **공통 규격** | `docs/workflow/` | 체크포인트·HITL·경로·태스크 스키마 |
| **스크립트 10개** | `scripts/` | 게이트 검증, 제약 검사, 상태 집계, index 생성 |
| **상태 저장소** | `memory-bank/` | 태스크별 체크포인트 (세션 복구) |
| **산출물** | `workflow_design/` | Phase별 JSON/MD |

의존성은 **Python 3.9+ 표준 라이브러리뿐**이다. 스크립트가 pre-commit 훅과 SessionStart
훅에서 실행되는데 그 시점에 가상환경이 활성화되어 있다는 보장이 없기 때문이다.

---

## 시작하기

### 1. 복사

```bash
cp -r claude-workflow-starter/{CLAUDE.md,.claude,docs,scripts,memory-bank,workflow_design} \
      /path/to/your-project/
cp claude-workflow-starter/{.gitignore,.pre-commit-config.yaml,.mcp.json.example} \
      /path/to/your-project/
```

기존 `.gitignore` 가 있으면 병합한다 (`.state/`, `.logs/`, `.backups/`, `.mcp.json` 항목).

### 2. `CLAUDE.md` 채우기

`<!-- TODO -->` 로 표시된 곳을 프로젝트에 맞게 채운다.

- 프로젝트 한 줄 설명, 스택
- **테스트·린트·빌드 명령** — Phase 3/4가 이 명령을 실제로 실행한다
- Frontend `sub_categories` 값 (앱이 여러 개인 경우)

### 3. 동작 확인

```bash
python3 scripts/wf_status.py                        # "진행 중인 태스크가 없습니다"
python3 scripts/hooks/session_start.py              # 출력 없음 (정상)
python3 scripts/hooks/check_artifact_paths.py --all # "경로 규약 위반 없음"
```

### 4. pre-commit (선택)

```bash
pip install pre-commit && pre-commit install
```

### 5. Foundation 부트스트랩

```
/wf-init
```

프로덕트 정의 → 아키텍처 → 첫 ADR 순으로 안내한다. 섹션마다 확인을 받으며 진행하고,
"나중에"라고 하면 TODO 를 남기고 넘어간다.

### 6. 브랜치 모델

git-flow 를 쓴다면 기준 브랜치를 선언한다. 미설정 시 `develop` → `main` 순으로 탐색한다.

```bash
git config workflow.baseBranch develop
```

### 7. 첫 기능

```
/wf-feature 매장검색권한        →  요구사항 문서 (§1-2 태스크 분리 포함)
/wf-tasks-from-doc docs/product/features/store-search-permission.md
/wf-start TASK-001              →  Phase 1~5
/wf-ship                        →  최종 검사 후 PR
```

급하면 `/wf-task-new "설명"` 으로 태스크 한 건만 바로 만들 수도 있다.

---

## 원본(Cursor) 대비 무엇이 달라졌나

이 스타터는 Cursor 기반 워크플로우(규칙 22,146줄)에서 코어만 추린 것이다.

### 줄어든 것

| 항목 | 원본 | 여기 |
|---|---|---|
| 워크플로우 규칙 | 22,146줄 | 4,500줄 |
| 지원 스크립트 | 51개 | 10개 (+훅 5개) |
| 표준 문서 | 323파일 / 59,747줄 | 0 (확장 지점만) |
| 상시 주입 컨텍스트 | 555줄 | 128줄 |
| 단일 Phase 최대 주입 | 5,142줄 | 235줄 (+필요 시 references) |

원본 22,146줄에는 없던 것(Foundation 문서 계층·태스크 생성 절차)이 여기에 포함되어 있으므로
단순 비교는 아니다. 같은 범위(Phase 1~5 워크플로우)만 보면 약 3,000줄이다.

가장 큰 감량은 **중복 제거**였다. 원본은 각 Phase 문서가 자기 완결적이려고 체크포인트
템플릿(21개 블록), Reject/State Machine(3중복), HITL 파싱(4중복), 안티패턴(3중복)을
반복해서 담고 있었다. Skills 는 참조 문서를 필요할 때 읽으므로 `docs/workflow/` 하나로 합쳤다.

### 버린 것

- **규칙 컴파일 파이프라인** (스크립트 5개 + 8.3MB JSON + 임베딩 인덱스)
  → 규칙이 수십 개인 초기에는 과하다. 켜는 조건은 `docs/project_standard_docs/README.md`
- **외부 트래커 연동** → 로컬 `tasks.json`
- **HITL 자연어 파싱** (4중복 200줄) → `AskUserQuestion` 이 선택지를 구조화한다
- **HITL 리포트 렌더러** (974줄 + HTML 템플릿) → Artifact
- **plan_hash 위변조 방지 체계** → 검증자 서브에이전트가 `tools` allowlist 상 파일을
  쓸 수 없으므로 위변조 경로 자체가 없다
- **모델 ID 하드코딩** → 서브에이전트 `model:` 필드

### 나아진 것

1. **세션 복구가 자동이다.** 원본은 매 세션 "task-XXXX 복구해줘"라는 지시가 필요했다.
   SessionStart 훅이 활성 태스크와 마지막 체크포인트를 자동 주입한다.
2. **경로 규약이 실행으로 강제된다.** 원본은 문서에만 있어서 오배치 파일 7개,
   이중접두 167건, 오타 디렉터리 3개가 생겼다. PreToolUse 훅이 쓰기를 차단한다.
3. **체크포인트 이름이 고정이다.** 원본은 같은 체크포인트가 6종 이상의 이름으로 존재했다
   (`CP-4.2_verification-complete` vs `_verification` 등). 19개 고정 목록으로 못박았다.
4. **태스크 ID가 한 형식이다.** 원본은 `TASK-NNN`(169건)과 `task-NNNN`(164건)이 공존해
   게이트가 "ID 표기 흡수" 로직을 따로 들고 있어야 했다.
5. **CP 번호 충돌이 없다.** 원본은 CP-2.4가 문서에 따라 "독립검증"과 "Red 코드" 둘 다였다.
6. **화이트리스트 경로 검사.** 원본의 블랙리스트는 예상 못 한 새 오타를 못 잡았다.

---

## 이 스타터가 하지 않는 것

- **CI 통합** — GitHub Actions 워크플로우는 아직 없다. `ship_preflight.py` 를 CI에서
  호출하면 되지만, 스크립트가 안정된 뒤에 붙이는 편이 낫다
- **릴리스·핫픽스** — `release/*`, `hotfix/*` 경로는 설계만 되어 있고 미구현이다.
  머지까지(`Ship`)가 현재 범위다
- **코딩 표준 강제** — 린트는 프로젝트 도구에 맡긴다
- **외부 문서 연동** — Google Slides·스프레드시트에서 태스크를 뽑는 기능은 없다.
  로컬 마크다운(`docs/product/features/*.md`)만 입력으로 받는다

---

## 디렉터리 구조

```
.
├── CLAUDE.md                       상시 컨텍스트
├── .claude/
│   ├── settings.json               훅 + 권한
│   ├── skills/
│   │   ├── product-definition/     PRD·요구사항 문서
│   │   ├── architecture-doc/       아키텍처 + 검사 가능한 제약
│   │   ├── adr/                    기술 결정 기록
│   │   ├── task-authoring/         WRU 판정·스키마·채번 (단일 기준)
│   │   └── wf-*/                   Phase 스킬 6개 + 오케스트레이터
│   ├── agents/                     scenario-validator, code-reviewer
│   └── commands/                   wf-init, wf-feature, wf-tasks-from-doc,
│                                   wf-task-new, wf-adr, wf-start, wf-resume,
│                                   wf-ship, wf-status, wf-checkpoint
├── docs/
│   ├── README.md                   새 기능을 어떻게 시작하는가
│   ├── product/
│   │   ├── product.md              무엇을 왜 만드는가
│   │   └── features/_TEMPLATE.md   요구사항 템플릿 (§1-2 태스크 분리)
│   ├── architecture/
│   │   ├── architecture.md         구조 (산문)
│   │   └── constraints.yaml        검사 가능한 제약 (기계)
│   ├── decisions/                  ADR-NNNN-*.md + 인덱스
│   ├── workflow/                   공통 규격 6개
│   └── project_standard_docs/      확장 지점 (비어 있음)
├── scripts/
│   ├── _utils.py                   경로·frontmatter·채번·파서 (단일 정본)
│   ├── wf_status.py
│   ├── rebuild_memory_bank_index.py
│   ├── rebuild_doc_index.py        ADR·기능 문서 인덱스
│   ├── validate_tasks.py           스키마·WRU·순환 의존
│   ├── validate_phase2a_gate.py
│   ├── check_architecture.py       constraints.yaml 위반 검사
│   ├── check_freshness.py          승인한 코드가 그대로인가
│   ├── ship_preflight.py           머지 전 종합 검사
│   ├── verify_workflow_artifacts.py
│   └── hooks/                      session_start, check_artifact_paths,
│                                   check_secrets, check_symlinks, check_uncommitted
├── memory-bank/                    태스크별 상태 (저장소 루트여야 한다)
└── workflow_design/                Phase 산출물
    ├── 02_tasks/  04_plan/  05_scenario/
    └── 06_dev/    07_verify/ 08_reflect/
```
