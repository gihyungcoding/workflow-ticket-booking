---
description: 기술 결정을 ADR로 기록한다 (번호 자동 채번)
argument-hint: "<결정 요약>"
---

`adr` 스킬로 ADR 을 작성한다.

## 1. 승격 대상인지 확인

기준: **이 결정이 이 태스크·이 기능을 넘어 적용되는가?**

그렇지 않다면 ADR 대신 기능 문서의 `## 7. 주요 결정 사항` 이나 체크포인트의
`decisions` 에 남기는 편이 맞다. 모든 결정을 ADR 로 만들면 인덱스가 소음이 된다.

판단이 서지 않으면 사용자에게 묻는다:
"이 결정이 다른 기능에서도 적용될까요, 아니면 이번 작업에만 해당하나요?"

## 2. 번호 채번

```bash
python3 -c "
import sys; sys.path.insert(0, 'scripts')
from _utils import next_adr_number
print(f'ADR-{next_adr_number():04d}')
"
```

슬러그는 결정의 **내용**을 담는 케밥케이스로. `ADR-0002-layered-architecture.md` (○)

```bash
cp docs/decisions/_TEMPLATE.md docs/decisions/ADR-NNNN-<슬러그>.md
```

## 3. 작성

`adr` 스킬 §2 를 따른다. 대화로 끌어낸다 — 사용자는 이미 결정했고, 그 이유를
문서로 옮기는 일이다.

물어볼 것:

1. **무엇 때문에 이 결정이 필요했나요?** (맥락 — 강제하는 힘들)
2. **어떤 선택지를 봤나요?** 최소 2개. 하나뿐이면 "다른 방법은 고려 안 하셨나요?"
3. **왜 그것들이 아니라 이걸 골랐나요?** ← 가장 중요. 탈락 이유를 반드시 받는다
4. **대신 무엇을 감수하나요?** "없다"고 하면 한 번 더 묻는다
5. **되돌리려면 뭘 해야 하나요?**
6. **코드로 강제할 규칙이 나오나요?**

6번이 있으면 `architecture-doc` 스킬로 `constraints.yaml` 에 제약을 추가하고
**실제로 걸리는지 검증**한 뒤 ADR 에 ID 를 적는다.

## 4. 관련 문서 연결

- `docs/architecture/architecture.md` 의 `## 6. 이 구조를 고른 이유` 에 링크
- 승격 원본인 기능 문서 `## 7` 표의 ADR 열에 번호 기입
- 제약을 추가했다면 `constraints.yaml` 의 `adr:` 필드

## 5. 마무리

```bash
python3 scripts/rebuild_doc_index.py
git add docs/decisions docs/architecture && git commit -m "docs(adr): ADR-0002 <제목>"
```

```
ADR-0002 계층형 아키텍처 채택

  선택지    3개 검토 (계층형 / 헥사고날 / 단순 MVC)
  제약      ARCH-001 추가 — API→Repository 직접 호출 금지 (검증 통과)
  연결      architecture.md §6, store-search-permission.md §7
```

## 기존 결정을 바꾸는 경우

기존 ADR 을 **고치지 않는다.** 새 ADR 을 쓰고 이전 것의 상태를
`ADR-NNNN 으로 대체됨` 으로 바꾼다. 절차는 `adr` 스킬 §4 를 따른다.

새 ADR 의 맥락에는 **무엇이 바뀌어서 재검토하게 됐는지**를 쓴다.
