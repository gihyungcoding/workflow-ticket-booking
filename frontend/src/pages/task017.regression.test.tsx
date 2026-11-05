import { render, screen } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { getPerformance, getPerformances } from '../api/performances'
import type { Performance, PerformanceListResponse, PerformanceStatus } from '../api/performances'
import App from '../App'
import { createAppTheme } from '../theme'
import { PerformanceDetailPage } from './PerformanceDetailPage'
import { PerformanceEditPage } from './PerformanceEditPage'
import { PerformanceListPage } from './PerformanceListPage'
import { PerformanceRegisterPage } from './PerformanceRegisterPage'

// SoT: workflow_design/05_scenario/SCENARIO_TASK-017.md SC-13

vi.mock('../api/performances', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/performances')>()
  return { ...actual, getPerformance: vi.fn(), getPerformances: vi.fn() }
})

const OLD_LABELS = ['예매가능', '예매예정', '예매마감', '공연취소']

function performance(id: number, status: PerformanceStatus): Performance {
  return {
    id,
    title: `공연 ${id}`,
    venue: 'OO홀',
    startAt: '2026-12-19T18:00:00+09:00',
    openAt: '2026-11-11T18:00:00+09:00',
    closeAt: '2026-12-18T23:59:59+09:00',
    totalSeats: 100,
    availableSeats: status === 'SOLD_OUT' ? 0 : 10,
    status,
    sections: [{ grade: 'VIP', price: 150000, seatCount: 100 }],
  }
}

function list(statuses: PerformanceStatus[]): PerformanceListResponse {
  return {
    content: statuses.map((s, i) => performance(i + 1, s)),
    page: 0,
    size: 20,
    totalElements: statuses.length,
  }
}

function renderAt(path: string, routePath: string, element: React.ReactElement) {
  return render(
    <ThemeProvider theme={createAppTheme()}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path={routePath} element={element} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

function expectNoOldLabels() {
  for (const old of OLD_LABELS) expect(screen.queryByText(old)).toBeNull()
}

describe('TASK-017 기존 화면 회귀', () => {
  beforeEach(() => {
    vi.mocked(getPerformance).mockReset()
    vi.mocked(getPerformances).mockReset()
  })

  test('TASK-017 SC-13 (regression) 기존 화면이 새 테마에서 깨지지 않고 새 배지 문구로 나온다', async () => {
    // 공연 목록 — 상태별 다섯 개
    vi.mocked(getPerformances).mockResolvedValue(list(['OPEN', 'UPCOMING', 'SOLD_OUT', 'CLOSED', 'CANCELLED']))
    const listView = renderAt('/', '/', <PerformanceListPage />)
    for (const label of ['예매 가능', '예정', '매진', '마감', '공연 취소']) {
      expect(await screen.findAllByText(label), label).toHaveLength(1)
    }
    expectNoOldLabels()
    listView.unmount()

    // 공연 상세 — OPEN 하나
    vi.mocked(getPerformance).mockResolvedValue(performance(1, 'OPEN'))
    const detail = renderAt('/performances/1', '/performances/:id', <PerformanceDetailPage />)
    expect(await screen.findAllByText('예매 가능')).toHaveLength(1)
    expect(screen.getByText('공연 1')).toBeInTheDocument()
    expectNoOldLabels()
    detail.unmount()

    // 공연 수정 — UPCOMING 하나
    vi.mocked(getPerformance).mockResolvedValue(performance(5, 'UPCOMING'))
    const edit = renderAt('/performances/5/edit', '/performances/:id/edit', <PerformanceEditPage />)
    expect(await screen.findAllByText('예정')).toHaveLength(1)
    expectNoOldLabels()
    edit.unmount()

    // 공연 등록 — 조회 없음, 배지 없음
    const register = renderAt('/performances/new', '/performances/new', <PerformanceRegisterPage />)
    expect(screen.getByText('공연 등록')).toBeInTheDocument()
    for (const label of ['예매 가능', '예정', '매진', '마감', '공연 취소']) expect(screen.queryByText(label)).toBeNull()
    register.unmount()

    // 앱 전체 — 첫 경로, 빈 목록
    vi.mocked(getPerformances).mockResolvedValue(list([]))
    render(
      <ThemeProvider theme={createAppTheme()}>
        <MemoryRouter initialEntries={['/']}>
          <App />
        </MemoryRouter>
      </ThemeProvider>,
    )
    expect(await screen.findByText('예정된 공연이 없습니다')).toBeInTheDocument()
  })
})
