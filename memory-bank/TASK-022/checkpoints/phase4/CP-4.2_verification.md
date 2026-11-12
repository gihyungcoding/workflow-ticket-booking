---
status: ARCHIVED
saved_at: 2026-11-12T00:00:00Z
summary: "VERIFY 1회차 FAIL — RC-2(배포 체크리스트가 X-Write-Key 제거의 브라우저 쪽 결과를 반영 못함, 변종 4 reproduced). 테스트 184/184, spotless 0, 제약 error 0·no_target 0, 리뷰 [correctness] 0, RC-1 해소."

next_steps:
  - "Phase 3 롤백(사용자 결정 2026-11-12) — 체크리스트 수정 → 수정 커밋 재리뷰 → Phase 4 2회차"

decisions:
  - decision: "리뷰어 [info](프론트 X-Write-Key)를 검증자가 확인 — 이 태스크가 고친 docs/release/first-deploy-checklist.md 102행 '넣어도 서버가 무시한다' 가 사실과 다르다. [correctness] RC-2 로 판정, status FAIL"
    rationale: "PLAN target_files 의 산출물이고, 그대로 따르면 배포된 프론트(VITE_WRITE_KEY 설정)의 가입·로그인 preflight 가 막힌다. 같은 뿌리의 §7 등록·키 오빌드 확인 단계와 배포 순서 부재도 함께 가져간다"
  - decision: "[design] HEAD 401/403 은 결함 아님 — 시나리오 결정과 같고 fail-closed. HITL 요약에 보인다"
    rationale: "리뷰어 판단, 저장소에 HEAD 사용처 없음"
  - decision: "AUTH-001 warn 은 develop 10 → 5건, 백엔드 0. 남은 프론트 5건은 TASK-024(promote_after)"
    rationale: "worktree 로 develop 에서 check_architecture 를 돌려 비교"

attempts:
  - round: 1
    result: FAIL
    detail: "RC-2 변종 4 reproduced(§5 문장·배포 순서·§7 등록 단계·§7 키 오빌드 단계). 코드 결함 0"
---

# CP-4.2 Verification — TASK-022

- `workflow_design/07_verify/VERIFY_TASK-022.json`
