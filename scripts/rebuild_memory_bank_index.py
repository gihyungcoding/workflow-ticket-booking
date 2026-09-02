#!/usr/bin/env python3
"""memory-bank/index.md 를 태스크 폴더에서 결정론적으로 재생성한다.

왜 이 스크립트만 index.md 를 쓰는가
----------------------------------
원본 저장소에는 정규 생성 경로가 없어서 에이전트가 매번 손으로 행을 덧붙였다. 그 결과
누락(폴더는 있는데 행이 없음), 중복(같은 태스크가 두 줄), 고아 행(폴더는 지웠는데 행은 남음)이
쌓였고, 활성 19건 중 제목이 비어 있는 것이 다수였다. 생성 경로가 하나면 이런 drift 가 생기지
않는다. index.md 를 직접 편집하면 다음 실행 때 사라진다.

사용
----
    python scripts/rebuild_memory_bank_index.py
    python scripts/rebuild_memory_bank_index.py --check   # 재생성 없이 최신인지만 확인

되돌리는 법
----------
이 스크립트는 index.md 하나만 덮어쓴다. 잘못되면 git checkout 으로 되돌린다.
태스크 폴더는 읽기만 한다.
"""

from __future__ import annotations

import argparse
import sys
from datetime import date

from _utils import PHASE_NAMES, list_tasks, memory_bank_root

HEADER_NOTE = (
    "> 이 파일은 `scripts/rebuild_memory_bank_index.py` 가 생성합니다. "
    "직접 편집하지 마세요 — 다음 재생성 때 사라집니다."
)


def phase_cell(phase: str) -> str:
    if not phase:
        return "—"
    name = PHASE_NAMES.get(phase)
    return f"{phase} {name}" if name else phase


def row(task: dict) -> str:
    title = task["title"] or "_(제목 없음)_"
    return (
        f"| {task['task_id']} | {title} | {phase_cell(task['phase'])} "
        f"| {task['last_updated'] or '—'} |"
    )


def build(tasks: list[dict]) -> str:
    active = sorted(
        [t for t in tasks if t["status"] == "ACTIVE"], key=lambda t: t["task_id"]
    )
    blocked = sorted(
        [t for t in tasks if t["status"] == "BLOCKED"], key=lambda t: t["task_id"]
    )
    done = sorted(
        [t for t in tasks if t["status"] == "DONE"], key=lambda t: t["task_id"]
    )
    other = sorted(
        [t for t in tasks if t["status"] not in ("ACTIVE", "BLOCKED", "DONE")],
        key=lambda t: t["task_id"],
    )

    out = [
        "# Memory Bank",
        "",
        f"마지막 재생성: {date.today().isoformat()} · "
        f"활성 {len(active)} · 차단 {len(blocked)} · 완료 {len(done)}",
        "",
        HEADER_NOTE,
        "",
    ]

    def table(title: str, rows: list[dict], extra_col: str | None = None) -> None:
        out.append(f"## {title} ({len(rows)})")
        out.append("")
        if not rows:
            out.append("_없음_")
            out.append("")
            return
        head = "| Task ID | 제목 | Phase | 갱신 |"
        sep = "|---|---|---|---|"
        if extra_col:
            head = head[:-1] + f" {extra_col} |"
            sep = sep[:-1] + "---|"
        out.append(head)
        out.append(sep)
        for task in rows:
            line = row(task)
            if extra_col:
                reason = task.get("blocked_reason") or "—"
                line = line[:-1] + f" {reason} |"
            out.append(line)
        out.append("")

    table("활성", active)
    table("차단", blocked, extra_col="사유")
    table("완료", done)
    if other:
        table("기타", other)

    mismatched = [t for t in tasks if t["folder_mismatch"]]
    if mismatched:
        out.append("## ⚠ 폴더명 불일치")
        out.append("")
        out.append("아래 태스크는 폴더명과 `activeContext.md` 의 `task_id` 가 다릅니다.")
        out.append("자동으로 고치지 않았습니다 — 어느 쪽이 맞는지 확인이 필요합니다.")
        out.append("")
        for task in mismatched:
            out.append(f"- 폴더 `{task['folder']}` vs `task_id: {task['task_id']}`")
        out.append("")

    return "\n".join(out).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument(
        "--check",
        action="store_true",
        help="재생성하지 않고 최신인지만 확인 (다르면 exit 1)",
    )
    args = parser.parse_args()

    root = memory_bank_root()
    if not root.is_dir():
        print(f"memory-bank 디렉터리가 없습니다: {root}", file=sys.stderr)
        return 1

    tasks = list_tasks()
    content = build(tasks)
    index_path = root / "index.md"

    if args.check:
        current = index_path.read_text(encoding="utf-8") if index_path.is_file() else ""
        # 재생성 날짜 줄은 매일 바뀌므로 비교에서 제외한다
        def strip_date(text: str) -> str:
            return "\n".join(
                line for line in text.splitlines() if not line.startswith("마지막 재생성:")
            )

        if strip_date(current) == strip_date(content):
            print("index.md 는 최신입니다.")
            return 0
        print("index.md 가 태스크 폴더와 다릅니다. 재생성이 필요합니다.", file=sys.stderr)
        return 1

    index_path.write_text(content, encoding="utf-8")
    active = sum(1 for t in tasks if t["status"] == "ACTIVE")
    blocked = sum(1 for t in tasks if t["status"] == "BLOCKED")
    done = sum(1 for t in tasks if t["status"] == "DONE")
    print(f"index.md 재생성 — 활성 {active} · 차단 {blocked} · 완료 {done} (총 {len(tasks)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
