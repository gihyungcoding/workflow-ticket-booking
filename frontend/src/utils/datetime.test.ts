import { fromDatetimeLocalInput, toDatetimeLocalInput } from './datetime'

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
