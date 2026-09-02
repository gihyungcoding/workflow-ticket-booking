#!/usr/bin/env python3
"""문서 인덱스를 재생성한다 — docs/decisions/README.md, docs/product/features/README.md.

왜 스크립트만 인덱스를 쓰는가
----------------------------
memory-bank/index.md 와 같은 이유다. 원본 저장소는 인덱스 생성 경로가 없어서 사람이
손으로 행을 덧붙였고, 그 결과 누락·중복·고아 행(파일은 지웠는데 행은 남음)이 쌓였다.
생성 경로가 하나면 그런 drift 가 생기지 않는다.

두 인덱스를 한 스크립트가 만드는 이유는 둘 다 "docs/ 를 훑어 표를 만든다"는 같은 일이고,
따로 두면 한쪽만 갱신되는 상태가 생기기 때문이다.

사용
----
    python3 scripts/rebuild_doc_index.py
    python3 scripts/rebuild_doc_index.py --check    # 재생성 없이 최신인지 확인

exit code: 0 최신/성공 · 1 (--check 에서) 낡음
"""

from __future__ import annotations

import argparse
import re
import sys
from datetime import date
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _utils import decisions_dir, features_dir, list_adrs, parse_task_split_table  # noqa: E402

GENERATED_NOTE = (
    "> 이 파일은 `scripts/rebuild_doc_index.py` 가 생성합니다. "
    "직접 편집하지 마세요 — 다음 재생성 때 사라집니다."
)

STATUS_ORDER = {"채택됨": 0, "제안됨": 1, "폐기됨": 3}


def build_adr_index() -> str:
    adrs = list_adrs()
    lines = [
        "# 기술 결정 기록 (ADR)",
        "",
        f"마지막 재생성: {date.today().isoformat()} · 총 {len(adrs)}건",
        "",
        GENERATED_NOTE,
        "",
        "새 결정을 기록하려면 `/wf-adr \"<제목>\"`. "
        "ADR 은 고치지 않고 쌓습니다 — 결정이 바뀌면 새 ADR 이 이전 것을 대체합니다.",
        "",
    ]

    if not adrs:
        lines += ["_아직 기록된 결정이 없습니다._", ""]
        return "\n".join(lines).rstrip() + "\n"

    def sort_key(adr: dict) -> tuple[int, int]:
        status = adr["status"]
        rank = STATUS_ORDER.get(status, 2)  # '대체됨' 류는 2
        return (rank, adr["number"])

    lines += ["| ID | 제목 | 상태 | 날짜 |", "|---|---|---|---|"]
    for adr in sorted(adrs, key=sort_key):
        title = adr["title"]
        # 'ADR-0001: 제목' 에서 접두사를 떼어 표를 좁게 유지한다
        title = re.sub(r"^ADR-\d+:\s*", "", title) or "_(제목 없음)_"
        lines.append(
            f"| [{adr['id']}]({adr['file']}) | {title} | {adr['status']} | {adr['date'] or '—'} |"
        )
    lines.append("")

    superseded = [a for a in adrs if a["status"] not in STATUS_ORDER]
    if superseded:
        lines += [
            "대체된 ADR 도 목록에 남깁니다. 과거에 왜 그렇게 판단했는지가 지워지면 "
            "같은 논의를 다시 하게 됩니다.",
            "",
        ]
    return "\n".join(lines).rstrip() + "\n"


def feature_meta(path: Path) -> dict:
    """요구사항 문서에서 제목·상태·갱신일·태스크 ID 를 뽑는다."""
    title, status, updated = "", "", ""
    try:
        for line in path.read_text(encoding="utf-8").splitlines()[:20]:
            stripped = line.strip()
            if stripped.startswith("# ") and not title:
                title = stripped[2:].strip()
            elif stripped.startswith("**상태**:"):
                status = stripped.split(":", 1)[1].split("<!--")[0].strip()
            elif stripped.startswith("**최종 갱신**:"):
                updated = stripped.split(":", 1)[1].split("<!--")[0].strip()
    except OSError:
        pass

    rows, _ = parse_task_split_table(path)
    task_ids: list[str] = []
    for row in rows:
        for key, value in row.items():
            if "태스크 ID" in str(key) or "task id" in str(key).lower():
                task_ids += re.findall(r"TASK-\d{3,}", str(value))
    return {
        "file": path.name,
        "title": title or path.stem,
        "status": status or "—",
        "updated": updated or "—",
        "task_count": len(rows),
        "task_ids": task_ids,
    }


def build_feature_index() -> str:
    directory = features_dir()
    features = []
    if directory.is_dir():
        for entry in sorted(directory.iterdir()):
            if entry.suffix != ".md" or entry.name in ("README.md", "_TEMPLATE.md"):
                continue
            features.append(feature_meta(entry))

    lines = [
        "# 기능 요구사항 문서",
        "",
        f"마지막 재생성: {date.today().isoformat()} · 총 {len(features)}건",
        "",
        GENERATED_NOTE,
        "",
        "새 기능은 `/wf-feature <이름>`. 문서의 `## 1-2. 태스크 분리` 표를 채운 뒤 "
        "`/wf-tasks-from-doc <경로>` 로 태스크를 만듭니다.",
        "",
    ]

    if not features:
        lines += ["_아직 작성된 요구사항 문서가 없습니다._", ""]
        return "\n".join(lines).rstrip() + "\n"

    lines += ["| 문서 | 제목 | 상태 | 태스크 | 갱신 |", "|---|---|---|---|---|"]
    for feature in features:
        if feature["task_ids"]:
            tasks = ", ".join(feature["task_ids"])
        elif feature["task_count"]:
            tasks = f"{feature['task_count']}건 (미생성)"
        else:
            tasks = "—"
        lines.append(
            f"| [{feature['file']}]({feature['file']}) | {feature['title']} "
            f"| {feature['status']} | {tasks} | {feature['updated']} |"
        )
    lines.append("")

    pending = [f for f in features if f["task_count"] and not f["task_ids"]]
    if pending:
        lines += [
            "## 태스크 미생성",
            "",
            "아래 문서는 태스크 분리 표가 있지만 아직 태스크가 만들어지지 않았습니다.",
            "",
        ]
        for feature in pending:
            lines.append(
                f"- `{feature['file']}` — `/wf-tasks-from-doc "
                f"docs/product/features/{feature['file']}`"
            )
        lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def write_or_check(path: Path, content: str, check: bool) -> bool:
    """반환: 최신이면 True."""
    current = path.read_text(encoding="utf-8") if path.is_file() else ""

    def strip_date(text: str) -> str:
        return "\n".join(l for l in text.splitlines() if not l.startswith("마지막 재생성:"))

    if strip_date(current) == strip_date(content):
        return True
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
    return False


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--check", action="store_true", help="재생성하지 않고 최신인지 확인")
    args = parser.parse_args()

    targets = [
        (decisions_dir() / "README.md", build_adr_index(), "ADR 인덱스"),
        (features_dir() / "README.md", build_feature_index(), "기능 문서 인덱스"),
    ]

    stale = []
    for path, content, label in targets:
        if not write_or_check(path, content, args.check):
            stale.append(label)

    if args.check:
        if stale:
            print(f"인덱스가 낡았습니다: {', '.join(stale)}. rebuild_doc_index.py 를 실행하세요.", file=sys.stderr)
            return 1
        print("문서 인덱스는 최신입니다.")
        return 0

    adr_count = len(list_adrs())
    feature_dir = features_dir()
    feature_count = (
        len([p for p in feature_dir.iterdir() if p.suffix == ".md" and p.name not in ("README.md", "_TEMPLATE.md")])
        if feature_dir.is_dir()
        else 0
    )
    print(f"문서 인덱스 재생성 — ADR {adr_count}건 · 기능 문서 {feature_count}건")
    return 0


if __name__ == "__main__":
    sys.exit(main())
