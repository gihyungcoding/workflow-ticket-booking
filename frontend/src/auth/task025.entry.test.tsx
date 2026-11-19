import { screen, waitFor } from '@testing-library/react'
import { hasGlobalHeader } from '../test/task020'
import {
  AUD,
  BROWSE,
  LOGIN,
  LOGIN_HERO,
  ME,
  O,
  ONLY_SUB,
  ONLY_TITLE,
  ORG,
  authApi,
  backButton,
  chipGroup,
  click,
  deferred,
  fab,
  fromState,
  heroTitles,
  listUrls,
  loginBody,
  loginWith,
  logoutNow,
  navigateTo,
  noticeShown,
  orgBody,
  pause,
  raw,
  renderApp,
  res,
  rowLinks,
  serve,
  token,
  tokenOf,
  where,
} from '../test/task025'
import type { User } from '../test/task025'

// TASK-025 시나리오 SC-01~SC-05, SC-20~SC-22 — 진입 경로·헤더·가드·복원 중 로그아웃.
// SoT: workflow_design/05_scenario/SCENARIO_TASK-025.md
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.

const browseControl = () => screen.queryByRole('link', { name: BROWSE }) ?? screen.getByRole('button', { name: BROWSE })
const myLink = () => screen.queryByRole('link', { name: '내 공연' })

describe('TASK-025 F1 — 가입 완료 「시작하기」', () => {
  const SC01: { row: string; user: User; to: string }[] = [
    { row: 'a 주최자', user: ORG, to: '/organizer' },
    { row: 'b 관객', user: AUD, to: '/' },
  ]
  test.each(SC01)('SC-01 (happy) 가입 완료 화면의 「시작하기」는 주최자를 /organizer 로, 관객을 / 로 보낸다 — $row', async ({ user, to }) => {
    // Given 표의 사용자가 로그인한 채 /joined 에 도착했다
    serve({ user })
    renderApp({ pathname: '/joined', state: { via: 'email' } })
    const start = await screen.findByRole('button', { name: '시작하기' })

    // When 「시작하기」를 누른다
    await click(start)

    // Then 주소가 표의 목적지다
    await waitFor(() => expect(where()).toBe(to))
    // And 저장된 토큰은 그대로다
    expect(token()).toBe(tokenOf(user))
  })
})

describe('TASK-025 F2 — 이메일 로그인 직후 목적지', () => {
  const SC02: { row: string; user: User; from: unknown; to: string }[] = [
    { row: 'a', user: ORG, from: undefined, to: '/organizer' },
    { row: 'b', user: ORG, from: '/performances/5/edit', to: '/performances/5/edit' },
    { row: 'c', user: ORG, from: '/organizer', to: '/organizer' },
    { row: 'd', user: AUD, from: undefined, to: '/' },
    { row: 'e', user: AUD, from: '/performances/5', to: '/performances/5' },
    { row: 'f 로그인 순환', user: ORG, from: '/login/', to: '/organizer' },
    { row: 'g 가입 순환', user: ORG, from: '/Signup?x=1', to: '/organizer' },
    { row: 'h 관객 로그인 순환', user: AUD, from: '/login', to: '/' },
    { row: 'i 프로토콜 상대', user: ORG, from: '//evil.example', to: '/organizer' },
    { row: 'j 외부 주소', user: ORG, from: 'https://evil.example/x', to: '/organizer' },
    { row: 'k 역슬래시', user: ORG, from: '/a\\b', to: '/organizer' },
    { row: 'l 앞 공백', user: ORG, from: ' /performances/5', to: '/organizer' },
    { row: 'm 앞 LF', user: ORG, from: '\n/performances/5', to: '/organizer' },
    { row: 'n 앞 NUL', user: ORG, from: '\u0000/performances/5', to: '/organizer' },
    { row: 'o 앞 전각 공백', user: ORG, from: '　/performances/5', to: '/organizer' },
    { row: 'p 숫자', user: ORG, from: 5, to: '/organizer' },
    { row: 'q 관객 앞 공백', user: AUD, from: ' /performances/5', to: '/' },
  ]
  test.each(SC02)('SC-02 (happy) 이메일 로그인 직후 목적지는 from 이 안전하면 from, 아니면 역할별 기본 경로다 — $row', async ({ user, from, to }) => {
    // Given 비로그인이고 /login 이 표의 from 상태로 열려 있다, 표의 사용자로 올바른 로그인 응답이 온다
    serve({ user: null, routes: { [LOGIN]: () => res(200, loginBody(tokenOf(user), user)) } })
    renderApp({ pathname: '/login', state: from === undefined ? undefined : { from } })

    // When 로그인을 제출한다
    await loginWith(String(user.email))

    // Then 주소가 표의 목적지다(로그인 화면으로는 돌아가지 않는다)
    await waitFor(() => expect(where()).toBe(to))
    // And 저장된 토큰은 그 사용자의 것이다
    expect(token()).toBe(tokenOf(user))
  })
})

describe('TASK-025 F3 — 소셜 로그인 콜백', () => {
  const SC03: { row: string; kind: 'user' | '401' | 'leave'; user?: User; to?: string }[] = [
    { row: 'a 주최자', kind: 'user', user: ORG, to: '/organizer' },
    { row: 'b 관객', kind: 'user', user: AUD, to: '/' },
    { row: 'c 401', kind: '401' },
    { row: 'd 떠난 뒤 도착', kind: 'leave', user: ORG },
  ]
  test.each(SC03)('SC-03 (happy) 소셜 로그인 콜백이 끝나면 주최자는 /organizer 로, 관객은 / 로 간다 — $row', async ({ kind, user, to }) => {
    // Given 비로그인이고 소셜 콜백 주소가 열렸다
    const held = deferred()
    const lookup =
      kind === 'user' ? () => res(200, user) : kind === '401' ? () => res(401, { code: 'UNAUTHORIZED' }) : () => held.promise
    serve({ user: null, routes: { [ME]: lookup } })
    renderApp('/oauth2/redirect?token=tok-soc')

    if (kind === 'leave') {
      // When 응답 전에 /signup 으로 이동한 뒤 응답이 도착한다
      await screen.findByRole('progressbar')
      await navigateTo('/signup')
      held.resolve(res(200, user))
      await pause()
      // Then 이동하지 않고 /signup 에 머문다
      expect(where()).toBe('/signup')
      return
    }

    // When 사용자 조회 응답이 도착한다 → Then
    if (kind === '401') {
      await waitFor(() => expect(where()).toBe('/login?error=social'))
      expect(token()).toBeNull()
      return
    }
    await waitFor(() => expect(where()).toBe(to))
    expect(token()).toBe('tok-soc')
  })
})

describe('TASK-025 F4 — 전역 헤더 「내 공연」', () => {
  const SC04: { row: string; kind: string }[] = [
    { row: 'a ORG 홈', kind: 'org-home' },
    { row: 'b AUD 홈', kind: 'aud-home' },
    { row: 'c 비로그인 홈', kind: 'anon-home' },
    { row: 'd 복원 중', kind: 'restoring' },
    { row: 'e ORG 등록 화면', kind: 'org-new' },
    { row: 'f 로그아웃 뒤', kind: 'after-logout' },
    { row: 'g 링크를 누른다', kind: 'click' },
  ]
  test.each(SC04)('SC-04 (happy) 주최자로 로그인했을 때만 전역 헤더에 「내 공연」 링크가 있다 — $row', async ({ kind }) => {
    // Given 화면이 표의 상태로 열려 있다
    const held = deferred()
    if (kind === 'aud-home') serve({ user: AUD })
    else if (kind === 'anon-home') serve({ user: null })
    else if (kind === 'restoring') serve({ user: ORG, routes: { [ME]: () => held.promise } })
    else serve({ user: ORG })
    renderApp(kind === 'org-new' ? '/performances/new' : '/')

    // When 화면이 그려지기를 기다린다
    if (kind === 'anon-home') await screen.findByRole('link', { name: '로그인' })
    else if (kind === 'restoring') await pause(60)
    else if (kind === 'org-new') await screen.findByLabelText(/^공연명/)
    else await screen.findByRole('button', { name: '로그아웃' })

    // Then 「내 공연」 링크의 유무가 표와 같다
    if (kind === 'org-home' || kind === 'org-new' || kind === 'click') {
      expect(myLink()).not.toBeNull()
      // And 링크가 있으면 href 가 /organizer 다
      expect(myLink()!.getAttribute('href')).toBe('/organizer')
    } else if (kind === 'restoring') {
      // 복원 중 헤더는 비어 있다
      expect(myLink()).toBeNull()
      expect(hasGlobalHeader()).toBe(false)
      held.resolve(res(200, ORG))
    } else if (kind === 'after-logout') {
      expect(myLink()).not.toBeNull()
      await click(screen.getByRole('button', { name: '로그아웃' }))
      await screen.findByRole('link', { name: '로그인' })
      expect(myLink()).toBeNull()
    } else {
      expect(myLink()).toBeNull()
    }

    // 행 g — 링크를 누르면 /organizer 다
    if (kind === 'click') {
      await click(myLink()!)
      await waitFor(() => expect(where()).toBe('/organizer'))
    }
  })
})

describe('TASK-025 F11 — 한 장 전체 화면', () => {
  const SC05: { row: string; path: string; mine: boolean }[] = [
    { row: 'a', path: '/organizer', mine: true },
    { row: 'b 끝 슬래시', path: '/organizer/', mine: true },
    { row: 'c 대소문자', path: '/Organizer', mine: true },
    { row: 'd 대문자·슬래시', path: '/ORGANIZER/', mine: true },
    { row: 'e 퍼센트 인코딩', path: '/%6Frganizer', mine: true },
    { row: 'f 쿼리', path: '/organizer?x=1', mine: true },
    { row: 'g 해시', path: '/organizer#top', mine: true },
    { row: 'h 다른 주소', path: '/organizers', mine: false },
    { row: 'i 상세 라우트(TASK-026)', path: '/organizer/performances/12', mine: false },
  ]
  test.each(SC05)('SC-05 (happy) /organizer 는 전역 헤더 없이 자체 앱바 「내 공연」만 그린다 — $row $path', async ({ path, mine }) => {
    // Given ORG 로그인 상태이고 LIST 가 BODY([O]) 를 준다
    serve({ list: { '': orgBody([O]) } })

    // When 표의 주소를 연다
    renderApp(path)

    if (mine) {
      // Then 제목 「내 공연」이 보이고 뒤로 버튼은 없다
      await waitFor(() => expect(listUrls()).toHaveLength(1))
      expect(heroTitles()).toContain('내 공연')
      expect(backButton()).toBeNull()
      // And 전역 헤더 요소가 없다
      expect(screen.queryByRole('link', { name: '로그인' })).toBeNull()
      expect(screen.queryByRole('link', { name: '가입' })).toBeNull()
      expect(screen.queryByRole('button', { name: '로그아웃' })).toBeNull()
      expect(myLink()).toBeNull()
    } else {
      // Then 이 화면이 아니다 — 전역 헤더가 있고 LIST 는 나가지 않는다
      await screen.findByRole('button', { name: '로그아웃' })
      await pause()
      expect(listUrls()).toHaveLength(0)
      // And 행·칩이 없다(행 i — 상세 라우트는 이 태스크가 만들지 않는다)
      expect(rowLinks()).toHaveLength(0)
      expect(chipGroup()).toBeNull()
    }
  })
})

describe('TASK-025 F11 — 가드 진입', () => {
  const resolveWith = {
    org200: () => res(200, ORG),
    aud200: () => res(200, AUD),
    r401: () => res(401, { code: 'UNAUTHORIZED' }),
    r500: () => res(500, { code: 'INTERNAL' }),
  }
  const SC20: { row: string; kind: 'anon' | 'anon-query' | 'aud' | 'restore'; late?: keyof typeof resolveWith }[] = [
    { row: 'a 비로그인', kind: 'anon' },
    { row: 'b 비로그인 쿼리·해시', kind: 'anon-query' },
    { row: 'c 관객', kind: 'aud' },
    { row: 'd 복원 중 → ORG', kind: 'restore', late: 'org200' },
    { row: 'e 복원 중 → AUD', kind: 'restore', late: 'aud200' },
    { row: 'f 복원 중 → 401', kind: 'restore', late: 'r401' },
    { row: 'g 복원 중 → 500', kind: 'restore', late: 'r500' },
  ]
  test.each(SC20)('SC-20 (error) 비로그인은 로그인으로, 관객은 안내로 보내고, 복원 중에는 아무것도 그리지 않는다 — $row', async ({ kind, late }) => {
    // Given 표의 로그인 상태 (복원 중 행은 GET /api/auth/me 응답을 붙들어 둔다)
    const held = deferred()
    const entry = kind === 'anon-query' ? '/organizer?x=1#top' : '/organizer'
    if (kind === 'anon' || kind === 'anon-query') serve({ user: null })
    else if (kind === 'aud') serve({ user: AUD })
    else serve({ user: late === 'aud200' ? AUD : ORG, routes: { [ME]: () => held.promise }, list: { '': orgBody([O]) } })

    // When 주소를 연다
    renderApp(entry)

    if (kind === 'anon' || kind === 'anon-query') {
      // Then /login 이고 from 이 연 주소다
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe(entry)
      expect(listUrls()).toHaveLength(0)
      return
    }
    if (kind === 'aud') {
      // Then 주최자 전용 안내(제목·부제·「공연 둘러보기」), 목록·칩·「공연 등록」·요청 없음
      expect(await screen.findByText(ONLY_TITLE)).toBeInTheDocument()
      expect(screen.getByText(ONLY_SUB)).toBeInTheDocument()
      expect(browseControl()).toBeInTheDocument()
      expect(chipGroup()).toBeNull()
      expect(fab()).toBeNull()
      expect(rowLinks()).toHaveLength(0)
      await pause()
      expect(listUrls()).toHaveLength(0)
      return
    }

    // Then (연 직후) 아무것도 그리지 않는다 — 앱바·본문 없고 주소 그대로, LIST 요청 0건
    await pause(60)
    expect(heroTitles()).toHaveLength(0)
    expect(chipGroup()).toBeNull()
    expect(fab()).toBeNull()
    expect(noticeShown()).toBe(false)
    expect(where()).toBe('/organizer')
    expect(listUrls()).toHaveLength(0)

    // When 복원 응답이 도착한다
    if (late === 'org200') held.resolve(resolveWith.org200())
    else if (late === 'aud200') held.resolve(resolveWith.aud200())
    else if (late === 'r401') held.resolve(resolveWith.r401())
    else held.resolve(resolveWith.r500())

    // Then 표의 결과
    if (late === 'org200') {
      await waitFor(() => expect(listUrls()).toHaveLength(1))
      await waitFor(() => expect(rowLinks()).toHaveLength(1))
    } else if (late === 'aud200') {
      expect(await screen.findByText(ONLY_TITLE)).toBeInTheDocument()
      expect(listUrls()).toHaveLength(0)
    } else {
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe('/organizer')
      expect(listUrls()).toHaveLength(0)
      // 401 은 저장된 토큰을 지우고, 500 은 지우지 않는다(ADR-0018 규칙 3)
      if (late === 'r401') expect(token()).toBeNull()
      else expect(token()).toBe('tok-org')
    }
  })
})

describe('TASK-025 F11 — 주최자 전용 안내의 앱바', () => {
  const SC21: { row: string; path: string; act: 'back' | 'browse' | 'none'; appbar: boolean }[] = [
    { row: 'a 뒤로', path: '/organizer', act: 'back', appbar: true },
    { row: 'b 공연 둘러보기', path: '/organizer', act: 'browse', appbar: true },
    { row: 'c 등록 화면', path: '/performances/new', act: 'none', appbar: false },
    { row: 'd 수정 화면', path: '/performances/5/edit', act: 'none', appbar: false },
  ]
  test.each(SC21)('SC-21 (happy) 관객에게 /organizer 의 주최자 전용 안내는 시안의 앱바를 갖고, 등록·수정의 안내에는 앱바를 더하지 않는다 — $row', async ({ path, act, appbar }) => {
    // Given AUD 로그인 상태
    serve({ user: AUD })

    // When 표의 주소를 연다
    renderApp(path)
    expect(await screen.findByText(ONLY_TITLE)).toBeInTheDocument()

    // Then 안내 본문은 같다
    expect(screen.getByText(ONLY_SUB)).toBeInTheDocument()
    expect(browseControl()).toBeInTheDocument()
    if (appbar) {
      // 제목 「내 공연」(제목 요소 하나)·뒤로 버튼이 있고 전역 헤더 요소는 없다
      expect(heroTitles()).toEqual(['내 공연'])
      expect(backButton()).not.toBeNull()
      expect(hasGlobalHeader()).toBe(false)
    } else {
      // 전역 헤더가 있고 앱바 제목 「내 공연」과 「뒤로」는 없다
      expect(hasGlobalHeader()).toBe(true)
      expect(heroTitles()).not.toContain('내 공연')
      expect(backButton()).toBeNull()
    }

    // When 누르는 것
    if (act === 'back') await click(backButton()!)
    if (act === 'browse') await click(browseControl())

    // Then 주소가 /
    if (act !== 'none') await waitFor(() => expect(where()).toBe('/'))
  })
})

describe('TASK-025 F12 — 복원 중 로그아웃', () => {
  type Late = 'ok' | '401' | '500' | 'network' | 'non-json'
  const lateOf = (late: Late, held: { resolve: (r: Response) => void; reject: (e: unknown) => void }) => {
    if (late === 'ok') held.resolve(res(200, ORG))
    else if (late === '401') held.resolve(res(401, { code: 'UNAUTHORIZED' }))
    else if (late === '500') held.resolve(res(500, { code: 'INTERNAL' }))
    else if (late === 'network') held.reject(new TypeError('Failed to fetch'))
    else held.resolve(raw(200, '<html>ok</html>'))
  }
  const SC22: { row: string; kind: 'logout' | 'leave' | 'after-restore'; path: string; late: Late }[] = [
    { row: 'a /organizer · 200', kind: 'logout', path: '/organizer', late: 'ok' },
    { row: 'b /organizer · 401', kind: 'logout', path: '/organizer', late: '401' },
    { row: 'c /organizer · 500', kind: 'logout', path: '/organizer', late: '500' },
    { row: 'd /organizer · 네트워크', kind: 'logout', path: '/organizer', late: 'network' },
    { row: 'e /organizer · 비 JSON', kind: 'logout', path: '/organizer', late: 'non-json' },
    { row: 'f / · 200', kind: 'logout', path: '/', late: 'ok' },
    { row: 'g / · 401', kind: 'logout', path: '/', late: '401' },
    { row: 'h /performances/new · 200', kind: 'logout', path: '/performances/new', late: 'ok' },
    { row: 'i 화면을 떠난 뒤', kind: 'leave', path: '/', late: 'ok' },
    { row: 'j 복원이 끝난 뒤 로그아웃', kind: 'after-restore', path: '/', late: 'ok' },
  ]
  test.each(SC22)('SC-22 (error) 복원 중에 로그아웃하면 복원이 끝난 것으로 보고, 늦은 복원 응답은 상태를 바꾸지 못한다 — $row', async ({ kind, path, late }) => {
    if (kind === 'after-restore') {
      // Given ORG 로그인 상태(복원 끝) → When 로그아웃한다
      serve({ user: ORG })
      renderApp('/')
      await click(await screen.findByRole('button', { name: '로그아웃' }))
      // Then 헤더가 비로그인 모양이다
      expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument()
      expect(myLink()).toBeNull()
      // And /organizer 는 /login 으로 보낸다
      await navigateTo('/organizer')
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe('/organizer')
      return
    }

    // Given ORG 토큰이 저장돼 있고 GET /api/auth/me 응답을 붙들어 둔다, 표의 화면이 복원을 기다리는 중이다
    const held = deferred()
    serve({ user: ORG, routes: { [ME]: () => held.promise } })
    renderApp(path)
    await pause(60)

    if (kind === 'leave') {
      // When 복원 중 /signup 으로 이동했고(로그아웃은 하지 않았다) 늦은 복원 응답이 도착한다
      await navigateTo('/signup')
      lateOf(late, held)
      await pause(60)
      // Then 이동한 화면이 바뀌지 않는다
      expect(where()).toBe('/signup')
      return
    }

    // When 앱 안에서 로그아웃을 한 번 호출한다
    await logoutNow()

    // Then (로그아웃 직후) 표의 화면 결과
    if (path === '/') {
      // 전역 헤더에 「로그인」「가입」 링크가 보인다
      expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    } else {
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe(path)
      expect(screen.getByRole('heading', { name: LOGIN_HERO })).toBeInTheDocument()
    }
    expect(token()).toBeNull()
    expect(listUrls()).toHaveLength(0)

    // When 붙들어 둔 복원 응답이 표의 것으로 도착한다
    lateOf(late, held)
    await pause(60)

    // Then 사용자·주소·저장된 토큰이 바뀌지 않는다
    expect(authApi().user).toBeNull()
    expect(token()).toBeNull()
    expect(listUrls()).toHaveLength(0)
    if (path === '/') expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    else expect(where()).toBe('/login')
  })
})
