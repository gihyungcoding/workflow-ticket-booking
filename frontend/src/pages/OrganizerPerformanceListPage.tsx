import Box from '@mui/material/Box'
import Skeleton from '@mui/material/Skeleton'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import clipboardArt from '../assets/fluent/clipboard.svg'
import { getMyPerformances } from '../api/organizer'
import type { OrganizerPerformance, PerformanceFilter } from '../api/organizer'
import { ForbiddenRoleError, UnauthorizedError } from '../api/performances'
import { useAuth } from '../auth/AuthContext'
import { OrganizerOnlyNotice } from '../auth/RequireOrganizer'
import { FilterChips } from '../components/StatusChips'
import type { ChipOption } from '../components/StatusChips'
import { OrganizerPerformanceRow } from '../components/OrganizerPerformanceRow'
import { AppBar, Block, Fab, Hero, Page } from '../components/ui'
import { isBlankDisplay } from '../components/ui/text'
import { componentSizes, token } from '../theme/tokens'

// 내 공연 — 시안 organizer.html·organizer--empty.html. 앱바 · 머리글 · 상태 칩 · 행 목록 · 더 보기(20개씩) · 떠 있는 「공연 등록」.
// 규칙은 홈(PerformanceListPage)과 같다 — 칩을 바꾸거나 화면을 떠나면 오래된 응답은 버리고(ADR-0018), 쪽 번호는 성공한 횟수로 센다.

type ListState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'forbidden' }
  | {
      status: 'success'
      items: OrganizerPerformance[]
      total: number
      /** 성공한 쪽 수(첫 쪽 포함) — 다음 요청은 이 값이 쪽 번호다(불러온 개수로 세지 않는다. 중복 id 가 있어도 쪽이 어긋나지 않게) */
      loaded: number
      /** 더 보기가 빈 쪽을 받았다 — 총수와 어긋나도 더 부르지 않는다 */
      exhausted: boolean
      more: 'idle' | 'loading' | 'error'
    }

const PAGE_SIZE = 20 // 서버 기본 쪽 크기 — size 는 보내지 않는다
const SKELETON_COUNT = 3

const CHIPS: ReadonlyArray<ChipOption<PerformanceFilter>> = [
  { label: '전체', value: 'ALL' },
  { label: '비공개', value: 'DRAFT' },
  { label: '예정', value: 'UPCOMING' },
  { label: '예매 가능', value: 'OPEN' },
  { label: '매진', value: 'SOLD_OUT' },
  { label: '마감', value: 'CLOSED' },
  { label: '취소', value: 'CANCELLED' },
]

/** 먼저 보인 행을 지키며 새 쪽을 붙인다 — 같은 공연은 한 번만 */
function merge(current: OrganizerPerformance[], incoming: OrganizerPerformance[]): OrganizerPerformance[] {
  const seen = new Set(current.map((p) => p.id))
  const added: OrganizerPerformance[] = []
  for (const p of incoming) {
    if (seen.has(p.id)) continue
    seen.add(p.id)
    added.push(p)
  }
  return [...current, ...added]
}

/** 머리글 제목 — 0건이면 등록을 권하고, 불러온 행 중 비공개(취소 아님)가 있으면 건수를 알리고, 그 외는 화면 이름 */
function heroTitle(list: ListState, filter: PerformanceFilter): string {
  if (list.status !== 'success') return '내 공연'
  if (list.items.length === 0 && filter === 'ALL') return '첫 공연을 등록해 보세요'
  const drafts = list.items.filter((p) => !p.published && p.status !== 'CANCELLED').length
  return drafts > 0 ? `비공개 공연이 ${drafts}건 있어요` : '내 공연'
}

function ActionButton({ children, onClick, disabled }: { children: ReactNode; onClick: () => void; disabled?: boolean }) {
  return (
    <Box
      component="button"
      type="button"
      onClick={onClick}
      disabled={disabled}
      sx={{
        minHeight: token('--h-btn-s'),
        padding: `0 ${token('--stack-m')}`,
        border: 0,
        borderRadius: token('--r-m'),
        cursor: disabled ? 'default' : 'pointer',
        fontFamily: 'inherit',
        fontSize: token('--fs-sub'),
        fontWeight: Number(token('--fw-strong')),
        backgroundColor: token('--c-bg'),
        color: token('--c-text-1'),
        '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
      }}
    >
      {children}
    </Box>
  )
}

function Notice({ text, action, onAction }: { text: string; action?: string; onAction?: () => void }) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: token('--stack-m'), padding: `${token('--stack-l')} 0` }}>
      <Box component="p" sx={{ margin: 0, textAlign: 'center', fontSize: token('--fs-body'), lineHeight: Number(token('--lh-body')), color: token('--c-text-2') }}>
        {text}
      </Box>
      {action && onAction && <ActionButton onClick={onAction}>{action}</ActionButton>}
    </Box>
  )
}

/** 시안 organizer--empty 의 .empty — 그림 · 제목 · 부제 · 주 버튼 하나 */
function FirstPerformance() {
  return (
    <Box sx={{ padding: `${token('--stack-l')} ${token('--block-pad')}`, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: token('--stack-s'), textAlign: 'center' }}>
      <Box component="img" src={clipboardArt} alt="" />
      <Box component="p" sx={{ margin: 0, fontSize: token('--fs-strong'), fontWeight: Number(token('--fw-strong')), lineHeight: Number(token('--lh-strong')), textWrap: 'balance' }}>
        아직 등록한 공연이 없어요
      </Box>
      <Box component="p" sx={{ margin: 0, maxWidth: componentSizes.emptySubMax, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2'), textWrap: 'pretty' }}>
        등록하면 비공개로 저장돼요. 준비가 끝나면 공개하세요
      </Box>
      <Box
        component={RouterLink}
        to="/performances/new"
        sx={{
          alignSelf: 'stretch',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: token('--h-btn'),
          padding: `0 ${token('--block-pad')}`,
          borderRadius: token('--r-m'),
          fontSize: token('--fs-strong'),
          fontWeight: Number(token('--fw-strong')),
          textDecoration: 'none',
          backgroundColor: token('--c-brand'),
          color: token('--c-on-brand'),
          '&:active': { backgroundColor: token('--c-brand-pressed') },
          '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
        }}
      >
        첫 공연 등록하기
      </Box>
    </Box>
  )
}

export function OrganizerPerformanceListPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [filter, setFilter] = useState<PerformanceFilter>('ALL')
  const [list, setList] = useState<ListState>({ status: 'loading' })
  // 칩을 바꾸거나 화면을 떠나면 오래된 응답은 버린다(ADR-0018). 더 보기 중복 요청은 렌더를 기다리지 않는 ref 로 막는다
  const generation = useRef(0)
  const moreInFlight = useRef(false)
  // 응답 처리 함수가 렌더마다 바뀌어도 불러오기 효과가 다시 돌지 않게 최신 값을 ref 로 들고 있는다
  const latest = useRef({ logout, navigate, from: location.pathname + location.search + location.hash })
  useEffect(() => {
    latest.current = { logout, navigate, from: location.pathname + location.search + location.hash }
  })

  /** 401 은 로그아웃하고 로그인으로(돌아올 곳은 이 화면), 403 은 주최자 전용 안내, 그 밖은 호출한 쪽이 정한다 */
  const onFailure = useCallback((error: unknown, other: () => void) => {
    if (error instanceof UnauthorizedError) {
      latest.current.logout()
      latest.current.navigate('/login', { replace: true, state: { from: latest.current.from } })
    } else if (error instanceof ForbiddenRoleError) {
      setList({ status: 'forbidden' })
    } else {
      other()
    }
  }, [])

  // 첫 쪽을 부른다 — 화면 상태는 건드리지 않는다(진입 때는 이미 loading 이다)
  const fetchFirst = useCallback(
    (next: PerformanceFilter) => {
      generation.current += 1
      const mine = generation.current
      moreInFlight.current = false
      getMyPerformances(next)
        .then((response) => {
          if (mine !== generation.current) return
          setList({ status: 'success', items: merge([], response.items), total: response.totalElements, loaded: 1, exhausted: false, more: 'idle' })
        })
        .catch((error: unknown) => {
          if (mine !== generation.current) return
          onFailure(error, () => setList({ status: 'error' }))
        })
    },
    [onFailure],
  )

  const loadFirst = (next: PerformanceFilter) => {
    setList({ status: 'loading' })
    fetchFirst(next)
  }

  useEffect(() => {
    fetchFirst('ALL')
    return () => {
      generation.current += 1
    }
  }, [fetchFirst])

  const selectChip = (next: PerformanceFilter) => {
    if (next === filter) return
    setFilter(next)
    loadFirst(next)
  }

  const loadMore = () => {
    if (list.status !== 'success' || moreInFlight.current) return
    moreInFlight.current = true
    const mine = generation.current
    const page = list.loaded
    setList({ ...list, more: 'loading' })
    getMyPerformances(filter, page)
      .then((response) => {
        if (mine !== generation.current) return
        moreInFlight.current = false
        setList((current) =>
          current.status !== 'success'
            ? current
            : { ...current, items: merge(current.items, response.items), loaded: current.loaded + 1, exhausted: response.items.length === 0, more: 'idle' },
        )
      })
      .catch((error: unknown) => {
        if (mine !== generation.current) return
        moreInFlight.current = false
        onFailure(error, () => setList((current) => (current.status !== 'success' ? current : { ...current, more: 'error' })))
      })
  }

  if (list.status === 'forbidden') return <OrganizerOnlyNotice appBar />

  const firstTime = list.status === 'success' && list.items.length === 0 && filter === 'ALL'
  const eyebrow = user && !isBlankDisplay(user.name) ? `${user.name} 님` : undefined

  let body: ReactNode
  if (list.status === 'loading') {
    body = (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
        {Array.from({ length: SKELETON_COUNT }).map((_, index) => (
          <Skeleton key={index} data-testid="organizer-row-skeleton" variant="rectangular" sx={{ width: '100%', height: token('--h-row'), borderRadius: token('--r-m') }} />
        ))}
      </Box>
    )
  } else if (list.status === 'error') {
    body = <Notice text="공연 목록을 불러오지 못했어요" action="다시 시도" onAction={() => loadFirst(filter)} />
  } else if (list.status === 'success' && list.items.length === 0 && !firstTime) {
    const label = CHIPS.find((chip) => chip.value === filter)?.label ?? ''
    body = <Notice text={`${label} 공연이 없어요`} action="전체 보기" onAction={() => selectChip('ALL')} />
  } else if (list.status === 'success') {
    // 불러온 개수가 총수에 닿았거나(홈과 같은 규칙) 받은 쪽 수가 총수를 덮으면(중복 id 로 개수가 모자라도) 끝이다
    const hasMore = !list.exhausted && list.items.length < list.total && list.loaded * PAGE_SIZE < list.total
    body = (
      <>
        {list.items.map((performance) => (
          <OrganizerPerformanceRow key={performance.id} performance={performance} />
        ))}
        {hasMore &&
          (list.more === 'error' ? (
            <ActionButton onClick={loadMore}>불러오지 못했어요 · 다시 시도</ActionButton>
          ) : (
            <ActionButton onClick={loadMore} disabled={list.more === 'loading'}>
              더 보기
            </ActionButton>
          ))}
      </>
    )
  }

  return (
    <>
      <AppBar title="내 공연" />
      <Page hasCta={!firstTime}>
        <Box sx={{ padding: `0 ${token('--page-x')}` }}>
          <Hero eyebrow={eyebrow} title={heroTitle(list, filter)} />
        </Box>
        {firstTime ? (
          <Block>
            <FirstPerformance />
          </Block>
        ) : (
          <Block list>
            <FilterChips chips={CHIPS} value={filter} onChange={selectChip} />
            {body}
          </Block>
        )}
      </Page>
      {!firstTime && <Fab to="/performances/new">공연 등록</Fab>}
    </>
  )
}
