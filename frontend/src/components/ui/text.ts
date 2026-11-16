import type { ReactNode } from 'react'

/**
 * '' · 공백만 · null · undefined 는 "문구 없음"이다 (PLAN prop_schema 의 무효값).
 * 개발자가 넘기는 부품 문구(Field·Badge·Segmented·Toast)용 trim 판정이다. 서버가 준 표시 값은 `isBlankDisplay`(ADR-0027)를 쓴다 —
 * 두 판정은 U+FEFF·U+200B·U+0085 에서 다르고 그것이 의도다(TASK-020 Phase 4 RC-1 — 부품 문구에는 서버 값이 들어오지 않는다).
 */
export function isBlank(value: string | null | undefined): value is '' | null | undefined {
  return value == null || value.trim() === ''
}

/**
 * 표시 문자열의 「공백 아님」 — 유니코드 공백(Zs·Zl·Zp)·탭·줄바꿈·U+0085·U+200B 를 공백으로 본다 (ADR-0027).
 * 서버 상수 `[^\p{Z}\s\u200b]`(자바, 유니코드 모드)와 같은 집합이다. JS 의 `\s` 는 U+FEFF(서식 문자)까지 공백으로 보고
 * U+0085 는 빼므로 쓰지 않는다.
 */
export function isBlankDisplay(value: unknown): boolean {
  return typeof value !== 'string' || value.replace(/[\p{Z}\t-\r\u0085\u200b]/gu, '') === ''
}

/**
 * 부품이 받은 문구(ReactNode)가 그릴 만한 내용인가 — 문자열은 isBlankDisplay 로(ADR-0027), 그 밖의 노드는 있으면 내용이다.
 * 표시 문구의 「없음」 판정을 한 곳에 둔다(TASK-020 RC-1).
 */
export function hasContent(node: ReactNode): boolean {
  return typeof node === 'string' ? !isBlankDisplay(node) : Boolean(node)
}
