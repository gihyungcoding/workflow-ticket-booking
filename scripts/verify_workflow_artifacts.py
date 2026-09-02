#!/usr/bin/env python3
"""태스크의 워크플로우 산출물이 규약대로 존재하는지 검사한다.

왜 필요한가
-----------
에이전트가 "Phase 3 완료"라고 보고하는 것과 DEV_TASK-001.json 이 실제로 커밋되어 있는 것은
다르다. 이 스크립트는 파일 시스템과 git 을 직접 보고 판정한다. /wf-status 와 Phase 5 종료
게이트, 그리고 세션 복구 시 무결성 확인에 쓴다.

파일 존재가 아니라 git 추적 여부를 보는 이유
-------------------------------------------
작업 트리에만 있는 산출물은 세션이 끊기면 복구되지 않는다. memory-bank 의 존재 이유가
세션 복구인데 untracked 파일은 복구 대상이 아니다. 다만 --allow-untracked 로 완화할 수
있게 뒀다 — 작업 중간에 상태를 확인할 때는 아직 커밋 전인 것이 정상이기 때문이다.

사용
----
    python scripts/verify_workflow_artifacts.py --task-id TASK-001
    python scripts/verify_workflow_artifacts.py --task-id TASK-001 --up-to-phase 3
    python scripts/verify_workflow_artifacts.py --all
    python scripts/verify_workflow_artifacts.py --task-id TASK-001 --allow-untracked

exit code
---------
    0  통과
    1  누락 있음
    2  인자 오류 (태스크 ID 형식 등)

이 검사가 못 잡는 것
-------------------
파일 내용이 규약에 맞는지는 보지 않는다. PLAN_TASK-001.json 이 `{}` 여도 존재하면 통과한다.
내용 검증은 각 Phase 의 EXIT GATE 와 validate_phase2a_gate.py 가 한다.
"""

from __future__ import annotations

import argparse
import sys

from _utils import (
    CHECKPOINTS,
    PHASE_NAMES,
    PHASE_ORDER,
    artifact_path,
    git_tracked,
    is_valid_task_id,
    list_tasks,
    load_json,
    task_dir,
    task_id_error,
)

#: Phase 별 필수 체크포인트 (CHECKPOINTS 에서 required=True 인 것)
PHASE_OF_CP = {
    "phase1": "1",
    "phase2a": "2a",
    "phase2b": "2b",
    "phase3": "3",
    "phase4": "4",
    "phase5": "5",
}


def phases_up_to(limit: str | None) -> list[str]:
    if limit is None:
        return list(PHASE_ORDER)
    if limit not in PHASE_ORDER:
        return list(PHASE_ORDER)
    return PHASE_ORDER[: PHASE_ORDER.index(limit) + 1]


def check_task(task_id: str, up_to: str | None, allow_untracked: bool) -> list[str]:
    """누락 목록을 반환한다. 빈 리스트면 통과."""
    problems: list[str] = []
    targets = phases_up_to(up_to)

    tdir = task_dir(task_id)
    if not tdir.is_dir():
        return [f"memory-bank/{task_id}/ 디렉터리가 없다"]

    if not (tdir / "activeContext.md").is_file():
        problems.append(f"memory-bank/{task_id}/activeContext.md 없음")

    # Phase 산출물
    for phase in targets:
        path = artifact_path(task_id, phase)
        if path is None:
            continue
        rel = path.relative_to(path.parents[2])
        if not path.is_file():
            problems.append(f"[Phase {phase} {PHASE_NAMES[phase]}] 산출물 없음: {rel}")
            continue
        data, err = load_json(path)
        if err:
            problems.append(f"[Phase {phase}] {err}")
            continue
        if not data:
            problems.append(f"[Phase {phase}] 산출물이 비어 있다: {rel}")
        if not allow_untracked and not git_tracked(path):
            problems.append(f"[Phase {phase}] 커밋되지 않음: {rel}")

    # Phase 2a 는 MD 가 SoT 이므로 함께 확인
    if "2a" in targets:
        md = artifact_path(task_id, "2a")
        if md is not None:
            md = md.with_name(f"SCENARIO_{task_id}.md")
            if not md.is_file():
                problems.append(f"[Phase 2a] 시나리오 SoT 없음: {md.name}")
            elif not allow_untracked and not git_tracked(md):
                problems.append(f"[Phase 2a] 커밋되지 않음: {md.name}")

    # 필수 체크포인트
    cp_base = tdir / "checkpoints"
    for phase_dir, filename, required in CHECKPOINTS:
        if not required:
            continue
        if PHASE_OF_CP[phase_dir] not in targets:
            continue
        path = cp_base / phase_dir / filename
        if not path.is_file():
            problems.append(f"[{PHASE_OF_CP[phase_dir]}] 필수 체크포인트 없음: {phase_dir}/{filename}")
        elif not allow_untracked and not git_tracked(path):
            problems.append(f"[{PHASE_OF_CP[phase_dir]}] 커밋되지 않음: {filename}")

    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--task-id", help="검사할 태스크 ID")
    parser.add_argument("--all", action="store_true", help="모든 태스크 검사")
    parser.add_argument(
        "--up-to-phase",
        choices=PHASE_ORDER,
        help="이 Phase 까지만 검사 (진행 중인 태스크용)",
    )
    parser.add_argument(
        "--allow-untracked",
        action="store_true",
        help="커밋 여부를 보지 않고 파일 존재만 확인",
    )
    args = parser.parse_args()

    if not args.task_id and not args.all:
        parser.error("--task-id 또는 --all 중 하나가 필요합니다")

    if args.task_id:
        if not is_valid_task_id(args.task_id):
            print(task_id_error(args.task_id), file=sys.stderr)
            return 2
        task_ids = [args.task_id]
        # 진행 중인 태스크는 현재 Phase 까지만 본다
        up_to = args.up_to_phase
        if up_to is None:
            for task in list_tasks():
                if task["task_id"] == args.task_id and task["status"] != "DONE":
                    up_to = task["phase"] or None
                    break
    else:
        task_ids = [t["task_id"] for t in list_tasks() if t["status"] == "DONE"]
        up_to = args.up_to_phase
        if not task_ids:
            print("완료된 태스크가 없습니다.")
            return 0

    failed = False
    for task_id in task_ids:
        problems = check_task(task_id, up_to, args.allow_untracked)
        if problems:
            failed = True
            print(f"✗ {task_id} — 누락 {len(problems)}건")
            for problem in problems:
                print(f"    {problem}")
        else:
            scope = f" (Phase {up_to} 까지)" if up_to else ""
            print(f"✓ {task_id} — 산출물 정상{scope}")

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
