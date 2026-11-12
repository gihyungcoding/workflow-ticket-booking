import {
  cancelPerformance,
  getPerformance,
  getPerformances,
  registerPerformance,
  updatePerformance,
} from './performances'
import type { RegisterPerformanceInput, UpdatePerformanceInput } from './performances'

// TASK-015 시나리오 SC-01 ~ SC-03. SoT: workflow_design/05_scenario/SCENARIO_TASK-015.md
// SC-04·SC-05(쓰기 키)는 TASK-024 에서 지웠다 — 헤더 규칙은 auth/task024.organizer.test.tsx SC-01·SC-02

const performanceBody = {
  id: 7,
  title: '가을 재즈 콘서트',
  venue: 'OO홀',
  startAt: '2026-10-01T19:00:00+09:00',
  openAt: '2026-09-10T10:00:00+09:00',
  closeAt: '2026-09-30T23:59:59+09:00',
  totalSeats: 20,
  availableSeats: 20,
  status: 'UPCOMING',
}

const listBody = { content: [performanceBody], page: 0, size: 20, totalElements: 1 }

const registerInput: RegisterPerformanceInput = {
  title: '가을 재즈 콘서트',
  venue: 'OO홀',
  startAt: '2026-10-01T19:00:00+09:00',
  openAt: '2026-09-10T10:00:00+09:00',
  closeAt: '2026-09-30T23:59:59+09:00',
  sections: [{ grade: 'VIP', price: 120000, rowStart: 'A', rowEnd: 'B', seatsPerRow: 10 }],
}

const updateInput: UpdatePerformanceInput = {
  title: '가을 재즈 콘서트 (수정)',
  venue: 'OO홀',
  startAt: '2026-10-01T19:00:00+09:00',
  openAt: '2026-09-10T10:00:00+09:00',
  closeAt: '2026-09-30T23:59:59+09:00',
}

type Call = { url: string; init: RequestInit | undefined }

function stubFetch() {
  const calls: Call[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: unknown, init?: RequestInit) => {
      calls.push({ url: String(url), init })
      const isList = String(url).endsWith('/api/performances') && (init?.method ?? 'GET') === 'GET'
      return new Response(JSON.stringify(isList ? listBody : performanceBody), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }),
  )
  return calls
}

function methodOf(call: Call): string {
  return (call.init?.method ?? 'GET').toUpperCase()
}

function headersOf(call: Call): Headers {
  return new Headers(call.init?.headers)
}

async function callAllFive(): Promise<void> {
  await getPerformances()
  await getPerformance('7')
  await registerPerformance(registerInput)
  await updatePerformance('7', updateInput)
  await cancelPerformance('7')
}

afterEach(() => {
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
})

describe('http — API 주소', () => {
  test('test_sc01_주소가_설정되면_모든_호출이_그_주소를_앞에_붙여_나간다', async () => {
    // Given VITE_API_BASE_URL 이 https://api.example.com 으로 설정돼 있다
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com')
    const calls = stubFetch()

    // When 호출 5곳을 각각 실행한다
    await callAllFive()

    // Then 5곳의 요청 URL 이 모두 https://api.example.com/api/... 이다
    expect(calls.map((c) => c.url)).toEqual([
      'https://api.example.com/api/performances',
      'https://api.example.com/api/performances/7',
      'https://api.example.com/api/performances',
      'https://api.example.com/api/performances/7',
      'https://api.example.com/api/performances/7/cancel',
    ])
    // And 상대 경로로만 나간 요청이 없다
    expect(calls.filter((c) => c.url.startsWith('/'))).toHaveLength(0)
  })

  test('test_sc02_주소_설정이_없으면_기존처럼_상대_경로로_나간다', async () => {
    // Given 미설정·빈 문자열·공백만 세 경우 모두 미설정으로 취급한다
    for (const value of [undefined, '', '   ']) {
      vi.unstubAllEnvs()
      if (value !== undefined) vi.stubEnv('VITE_API_BASE_URL', value)
      const calls = stubFetch()

      // When 호출 5곳을 각각 실행한다
      await callAllFive()

      // Then 요청 URL 이 정확히 상대 경로이다
      expect(calls.map((c) => c.url)).toEqual([
        '/api/performances',
        '/api/performances/7',
        '/api/performances',
        '/api/performances/7',
        '/api/performances/7/cancel',
      ])
      // And 메서드: 조회 GET, 등록 POST, 수정 PUT, 취소 POST
      expect(calls.map(methodOf)).toEqual(['GET', 'GET', 'POST', 'PUT', 'POST'])
      // And 등록·수정의 헤더는 정확히 Content-Type 하나이고 본문은 입력의 JSON 문자열이다
      expect([...headersOf(calls[2]).entries()]).toEqual([['content-type', 'application/json']])
      expect([...headersOf(calls[3]).entries()]).toEqual([['content-type', 'application/json']])
      expect(calls[2].init?.body).toBe(JSON.stringify(registerInput))
      expect(calls[3].init?.body).toBe(JSON.stringify(updateInput))
      // And 취소·조회 요청에는 본문이 없다
      expect(calls[0].init?.body).toBeUndefined()
      expect(calls[1].init?.body).toBeUndefined()
      expect(calls[4].init?.body).toBeUndefined()
    }
  })

  test('test_sc03_주소_끝에_슬래시가_있어도_이중_슬래시가_생기지_않는다', async () => {
    // Given 끝 슬래시 1개, 2개 두 경우
    for (const base of ['https://api.example.com/', 'https://api.example.com//']) {
      vi.unstubAllEnvs()
      vi.stubEnv('VITE_API_BASE_URL', base)
      const calls = stubFetch()

      // When getPerformances 를 실행한다
      await getPerformances()

      // Then 요청 URL 은 https://api.example.com/api/performances 이다
      expect(calls[0].url).toBe('https://api.example.com/api/performances')
      // And 스킴 뒤에 // 가 없다
      expect(calls[0].url.replace('https://', '')).not.toContain('//')
    }
  })
})
