<!--
  첫 완주 기록 템플릿.

  이 워크플로우를 처음 쓸 때, 완주하면서 마찰을 그 자리에서 적는 파일이다.
  회고(Phase 5)에서 이 기록을 읽어 rule_proposals 로 정리한다 — 기억에 의존하지 않기 위해서다.

  사용법:
    cp docs/workflow/first-run-notes-template.md WORKFLOW-NOTES.md

  ★ 저장소 루트에 두고, 워크플로우 스타터로 되돌리지 않는다.
    이건 이 프로젝트에서 겪은 기록이지 스타터의 자산이 아니다.

  ★ 나중에 정리하려 하지 말고 그 자리에서 적는다.
    "이따 적어야지" 하면 왜 불편했는지가 사라진다. 한 줄이면 충분하다.
-->

# 워크플로우 첫 완주 기록

**대상 태스크**: TASK-___
**시작**: YYYY-MM-DD HH:MM

---

## 무엇을 적나

| 적을 것 | 왜 |
|---|---|
| **시각** | Phase별 소요 시간이 나온다. 어디가 무거운지 |
| **스킬 자동 호출 실패** | `description` 트리거 품질의 유일한 실측 데이터. **횟수를 센다** |
| **막힌 지점** | 버그인지 설계 문제인지 |
| **안 쓴 산출물** | 과한 부분. 감량 근거 |
| **모르겠던 것** | 문서를 찾아야 했다면 그 문서가 잘못된 위치에 있는 것 |

판정 기준은 하나다 — **진행이 되느냐 안 되느냐.**

- 진행 불가(크래시·경로 오류·훅 오작동·모순된 지시) → **즉시 고치고** `fix(workflow):` 로 커밋
- 수동으로 우회 가능(불편·장황·어색) → **여기 적고 넘어간다**

---

## Foundation

### /wf-init

- 시작 __:__ / 종료 __:__
- 1단계 문제 정의: "지금 누가 곤란한가"가 기존 시스템 개선을 전제하는 문구라
  신규 프로젝트에서 뭘 써야 할지 애매했다. 신규는 "제품이 없어서 지금 겪는 것"
  이라고 안내해야 함 → product.md 템플릿 §2, product-definition 스킬
- 4단계 범위 선택: 고른 것만 확정되는 건지, 나중에 추가는 어떻게 하는지 안내가 없었다.
  "범위는 경계이고 넓히려면 product.md 를 먼저 고친다"를 그 자리에서 알려줘야 함
  → product-definition 스킬 §4, product.md 템플릿
- 5단계 성공 기준: "측정 방법까지" + 표(기준/측정방법/목표) 형식이 신규 프로젝트에
  과했다. 운영 데이터가 없어 목표 수치를 정할 수 없는데 표가 그걸 요구하는 모양새.
  초기에는 "관찰 가능한 상태" 서술만으로 충분하다고 안내해야 함
  → product.md 템플릿 §5, product-definition 스킬
- 아키텍처 단계: 규모 가정을 묻지 않고 락 전략 같은 구현 메커니즘을 먼저 물었다.
  규모 → 계층 → 메커니즘 순서여야 하는데 3번을 1번에 물음.
  architecture.md 에 「규모 가정 + 재검토 트리거」 섹션을 §0 으로 추가하고,
  architecture-doc 스킬이 구조 이전에 규모를 먼저 묻게 할 것
  → docs/architecture/architecture.md, architecture-doc 스킬
- /wf-init 마무리: 다음 기능 예시로 가장 복잡한 것("좌석 선점")을 제안했다.
  첫 기능은 의존성이 적고 순수 로직인 쪽을 권하는 게 맞다.
  wf-init 5단계 마무리 메시지에서 "첫 기능은 선행 의존이 없고 판정 로직 중심인 것"
  기준을 함께 제시할 것
  → wf-init 커맨드 §5
- /wf-feature 진입: product.md 범위(큰 덩어리)와 feature(문서 단위) 사이가 비어 있다.
  도메인을 모르면 "공연 조회/검색"을 어디서 잘라야 할지 알 수 없다.
  인자 없이 실행하면 범위를 읽어 feature 후보를 제안해야 함
  (wf-start 에는 "태스크 없으면 후보 제시"가 있는데 wf-feature 에는 없음).
  쪼개는 기준: 사용자 여정 → 화면 단위 → 태스크 2~4개 규모
  → wf-feature 커맨드에 "인자가 없으면" 절 추가, product-definition 스킬에 분해 기준
- 시험 워크스페이스 설정: clone 으로 만들면 origin 이 starter 를 가리켜
  /wf-ship 의 git push -u origin 이 시험 브랜치를 starter 로 밀어넣는다.
  clone 직후 git remote remove origin 을 해야 함 (되돌리기는 starter 쪽에서 fetch)
  → README.md 「처음 쓴다면」 절차에 remote 제거 단계 추가

- 구분도 명시할 것: 시험 워크스페이스는 clone(히스토리 공유, cherry-pick),
  실제 프로젝트는 파일 복사 + 새 git init (starter 히스토리를 제품 저장소에 섞지 않는다)
- /wf-init 이 스택을 정하고 CLAUDE.md 에 빌드·테스트 명령까지 채웠는데,
  그 명령이 실행될 프로젝트 골격 생성을 안내하지 않았다.
  그래서 첫 태스크 Phase 1 에서 "부트스트랩을 태스크에 넣을까"를 묻게 됐고,
  제시된 두 선택지가 모두 부적절했다 (태스크에 포함 = 완료 조건과 무관,
  별도 태스크 = Red 테스트를 쓸 수 없어 WRU 위반).

  /wf-init 마지막에 "프로젝트 골격이 있는가"를 확인하고, 없으면 스캐폴딩 명령을
  안내한 뒤 CLAUDE.md 의 빌드·테스트 명령이 실제로 도는지 검증할 것.
  부트스트랩은 워크플로우 태스크가 아니라 환경 준비다.
  → wf-init 커맨드 §4~5, task-schema.md §1 금지 패턴에 "환경 구성" 추가
- /wf-start 가 feature 브랜치를 만든 뒤에야 프로젝트 골격이 없다는 걸 알게 됐다.
  부트스트랩을 그 브랜치에서 하면 PR 에 골격이 섞이므로 develop 으로 되돌아가야 했다.
  /wf-start 2번(이미 시작됐는지 확인) 다음에 "빌드 도구·테스트 명령이 실제로 도는가"를
  확인하고, 안 되면 브랜치를 만들기 전에 멈추고 안내할 것.
  → wf-start 커맨드, wf-init §5 (골격 생성 안내)

- /wf-init 이 스택(PostgreSQL)과 ADR-0004(PostgreSQL 전용 UPSERT)를 정했는데
  테스트 DB 전략을 묻지 않았다. Spring Initializr 직후 contextLoads() 가 실패하고 나서야
  드러났다. 프로덕션 DB 를 정할 때 "테스트는 무엇으로 도는가"를 함께 물어야 한다 —
  특히 DB 전용 기능을 쓰기로 한 ADR 이 있으면 그 검증 가능성이 걸린다.
  → wf-init 2단계(아키텍처), architecture-doc 스킬

### /wf-feature

- 시작 __:__ / 종료 __:__
- §1-2 태스크 분리를 쓰는 데 걸린 시간:
-

### /wf-tasks-from-doc

- 생성된 태스크: TASK-___, TASK-___
- BE/FE 분리와 `depends_on` 이 자동으로 잡혔나?
-

---

## Phase 1 — Plan

- 시작 __:__ / 종료 __:__
- `wf-plan` 자동 호출: 예 / **아니오** (수동 지목)
- 아키텍처·ADR 참조(Step 1.5)가 실제로 도움이 됐나?
- Phase 1 진행 중 나온 ADR 과 환경 설정이 feature 브랜치에 커밋됐다.
  ADR 은 프로젝트 전체 결정이고 테스트 설정은 다른 태스크도 쓰므로 develop 에 있어야 한다.
  feature 에 두면 TASK-002 에서 테스트가 또 깨지고, ADR 은 머지 전까지 존재하지 않는
  결정이 된다.

  기준: "다른 태스크도 쓰는 것이면 develop, 이 태스크의 작업물이면 feature"
  adr 스킬과 wf-plan 에 커밋 대상 브랜치 안내를 넣을 것.
  Phase 1 에서 ADR 이 필요해지면 develop 에서 쓰고 feature 를 rebase 하는 절차도.
  → adr 스킬 §3, wf-plan Step 1.5, docs/workflow/artifact-paths.md §4 커밋 규칙

Keep — /wf-start 가 이미 시작된 태스크를 정확히 감지하고 /wf-resume 으로 안내했다.
       memory-bank 존재 확인이 의도대로 동작.

- check_architecture.py 가 "검사 대상 0개"와 "위반 0건"을 구분하지 않는다.
  glob 이 틀려 아무 파일도 매칭되지 않으면 '통과'로 보고한다.
  /wf-init 에서 ARCH-001/002 를 "검증 통과"로 보고했지만 실제로는 대상이 없었고,
  Phase 1 이 architecture.md 와 대조하고 나서야 드러났다.

  고칠 것:
  - check_architecture.py: forbidden_import 인데 매칭 파일이 0개면 WARN 으로 보고
    ("검사 대상 없음 — glob 을 확인하라")
  - architecture-doc 스킬: 코드가 없는 시점에 제약을 만들면 검증할 수 없다는 점과,
    첫 구현 태스크의 Phase 1 에서 glob 을 재확인하라는 지침
  → scripts/check_architecture.py, architecture-doc 스킬

- Phase 1 완료 보고가 숫자 요약뿐이라 설계를 검토할 수 없었다.
  "flows 6"으로는 상태 계산 우선순위가 맞는지 판단할 수 없고, PLAN.json 을 직접
  열어야 알 수 있었다. Phase 1 에는 HITL 이 없어 열어볼 유인도 약하다.

  보고에 포함할 것:
  - flows 를 id·이름·covers 까지 나열 (steps 는 접어도 됨)
  - target_files 를 경로 + 역할 한 줄로
  - unresolved 는 전부 (여기에 위험이 담긴다)
  - acceptance_criteria ↔ flows 매핑 표

## Phase 2a — Scenario

- 시작 __:__ / 종료 __:__
- `wf-scenario` 자동 호출: 예 / **아니오**
- `references/` (gwt-canonical, coverage-policy)를 읽었나?
- 독립검증자가 잡아낸 것:
- 시나리오 수: ___건 — 나중에 실제로 다 쓰였나?
  Phase 2a/4/5 는 HITL 이 있어 자료를 제시하는데, Phase 1·3 은 승인이 없어
  보고 형식이 부실하다. 승인 없는 Phase 일수록 보고가 충실해야 한다.
  → wf-plan Step 6, wf-develop Step 7

- wf-red 스킬이 동적 언어(Python)를 전제로 쓰였다. "SyntaxError/ImportError 는 Red 가
  아니다"라는 판정 기준이 Java/Kotlin/Go/Rust 같은 정적 컴파일 언어에서 성립하지 않는다.
  클래스가 없으면 테스트가 컴파일조차 안 되므로, 컴파일용 스켈레톤이 선행돼야 한다.

  고칠 것 — 판정 기준을 실패 유형이 아니라 원인으로:
    Red 다:     단언이 실패한다 (로직 없음/틀림)
    Red 아니다:  테스트 코드 자체가 잘못됐다 (오타, 잘못된 임포트)

  정적 언어 절에 스켈레톤 허용 범위를 명시:
    허용 — 선언·시그니처·필드·enum, 본문은 UnsupportedOperationException 만
    금지 — 조건 분기·계산·쿼리, 실제 값 반환(빈 리스트 포함)
    검증 — 모든 실패가 UnsupportedOperationException 이면 스켈레톤이 순수하다

  → .claude/skills/wf-red/SKILL.md (Step 3 실패 확인, FORBIDDEN)

- docs/project_standard_docs/ 를 참조하는 스킬이 0건이다. 확장 지점만 만들고
  소비 지점을 연결하지 않았다. 표준 문서를 만들어도 아무도 읽지 않는다.

  연결할 곳:
  - wf-plan Step 1.5: architecture.md·ADR 과 함께 project_standard_docs/ 도 읽는다
    (route 에 해당하는 것만 — Backend 태스크면 backend/, Frontend 면 frontend/)
  - wf-develop Step 2: 구현 시 해당 표준을 따른다
  - wf-verify: 표준 준수를 CP-4.1_rule-compliance 에 기록 (체크포인트 이름이 이미 그건데
    실제로 무엇을 검사하는지 스킬에 안 적혀 있다)

  단, 디렉터리가 비어 있으면 조용히 건너뛴다 — 지금처럼 표준이 없는 초기에는
  경고를 내지 않아야 한다.
  → wf-plan, wf-develop, wf-verify 스킬

## Phase 2b — Red

- 시작 __:__ / 종료 __:__
- `wf-red` 자동 호출: 예 / **아니오**
- 테스트가 실제로 실패했나? 실패 사유가 "구현 없음"이었나?
-

## Phase 3 — Green

- 시작 __:__ / 종료 __:__
- `CLAUDE.md` 의 테스트·린트 명령이 정확했나?
-

## Phase 4 — Verify

- 시작 __:__ / 종료 __:__
- `verified_commit` 기록을 잊지 않았나?
- `check_architecture.py` 가 무언가 잡았나?
- WARN 이 남았다면 무엇 때문에:
- 산문 규칙(architecture.md "하지 않는 것" 열)이 constraints.yaml 에 인코딩됐는지
      아무도 대조하지 않는다. "SQL 직접 작성 금지"가 문서에 있는데 제약이 없어
      Phase 4 가 "위반 0건" 으로 통과시켰다. 12·17번과 같은 패턴의 3번째.
      → check_architecture.py 가 매칭 파일 수·평가 제약 수를 반드시 출력하고,
        0 이면 PASS 가 아니라 WARN/FAIL 로 판정할 것.

- architecture.md ↔ constraints.yaml 커버리지 갭이 보이지 않는다.
      /wf-init 또는 wf-verify 에 "산문 규칙 중 기계 검사 없는 항목" 목록을 내는 절차 추가.

## Phase 5 — Reflect

- 시작 __:__ / 종료 __:__
- ADR 승격 후보가 나왔나?
- 아키텍처 drift 를 발견했나?
- Phase 5 drift 검사가 단방향이다 — "문서에 있는 게 코드에 있나"만 보고
      "코드에 있는 게 문서에 있나"를 안 본다. domain/ 패키지가 계층 표에 없는데
      통과했다. → wf-reflect Step 3-2 에 역방향 검사 추가.

## Ship

- 시작 __:__ / 종료 __:__
- `ship_preflight.py` 결과:
- 신선도 검사가 stale 로 막았나? 막았다면 무엇 때문에:
-

---

## 즉시 고친 것

진행이 막혀서 그 자리에서 수정한 것. `fix(workflow):` 커밋으로 남기고 여기에도 적는다.

| 시각 | 무엇이 막혔나 | 어떻게 고쳤나 | 커밋 |
|---|---|---|---|
| | | | |

---

## 집계

완주 후 채운다.

| 항목 | 값 |
|---|---|
| 전체 소요 | |
| 가장 오래 걸린 Phase | |
| **스킬 자동 호출 실패 횟수** | ___회 / 7회 중 |
| 즉시 고친 버그 | ___건 |
| 안 쓴 산출물 | |
| 되돌아간 횟수 (reject) | |

### 워크플로우가 값을 했나

솔직하게 쓴다. "무겁다"는 결론도 유효한 결과다 — 그 경우 답은 워크플로우를 고치는 게 아니라
**어떤 태스크에 이걸 쓸지 기준을 정하는 것**이다.

-

### 스타터로 가져갈 것

Phase 5의 `rule_proposals` 로 옮길 항목. 각각 **어느 파일의 무엇을 바꿀지**까지 적는다.

### 반영 완료 (2026-09-09, starter 커밋 5건)

`claude-workflow-starter` 의 `9beeb38..dbb919a`. 같은 커밋을 이 저장소 develop 에도
cherry-pick 했다.

| 무엇 | 어느 파일 | starter 커밋 |
|---|---|---|
| 검사 대상 0건을 통과로 보고 | `check_architecture.py` (`files_scanned`/`no_target`), `wf-verify` EXIT GATE | `cebb1a8` |
| 린트 도구 없을 때 `errors: 0` 우회 | `wf-develop` EXIT GATE, `wf-verify` Step 1 | `cebb1a8` |
| 산문 규칙 ↔ constraints.yaml 커버리지 갭 | `wf-verify` Step 3.5 신설 + `references/evidence-integrity.md` | `cebb1a8` |
| 린트·테스트 명령이 Python 으로 하드코딩 | `wf-develop` Step 5, `wf-verify` Step 1, `wf-red` Step 3 | `cebb1a8` `8366c8b` |
| 태스크 종료가 tasks.json 에 반영 안 됨 | `_utils.sync_tasks_json_status()`, `rebuild_memory_bank_index.py`, `wf-reflect` Step 7 | `6778412` |
| 정적 컴파일 언어의 Red 판정 | `wf-red` Step 3 「컴파일 언어」 절 | `8366c8b` |
| 프로젝트 골격 생성 미안내 | `wf-init` 4.5 절 신설 | `dbb919a` |
| 골격 확인이 브랜치 생성 이후 | `wf-start` 2.5 절 신설 | `dbb919a` |
| 테스트 DB 전략 미확인 | `wf-init` 2 절 | `dbb919a` |
| `.SKILL.md.swp` 가 추적됨 | `.gitignore` | `9beeb38` |

### 추가 발견 (2026-09-09)

| 무엇 | 어느 파일 | 상태 |
|---|---|---|
| 기능 문서가 API 를 화면보다 먼저 쓰게 돼 있었다. 템플릿 본문은 반대로 시키는데 문서 순서가 어긋나 있었고, 화면에 필요한 필드가 데이터 모델에 없는 채로 구현까지 갔다 | `_TEMPLATE.md` §2 신설, `wf-feature`, `product-definition`, `docs/README.md` | **반영됨** |
| 화면 설계가 `§5 구현 참고` 의 하위 항목이었다 — 기획이 구현 세부사항 취급 | 같음 | **반영됨** |
| `/wf-init` 2.5 가 화면을 하나도 안 본 상태에서 색·간격을 확정하게 했다. 순서가 거꾸로 | `wf-init` 2.5, `design.md` 템플릿 | **반영됨** |
| 와이어프레임 필요 여부를 "구조가 바뀌는가"로만 판정하면 늦다. **지금 구조가 충분한가**를 먼저 물어야 한다 | `_TEMPLATE.md` §2-4 | **반영됨** |

**검증**: 새 §2-2 참조 절로 기존 기능을 소급 대조하니 데이터 모델 누락 **3건**
(가격·포스터·공연 기간)이 드러났다. 원래 찾으려던 포스터 하나가 아니었다.

★ 그 3건을 어디에도 목록으로 남기지 않았다. `product.md` §7 은 "판단이 필요한 것"
  이지 백로그가 아니고, 무엇보다 **다음 `/wf-feature` 의 2-2 가 다시 찾아낸다.**
  절차가 재생산하는 것을 목록으로 들고 있으면 두 곳이 어긋난다.
  발견 기록이 필요하면 커밋 히스토리가 그 역할을 한다.

### 남은 항목

다음 프로젝트에서 `/wf-init` 을 한 번 더 돌려보며 고친다. 대부분 문구·순서 조정이라
지금 고치면 써보지 않은 것을 문서로 굳히게 된다.

| 무엇 | 어느 파일 |
|---|---|
| 문제 정의가 기존 시스템 개선을 전제 | `product.md` 템플릿 §2, `product-definition` |
| 범위 선택 후 확장 절차 미안내 | `product-definition` §4 |
| 성공 기준 표가 신규 프로젝트에 과함 | `product.md` 템플릿 §5 |
| 규모 가정을 구조보다 나중에 물음 | `architecture.md` §0 신설, `architecture-doc` |
| 첫 기능 제안이 가장 복잡한 것 | `wf-init` §5 — **부분 반영됨** (`dbb919a`) |
| product.md 범위 → feature 분해 기준 없음 | `wf-feature` 「인자가 없으면」 절 |
| clone 시 origin 제거 안내 | `README.md` |
| ADR·환경설정의 커밋 대상 브랜치 | `adr` §3, `wf-plan` Step 1.5 |
| 승인 없는 Phase(1·3)의 보고가 부실 | `wf-plan` Step 6, `wf-develop` Step 7 |
| `project_standard_docs/` 를 읽는 스킬이 0건 | `wf-plan`, `wf-develop`, `wf-verify` |
| Phase 5 drift 검사가 단방향 | `wf-reflect` Step 3-2 |
