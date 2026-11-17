import { render, screen, within } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter } from 'react-router-dom'
import { getNextOpening, getPerformances } from '../api/performances'
import type { PerformanceListResponse } from '../api/performances'
import { PerformanceListPage } from './PerformanceListPage'
import { theme } from '../theme'
import { GlobalTokens } from '../theme/GlobalTokens'

vi.mock('../api/performances')

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      {/* TASK-021 — 새 화면의 글자는 body 에서 폰트를 물려받는다. 앱(main.tsx)과 같이 전역 토큰을 둔다 */}
      <GlobalTokens />
      <MemoryRouter>
        <PerformanceListPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('PerformanceListPage', () => {
  beforeEach(() => {
    vi.mocked(getPerformances).mockReset()
    // TASK-021 — 홈이 머리글 후보를 따로 부른다. 이 파일은 목록만 본다(후보 없음)
    vi.mocked(getNextOpening).mockReset()
    vi.mocked(getNextOpening).mockResolvedValue(null)
  })

  test('SC-01 (happy) 목록에 공연이 카드와 상태 배지로 표시된다', async () => {
    // Given
    // GET /api/performances 가 공연 2건을 담은 200 응답을 반환한다 —
    // 제목 "재즈의 밤"인 공연은 status: "OPEN", 제목 "클래식 갈라"인 공연은 status: "SOLD_OUT"이다
    const response: PerformanceListResponse = {
      content: [
        {
          id: 1,
          title: '재즈의 밤',
          venue: 'OO홀',
          startAt: '2026-10-01T19:00:00+09:00',
          openAt: '2026-09-10T10:00:00+09:00',
          closeAt: '2026-09-30T23:59:59+09:00',
          totalSeats: 100,
          availableSeats: 10,
          status: 'OPEN',
        },
        {
          id: 2,
          title: '클래식 갈라',
          venue: 'XX아트홀',
          startAt: '2026-10-05T19:00:00+09:00',
          openAt: '2026-09-01T10:00:00+09:00',
          closeAt: '2026-09-20T23:59:59+09:00',
          totalSeats: 100,
          availableSeats: 0,
          status: 'SOLD_OUT',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
    }
    vi.mocked(getPerformances).mockResolvedValue(response)

    // When
    // 관객이 공연 목록 화면에 진입한다
    renderPage()

    // Then
    // 카드가 2개 렌더된다
    const cards = await screen.findAllByRole('link')
    expect(cards).toHaveLength(2)

    // 각 카드에 제목·장소·공연 일시가 보인다
    const jazzCard = cards.find((card) => within(card).queryByText('재즈의 밤'))
    const galaCard = cards.find((card) => within(card).queryByText('클래식 갈라'))
    expect(jazzCard).toBeDefined()
    expect(galaCard).toBeDefined()
    expect(within(jazzCard!).getByText('OO홀', { exact: false })).toBeInTheDocument()
    expect(within(galaCard!).getByText('XX아트홀', { exact: false })).toBeInTheDocument()

    // 제목이 "재즈의 밤"인 카드에는 "예매 가능" 배지가 보인다 (TASK-017: design.md §5 문구)
    expect(within(jazzCard!).getByText('예매 가능')).toBeInTheDocument()

    // 제목이 "클래식 갈라"인 카드에는 "매진" 배지가 보인다
    expect(within(galaCard!).getByText('매진')).toBeInTheDocument()
  })

  test('SC-02 (happy) 목록 카드의 공연명과 본문에 Pretendard 가 적용된다', async () => {
    // Given
    // PerformanceListPage 가 공연 1건("재즈의 밤")을 렌더한다
    const response: PerformanceListResponse = {
      content: [
        {
          id: 1,
          title: '재즈의 밤',
          venue: 'OO홀',
          startAt: '2026-10-01T19:00:00+09:00',
          openAt: '2026-09-10T10:00:00+09:00',
          closeAt: '2026-09-30T23:59:59+09:00',
          totalSeats: 100,
          availableSeats: 10,
          status: 'OPEN',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
    }
    vi.mocked(getPerformances).mockResolvedValue(response)

    // When
    // 카드 안의 공연명 텍스트 요소의 스타일을 확인한다
    renderPage()
    const titleEl = await screen.findByText('재즈의 밤')
    const venueEl = screen.getByText('OO홀', { exact: false })

    // Then
    // 그 요소의 font-family 에 'Pretendard' 가 포함된다 (TASK-017: 서체는 Pretendard 한 가지 — design.md §4)
    expect(getComputedStyle(titleEl).fontFamily).toContain('Pretendard')

    // 같은 카드의 장소 텍스트 요소도 'Pretendard' 이고 옛 명조('Noto Serif KR')가 아니다
    expect(getComputedStyle(venueEl).fontFamily).toContain('Pretendard')
    expect(getComputedStyle(venueEl).fontFamily).not.toContain('Noto Serif KR')
  })

  test('SC-03 (error) 목록 API 호출 실패 시 오류 메시지와 다시 시도 버튼이 표시된다', async () => {
    // Given
    // GET /api/performances 가 500 응답을 반환한다
    vi.mocked(getPerformances).mockRejectedValue(new Error('공연 목록 조회 실패: 500'))

    // When
    // 관객이 공연 목록 화면에 진입한다
    renderPage()

    // Then
    // '공연 목록을 불러오지 못했어요' 오류 메시지가 보인다 (TASK-021 SC-21 — 시안 문구)
    expect(await screen.findByText('공연 목록을 불러오지 못했어요')).toBeInTheDocument()

    // [다시 시도] 버튼이 보인다
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })

  test('SC-05 (boundary) 로딩 중에는 스켈레톤이 표시된다', () => {
    // Given
    // GET /api/performances 요청이 아직 응답하지 않은 상태(pending)다
    vi.mocked(getPerformances).mockReturnValue(new Promise(() => {}))

    // When
    // 관객이 공연 목록 화면에 진입한다
    const { container } = renderPage()

    // Then
    // 카드 자리에 스켈레톤이 3개 이상 보인다
    const skeletons = container.querySelectorAll('[data-testid="performance-card-skeleton"]')
    expect(skeletons.length).toBeGreaterThanOrEqual(3)

    // 실제 공연 카드는 보이지 않는다
    expect(screen.queryAllByRole('link')).toHaveLength(0)
  })

  test('SC-06 (boundary) 목록이 빈 배열이면 안내 문구가 표시된다', async () => {
    // Given
    // GET /api/performances 가 content: [] 를 담은 200 응답을 반환한다
    vi.mocked(getPerformances).mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0 })

    // When
    // 관객이 공연 목록 화면에 진입한다
    renderPage()

    // Then
    // '다가오는 공연이 없어요' 안내가 보인다 (TASK-021 SC-19 — 시안 문구)
    expect(await screen.findByText('다가오는 공연이 없어요')).toBeInTheDocument()

    // 카드가 하나도 렌더되지 않는다
    expect(screen.queryAllByRole('link')).toHaveLength(0)
  })
})
