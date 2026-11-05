/** '' · 공백만 · null · undefined 는 "문구 없음"이다 (PLAN prop_schema 의 무효값) */
export function isBlank(value: string | null | undefined): value is '' | null | undefined {
  return value == null || value.trim() === ''
}
