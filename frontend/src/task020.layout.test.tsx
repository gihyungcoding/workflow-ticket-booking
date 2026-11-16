import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AppBar } from './components/ui'
import { renderWithTheme } from './test/task017'
import {
  DETAIL,
  P_OPEN,
  P_UP,
  TIME,
  USER,
  callsTo,
  hasGlobalHeader,
  renderAt,
  res,
  resetDocumentVisibility,
  stubServer,
  timeRoute,
  where,
} from './test/task020'

// TASK-020 시나리오 — 빈 h1·전역 헤더 분기·로그인 복귀. SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md

const BLANK = /^[\s ​　]*$/
const blankHeadings = () => screen.queryAllByRole('heading').filter((h) => BLANK.test(h.textContent ?? ''))
const LIST = {
  'GET /api/performances': () =>
    res(200, { content: [{ ...P_OPEN(), id: 3, title: '가을 재즈 콘서트' }], page: 0, size: 20, totalElements: 1 }),
}
const ORG = { id: 1, email: 'org@ticket.on', name: '주최자', role: 'ORGANIZER', handle: 'org01' }
const LOGIN_RESP = { accessToken: 'tok-1', tokenType: 'Bearer', expiresInSeconds: 86400, user: USER }

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  resetDocumentVisibility()
  localStorage.clear()
})

describe('TASK-020 — 앱바·전역 헤더·로그인 복귀', () => {
  test.each([
    ['a', '`AppBar` — title 없음', 'bar', undefined, 0],
    ['b', "`AppBar` — title=''", 'bar', '', 0],
    ['c', "`AppBar` — title='   '", 'bar', '   ', 0],
    ['d', '`AppBar` — 제로폭 공백', 'bar', '​', 0],
    ['e', "`AppBar` — title='예매 확인'", 'bar', '예매 확인', 1],
    ['f', '상세 /performances/7 (P_UP, 카운트다운 중)', 'detail', undefined, 1],
    ['g', '가입 /signup', 'signup', undefined, 1],
    ['h', '역할 선택 /oauth2/role', 'role', undefined, 1],
  ])('SC-27 (happy) 제목이 없는 앱바는 빈 h1 을 그리지 않는다 — %s %s', async (_row, _label, kind, title, h1Count) => {
    // Given 표의 화면 또는 AppBar 를 표의 조건으로 렌더한다 (AppBar 단독 행은 onBack 이 있다)
    // When 렌더한다
    if (kind === 'bar') {
      renderWithTheme(<AppBar onBack={() => {}} title={title as string | undefined} />)
    } else if (kind === 'detail') {
      const openAt = new Date(Date.now() + 272_000).toISOString()
      stubServer({ [DETAIL]: () => res(200, P_UP({ openAt })), [TIME]: timeRoute() })
      renderAt('/performances/7')
      await screen.findByRole('timer')
    } else if (kind === 'signup') {
      stubServer({})
      renderAt('/signup')
      await screen.findByRole('button', { name: '가입하기' })
    } else {
      stubServer({
        'POST /api/auth/social/pending': () =>
          res(200, { provider: 'KAKAO', email: 'harin.kim@kakao.com', name: '김하린', expiresAt: new Date(Date.now() + 300_000).toISOString() }),
      })
      renderAt({ pathname: '/oauth2/role', state: { pendingToken: 'pend-1' } })
      await screen.findByRole('button', { name: '계속하기' })
    }

    // Then 텍스트가 비었거나 공백만인 heading 이 하나도 없다
    expect(blankHeadings()).toHaveLength(0)
    // And 「뒤로」 버튼은 그대로 있다
    expect(screen.getByRole('button', { name: '뒤로' })).toBeInTheDocument()
    // And 표의 h1 개수다
    expect(screen.queryAllByRole('heading', { level: 1 })).toHaveLength(h1Count)
    if (title === '예매 확인') expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('예매 확인')
  })

  // ---------- SC-28 ----------

  const rows: Array<[string, string, string, boolean]> = [
    ['a', '/performances/7', 'detail', false],
    ['b', '/performances/7/', 'detail', false],
    ['c', '/Performances/7', 'detail', false],
    ['d', '/performances/%37', 'detail', false],
    ['e', '/performances/7/queue', 'queue', false],
    ['f', '/performances/7/queue/', 'queue', false],
    ['g', '/PERFORMANCES/7/QUEUE', 'queue', false],
    ['h', '/performances/%E0', 'notfound', false],
    ['i', '/performances/new', 'form', true],
    ['j', '/performances/new/', 'form', true],
    ['k', '/Performances/NEW', 'form', true],
    ['l', '/performances/7/edit', 'form', true],
    ['m', '/performances/7/edit/', 'form', true],
    ['n', '/', 'home', true],
  ]
  test.each(rows)('SC-28 (regression) 전역 헤더는 상세·대기열에서만 사라지고 홈·등록·수정에는 남는다 — %s %s', async (_row, address, screenKind, header) => {
    // Given 주최자(ORG)로 로그인해 둔다. /api/auth/me 에 ORG, GET /api/performances/7 에 P_OPEN
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    localStorage.setItem('accessToken', 'tok-org')
    stubServer({ [DETAIL]: () => res(200, P_OPEN()), 'GET /api/auth/me': () => res(200, ORG), ...LIST })

    // When 표의 주소를 연다
    renderAt(address)

    // Then 표의 화면이 그려진다
    if (screenKind === 'detail') expect(await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).toBeInTheDocument()
    else if (screenKind === 'queue') expect(await screen.findByRole('heading', { level: 1, name: '대기열을 준비하고 있어요' })).toBeInTheDocument()
    else if (screenKind === 'notfound') expect(await screen.findByText('찾는 공연이 없어요')).toBeInTheDocument()
    else if (screenKind === 'form') expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
    else expect(await screen.findByText('가을 재즈 콘서트')).toBeInTheDocument()
    // And 전역 헤더가 표대로 있거나 없다 — 있으면 이름 「주최자」와 「로그아웃」 버튼이 보이고 없으면 둘 다 없다
    if (header) {
      expect(await screen.findByRole('button', { name: '로그아웃' })).toBeInTheDocument()
      expect(screen.getByText('주최자')).toBeInTheDocument()
    } else {
      await waitFor(() => expect(callsTo('GET /api/auth/me').length).toBeGreaterThan(0))
      expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
      expect(screen.queryByText('주최자')).not.toBeInTheDocument()
    }
  })

  // ---------- SC-29 ----------

  const setField = (label: RegExp, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } })
  test.each([
    ['a', '비로그인, / 에서 헤더의 「로그인」 링크로 로그인에 성공한다'],
    ['b', '비로그인, / 에서 헤더의 「가입」 링크를 누른다'],
    ['c', '로그인 상태(홍길동), / 를 연다'],
    ['d', '로그인 상태, /performances/7 을 연다'],
    ['e', '비로그인, /performances/7 을 연다'],
  ])('SC-29 (regression) 로그인·가입 진입과 로그인 상태 헤더는 홈에서 그대로 동작한다 — %s %s', async (row, _label) => {
    // Given 표의 시작 상태다
    const loggedIn = row === 'c' || row === 'd'
    if (loggedIn) localStorage.setItem('accessToken', 'tok-1')
    stubServer({
      [DETAIL]: () => res(200, P_OPEN()),
      'GET /api/auth/me': () => res(200, USER),
      'POST /api/auth/login': () => res(200, LOGIN_RESP),
      ...LIST,
    })
    renderAt(row === 'd' || row === 'e' ? '/performances/7' : '/')

    // When 표의 동작을 한다
    if (row === 'a') {
      await userEvent.click(await screen.findByRole('link', { name: '로그인' }))
      await waitFor(() => expect(where()).toBe('/login'))
      setField(/^이메일/, 'hong@example.com')
      setField(/^비밀번호(?! *확인)/, 'password1!')
      await userEvent.click(screen.getByRole('button', { name: '로그인하기' }))
    } else if (row === 'b') {
      await userEvent.click(await screen.findByRole('link', { name: '가입' }))
    }

    // Then 표의 Then 이다
    if (row === 'a') {
      await waitFor(() => expect(localStorage.getItem('accessToken')).toBe('tok-1'))
      await waitFor(() => expect(where()).toBe('/'))
    } else if (row === 'b') {
      await waitFor(() => expect(where()).toBe('/signup'))
      expect(screen.getByRole('button', { name: '가입하기' })).toBeInTheDocument()
    } else if (row === 'c') {
      expect(await screen.findByText('홍길동')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: '로그인' })).not.toBeInTheDocument()
      expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
    } else if (row === 'd') {
      await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })
      await waitFor(() => expect(callsTo('GET /api/auth/me').length).toBeGreaterThan(0))
      expect(screen.queryByText('홍길동')).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
    } else {
      await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })
      expect(screen.queryByRole('link', { name: '로그인' })).not.toBeInTheDocument()
      expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
      expect(hasGlobalHeader()).toBe(false)
    }
  })
})
