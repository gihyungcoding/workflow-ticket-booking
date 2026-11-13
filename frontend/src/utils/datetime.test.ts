import { formatClock, formatDateTime, fromDatetimeLocalInput, toDatetimeLocalInput } from './datetime'

describe('fromDatetimeLocalInput', () => {
  test('REGRESSION 빈 문자열이면 RangeError 없이 그대로 반환한다', () => {
    // Phase 4에서 발견된 결함: new Date('').toISOString() 이 RangeError를 던져
    // 등록/수정 폼에서 "네트워크 실패"로 오분류되고 요청이 전송되지 않았다.
    expect(fromDatetimeLocalInput('')).toBe('')
  })

  test('REGRESSION 유효하지 않은 값이면 RangeError 없이 원본을 그대로 반환한다', () => {
    expect(fromDatetimeLocalInput('not-a-date')).toBe('not-a-date')
  })

  test('유효한 datetime-local 값은 ISO 문자열로 변환한다', () => {
    const result = fromDatetimeLocalInput('2026-10-01T19:00')
    expect(() => new Date(result)).not.toThrow()
    expect(Number.isNaN(new Date(result).getTime())).toBe(false)
  })
})

describe('toDatetimeLocalInput', () => {
  test('ISO 문자열을 datetime-local 입력 형식으로 변환한다', () => {
    const result = toDatetimeLocalInput('2026-10-01T10:00:00Z')
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/)
  })
})

// TASK-020 SC-32 — SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md
describe('TASK-020 한국 시간 표기', () => {
  const ROWS: Array<[string, 'datetime' | 'clock', string, string]> = [
    ['a', 'datetime', '2026-11-21T10:00:00Z', '11월 21일(토) 오후 7:00'],
    ['b', 'datetime', '2026-12-19T09:00:00Z', '12월 19일(토) 오후 6:00'],
    ['c', 'datetime', '2026-11-11T15:00:00Z', '11월 12일(목) 오전 12:00'],
    ['d', 'datetime', '2026-11-11T03:00:00Z', '11월 11일(수) 오후 12:00'],
    ['e', 'datetime', '2026-01-04T15:30:00Z', '1월 5일(월) 오전 12:30'],
    ['f', 'clock', '2026-11-11T08:55:28Z', '오후 5:55:28'],
    ['g', 'clock', '2026-11-11T15:00:05Z', '오전 12:00:05'],
    ['h', 'clock', '2026-11-11T03:00:00Z', '오후 12:00:00'],
  ]

  test('SC-32 (boundary) 날짜·시각 표기가 기기 시간대와 무관하게 한국 시간으로 나온다', () => {
    // Given 기기 시간대가 UTC·America/Los_Angeles·Asia/Seoul 각각이다
    // 앱 tsconfig 에 node 타입이 없다 — 환경변수는 타입 없이 다룬다
    const env = (globalThis as unknown as { process: { env: Record<string, string | undefined> } }).process.env
    const original = env.TZ
    try {
      for (const zone of ['UTC', 'America/Los_Angeles', 'Asia/Seoul']) {
        env.TZ = zone
        for (const [row, kind, input, expected] of ROWS) {
          // When 날짜·시각 표기 유틸에 표의 ISO 시각을 넣는다
          const actual = kind === 'datetime' ? formatDateTime(input) : formatClock(input)
          // Then 세 시간대 모두 표의 결과가 같다
          expect(actual, `${zone} ${row}`).toBe(expected)
        }
      }
    } finally {
      if (original === undefined) delete env.TZ
      else env.TZ = original
    }
  })
})
