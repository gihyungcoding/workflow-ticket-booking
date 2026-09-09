# ADR-0007: 프론트엔드 테스트 프레임워크로 Vitest + Testing Library를 쓴다

- **상태**: 채택됨
- **날짜**: 2026-09-08
- **관련**: ADR-0003, TASK-002

---

## 맥락

TASK-002(공연 목록/상세 화면)는 이 프로젝트의 첫 프론트엔드 태스크였다.
`frontend/` 디렉터리 자체가 비어 있었고(`CLAUDE.md`가 "frontend 테스트: 미생성"이라고
적어둔 그대로), 스택은 이미 Vite + React로 확정돼 있었다(ADR-0003). 워크플로우의
Phase 2b(Red)를 진행하려면 실패하는 자동 테스트를 작성·실행할 수 있는 프레임워크가
있어야 하는데, 아직 이 프로젝트에 프론트엔드 테스트 관례가 전혀 없었다.

## 검토한 선택지

### A. Jest

- 장점: React 생태계에서 가장 널리 쓰이고 자료가 압도적으로 많다. Create React App
  시절부터의 사실상 표준.
- 단점: Vite 프로젝트에서 쓰려면 별도 트랜스폼 설정(babel-jest 또는 ts-jest,
  moduleNameMapper 등)이 필요해 Vite의 esbuild 기반 빌드 파이프라인과 별개로
  테스트 전용 파이프라인을 유지해야 한다.

### B. Vitest + @testing-library/react + jsdom

- 장점: `vite.config.ts` 를 그대로 재사용해 별도 트랜스폼 설정이 필요 없다(같은
  esbuild 엔진, 실행도 빠르다). Jest와 거의 동일한 API(`describe`/`test`/`expect`/
  `vi.mock`)라 학습 비용이 낮다.
- 단점: Jest보다 생태계가 작아 일부 서드파티 자료·리포터가 부족할 수 있다.

## 결정

**B — Vitest + @testing-library/react + @testing-library/jest-dom + jsdom.**

### 이유

이미 Vite로 빌드 도구가 확정된 상태(ADR-0003)에서 Jest를 쓰면 빌드 설정과 테스트
설정을 이중으로 유지해야 하는데, 1인 개발에서 이 비용이 정당화되지 않는다. Vitest는
`vite.config.ts` 하나에 `test` 필드만 추가하면 되고, Jest와 API가 거의 동일해
자료를 찾는 데도 큰 어려움이 없다.

## 결과

- 좋아지는 것: 빌드 설정과 테스트 설정이 `vite.config.ts` 하나로 통합된다. Jest
  대비 실행 속도가 빠르다.
- 감수하는 것: Vitest 전용 이슈를 직접 찾아 해결해야 한다 — 예를 들어 TASK-002에서
  실제로 `@testing-library/jest-dom`의 매처 타입이 일반 진입점이 아니라
  `'@testing-library/jest-dom/vitest'` 서브패스 임포트로만 활성화됨을 빌드 중에
  발견했다(`DEV_TASK-002.json` 참고). Jest만큼 스택오버플로우 자료가 많지 않다.
- 되돌리려면: 테스트 파일의 API(`describe`/`test`/`expect`)는 Jest와 거의 동일해
  테스트 코드 자체의 마이그레이션 비용은 작지만, `vite.config.ts`의 `test` 필드를
  분리해 별도 `jest.config`와 트랜스폼(ts-jest/babel-jest)을 새로 구성해야 한다.

## 검사 가능한 제약

- 없음 (테스트 프레임워크 선택은 코드로 강제할 규칙이 아니라 컨벤션이다)
