/** '' · 공백만 · null · undefined 는 "문구 없음"이다 (PLAN prop_schema 의 무효값) */
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
