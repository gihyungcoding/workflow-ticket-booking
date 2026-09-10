import { render, screen } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { getPerformance, PerformanceNotFoundError } from '../api/performances'
import type { Performance } from '../api/performances'
import { PerformanceDetailPage } from './PerformanceDetailPage'
import { theme } from '../theme'

vi.mock('../api/performances', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/performances')>()
  return { ...actual, getPerformance: vi.fn() }
})

function renderPage(id: string) {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[`/performances/${id}`]}>
        <Routes>
          <Route path="/performances/:id" element={<PerformanceDetailPage />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('PerformanceDetailPage', () => {
  beforeEach(() => {
    vi.mocked(getPerformance).mockReset()
  })

  test('SC-02 (happy) 상세 화면에 공연 정보 전체가 표시된다', async () => {
    // Given
    // GET /api/performances/{id} 가 공연 1건(title, venue, startAt, openAt, closeAt,
    // availableSeats, status: "OPEN")을 담은 200 응답을 반환한다
    const performance: Performance = {
      id: 1,
      title: '재즈의 밤',
      venue: 'OO홀',
      startAt: '2026-10-01T19:00:00+09:00',
      openAt: '2026-09-10T10:00:00+09:00',
      closeAt: '2026-09-30T23:59:59+09:00',
      totalSeats: 100,
      availableSeats: 37,
      status: 'OPEN',
    }
    vi.mocked(getPerformance).mockResolvedValue(performance)

    // When
    // 관객이 해당 id의 공연 상세 화면에 진입한다
    renderPage('1')

    // Then
    // 화면에 공연명·장소·공연 일시·오픈 시각·마감 시각·잔여 좌석 수가 보인다
    expect(await screen.findByText('재즈의 밤')).toBeInTheDocument()
    expect(screen.getByText('OO홀')).toBeInTheDocument()
    expect(screen.getByText(new Date(performance.startAt).toLocaleString(), { exact: false })).toBeInTheDocument()
    expect(screen.getByText(new Date(performance.openAt).toLocaleString(), { exact: false })).toBeInTheDocument()
    expect(screen.getByText(new Date(performance.closeAt).toLocaleString(), { exact: false })).toBeInTheDocument()
    expect(screen.getByText('37석', { exact: false })).toBeInTheDocument()

    // '예매가능' 상태 배지가 보인다
    expect(screen.getByText('예매가능')).toBeInTheDocument()
  })

  test('SC-03 (happy) 상세 화면의 공연명에 Noto Serif KR이 적용된다', async () => {
    // Given
    // PerformanceDetailPage 가 공연 1건("재즈의 밤")을 렌더한다
    const performance: Performance = {
      id: 1,
      title: '재즈의 밤',
      venue: 'OO홀',
      startAt: '2026-10-01T19:00:00+09:00',
      openAt: '2026-09-10T10:00:00+09:00',
      closeAt: '2026-09-30T23:59:59+09:00',
      totalSeats: 100,
      availableSeats: 37,
      status: 'OPEN',
    }
    vi.mocked(getPerformance).mockResolvedValue(performance)

    // When
    // 공연명 텍스트 요소의 스타일을 확인한다
    renderPage('1')
    const titleEl = await screen.findByText('재즈의 밤')
    const venueEl = screen.getByText('OO홀')

    // Then
    // 그 요소의 font-family 에 'Noto Serif KR' 이 포함된다
    expect(getComputedStyle(titleEl).fontFamily).toContain('Noto Serif KR')

    // 같은 화면의 장소 텍스트 요소의 font-family 는 'IBM Plex Sans KR' 이다('Noto Serif KR' 이 아니다)
    expect(getComputedStyle(venueEl).fontFamily).toContain('IBM Plex Sans KR')
    expect(getComputedStyle(venueEl).fontFamily).not.toContain('Noto Serif KR')
  })

  test('SC-04 (error) 존재하지 않는 공연 상세는 404 안내를 보여준다', async () => {
    // Given
    // GET /api/performances/{id} 가 404와 {code: "PERFORMANCE_NOT_FOUND"} 를 반환한다
    vi.mocked(getPerformance).mockRejectedValue(new PerformanceNotFoundError('공연을 찾을 수 없습니다: 999'))

    // When
    // 관객이 해당 id의 공연 상세 화면에 진입한다
    renderPage('999')

    // Then
    // '존재하지 않는 공연입니다' 안내가 보인다
    expect(await screen.findByText('존재하지 않는 공연입니다')).toBeInTheDocument()

    // [목록으로] 버튼이 보인다
    expect(screen.getByRole('link', { name: '목록으로' })).toBeInTheDocument()
  })
})
