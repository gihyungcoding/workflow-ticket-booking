---
description: 신규 프로젝트 부트스트랩 — 프로덕트 정의 → 아키텍처 → 첫 ADR
---

Foundation 문서를 순서대로 만든다. 이미 있는 것은 건너뛴다.

## 0. 현재 상태 확인

```bash
ls docs/product/product.md docs/architecture/architecture.md docs/decisions/ADR-*.md 2>/dev/null
grep -c "TODO" docs/product/product.md docs/architecture/architecture.md 2>/dev/null
```

TODO 가 남아 있으면 미완성으로 본다. 어디까지 되어 있는지 사용자에게 먼저 알린다.

```
Foundation 상태

  product.md        미작성 (TODO 12개)
  architecture.md   미작성 (TODO 8개)
  ADR               1건 (ADR-0001, 형식 예시)
  CLAUDE.md         TODO 3곳
```

## 1. 프로덕트 정의

`product-definition` 스킬로 `docs/product/product.md` 를 채운다.

**한 번에 다 묻지 않는다.** 섹션 단위로 물어보고 확인을 받으며 진행한다:
한 문장 → 문제 → 사용자 → 범위(특히 하지 않는 것) → 성공 기준 → 제약 → 열린 질문.

사용자가 "나중에"라고 하면 그 섹션에 TODO 를 남기고 넘어간다. 막지 않는다.

완료 후 `CLAUDE.md` 의 `sub_categories` 목록을 여기서 정한 역할로 채운다.

## 2. 아키텍처

`architecture-doc` 스킬로 `docs/architecture/architecture.md` 를 채운다.

기존 코드가 있으면 **먼저 읽고 현재 구조를 파악한 뒤** 쓴다 — 이상을 쓰고 현실과
다른 채로 두면 Phase 1이 잘못된 기준으로 설계한다.

```bash
ls -d src/*/ app/*/ lib/*/ 2>/dev/null
```

빈 저장소라면 사용자와 구조를 정하면서 쓴다.

`## 3. 의존 방향` 에서 "금지"로 적은 것 중 검사 가능한 것을 `constraints.yaml` 로 옮기고,
**실제로 걸리는지 확인한다**:

```bash
python3 scripts/check_architecture.py
```

## 3. 첫 ADR

`adr` 스킬로 이 프로젝트의 첫 실질적 결정을 기록한다.

2단계에서 구조를 정하며 내린 결정이 있으면 그것부터 쓴다 (계층 구조, 상태 관리 선택,
데이터베이스 선택 등). 사용자에게 "지금까지 정한 것 중 나중에 '왜 그랬지?'라고 물을 만한
게 있나요?"라고 묻는다.

`ADR-0001` 은 ADR 형식 자체에 대한 결정이므로 그대로 두고 `ADR-0002` 부터 쓴다.

## 4. CLAUDE.md 채우기

```bash
grep -n "TODO" CLAUDE.md
```

- 프로젝트 한 줄 설명, 스택
- **테스트·린트·빌드 명령** — Phase 3/4 가 이 명령을 실제로 실행하므로 정확해야 한다
- `sub_categories` 값 (1단계에서 정한 역할)

## 5. 마무리

```bash
python3 scripts/rebuild_doc_index.py
python3 scripts/hooks/check_artifact_paths.py --all
git add docs/ CLAUDE.md && git commit -m "docs: Foundation 문서 초기 작성"
```

```
Foundation 준비 완료

  product.md       ✓  사용자 3역할, 범위 외 4항목
  architecture.md  ✓  3계층, 제약 2건
  constraints.yaml ✓  ARCH-001(error), ARCH-002(warn) — 검증 통과
  ADR              2건
  CLAUDE.md        ✓  pytest / ruff check

다음: /wf-feature <기능이름> 으로 첫 기능 요구사항을 작성하세요.
```

## 이미 다 되어 있으면

무엇이 있는지 보여주고, 갱신할 것이 있는지 묻는다. 덮어쓰지 않는다.
