import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { cancelPerformance, getPerformance, updatePerformance, ApiError } from '../api/performances'
import type { Performance } from '../api/performances'
import { PerformanceEditPage } from './PerformanceEditPage'
import { theme } from '../theme'

vi.mock('../api/performances', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/performances')>()
  return {
    ...actual,
    getPerformance: vi.fn(),
    updatePerformance: vi.fn(),
    cancelPerformance: vi.fn(),
  }
})

function renderPage(id = '5') {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[`/performances/${id}/edit`]}>
        <Routes>
          <Route path="/performances/:id/edit" element={<PerformanceEditPage />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

function basePerformance(): Performance {
  return {
    id: 5,
    title: '가을 재즈 콘서트',
    venue: 'OO홀',
    startAt: '2026-10-01T19:00:00+09:00',
    openAt: '2026-09-10T10:00:00+09:00',
    closeAt: '2026-09-30T23:59:59+09:00',
    totalSeats: 20,
    availableSeats: 20,
    status: 'UPCOMING',
    sections: [{ grade: 'VIP', price: 120000, seatCount: 20 }],
  }
}

describe('PerformanceEditPage', () => {
  beforeEach(() => {
    vi.mocked(getPerformance).mockReset()
    vi.mocked(updatePerformance).mockReset()
    vi.mocked(cancelPerformance).mockReset()
  })

  test('SC-06 (happy) 수정 화면 진입 시 기존 값과 좌석 구성 요약이 표시된다', async () => {
    // Given
    // GET /api/performances/5 가 200과 공연 상세(sections 포함)를 반환한다
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())

    // When
    // 수정 화면(/performances/5/edit)에 진입한다
    renderPage('5')

    // Then
    // 공연명 입력란에 "가을 재즈 콘서트"가, 장소 입력란에 "OO홀"이 채워져 표시된다
    expect(await screen.findByDisplayValue('가을 재즈 콘서트')).toBeInTheDocument()
    expect(screen.getByDisplayValue('OO홀')).toBeInTheDocument()

    // 좌석 구성 요약 영역에 "VIP · 120,000원 · 20석" 텍스트가 표시된다
    const summary = screen.getByTestId('section-summary')
    expect(within(summary).getByText('VIP · 120,000원 · 20석')).toBeInTheDocument()

    // 그 영역에는 값을 고칠 수 있는 입력 요소(input/textarea 등)가 없다
    expect(within(summary).queryAllByRole('textbox')).toHaveLength(0)
  })

  test('SC-07 (error) 오픈 이후 수정하려 하면 전용 안내가 표시된다', async () => {
    // Given
    // 수정 화면에 기존 값이 채워져 있고 PUT 이 REGISTRATION_ALREADY_OPEN 409를 반환한다
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())
    vi.mocked(updatePerformance).mockRejectedValue(
      new ApiError('REGISTRATION_ALREADY_OPEN', '이미 오픈된 공연은 수정할 수 없습니다: 5'),
    )
    renderPage('5')
    await screen.findByDisplayValue('가을 재즈 콘서트')

    // When
    // [저장] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '저장' }))

    // Then
    // "이미 오픈된 공연은 기본 정보를 수정할 수 없습니다" 안내가 표시된다
    expect(
      await screen.findByText('이미 오픈된 공연은 기본 정보를 수정할 수 없습니다'),
    ).toBeInTheDocument()
  })

  test('SC-08 (happy) 취소 버튼을 클릭하면 확인 다이얼로그가 뜬다', async () => {
    // Given
    // 수정 화면이 열려 있다
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())
    renderPage('5')
    await screen.findByDisplayValue('가을 재즈 콘서트')

    // When
    // [공연 취소] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '공연 취소' }))

    // Then
    // "이 공연을 취소하시겠습니까?" 확인 다이얼로그가 표시된다
    expect(await screen.findByText('이 공연을 취소하시겠습니까?')).toBeInTheDocument()
  })

  test('SC-09 (happy) 확인 다이얼로그에서 확인하면 취소 상태가 반영된다', async () => {
    // Given
    // 수정 화면에서 [공연 취소] 버튼을 클릭해 확인 다이얼로그가 열려 있다
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())
    // POST cancel 응답에는 sections가 없다(PerformanceResponse — 상세 GET에서만 채워짐).
    // 실제 API 계약과 다르게 sections를 포함시키면 화면이 응답을 그대로 신뢰해도 되는 것처럼
    // 착시를 일으켜 sections 소실 결함을 가린다(Phase 4 code-reviewer 지적).
    const { sections: _sections, ...cancelledWithoutSections } = basePerformance()
    vi.mocked(cancelPerformance).mockResolvedValue({ ...cancelledWithoutSections, status: 'CANCELLED' })
    renderPage('5')
    await screen.findByDisplayValue('가을 재즈 콘서트')
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '공연 취소' }))
    await screen.findByText('이 공연을 취소하시겠습니까?')

    // When
    // 다이얼로그의 확인 버튼을 클릭한다
    await user.click(screen.getByRole('button', { name: '확인' }))

    // Then
    // POST /api/performances/5/cancel 요청이 전송된다
    await vi.waitFor(() => expect(cancelPerformance).toHaveBeenCalledWith('5'))

    // 화면의 상태 표시 영역에 "공연취소" 텍스트가 표시된다
    expect(await screen.findByText('공연취소')).toBeInTheDocument()

    // 취소 응답에 sections가 없어도 기존에 표시되던 좌석 구성 요약은 사라지지 않는다
    // (Phase 4 검증에서 실물로 발견된 결함 — 회귀 방지)
    const summary = screen.getByTestId('section-summary')
    expect(within(summary).getByText('VIP · 120,000원 · 20석')).toBeInTheDocument()
  })

  test('REGRESSION 취소 API 실패 시 다이얼로그가 닫히고 오류가 표시된다', async () => {
    // Given
    // 수정 화면에서 [공연 취소] 버튼을 클릭해 확인 다이얼로그가 열려 있고
    // POST cancel 이 네트워크 오류로 거부된다
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())
    vi.mocked(cancelPerformance).mockRejectedValue(new Error('network error'))
    renderPage('5')
    await screen.findByDisplayValue('가을 재즈 콘서트')
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '공연 취소' }))
    await screen.findByText('이 공연을 취소하시겠습니까?')

    // When
    // 다이얼로그의 확인 버튼을 클릭한다
    await user.click(screen.getByRole('button', { name: '확인' }))

    // Then
    // 실패가 조용히 삼켜지지 않고 오류 메시지가 표시되며 다이얼로그는 닫힌다
    expect(await screen.findByText('취소하지 못했습니다. 다시 시도해 주세요')).toBeInTheDocument()
    await vi.waitFor(() =>
      expect(screen.queryByText('이 공연을 취소하시겠습니까?')).not.toBeInTheDocument(),
    )
  })

  test('REGRESSION 취소 성공 시 이전 저장 실패 배너가 남지 않는다', async () => {
    // Given
    // 저장이 실패해 오류 배너가 표시된 상태에서
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())
    vi.mocked(updatePerformance).mockRejectedValue(new Error('network error'))
    const { sections: _sections, ...cancelledWithoutSections } = basePerformance()
    vi.mocked(cancelPerformance).mockResolvedValue({ ...cancelledWithoutSections, status: 'CANCELLED' })
    renderPage('5')
    await screen.findByDisplayValue('가을 재즈 콘서트')
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '저장' }))
    expect(await screen.findByText('저장하지 못했습니다. 다시 시도해 주세요')).toBeInTheDocument()

    // When
    // 취소를 확인한다
    await user.click(screen.getByRole('button', { name: '공연 취소' }))
    await screen.findByText('이 공연을 취소하시겠습니까?')
    await user.click(screen.getByRole('button', { name: '확인' }))

    // Then
    // 취소가 성공하면 남아 있던 저장 실패 배너가 사라진다(사용자가 취소 실패로 오인하지 않도록)
    await screen.findByText('공연취소')
    expect(screen.queryByText('저장하지 못했습니다. 다시 시도해 주세요')).not.toBeInTheDocument()
  })

  test('REGRESSION 저장 성공 후에도 좌석 구성 요약이 유지된다', async () => {
    // Given
    // 수정 화면에 기존 값이 채워져 있고 PUT 이 200과 sections 없는 응답을 반환한다
    // (PerformanceResponse — PUT 응답에는 sections가 채워지지 않는다)
    vi.mocked(getPerformance).mockResolvedValue(basePerformance())
    const { sections: _sections, ...updatedWithoutSections } = basePerformance()
    vi.mocked(updatePerformance).mockResolvedValue({
      ...updatedWithoutSections,
      title: '가을 재즈 콘서트(수정)',
    })
    renderPage('5')
    await screen.findByDisplayValue('가을 재즈 콘서트')

    // When
    // [저장] 버튼을 클릭한다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '저장' }))

    // Then
    // 저장 응답에 sections가 없어도 좌석 구성 요약은 사라지지 않는다
    await vi.waitFor(() => expect(updatePerformance).toHaveBeenCalled())
    const summary = screen.getByTestId('section-summary')
    expect(within(summary).getByText('VIP · 120,000원 · 20석')).toBeInTheDocument()
  })
})
