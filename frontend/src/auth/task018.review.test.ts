import { fetchPendingSignup } from '../api/auth'

// TASK-018 Phase 3 자체 리뷰 RC-1 — 시각 문자열의 형식만 거르고 달력상 없는 날짜·시각을 넘겨 읽던 결함.
// SCENARIO_TASK-018 제가 정한 값 7 「expiresAt 은 시각으로 읽히는 문자열」 의 변종이다. 실제 입구(fetch 응답)로 넣는다.

function stubPending(expiresAt: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      new Response(JSON.stringify({ provider: 'KAKAO', email: 'a@b.com', name: '김하린', expiresAt }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('TASK-018 RC-1 대기 정보 expiresAt 은 달력상 있는 시각만 받는다', () => {
  test.each([
    '2026-02-30T00:00:00Z',
    '2026-04-31T08:40:00Z',
    '2026-13-01T08:40:00Z',
    '2026-11-11T24:00:00Z',
    '2026-11-11T08:60:00Z',
    '2026-11-11T08:40:60Z',
    '2026-11-11T08:40:00+24:00',
    '2026-11-11T08:40:00+09:60',
  ])('없는 시각 %s 은 대기 정보 오류다', async (expiresAt) => {
    stubPending(expiresAt)
    await expect(fetchPendingSignup('pend-1')).rejects.toThrow()
  })

  test.each([
    ['2026-11-11T08:40:00Z', Date.UTC(2026, 10, 11, 8, 40)],
    ['2026-11-11T17:40:00+09:00', Date.UTC(2026, 10, 11, 8, 40)],
    ['2026-11-11T08:40:00.123456789Z', Date.UTC(2026, 10, 11, 8, 40, 0, 123)],
    ['2028-02-29T00:00:00Z', Date.UTC(2028, 1, 29)],
    ['2026-11-11T00:30:00-14:00', Date.UTC(2026, 10, 11, 14, 30)],
  ])('있는 시각 %s 은 그 순간으로 읽는다', async (expiresAt, at) => {
    stubPending(expiresAt)
    await expect(fetchPendingSignup('pend-1')).resolves.toMatchObject({ expiresAt: at })
  })
})
