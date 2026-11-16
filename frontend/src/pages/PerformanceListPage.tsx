import Box from '@mui/material/Box'
import Skeleton from '@mui/material/Skeleton'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { getNextOpening, getPerformances } from '../api/performances'
import type { Performance, PerformanceStatus } from '../api/performances'
import { MoreLink } from '../auth/AuthParts'
import { PerformanceCard } from '../components/PerformanceCard'
import { STATUS_CHIPS, StatusChips } from '../components/StatusChips'
import { Block, Hero, Page } from '../components/ui'
import { useServerClock } from '../hooks/useServerClock'
import { token } from '../theme/tokens'
import { formatDateTime, formatWhen, kstDay } from '../utils/datetime'

// 홈 — 시안 home.html. 머리글(곧 열리는 공연 + 기준 시각) · 상태 칩 · 2열 포스터 카드 · 더 보기(20개씩).
// 목록과 머리글 후보는 서로의 실패에 영향을 주지 않는다. 기준 시각은 표시용이다(ADR-0024) — 홈에는 켜는 동작이 없다.

type ListState =
  | { status: 'loading' }
  | { status: 'error' }
  | {
      status: 'success'
      items: Performance[]
      total: number
      /** 성공한 더 보기 횟수 — 다음 요청은 pages + 1 쪽이다(불러온 개수로 세지 않는다. 중복 id 가 있어도 쪽이 어긋나지 않게) */
      pages: number
      /** 더 보기가 빈 쪽을 받았다 — 총수와 어긋나도 더 부르지 않는다 */
      exhausted: boolean
      more: 'idle' | 'loading' | 'error'
    }

type NextState = { status: 'loading' } | { status: 'ready'; performance: Performance | null }

const SKELETON_COUNT = 4
const GENERAL_HERO = '다가오는 공연을 골라 보세요'

/** 먼저 보인 카드를 지키며 새 쪽을 붙인다 — 같은 공연은 한 번만 */
function merge(current: Performance[], incoming: Performance[]): Performance[] {
  const seen = new Set(current.map((p) => p.id))
  const added: Performance[] = []
  for (const p of incoming) {
    if (seen.has(p.id)) continue
    seen.add(p.id)
    added.push(p)
  }
  return [...current, ...added]
}

const photoGrid = {
  display: 'grid',
  gridTemplateColumns: 'repeat(2, 1fr)',
  gap: `${token('--stack-l')} ${token('--stack-s')}`,
} as const

/** 문구 + 해결 행동 하나(주 버튼은 하나 — design.md §6) */
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

export function PerformanceListPage() {
  const clock = useServerClock({ enabled: true })
  const [chip, setChip] = useState<PerformanceStatus | null>(null)
  const [list, setList] = useState<ListState>({ status: 'loading' })
  const [next, setNext] = useState<NextState>({ status: 'loading' })
  // 칩을 바꾸거나 화면을 떠나면 오래된 응답은 버린다(ADR-0018). 더 보기 중복 요청은 렌더를 기다리지 않는 ref 로 막는다
  const generation = useRef(0)
  const moreInFlight = useRef(false)

  // 첫 쪽을 부른다 — 화면 상태는 건드리지 않는다(진입 때는 이미 loading 이다)
  const fetchFirst = useCallback((status: PerformanceStatus | null) => {
    generation.current += 1
    const mine = generation.current
    moreInFlight.current = false
    getPerformances(status)
      .then((response) => {
        if (mine !== generation.current) return
        setList({ status: 'success', items: merge([], response.content), total: response.totalElements, pages: 0, exhausted: false, more: 'idle' })
      })
      .catch(() => {
        if (mine !== generation.current) return
        setList({ status: 'error' })
      })
  }, [])

  const loadFirst = (status: PerformanceStatus | null) => {
    setList({ status: 'loading' })
    fetchFirst(status)
  }

  useEffect(() => {
    fetchFirst(null)
    return () => {
      generation.current += 1
    }
  }, [fetchFirst])

  // 머리글 후보 — 없거나 못 가져와도 같은 일반 문구다
  useEffect(() => {
    let alive = true
    getNextOpening()
      .then((performance) => alive && setNext({ status: 'ready', performance }))
      .catch(() => alive && setNext({ status: 'ready', performance: null }))
    return () => {
      alive = false
    }
  }, [])

  const selectChip = (status: PerformanceStatus | null) => {
    if (status === chip) return
    setChip(status)
    loadFirst(status)
  }

  const loadMore = () => {
    if (list.status !== 'success' || moreInFlight.current) return
    moreInFlight.current = true
    const mine = generation.current
    const page = list.pages + 1
    setList({ ...list, more: 'loading' })
    getPerformances(chip, page)
      .then((response) => {
        if (mine !== generation.current) return
        moreInFlight.current = false
        setList((current) =>
          current.status !== 'success'
            ? current
            : { ...current, items: merge(current.items, response.content), pages: page, exhausted: response.content.length === 0, more: 'idle' },
        )
      })
      .catch(() => {
        if (mine !== generation.current) return
        moreInFlight.current = false
        setList((current) => (current.status !== 'success' ? current : { ...current, more: 'error' }))
      })
  }

  // 「오늘」 판정 — 기준 시각이 있으면 그 한국 날짜, 없으면 기기 날짜로만 문구를 가른다(켜는 데는 쓰지 않는다 — 상세와 같은 규칙)
  const today = kstDay(clock.now ?? Date.now())
  const eyebrow = clock.status === 'ready' && clock.now !== null ? `티켓온 · ${formatDateTime(new Date(clock.now).toISOString())}` : undefined
  const candidate = next.status === 'ready' ? next.performance : null

  let body: ReactNode
  if (list.status === 'loading') {
    body = (
      <Box sx={photoGrid}>
        {Array.from({ length: SKELETON_COUNT }).map((_, index) => (
          <Skeleton
            key={index}
            data-testid="performance-card-skeleton"
            variant="rectangular"
            sx={{ width: '100%', height: 'auto', aspectRatio: '1 / 1', borderRadius: token('--r-m') }}
          />
        ))}
      </Box>
    )
  } else if (list.status === 'error') {
    body = <Notice text="공연 목록을 불러오지 못했어요" action="다시 시도" onAction={() => loadFirst(chip)} />
  } else if (list.items.length === 0) {
    const label = STATUS_CHIPS.find((c) => c.status === chip)?.label ?? ''
    body =
      chip === null ? (
        <Notice text="다가오는 공연이 없어요" />
      ) : (
        <Notice text={`${label} 공연이 없어요`} action="전체 보기" onAction={() => selectChip(null)} />
      )
  } else {
    const hasMore = !list.exhausted && list.items.length < list.total
    body = (
      <>
        <Box sx={photoGrid}>
          {list.items.map((performance) => (
            <PerformanceCard key={performance.id} performance={performance} today={today} />
          ))}
        </Box>
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

  // 홈은 App 의 Container(+전역 헤더) 안에 있다(사용자 결정). Container 의 좌우 거터(MUI: xs 16px · sm 24px)를 상쇄해 시안(.page)과 같은 가장자리에서 시작한다
  return (
    <Box sx={{ marginX: { xs: -2, sm: -3 } }}>
      <Page>
        <Box sx={{ padding: `0 ${token('--page-x')}` }}>
          {next.status === 'loading' ? (
            <Box sx={{ padding: `${token('--stack-l')} 0 ${token('--stack-m')}` }}>
              <Skeleton data-testid="hero-skeleton" variant="rectangular" sx={{ width: '100%', height: token('--h-row'), borderRadius: token('--r-m') }} />
            </Box>
          ) : (
            <Hero
              eyebrow={eyebrow}
              title={candidate ? `${formatWhen(candidate.openAt, today)}에 ${candidate.title} 예매가 열려요` : GENERAL_HERO}
              lead={candidate ? <MoreLink to={`/performances/${candidate.id}`}>{`${candidate.title} 공연 보기`}</MoreLink> : undefined}
            />
          )}
        </Box>
        <Block title="다가오는 공연">
          <StatusChips value={chip} onChange={selectChip} />
          {body}
        </Block>
      </Page>
    </Box>
  )
}
