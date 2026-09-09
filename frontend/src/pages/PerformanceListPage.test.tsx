import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { getPerformances } from '../api/performances'
import type { PerformanceListResponse } from '../api/performances'
import { PerformanceListPage } from './PerformanceListPage'

vi.mock('../api/performances')

function renderPage() {
  return render(
    <MemoryRouter>
      <PerformanceListPage />
    </MemoryRouter>,
  )
}

describe('PerformanceListPage', () => {
  beforeEach(() => {
    vi.mocked(getPerformances).mockReset()
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

    // 제목이 "재즈의 밤"인 카드에는 "예매가능" 배지가 보인다
    expect(within(jazzCard!).getByText('예매가능')).toBeInTheDocument()

    // 제목이 "클래식 갈라"인 카드에는 "매진" 배지가 보인다
    expect(within(galaCard!).getByText('매진')).toBeInTheDocument()
  })

  test('SC-03 (error) 목록 API 호출 실패 시 오류 메시지와 다시 시도 버튼이 표시된다', async () => {
    // Given
    // GET /api/performances 가 500 응답을 반환한다
    vi.mocked(getPerformances).mockRejectedValue(new Error('공연 목록 조회 실패: 500'))

    // When
    // 관객이 공연 목록 화면에 진입한다
    renderPage()

    // Then
    // '목록을 불러오지 못했습니다' 오류 메시지가 보인다
    expect(await screen.findByText('목록을 불러오지 못했습니다')).toBeInTheDocument()

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
    // '예정된 공연이 없습니다' 안내가 보인다
    expect(await screen.findByText('예정된 공연이 없습니다')).toBeInTheDocument()

    // 카드가 하나도 렌더되지 않는다
    expect(screen.queryAllByRole('link')).toHaveLength(0)
  })
})
