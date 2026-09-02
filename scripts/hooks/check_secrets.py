#!/usr/bin/env python3
"""커밋되면 안 되는 비밀정보를 찾는다.

왜 필요한가
-----------
토큰이 한 번 커밋되면 히스토리에서 지우는 비용이 크고, 그 사이에 유출된 것으로 간주하고
회전(rotate)해야 한다. 초 단위로 끝나는 검사로 막는 편이 훨씬 싸다.

설계 원칙 — 훅에는 초 단위로 끝나는 것만 넣는다
---------------------------------------------
빌드나 테스트를 pre-commit 에 넣으면 첫 주에 전원이 --no-verify 를 쓰기 시작한다.
이 검사는 정규식 매칭만 하므로 파일 수백 개에도 1초 안에 끝난다.

사용
----
    python scripts/hooks/check_secrets.py <파일...>       # pre-commit
    python scripts/hooks/check_secrets.py --all           # 저장소 전체

exit code: 0 통과 / 1 발견

이 검사가 못 잡는 것
-------------------
알려진 형태의 토큰만 찾는다. 사내 시스템의 자체 형식이나 base64 로 인코딩된 자격증명은
잡지 못한다. 새 형식을 만나면 PATTERNS 에 추가한다.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from _utils import repo_root  # noqa: E402

PATTERNS: list[tuple[str, re.Pattern]] = [
    ("AWS Access Key", re.compile(r"\b(A3T[A-Z0-9]|AKIA|ASIA|ABIA|ACCA)[A-Z0-9]{16}\b")),
    ("AWS Secret Key", re.compile(r"(?i)aws.{0,20}secret.{0,20}['\"][0-9a-zA-Z/+]{40}['\"]")),
    ("GitHub Token", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36,}\b")),
    ("GitLab PAT", re.compile(r"\bglpat-[A-Za-z0-9_\-]{20,}\b")),
    ("Slack Webhook", re.compile(r"https://hooks\.slack\.com/services/T[A-Za-z0-9/+]{20,}")),
    ("Slack Token", re.compile(r"\bxox[baprs]-[A-Za-z0-9\-]{10,}\b")),
    ("Anthropic API Key", re.compile(r"\bsk-ant-[A-Za-z0-9\-_]{20,}\b")),
    ("OpenAI API Key", re.compile(r"\bsk-[A-Za-z0-9]{32,}\b")),
    ("Google API Key", re.compile(r"\bAIza[0-9A-Za-z\-_]{35}\b")),
    ("Private Key", re.compile(r"-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----")),
    (
        "하드코딩된 자격증명",
        re.compile(
            r"(?i)\b(password|passwd|secret|api_?key|access_?token)\s*[:=]\s*"
            r"['\"][^'\"\s${}<>]{8,}['\"]"
        ),
    ),
]

#: 유출 사례를 설명하는 문서는 예시 토큰을 담을 수밖에 없다
EXCLUDE_PATHS = (
    "docs/security/",
    "scripts/hooks/check_secrets.py",
)

#: 명백한 플레이스홀더는 무시한다
PLACEHOLDER = re.compile(
    r"(?i)(your[_-]?|example|placeholder|dummy|sample|xxx+|\.\.\.|<[^>]+>|\$\{)"
)

BINARY_SUFFIXES = {
    ".png", ".jpg", ".jpeg", ".gif", ".pdf", ".zip", ".gz", ".woff", ".woff2",
    ".ico", ".mp4", ".so", ".dylib", ".pyc",
}


def excluded(rel: str) -> bool:
    return any(rel.startswith(prefix) or rel == prefix for prefix in EXCLUDE_PATHS)


def scan_file(path: Path, rel: str) -> list[tuple[int, str, str]]:
    if path.suffix.lower() in BINARY_SUFFIXES or excluded(rel):
        return []
    try:
        if path.stat().st_size > 2_000_000:
            return []
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return []

    findings = []
    for lineno, line in enumerate(text.splitlines(), 1):
        if PLACEHOLDER.search(line):
            continue
        for name, pattern in PATTERNS:
            match = pattern.search(line)
            if match:
                snippet = match.group(0)
                if len(snippet) > 24:
                    snippet = snippet[:12] + "…" + snippet[-4:]
                findings.append((lineno, name, snippet))
                break
    return findings


def main() -> int:
    args = sys.argv[1:]
    root = repo_root()

    if "--all" in args:
        try:
            out = subprocess.run(
                ["git", "ls-files"], capture_output=True, text=True, cwd=root, check=True
            )
            targets = [root / rel for rel in out.stdout.splitlines()]
        except (subprocess.CalledProcessError, FileNotFoundError):
            targets = [p for p in root.rglob("*") if p.is_file() and ".git" not in p.parts]
    elif args:
        targets = [Path(a) for a in args]
    else:
        print("사용: check_secrets.py <파일...> | --all", file=sys.stderr)
        return 1

    total = 0
    for path in targets:
        if not path.is_file():
            continue
        try:
            rel = str(path.resolve().relative_to(root))
        except ValueError:
            rel = str(path)
        for lineno, name, snippet in scan_file(path, rel):
            total += 1
            print(f"✗ {rel}:{lineno}  {name}  {snippet}")

    if total:
        print(f"\n비밀정보 의심 {total}건. 커밋을 중단합니다.")
        print("오탐이면 값을 환경변수로 옮기거나, 형식이 새로운 것이면")
        print("scripts/hooks/check_secrets.py 의 PATTERNS 를 확인하세요.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
