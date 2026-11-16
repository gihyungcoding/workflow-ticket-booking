import { act, fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DETAIL, P_OPEN, renderAt, res, resetDocumentVisibility, stubServer, where } from '../test/task020'

// TASK-020 Phase 3 자체 리뷰(RC-2)로 추가 — 시트를 닫는 입력이 같은 틱에 겹쳐도 history 를 한 칸만 되돌린다.
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  resetDocumentVisibility()
  localStorage.clear()
})

describe('TASK-020 시트 닫기 중복 입력', () => {
  test('같은 틱의 닫기 입력(Esc 연타·바탕 더블 탭)은 상세를 떠나지 않는다', async () => {
    // Given 홈에서 상세로 들어왔고 시트가 열려 있다
    stubServer({
      [DETAIL]: () => res(200, P_OPEN()),
      'GET /api/performances': () => res(200, { content: [], page: 0, size: 20, totalElements: 0 }),
    })
    renderAt(['/', '/performances/7'])
    await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })
    await userEvent.click(screen.getByText('구역 위치 보기'))
    expect(screen.getByRole('dialog', { name: '구역 위치' })).toBeInTheDocument()

    // When 한 번의 갱신 안에서 닫기 입력이 두 번 들어온다
    await act(async () => {
      fireEvent.keyDown(document, { key: 'Escape' })
      fireEvent.keyDown(document, { key: 'Escape' })
    })

    // Then 시트만 닫히고 주소는 /performances/7 이다
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(where()).toBe('/performances/7')
  })
})
