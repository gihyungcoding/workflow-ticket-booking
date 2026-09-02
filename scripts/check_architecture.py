#!/usr/bin/env python3
"""아키텍처 제약 위반을 검사한다. (docs/architecture/constraints.yaml)

왜 필요한가
-----------
"API 계층은 리포지토리를 직접 호출하지 않는다" 같은 규칙을 아키텍처 문서에 산문으로
적어두면 아무도 검사하지 않는다. 지키는지 확인할 방법이 없는 규칙은 시간이 지나면
그냥 문서에만 남는다. 이 스크립트는 constraints.yaml 의 규칙을 실제로 검사한다.

Phase 4(검증)가 호출하며, severity: error 위반이 하나라도 있으면 그 태스크는
status: FAIL 로 판정되어 Phase 5 로 넘어갈 수 없다.

사용
----
    python3 scripts/check_architecture.py                    # 전체
    python3 scripts/check_architecture.py --changed-only     # git diff 대상만
    python3 scripts/check_architecture.py --json
    python3 scripts/check_architecture.py --id ARCH-001

exit code
---------
    0  위반 없음 (warn 만 있어도 0)
    1  error 위반 있음
    2  constraints.yaml 을 읽을 수 없음

이 검사가 못 잡는 것
-------------------
정규식 매칭이다. 문자열이 주석 안에 있는지, 실제로 실행되는 코드인지 구분하지 않는다.
동적 import(importlib.import_module("repositories.store"))도 잡지 못한다.
정적 분석기가 필요해지면 그때 도구를 바꾸고, 그 전까지는 이 한계를 알고 쓴다.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _utils import constraints_path, load_constraints, repo_root  # noqa: E402

VALID_TYPES = {"forbidden_import", "forbidden_path", "required_path"}
SKIP_DIRS = {".git", "node_modules", "__pycache__", ".venv", "venv", "dist", "build"}


def iter_files(patterns: list[str], root: Path) -> list[Path]:
    """glob 패턴들에 해당하는 파일. 빌드 산출물은 제외한다."""
    found: list[Path] = []
    for pattern in patterns:
        for path in root.glob(pattern):
            if not path.is_file():
                continue
            if SKIP_DIRS & set(path.parts):
                continue
            found.append(path)
    return sorted(set(found))


def changed_files(root: Path) -> set[str] | None:
    """git diff 로 변경된 파일. 실패하면 None (전체 검사로 폴백)."""
    for args in (
        ["git", "diff", "--name-only", "--diff-filter=ACMR", "HEAD"],
        ["git", "diff", "--name-only", "--diff-filter=ACMR", "--cached"],
    ):
        try:
            out = subprocess.run(args, capture_output=True, text=True, cwd=root, check=True)
            names = {line for line in out.stdout.splitlines() if line}
            if names:
                return names
        except (subprocess.CalledProcessError, FileNotFoundError):
            return None
    return set()


def check_one(constraint: dict, root: Path, limit: set[str] | None) -> list[dict]:
    """제약 하나를 검사해 위반 목록을 반환한다."""
    detect = constraint.get("detect") or {}
    kind = detect.get("type")
    patterns = detect.get("paths") or []
    if isinstance(patterns, str):
        patterns = [patterns]

    if kind not in VALID_TYPES:
        return [
            {
                "file": str(constraints_path().relative_to(root)),
                "line": 0,
                "detail": f"detect.type 이 올바르지 않다: {kind!r} — {sorted(VALID_TYPES)} 중 하나여야 한다",
            }
        ]

    files = iter_files(patterns, root)
    if limit is not None:
        files = [f for f in files if str(f.relative_to(root)) in limit]

    if kind == "required_path":
        # 변경분만 볼 때는 존재 검사를 건너뛴다 (diff 에 없다고 파일이 없는 건 아니다)
        if limit is not None:
            return []
        if not files:
            return [{"file": ", ".join(patterns), "line": 0, "detail": "해당하는 파일이 하나도 없다"}]
        return []

    if kind == "forbidden_path":
        return [
            {"file": str(f.relative_to(root)), "line": 0, "detail": "존재해서는 안 되는 경로"}
            for f in files
        ]

    # forbidden_import
    raw = detect.get("pattern")
    if not raw:
        return [
            {
                "file": str(constraints_path().relative_to(root)),
                "line": 0,
                "detail": "forbidden_import 인데 detect.pattern 이 없다",
            }
        ]
    try:
        regex = re.compile(raw)
    except re.error as exc:
        return [
            {
                "file": str(constraints_path().relative_to(root)),
                "line": 0,
                "detail": f"detect.pattern 이 올바른 정규식이 아니다: {exc}",
            }
        ]

    violations = []
    for path in files:
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        for lineno, line in enumerate(text.splitlines(), 1):
            if regex.search(line):
                violations.append(
                    {
                        "file": str(path.relative_to(root)),
                        "line": lineno,
                        "detail": line.strip()[:120],
                    }
                )
    return violations


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--json", action="store_true", help="JSON 으로 출력")
    parser.add_argument("--changed-only", action="store_true", help="git 변경분만 검사")
    parser.add_argument("--id", help="특정 제약 ID 만 검사")
    args = parser.parse_args()

    root = repo_root()
    constraints, err = load_constraints()
    if err:
        if not constraints_path().is_file():
            # 제약 파일이 없는 것은 위반이 아니다 — 아직 정의하지 않은 것뿐이다
            if args.json:
                print(json.dumps({"status": "skipped", "reason": err}, ensure_ascii=False))
            else:
                print(f"constraints.yaml 이 없어 건너뜁니다 ({err})")
            return 0
        print(err, file=sys.stderr)
        return 2

    if args.id:
        constraints = [c for c in constraints if c.get("id") == args.id]
        if not constraints:
            print(f"제약을 찾지 못했다: {args.id}", file=sys.stderr)
            return 2

    if not constraints:
        if args.json:
            print(json.dumps({"status": "empty", "constraints": 0}, ensure_ascii=False))
        else:
            print("정의된 제약이 없습니다. docs/architecture/constraints.yaml 을 채우세요.")
        return 0

    limit = changed_files(root) if args.changed_only else None

    results = []
    errors = warns = 0
    for constraint in constraints:
        violations = check_one(constraint, root, limit)
        severity = str(constraint.get("severity", "error")).lower()
        if violations:
            if severity == "error":
                errors += len(violations)
            else:
                warns += len(violations)
        results.append(
            {
                "id": constraint.get("id", "?"),
                "rule": constraint.get("rule", ""),
                "adr": constraint.get("adr"),
                "severity": severity,
                "violations": violations,
            }
        )

    if args.json:
        print(
            json.dumps(
                {
                    "status": "fail" if errors else "pass",
                    "error_count": errors,
                    "warn_count": warns,
                    "results": results,
                },
                ensure_ascii=False,
                indent=2,
            )
        )
        return 1 if errors else 0

    for result in results:
        mark = "✓" if not result["violations"] else ("✗" if result["severity"] == "error" else "⚠")
        print(f"  {mark} {result['id']}  {result['rule']}")
        for violation in result["violations"]:
            location = f"{violation['file']}:{violation['line']}" if violation["line"] else violation["file"]
            print(f"      {location}")
            if violation["detail"]:
                print(f"        {violation['detail']}")
        if result["violations"] and result.get("adr"):
            print(f"      근거: {result['adr']} (docs/decisions/)")

    print()
    scope = " (변경분만)" if args.changed_only else ""
    if errors:
        print(f"제약 위반 {errors}건{scope} — Phase 4 는 status: FAIL 입니다.")
        if warns:
            print(f"경고 {warns}건도 함께 확인하세요.")
        return 1
    if warns:
        print(f"경고 {warns}건{scope}. 진행은 가능합니다.")
        return 0
    print(f"제약 {len(constraints)}건 모두 통과{scope}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
