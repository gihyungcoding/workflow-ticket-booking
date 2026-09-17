import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom'
import { ApiError, registerPerformance } from '../api/performances'
import { PerformanceRegisterPage } from './PerformanceRegisterPage'
import { theme } from '../theme'

vi.mock('../api/performances', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/performances')>()
  return { ...actual, registerPerformance: vi.fn() }
})

function DetailStub() {
  const { id } = useParams<{ id: string }>()
  return <div>DETAIL PAGE {id}</div>
}

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={['/performances/new']}>
        <Routes>
          <Route path="/performances/new" element={<PerformanceRegisterPage />} />
          <Route path="/performances/:id" element={<DetailStub />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

async function fillBasicInfo() {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('공연명'), '가을 재즈 콘서트')
  await user.type(screen.getByLabelText('장소'), 'OO홀')
  fireEvent.change(screen.getByLabelText('공연일시'), { target: { value: '2026-10-01T19:00' } })
  fireEvent.change(screen.getByLabelText('오픈'), { target: { value: '2026-09-10T10:00' } })
  fireEvent.change(screen.getByLabelText('마감'), { target: { value: '2026-09-30T23:59' } })
}

async function fillSectionAt(index: number, section: {
  grade: string
  price: string
  rowStart: string
  rowEnd: string
  seatsPerRow: string
}) {
  const user = userEvent.setup()
  await user.type(screen.getAllByLabelText('등급')[index], section.grade)
  await user.type(screen.getAllByLabelText('가격')[index], section.price)
  await user.type(screen.getAllByLabelText('시작 행')[index], section.rowStart)
  await user.type(screen.getAllByLabelText('종료 행')[index], section.rowEnd)
  await user.type(screen.getAllByLabelText('행당 좌석수')[index], section.seatsPerRow)
}

describe('PerformanceRegisterPage', () => {
  beforeEach(() => {
    vi.mocked(registerPerformance).mockReset()
  })

  test('SC-01 (happy) 등록 폼 제출 시 공연이 생성되고 상세 화면으로 이동한다', async () => {
    // Given
    // 등록 화면이 열려 있고 기본 정보 + 구역 1개가 입력되어 있다
    vi.mocked(registerPerformance).mockResolvedValue({
      id: 1,
      title: '가을 재즈 콘서트',
      venue: 'OO홀',
      startAt: '2026-10-01T19:00:00+09:00',
      openAt: '2026-09-10T10:00:00+09:00',
      closeAt: '2026-09-30T23:59:59+09:00',
      totalSeats: 20,
      availableSeats: 20,
      status: 'UPCOMING',
    })
    renderPage()
    await fillBasicInfo()
    await fillSectionAt(0, {
      grade: 'VIP',
      price: '120000',
      rowStart: 'A',
      rowEnd: 'B',
      seatsPerRow: '10',
    })

    // When
    // [등록] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '등록' }))

    // Then
    // POST 요청 본문에 입력한 기본 정보와 sections 가 담겨 전송된다
    await waitFor(() => expect(registerPerformance).toHaveBeenCalledTimes(1))
    expect(registerPerformance).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '가을 재즈 콘서트',
        venue: 'OO홀',
        sections: [
          expect.objectContaining({
            grade: 'VIP',
            price: 120000,
            rowStart: 'A',
            rowEnd: 'B',
            seatsPerRow: 10,
          }),
        ],
      }),
    )

    // 화면이 /performances/1 로 이동한다
    expect(await screen.findByText('DETAIL PAGE 1')).toBeInTheDocument()
  })

  test('SC-02 (happy) 구역을 추가하면 생성될 좌석 수 미리보기가 즉시 갱신된다', async () => {
    // Given
    // 등록 화면이 열려 있고 구역 1개(VIP, A~B, 10 → 20석)가 입력되어 있다
    renderPage()
    await fillSectionAt(0, {
      grade: 'VIP',
      price: '120000',
      rowStart: 'A',
      rowEnd: 'B',
      seatsPerRow: '10',
    })
    expect(within(screen.getByTestId('seat-preview')).getByText(/총\s*20/)).toBeInTheDocument()

    // [+ 구역 추가] 버튼을 눌러 빈 구역 카드가 하나 더 추가되어 있다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '구역 추가' }))

    // When
    // 새 구역 카드에 등급 R, 행 C~E, 행당 좌석수 20을 입력한다
    await fillSectionAt(1, {
      grade: 'R',
      price: '80000',
      rowStart: 'C',
      rowEnd: 'E',
      seatsPerRow: '20',
    })

    // Then
    // API 호출 없이 좌석 수 미리보기의 총합이 80(VIP 20 + R 60)으로 갱신된다
    const preview = screen.getByTestId('seat-preview')
    expect(within(preview).getByText(/총\s*80/)).toBeInTheDocument()
    // 구역별 미리보기에 VIP 20, R 60 이 각각 표시된다
    expect(within(preview).getByText(/VIP.*20/)).toBeInTheDocument()
    expect(within(preview).getByText(/R.*60/)).toBeInTheDocument()
    expect(registerPerformance).not.toHaveBeenCalled()
  })

  test('SC-03 (error) 시각 순서 위반 응답을 받으면 필드 아래 인라인 오류가 표시된다', async () => {
    // Given
    // 등록 화면에 기본 정보 + 구역 1개가 입력되어 있고 POST 가 INVALID_TIME_ORDER 400을 반환한다
    vi.mocked(registerPerformance).mockRejectedValue(
      new ApiError('INVALID_TIME_ORDER', 'openAt <= closeAt <= startAt 를 만족해야 합니다'),
    )
    renderPage()
    await fillBasicInfo()
    await fillSectionAt(0, {
      grade: 'VIP',
      price: '120000',
      rowStart: 'A',
      rowEnd: 'B',
      seatsPerRow: '10',
    })

    // When
    // [등록] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '등록' }))

    // Then
    // 시각 입력 영역 아래에 오류 메시지가 표시된다
    const timeFieldsError = await screen.findByTestId('time-fields-error')
    expect(
      within(timeFieldsError).getByText('예매 오픈·마감·공연 일시 순서가 올바르지 않습니다'),
    ).toBeInTheDocument()

    // 화면은 여전히 등록 폼이다(다른 라우트로 이동하지 않는다)
    expect(screen.queryByText(/DETAIL PAGE/)).not.toBeInTheDocument()
  })

  test('SC-04 (error) 좌석 총수 초과 응답을 받으면 해당 구역 아래 오류가 표시된다', async () => {
    // Given
    // 등록 화면에 기본 정보 + 구역 1개(R, A~Z, 200 → 5,200석)가 입력되어 있고
    // POST 가 SEAT_LIMIT_EXCEEDED 400을 반환한다
    vi.mocked(registerPerformance).mockRejectedValue(
      new ApiError('SEAT_LIMIT_EXCEEDED', '좌석 총수는 5,000을 넘을 수 없습니다: 5200'),
    )
    renderPage()
    await fillBasicInfo()
    await fillSectionAt(0, {
      grade: 'R',
      price: '80000',
      rowStart: 'A',
      rowEnd: 'Z',
      seatsPerRow: '200',
    })

    // When
    // [등록] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '등록' }))

    // Then
    // 그 구역 카드 아래에 오류 메시지가 표시된다
    const sectionCard = await screen.findByTestId('section-card-0')
    expect(
      within(sectionCard).getByText('좌석 총수는 5,000석을 넘을 수 없습니다'),
    ).toBeInTheDocument()
  })

  test('SC-05 (error) 등록 API 호출이 실패하면 폼 상단에 오류와 다시 시도가 표시된다', async () => {
    // Given
    // 등록 화면에 기본 정보 + 구역 1개가 입력되어 있고 POST 요청이 네트워크 오류로 거부된다
    vi.mocked(registerPerformance).mockRejectedValue(new Error('network error'))
    renderPage()
    await fillBasicInfo()
    await fillSectionAt(0, {
      grade: 'VIP',
      price: '120000',
      rowStart: 'A',
      rowEnd: 'B',
      seatsPerRow: '10',
    })

    // When
    // [등록] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '등록' }))

    // Then
    // 폼 상단에 "등록하지 못했습니다. 다시 시도해 주세요" 오류 메시지가 표시된다
    const formError = await screen.findByTestId('form-error')
    expect(within(formError).getByText('등록하지 못했습니다. 다시 시도해 주세요')).toBeInTheDocument()

    // [다시 시도] 버튼이 표시된다
    expect(within(formError).getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
  })
})
