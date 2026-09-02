#!/usr/bin/env python3
"""SessionStart 훅 — 진행 중인 태스크를 세션 컨텍스트에 주입한다.

왜 필요한가
-----------
원본(Cursor) 워크플로우에서 세션 복구는 사람이 매번 "task-0310 복구해줘"라고 지시해야
시작됐다. 그러면 에이전트가 memory-bank 를 뒤지고, 마지막 체크포인트를 찾고, 승인 상태를
확인하는 절차를 밟았다. 매 세션마다 반복되는 이 왕복을 훅이 없앤다.

이 훅이 주입하는 정보로 에이전트는 "어떤 태스크를 하고 계셨나요?"라고 묻지 않아도 된다.

동작
----
- 활성/차단 태스크가 없으면 아무것도 출력하지 않는다 (조용히 통과)
- 활성 태스크가 1건이면 그 태스크의 activeContext.md 전문 + 마지막 체크포인트 요약
- 여러 건이면 목록만 (전문을 다 넣으면 컨텍스트를 낭비한다)
- 14일 이상 갱신 없는 활성 태스크와 차단 태스크는 눈에 띄게 표시

수동 실행
--------
    python scripts/hooks/session_start.py          # 출력 확인용

exit code 는 항상 0 이다. 이 훅이 실패해서 세션 시작을 막아서는 안 된다.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from _utils import PHASE_NAMES, list_tasks, saved_checkpoints, task_dir  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from wf_status import days_since  # noqa: E402

STALE_DAYS = 14
MAX_CONTEXT_CHARS = 6000


def last_checkpoint_summary(task_id: str) -> str:
    """마지막 체크포인트의 핵심만 뽑는다."""
    saved = saved_checkpoints(task_id)
    if not saved:
        return ""
    filename = saved[-1]
    base = task_dir(task_id) / "checkpoints"
    for phase_dir in ("phase1", "phase2a", "phase2b", "phase3", "phase4", "phase5"):
        path = base / phase_dir / filename
        if path.is_file():
            try:
                text = path.read_text(encoding="utf-8")
            except OSError:
                return ""
            return f"### 마지막 체크포인트: {phase_dir}/{filename}\n\n{text.strip()}"
    return ""


def build_context() -> str:
    tasks = list_tasks()
    active = [t for t in tasks if t["status"] == "ACTIVE"]
    blocked = [t for t in tasks if t["status"] == "BLOCKED"]

    if not active and not blocked:
        return ""

    lines = ["## 진행 중인 워크플로우 태스크", ""]
    lines.append(
        "아래는 `memory-bank/` 에서 읽은 현재 상태입니다. "
        "사용자에게 어떤 태스크였는지 되묻지 말고 이 정보를 사용하세요."
    )
    lines.append("")

    for task in sorted(active, key=lambda t: t["task_id"]):
        age = days_since(task["last_updated"])
        when = f"{age}일 전 갱신" if age is not None else "갱신일 미상"
        stale = " ⚠ 정체" if age is not None and age >= STALE_DAYS else ""
        phase = task["phase"]
        label = f"Phase {phase} ({PHASE_NAMES.get(phase, '?')})" if phase else "Phase 미상"
        lines.append(
            f"- **{task['task_id']}** — {label} · {when}{stale}"
            + (f"\n  {task['title']}" if task["title"] else "")
        )
        if task["last_checkpoint"]:
            lines.append(f"  마지막 체크포인트: {task['last_checkpoint']}")

    for task in sorted(blocked, key=lambda t: t["task_id"]):
        days = task["blocked_days"] if "blocked_days" in task else days_since(
            task["blocked_since"]
        )
        span = f"{days}일째" if days is not None else ""
        lines.append("")
        lines.append(f"- ⛔ **{task['task_id']}** — 차단 {span}")
        if task["blocked_reason"]:
            lines.append(f"  사유: {task['blocked_reason']}")
        if task["unblock_condition"]:
            lines.append(f"  해제 조건: {task['unblock_condition']}")
        lines.append("  차단 태스크는 사용자가 명시적으로 지시하기 전까지 진행하지 않습니다.")

    lines.append("")
    lines.append(
        "이어서 작업하려면 `/wf-resume <TASK-ID>`, 새로 시작하려면 `/wf-start <TASK-ID>`. "
        "전체 현황은 `/wf-status`."
    )

    # 활성이 1건이면 상세 컨텍스트를 붙인다
    if len(active) == 1 and not blocked:
        task_id = active[0]["task_id"]
        ctx_path = task_dir(task_id) / "activeContext.md"
        if ctx_path.is_file():
            try:
                content = ctx_path.read_text(encoding="utf-8").strip()
                lines.append("")
                lines.append(f"### {task_id} activeContext.md")
                lines.append("")
                lines.append(content)
            except OSError:
                pass
        summary = last_checkpoint_summary(task_id)
        if summary:
            lines.append("")
            lines.append(summary)

    text = "\n".join(lines)
    if len(text) > MAX_CONTEXT_CHARS:
        text = text[:MAX_CONTEXT_CHARS] + "\n\n_(길이 제한으로 잘렸습니다. 전문은 `/wf-resume` 로 확인하세요.)_"
    return text


def main() -> int:
    # stdin 페이로드는 쓰지 않지만, 훅이 파이프로 호출되므로 비워둔다
    if not sys.stdin.isatty():
        try:
            sys.stdin.read()
        except Exception:
            pass

    try:
        context = build_context()
    except Exception as exc:  # noqa: BLE001 — 훅은 세션 시작을 막지 않는다
        print(f"[session_start] 상태를 읽지 못했습니다: {exc}", file=sys.stderr)
        return 0

    if not context:
        return 0

    print(
        json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": "SessionStart",
                    "additionalContext": context,
                }
            },
            ensure_ascii=False,
        )
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
