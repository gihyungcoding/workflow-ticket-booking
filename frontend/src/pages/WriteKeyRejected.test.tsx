import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { PerformanceEditPage } from './PerformanceEditPage'
import { PerformanceRegisterPage } from './PerformanceRegisterPage'
import { theme } from '../theme'

// TASK-015 시나리오 SC-06 ~ SC-09, SC-12. SoT: workflow_design/05_scenario/SCENARIO_TASK-015.md
// 서버 응답(fetch)만 대체한다 — 상태·본문 → ApiError 변환까지 실제 코드로 통과시킨다.

const REJECTED = '쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요'
const GENERIC_REGISTER = '등록하지 못했습니다. 다시 시도해 주세요'
const GENERIC_SAVE = '저장하지 못했습니다. 다시 시도해 주세요'
const GENERIC_CANCEL = '취소하지 못했습니다. 다시 시도해 주세요'

const performance = {
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

const rejectedBody = JSON.stringify({ code: 'WRITE_KEY_INVALID', message: 'X-Write-Key 가 없거나 올바르지 않습니다' })

// 쓰기 요청(GET 이 아닌 것)에만 write 응답을 돌려주고, GET 은 상세 조회 응답을 준다.
function stubServer(write: () => Response) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (_url: unknown, init?: RequestInit) => {
      if ((init?.method ?? 'GET').toUpperCase() === 'GET') {
        return new Response(JSON.stringify(performance), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }
      return write()
    }),
  )
}

function json(status: number, body: string): Response {
  return new Response(body, { status, headers: { 'Content-Type': 'application/json' } })
}

function renderRegister() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={['/performances/new']}>
        <Routes>
          <Route path="/performances/new" element={<PerformanceRegisterPage />} />
          <Route path="/performances/:id" element={<div>DETAIL PAGE</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

function renderEdit() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={['/performances/5/edit']}>
        <Routes>
          <Route path="/performances/:id/edit" element={<PerformanceEditPage />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

async function fillRegisterForm() {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText(/^공연명/), '가을 재즈 콘서트')
  await user.type(screen.getByLabelText(/^장소/), 'OO홀')
  fireEvent.change(screen.getByLabelText(/^공연일시/), { target: { value: '2026-10-01T19:00' } })
  fireEvent.change(screen.getByLabelText(/^오픈/), { target: { value: '2026-09-10T10:00' } })
  fireEvent.change(screen.getByLabelText(/^마감/), { target: { value: '2026-09-30T23:59' } })
  await user.type(screen.getByLabelText(/^등급/), 'VIP')
  await user.type(screen.getByLabelText(/^가격/), '120000')
  await user.type(screen.getByLabelText(/^시작 행/), 'A')
  await user.type(screen.getByLabelText(/^종료 행/), 'B')
  await user.type(screen.getByLabelText(/^행당 좌석수/), '10')
  return user
}

async function submitRegister() {
  const user = await fillRegisterForm()
  await user.click(screen.getByRole('button', { name: '등록' }))
}

async function openCancelDialogAndConfirm() {
  await screen.findByDisplayValue('가을 재즈 콘서트')
  const user = userEvent.setup()
  await user.click(screen.getByRole('button', { name: '공연 취소' }))
  await screen.findByText('이 공연을 취소하시겠습니까?')
  await user.click(screen.getByRole('button', { name: '확인' }))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('쓰기 거부(401) 안내', () => {
  test('test_sc06_등록이_쓰기_거부되면_구분된_안내가_보인다', async () => {
    // Given 등록 화면에 올바른 값이 입력돼 있고, 서버가 401 WRITE_KEY_INVALID 로 응답한다
    stubServer(() => json(401, rejectedBody))
    renderRegister()
    const user = await fillRegisterForm()

    // When 등록 버튼을 누른다
    await user.click(screen.getByRole('button', { name: '등록' }))

    // Then 구분된 안내가 표시된다
    expect(await screen.findByText(REJECTED)).toBeInTheDocument()
    // And 일반 오류 문구는 표시되지 않는다
    expect(screen.queryByText(GENERIC_REGISTER)).not.toBeInTheDocument()
    // And 다시 시도 버튼이 없다
    expect(screen.queryByRole('button', { name: '다시 시도' })).not.toBeInTheDocument()
    // And 등록 화면에 머물고 공연명 입력란의 값이 제출 전과 같다
    expect(screen.getByLabelText(/^공연명/)).toHaveValue('가을 재즈 콘서트')
    expect(screen.queryByText('DETAIL PAGE')).not.toBeInTheDocument()
  })

  test('test_sc07_수정_저장이_쓰기_거부되면_구분된_안내가_보인다', async () => {
    // Given 수정 화면이 열려 있고 서버가 PUT 에 401 WRITE_KEY_INVALID 로 응답한다
    stubServer(() => json(401, rejectedBody))
    renderEdit()
    await screen.findByDisplayValue('가을 재즈 콘서트')

    // When 저장 버튼을 누른다
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: '저장' }))

    // Then 구분된 안내가 표시된다
    expect(await screen.findByText(REJECTED)).toBeInTheDocument()
    // And 일반 저장 실패 문구는 표시되지 않는다
    expect(screen.queryByText(GENERIC_SAVE)).not.toBeInTheDocument()
  })

  test('test_sc08_취소가_쓰기_거부되면_구분된_안내가_보인다', async () => {
    // Given 취소 확인 대화상자가 열려 있고 서버가 POST 에 401 WRITE_KEY_INVALID 로 응답한다
    stubServer(() => json(401, rejectedBody))
    renderEdit()

    // When 취소를 확정한다
    await openCancelDialogAndConfirm()

    // Then 대화상자가 닫히고 구분된 안내가 표시된다
    expect(await screen.findByText(REJECTED)).toBeInTheDocument()
    await vi.waitFor(() =>
      expect(screen.queryByText('이 공연을 취소하시겠습니까?')).not.toBeInTheDocument(),
    )
    // And 일반 취소 실패 문구는 표시되지 않는다
    expect(screen.queryByText(GENERIC_CANCEL)).not.toBeInTheDocument()
  })

  test('test_sc09_401_이_아닌_일반_실패는_기존_문구를_유지한다', async () => {
    // Given 서버가 쓰기 요청에 500(코드 없는 바디)으로 응답한다
    stubServer(() => json(500, '{}'))

    // When 등록 · 저장 · 취소를 각 화면에서 시도한다
    const register = renderRegister()
    await submitRegister()
    // Then 등록 화면은 기존 등록 실패 문구를 보인다
    expect(await screen.findByText(GENERIC_REGISTER)).toBeInTheDocument()
    expect(screen.queryByText(REJECTED)).not.toBeInTheDocument()
    register.unmount()

    const save = renderEdit()
    await screen.findByDisplayValue('가을 재즈 콘서트')
    await userEvent.setup().click(screen.getByRole('button', { name: '저장' }))
    expect(await screen.findByText(GENERIC_SAVE)).toBeInTheDocument()
    expect(screen.queryByText(REJECTED)).not.toBeInTheDocument()
    save.unmount()

    renderEdit()
    await openCancelDialogAndConfirm()
    expect(await screen.findByText(GENERIC_CANCEL)).toBeInTheDocument()
    expect(screen.queryByText(REJECTED)).not.toBeInTheDocument()
  })

  test('test_sc12_401_이어도_쓰기_가드_코드가_아니면_일반_오류로_처리한다', async () => {
    // Given 등록 화면에 올바른 값이 입력돼 있고, 401 바디가 아래 중 하나다
    const bodies = [
      JSON.stringify({ code: 'UNAUTHORIZED', message: '인증이 필요합니다' }),
      '',
      'Unauthorized',
    ]
    for (const body of bodies) {
      stubServer(() => json(401, body))
      const view = renderRegister()

      // When 등록 버튼을 누른다
      await submitRegister()

      // Then 일반 오류 문구가 표시된다
      expect(await screen.findByText(GENERIC_REGISTER)).toBeInTheDocument()
      // And 쓰기 거부 안내는 표시되지 않는다
      expect(screen.queryByText(REJECTED)).not.toBeInTheDocument()
      view.unmount()
    }
  })
})
