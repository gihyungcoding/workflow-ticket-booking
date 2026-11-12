---
status: ACTIVE
saved_at: 2026-11-12T00:00:00Z
summary: "VERIFY 3회차 FAIL — RC-2 later 변종 3 재현(롤백 시 가드 OFF · HEAD/OPTIONS 401 · §5-2 SQL 대소문자). 리뷰가 changed_files 11 항목 전부를 운영 결과와 대조 — 남은 갭은 이 3건. 코드 결함 0."

next_steps:
  - "HITL #3 — FAIL 이라 롤백만. 권장 Phase 3: 체크리스트 §5-1 에 기존 공연 소유자 없음 영향, §4 마이그레이션 V1~V7"

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
  - round: 2
    result: FAIL
    detail: "RC-2 later 변종 2 reproduced — V7 운영 영향 미서술 · §4 마이그레이션 V1·V2. 1회차 4건·전환 구간 해소"
  - round: 3
    result: FAIL
    detail: "RC-2 later 변종 3 reproduced. 같은 뿌리 3라운드 연속 — 리뷰가 라운드마다 새 운영 결과를 찾음(목표가 움직임). 3회차에서 changed_files 전수 표로 닫음"
---

# CP-4.2 Verification — TASK-022

- `workflow_design/07_verify/VERIFY_TASK-022.json`
