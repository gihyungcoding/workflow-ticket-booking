/** '' · 공백만 · null · undefined 는 "문구 없음"이다 (PLAN prop_schema 의 무효값) */
export function isBlank(value: string | null | undefined): value is '' | null | undefined {
  return value == null || value.trim() === ''
}

/** 표시 문자열의 「공백 아님」 — 유니코드 공백·NBSP·U+200B 를 공백으로 본다 (ADR-0027) */
export function isBlankDisplay(value: unknown): boolean {
  return typeof value !== 'string' || value.replace(/[\s\u00a0\u200b\u3000]/g, '') === ''
}
