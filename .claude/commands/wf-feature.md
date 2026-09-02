---
description: 기능 요구사항 문서를 작성한다 (§1-2 태스크 분리 포함)
argument-hint: "<기능-이름>"
---

`product-definition` 스킬로 요구사항 문서를 작성한다.

## 1. 파일 준비

인자를 케밥케이스로 정규화한다 (`매장 검색 권한` → `store-search-permission`).
영문 슬러그를 사용자에게 확인받는다 — 파일명은 나중에 바꾸기 번거롭다.

```bash
ls docs/product/features/<슬러그>.md 2>/dev/null   # 이미 있으면 갱신 모드
cp docs/product/features/_TEMPLATE.md docs/product/features/<슬러그>.md
```

## 2. 맥락 파악

```bash
cat docs/product/product.md
cat docs/architecture/architecture.md
```

- `product.md` 의 **범위**에 이 기능이 들어가는가? 벗어나면 진행 전에 알린다
- `architecture.md` 로 이 기능이 어느 계층에 걸치는지 감을 잡는다

기존 코드에서 관련 부분을 찾아 둔다 (Grep/Glob). §5 구현 참고에 쓸 재료다.

## 3. 작성

`product-definition` 스킬의 순서를 따른다.

배경 → 상세(§1-3, §2, §3) → **§1-2 태스크 분리** → 완료 조건 → 구현 참고 → 범위 외 →
주요 결정 사항.

§1-2 를 상세보다 나중에 쓴다. 무엇을 만들지 모르면 쪼갤 수 없다.

각 큰 단계가 끝날 때 사용자에게 확인을 받는다. 문서 전체를 한 번에 쓰고 보여주지 않는다.

## 4. §1-2 검토

작성한 태스크 분리 표를 사용자와 함께 확인한다. 특히:

- 각 행이 4~16시간인가
- Backend / Frontend 가 다른 행인가. FE 행에 선행이 적혔는가
- "포함하지 않음"이 비어 있지 않은가
- "원인 분석만" / "QA만" 하는 행이 없는가

문제가 있으면 고친 뒤 넘어간다. 여기서 잘못 쪼개면 워크플로우 전체가 비용을 나눠 낸다.

## 5. 마무리

```bash
python3 scripts/rebuild_doc_index.py
python3 scripts/hooks/check_artifact_paths.py docs/product/features/<슬러그>.md
git add docs/product/ && git commit -m "docs(feature): <기능명> 요구사항"
```

```
docs/product/features/store-search-permission.md 작성

  태스크 분리    2건 (Backend 8h → Frontend 6h)
  완료 조건      5건
  구현 참고      3개 경로 (신규 1)
  ADR 승격 후보  1건 — "권한 판정을 데코레이터로 통일"

다음: /wf-tasks-from-doc docs/product/features/store-search-permission.md
```

ADR 승격 후보가 있으면 `/wf-adr` 를 먼저 하자고 제안한다 — 태스크를 만들기 전에
결정이 확정되어 있으면 Phase 1이 그것을 참조할 수 있다.
