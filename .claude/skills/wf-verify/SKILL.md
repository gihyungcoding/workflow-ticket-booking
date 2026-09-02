---
name: wf-verify
description: >-
  Phase 4 — 증거를 모아 태스크 완료를 판정하고 사람 승인(HITL#3)을 받는다. acceptance_criteria
  충족, 회귀, 보안, 범위 이탈을 확인한다. Use when Phase 4를 시작할 때, "검증", "완료 판정",
  "verify" 요청을 받을 때, 또는 Phase 3 구현이 끝났을 때.
---

# Phase 4 — Verification

구현이 끝났다는 **주장**을 **증거**로 바꾼다. 명령을 실제로 실행하고 그 출력을 기록한다.

## MUST

1. 모든 판정에 **실행한 명령과 그 출력**을 근거로 붙인다
2. `acceptance_criteria` 를 하나씩 대조한다 — 통째로 "충족"이라고 하지 않는다
3. **`check_architecture.py` 를 실행한다.** `severity: error` 위반은 `status: FAIL`
4. `code-reviewer` 서브에이전트로 변경 코드를 리뷰한다
5. HITL#3 승인을 `AskUserQuestion` 으로 받는다
6. **승인 시 `verified_commit` 에 현재 HEAD 를 기록한다** — 무엇을 승인했는지의 증거

## FORBIDDEN

1. ❌ **실행하지 않은 명령의 결과를 적기** — 이 Phase의 존재 이유가 무너진다
2. ❌ 실패를 경고로 낮춰 적기
3. ❌ `status: FAIL` 인 채로 Phase 5 진입
4. ❌ 코드 수정 — 문제를 찾으면 Phase 3으로 되돌린다

## EXIT GATE

- `workflow_design/07_verify/VERIFY_<ID>.json` 존재
- `status ∈ {PASS, WARN}` — **FAIL이면 통과 불가**
- 모든 `acceptance_criteria` 에 판정과 증거가 있음
- `architecture.error_count == 0` (제약이 정의된 경우)
- `human_review.decision ∈ {APPROVE, EXCEPTION_APPROVE}`
- `human_review.verified_commit` 기록됨
- `CP-4.2`, `CP-4.3` 저장 및 커밋

---

## 절차

### Step 1 — 증거 수집

명령을 실제로 돌리고 출력을 보관한다.

```bash
pytest -v                              # 전체 테스트
pytest --cov=src --cov-report=term     # 커버리지 (설정돼 있으면)
ruff check .                           # 린트
git diff --stat main...HEAD            # 변경 규모
```

각 명령의 **exit code와 요약 출력**을 기록한다. "통과했다"가 아니라 "18 passed in 2.3s"를 적는다.

### Step 2 — acceptance_criteria 대조

기준 하나당 한 행. 증거 없는 판정은 쓰지 않는다.

| 기준 | 판정 | 증거 |
|---|---|---|
| 담당하지 않는 매장은 결과에 안 나옴 | PASS | `test_sc01`, `test_sc02` 통과 |
| 기존 조회 동작이 안 깨짐 | PASS | 기존 테스트 12개 통과 (`pytest` 출력) |
| 응답 시간 20% 이내 증가 | **WARN** | 측정 안 함 — 성능 테스트 없음 |

측정하지 않은 것은 PASS가 아니라 WARN이다. **"아마 괜찮을 것"은 증거가 아니다.**

### Step 3 — 아키텍처 제약 검증

```bash
python3 scripts/check_architecture.py --json
```

이 검사는 `docs/architecture/constraints.yaml` 에 정의된 제약을 실제로 확인한다.
Plan의 `architecture_refs.constraints_applied` 에 적힌 제약이 특히 관심 대상이다.

| 결과 | 판정 |
|---|---|
| `severity: error` 위반 | **`status: FAIL`** — Phase 3으로 되돌린다 |
| `severity: warn` 위반 | `status: WARN` 후보. 사용자에게 보여준다 |
| 제약 파일 없음 | 건너뛴다 (아직 정의하지 않은 것이지 위반이 아니다) |

**위반을 발견하면 코드를 고치지 않는다.** Phase 4는 판정만 한다.
제약 자체가 잘못됐다고 판단되면 그 근거(ADR)를 확인하고 사용자에게 알린다:

```
ARCH-001 위반 — src/api/store_search.py:12
  from ..repositories.store import StoreRepository

이 제약의 근거는 ADR-0002(계층형 아키텍처)입니다. 트랜잭션 경계를 서비스 계층에
고정하기 위한 것입니다.

두 가지입니다:
  1. 서비스를 거치도록 구현을 고친다 (Phase 3으로 롤백)
  2. 제약이 더 이상 맞지 않다면 새 ADR로 결정을 바꾸고 constraints.yaml 을 고친다
```

### Step 4 — 회귀 확인

- 전체 테스트 스위트가 통과하는가
- 변경한 함수를 호출하는 다른 코드가 있는가 (Grep)
- Plan에 없던 파일을 고쳤는가 (`DEV_<ID>.json` 의 `scope_deviations`)

`CP-4.1_rule-compliance.md` 저장 (권장).

### Step 5 — 코드 리뷰

```
Agent({
  subagent_type: "code-reviewer",
  description: "TASK-001 변경 코드 리뷰",
  run_in_background: false,
  prompt: `
TASK-001 의 변경 코드를 리뷰해주세요.

변경 파일: src/api/store_search.py, src/repositories/store.py
비교 기준: git diff main...HEAD
설계: workflow_design/04_plan/PLAN_TASK-001.json
`
})
```

리뷰 결과를 `code_review` 에 그대로 담는다. 지적 사항이 있으면:

- **결함(correctness)** → `status: FAIL`. Phase 3으로 되돌린다
- **개선 제안(quality)** → `status: WARN` 또는 Phase 5의 `try` 항목으로

### Step 6 — 보안 점검

변경 범위에 해당하는 것만 본다.

- 입력 검증 — 사용자 입력이 검증 없이 쿼리·명령에 들어가는가
- 권한 — 새 엔드포인트·메서드에 권한 체크가 있는가
- 비밀정보 — 하드코딩된 토큰·키·비밀번호가 있는가
- 로그 — 개인정보·자격증명이 로그에 남는가

해당 없으면 "해당 없음"이라고 적는다. 항목을 지우지 않는다.

### Step 7 — VERIFY JSON 저장

```jsonc
{
  "task_id": "TASK-001",
  "status": "WARN",                    // PASS | WARN | FAIL
  "status_reason": "성능 기준을 측정하지 못함",

  "evidence": {
    "tests": { "command": "pytest -v", "exit_code": 0,
               "summary": "18 passed in 2.31s" },
    "lint":  { "command": "ruff check .", "exit_code": 0,
               "summary": "All checks passed" },
    "diff":  { "command": "git diff --stat main...HEAD",
               "summary": "3 files changed, 42 insertions(+), 4 deletions(-)" }
  },

  "acceptance_criteria": [
    { "criterion": "담당하지 않는 매장은 결과에 안 나옴", "verdict": "PASS",
      "evidence": "test_sc01, test_sc02 통과" },
    { "criterion": "응답 시간 20% 이내 증가", "verdict": "WARN",
      "evidence": "측정하지 않음 — 성능 테스트 부재" }
  ],

  "architecture": {
    "command": "python3 scripts/check_architecture.py",
    "error_count": 0,
    "warn_count": 1,
    "violations": [
      { "id": "ARCH-003", "severity": "warn",
        "detail": "tests/api/ 에 해당 파일 없음" }
    ]
  },

  "regression": { "full_suite": "18/18 passed", "callers_checked": true },
  "scope_deviations": [],
  "code_review": { "findings": [] },
  "security": {
    "input_validation": "keyword 길이 제한 적용됨",
    "authorization": "require_role 데코레이터 적용",
    "secrets": "해당 없음",
    "logging": "해당 없음"
  },

  "human_review": {
    "decision": null,
    "verified_commit": null,       // 승인 시 기록 — Step 8
    "verified_at": null
  }
}
```

`CP-4.2_verification.md` 저장.

### Step 8 — HITL #3

검증 결과가 길고 표가 많으면 **Artifact로 발행**해서 링크를 준다. 짧으면 터미널 요약으로 충분하다.

```
Phase 4 — TASK-001    status: WARN

  테스트   18/18 통과
  린트     통과
  아키텍처 제약 2건 통과, 경고 1건 (ARCH-003)
  변경     3 files, +42 -4
  리뷰     지적 없음

  acceptance_criteria  2/3 PASS, 1 WARN
    ⚠ 응답 시간 20% 이내 증가 — 측정하지 않음 (성능 테스트 부재)
```

`AskUserQuestion`:

- **승인** → Phase 5
- **예외 승인** (WARN일 때만) → 예외 항목을 기록하고 Phase 5
- **구현으로 롤백** → Phase 3
- **시나리오로 롤백** → Phase 2a (시나리오 자체가 부족했던 경우)

> `status: FAIL` 이면 승인 선택지를 제시하지 않는다. FAIL은 롤백만 가능하다.

승인 후:

1. `human_review.decision` 기록
2. **`verified_commit` 에 현재 HEAD 를 기록한다**

   ```bash
   git rev-parse HEAD
   ```

   ```jsonc
   "human_review": {
     "decision": "APPROVE",
     "verified_commit": "a3f21c9…",     // 사람이 승인한 그 코드
     "verified_at": "2026-09-02T10:30:00Z"
   }
   ```

   이 값이 머지 직전 `/wf-ship` 에서 대조된다. 승인 이후 소스가 바뀌면 Ship 이 막고
   재검증을 요구한다 — 사람이 검토하지 않은 코드가 나가는 것을 막는 장치다.
   승인 전에 미리 채워두지 않는다.
3. `EXCEPTION_APPROVE` 면 `exceptions` 배열에 무엇을 예외 처리했는지 남긴다
4. `CP-4.3_hitl3-approved.md` 저장
5. `activeContext.md` 갱신, 커밋

> 커밋되지 않은 변경이 있는 상태로 승인하면 `verified_commit` 이 실제 검증 대상과
> 달라진다. 승인 전에 작업 트리가 깨끗한지 확인한다.
