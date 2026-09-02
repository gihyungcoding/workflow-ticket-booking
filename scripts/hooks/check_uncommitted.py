#!/usr/bin/env python3
"""Stop 훅 — 저장했지만 커밋하지 않은 워크플로우 산출물이 있으면 알린다.

왜 필요한가
-----------
체크포인트를 만들고 커밋하지 않으면 세션이 끊겼을 때 복구 대상 자체가 사라진다.
Memory Bank 의 존재 이유가 세션 복구인데 untracked 파일은 복구되지 않는다.
"저장 즉시 커밋"이 규칙이지만, 규칙만으로는 지켜지지 않으므로 세션이 끝나는 시점에 확인한다.

경고만 하고 막지 않는 이유
-------------------------
커밋 시점은 사람이 정할 일이다. 작업 중간에 세션을 잠깐 끊는 것은 정상이고, 여기서 차단하면
세션을 끝낼 수 없게 된다. 이 훅은 "커밋 안 된 게 있다"는 사실만 알린다.

exit code 는 항상 0 이다.

이 검사가 못 잡는 것
-------------------
커밋했는지만 본다. 커밋 메시지가 규약을 지키는지, 올바른 브랜치인지는 보지 않는다.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from _utils import repo_root  # noqa: E402

WATCHED = ("memory-bank/", "workflow_design/")


def main() -> int:
    if not sys.stdin.isatty():
        try:
            sys.stdin.read()
        except Exception:
            pass

    root = repo_root()
    try:
        out = subprocess.run(
            ["git", "status", "--porcelain"],
            capture_output=True,
            text=True,
            cwd=root,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return 0

    pending: list[str] = []
    for line in out.stdout.splitlines():
        if len(line) < 4:
            continue
        path = line[3:].strip().strip('"')
        if path.startswith(WATCHED):
            pending.append(f"{line[:2].strip() or '??'} {path}")

    if not pending:
        return 0

    print(
        f"⚠ 커밋되지 않은 워크플로우 산출물 {len(pending)}건 — "
        "세션이 끊기면 복구 대상에서 빠집니다.",
        file=sys.stderr,
    )
    for item in pending[:10]:
        print(f"    {item}", file=sys.stderr)
    if len(pending) > 10:
        print(f"    ... 외 {len(pending) - 10}건", file=sys.stderr)
    print("    git add memory-bank/ workflow_design/ && git commit", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
