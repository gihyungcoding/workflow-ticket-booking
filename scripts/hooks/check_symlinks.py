#!/usr/bin/env python3
"""절대경로 심볼릭 링크와 저장소 밖을 가리키는 링크를 차단한다.

왜 필요한가
-----------
절대경로 심볼릭 링크(/Users/someone/... 또는 /home/someone/...)는 만든 사람의 기계에서만
동작한다. 커밋되면 다른 사람의 clone 에서 깨진 링크가 되고, 깨졌다는 사실이 그 파일을
실제로 열기 전까지 드러나지 않는다. 저장소 밖을 가리키는 상대 링크(../../etc/passwd)도
같은 이유로 막는다.

사용
----
    python scripts/hooks/check_symlinks.py <파일...>     # pre-commit
    python scripts/hooks/check_symlinks.py --all          # 저장소 전체

exit code: 0 통과 / 1 발견

이 검사가 못 잡는 것
-------------------
저장소 안을 가리키는 상대 링크는 통과시킨다. 그 링크의 대상이 실제로 존재하는지는
보지 않는다 — 대상이 아직 없는 링크가 정당한 경우가 있기 때문이다.
"""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from _utils import repo_root  # noqa: E402


def check(path: Path, root: Path) -> str:
    """위반 사유. 문제 없으면 빈 문자열."""
    if not path.is_symlink():
        return ""

    target = os.readlink(path)

    if os.path.isabs(target):
        return f"절대경로 링크: → {target} (만든 사람의 기계에서만 동작한다)"

    resolved = (path.parent / target).resolve()
    try:
        resolved.relative_to(root)
    except ValueError:
        return f"저장소 밖을 가리킨다: → {target}"

    return ""


def main() -> int:
    args = sys.argv[1:]
    root = repo_root().resolve()

    if "--all" in args:
        try:
            out = subprocess.run(
                ["git", "ls-files"], capture_output=True, text=True, cwd=root, check=True
            )
            targets = [root / rel for rel in out.stdout.splitlines()]
        except (subprocess.CalledProcessError, FileNotFoundError):
            targets = [p for p in root.rglob("*") if ".git" not in p.parts]
    elif args:
        targets = [Path(a) for a in args]
    else:
        print("사용: check_symlinks.py <파일...> | --all", file=sys.stderr)
        return 1

    total = 0
    for path in targets:
        if not path.exists() and not path.is_symlink():
            continue
        problem = check(path, root)
        if problem:
            total += 1
            try:
                rel = str(path.resolve(strict=False).relative_to(root))
            except ValueError:
                rel = str(path)
            print(f"✗ {rel}  {problem}")

    if total:
        print(f"\n심볼릭 링크 위반 {total}건. 저장소 내부 상대경로로 바꾸세요.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
