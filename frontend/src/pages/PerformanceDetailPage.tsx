import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { getPerformance, parseDetail, PerformanceNotFoundError } from '../api/performances'
import type { Performance } from '../api/performances'
import calendarArt from '../assets/fluent/calendar.svg'
import pinArt from '../assets/fluent/pin.svg'
import ticketArt from '../assets/fluent/ticket.svg'
import { ZoneMapSheet } from '../components/ZoneMapSheet'
import { AppBar, CtaBar, Hero, Info, Page, Row } from '../components/ui'
import { useServerClock } from '../hooks/useServerClock'
import { componentSizes, token } from '../theme/tokens'
import { formatClock, formatCountdown, formatDateTime, formatTimeOfDay, kstDay } from '../utils/datetime'
import { priceRange, zoneOverview } from '../utils/zones'

type DetailState =
  | { status: 'loading' }
  | { status: 'not-found' }
  | { status: 'error' }
  | { status: 'success'; performance: Performance }

// 시안 concert.html 의 고정 문구
const STEPS = [
  ['1 · 회차', '회차와 매수를 함께 골라요'],
  ['2 · 구역', '고른 매수만큼 붙어 남은 곳을 구역마다 보여 줘요'],
  ['3 · 좌석', '시작 자리 하나를 누르면 옆자리까지 한 번에 잡혀요'],
  ['4 · 확인', '7분 안에 확인하고 무통장 입금으로 예약해요'],
] as const

const eyebrowStyle = {
  margin: 0,
  fontSize: token('--fs-sub'),
  fontWeight: Number(token('--fw-strong')),
  lineHeight: Number(token('--lh-sub')),
  color: token('--c-text-2'),
}

/** 포스터 — 없거나 불러오지 못하면 같은 크기의 회색 면과 아이콘 (시안 .photo) */
function Poster({ title, url }: { title: string; url: unknown }) {
  const [broken, setBroken] = useState(false)
  // http·https 만 그린다 — javascript:·data: 같은 스킴은 포스터 없음으로 본다(서버는 https 만 받는다 — ADR-0028)
  const usable = !broken && typeof url === 'string' && /^https?:\/\//i.test(url.trim())
  const alt = `${title} 포스터`
  return (
    <Box
      data-testid="poster"
      sx={{
        position: 'relative',
        overflow: 'hidden',
        borderRadius: token('--r-m'),
        backgroundColor: token('--c-bg'),
        aspectRatio: '4 / 3',
        marginBottom: token('--stack-s'), // 시안 .show .photo
      }}
    >
      {usable ? (
        <Box
          component="img"
          src={url as string}
          alt={alt}
          onError={() => setBroken(true)}
          sx={{ display: 'block', width: '100%', height: '100%', objectFit: 'cover' }}
        />
      ) : (
        <Box role="img" aria-label={alt} sx={{ width: '100%', height: '100%', display: 'grid', placeItems: 'center' }}>
          <Box component="img" src={ticketArt} alt="" sx={{ width: componentSizes.waitArt, height: componentSizes.waitArt, opacity: 0.45 }} />
        </Box>
      )}
    </Box>
  )
}

/** 「열리면 이 순서로 골라요」 — 시안 .timeline */
function Steps() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
      <Box component="h2" sx={{ margin: 0, fontSize: token('--fs-h2'), fontWeight: Number(token('--fw-h2')), lineHeight: Number(token('--lh-h2')) }}>
        열리면 이 순서로 골라요
      </Box>
      <Box component="ol" sx={{ margin: 0, padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column' }}>
        {STEPS.map(([date, body], i) => (
          <Box
            component="li"
            key={date}
            sx={{
              position: 'relative',
              paddingLeft: token('--stack-l'),
              paddingBottom: i === STEPS.length - 1 ? 0 : token('--stack-m'),
              display: 'flex',
              flexDirection: 'column',
              gap: componentSizes.rowGap,
              '&::before': {
                content: '""',
                position: 'absolute',
                left: 0,
                top: `calc(${token('--fs-sub')} * ${token('--lh-sub')} / 2)`,
                transform: 'translateY(-50%)',
                width: '12px',
                height: '12px',
                borderRadius: token('--r-pill'),
                backgroundColor: token('--c-surface'),
                border: `2px solid ${token('--c-line-strong')}`,
                zIndex: 1,
              },
              // 점과 점을 잇는 세로선 — 마지막 칸에는 없다 (시안 .tl-item::after)
              '&:not(:last-child)::after': {
                content: '""',
                position: 'absolute',
                left: 0,
                top: `calc(${token('--fs-sub')} * ${token('--lh-sub')} / 2)`,
                bottom: `calc(${token('--fs-sub')} * ${token('--lh-sub')} / -2)`,
                width: '12px',
                background: `linear-gradient(${token('--c-line-strong')}, ${token('--c-line-strong')}) center / 2px 100% no-repeat`,
              },
            }}
          >
            <Box component="p" sx={{ ...eyebrowStyle, fontSize: token('--fs-sub') }}>
              {date}
            </Box>
            <Box component="p" sx={{ margin: 0, fontSize: token('--fs-body'), lineHeight: Number(token('--lh-body')) }}>
              {body}
            </Box>
          </Box>
        ))}
      </Box>
    </Box>
  )
}

export function PerformanceDetailPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const [state, setState] = useState<DetailState>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  // 시트는 history 칸 하나로 연다 — 뒤로 가기가 시트를 닫고 상세를 떠나지 않는다.
  // 이 화면에서 눌러 연 시트(pressed)는 그 칸을 한 칸 뒤로 닫는다. 열린 채 들어온 주소(새로고침·복원·붙여넣기)는
  // 되돌아갈 칸이 상세라는 보장이 없어 그 칸을 시트 없는 상세로 바꿔 닫는다(SC-41).
  // 닫기 입력은 시트가 실제로 닫힐 때까지 한 번만 받는다 — 한 칸 뒤로 가기는 브라우저에서 다음 태스크에 반영된다
  const zoneSheetState = (location.state as { zoneSheet?: boolean } | null)?.zoneSheet === true
  const pressed = useRef(false)
  const closing = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    if (zoneSheetState) return
    pressed.current = false
    if (closing.current !== null) {
      clearTimeout(closing.current)
      closing.current = null
    }
  }, [zoneSheetState])
  const openSheet = () => {
    pressed.current = true
    navigate(location.pathname + location.search, { state: { zoneSheet: true } })
  }
  const closeSheet = () => {
    if (closing.current !== null) return
    // 안전망 — 닫히지 않은 채 오래 남으면 다음 입력을 다시 받는다
    closing.current = setTimeout(() => {
      closing.current = null
    }, 1000)
    if (pressed.current) navigate(-1)
    else navigate(location.pathname + location.search, { replace: true, state: null })
  }
  // 기준 시각이 openAt 에 닿으면 그 뒤로는 시계가 필요 없다 — 시계를 끄고 이 사실만 기억한다(예정 → 예매 가능, 한 방향)
  const [opened, setOpened] = useState(false)
  const performance = state.status === 'success' ? state.performance : null
  const openAt = performance ? Date.parse(performance.openAt) : undefined
  const clock = useServerClock({ enabled: performance?.status === 'UPCOMING' && !opened, deadline: openAt })
  const reached = clock.now !== null && openAt !== undefined && clock.now >= openAt

  useEffect(() => {
    if (reached) setOpened(true)
  }, [reached])

  useEffect(() => {
    // 응답이 오기 전에 화면을 떠나거나 id 가 바뀌면 늦게 온 응답은 버린다 (ADR-0018)
    let live = true
    setState({ status: 'loading' })
    setOpened(false)
    getPerformance(id)
      .then((value) => {
        if (!live) return
        const parsed = parseDetail(value as unknown)
        setState(parsed ? { status: 'success', performance: parsed } : { status: 'error' })
      })
      .catch((error: unknown) => {
        if (!live) return
        setState(error instanceof PerformanceNotFoundError ? { status: 'not-found' } : { status: 'error' })
      })
    return () => {
      live = false
    }
  }, [id, attempt])

  const back = <AppBar plain onBack={() => navigate('/')} />

  if (state.status === 'loading') {
    return (
      <>
        {back}
        <Page plain>
          <Skeleton variant="rectangular" sx={{ height: componentSizes.waitArt, borderRadius: token('--r-m') }} />
          <Skeleton variant="rectangular" sx={{ aspectRatio: '4 / 3', height: 'auto', borderRadius: token('--r-m') }} />
        </Page>
      </>
    )
  }

  if (state.status === 'not-found' || state.status === 'error') {
    return (
      <>
        {back}
        <Page plain>
          <Hero title={state.status === 'not-found' ? '찾는 공연이 없어요' : '공연 정보를 불러오지 못했어요'} />
          {state.status === 'not-found' ? (
            <Button component={Link} to="/" variant="outlined" sx={{ alignSelf: 'flex-start' }}>
              홈으로
            </Button>
          ) : (
            <Button variant="outlined" onClick={() => setAttempt((n) => n + 1)} sx={{ alignSelf: 'flex-start' }}>
              다시 시도
            </Button>
          )}
        </Page>
      </>
    )
  }

  const p = state.performance
  const sections = p.sections ?? []
  const closeAt = p.closeAt
  const queuePath = `/performances/${encodeURIComponent(id)}/queue`

  // 예정 → 예매 가능/매진 은 기준 시각이 openAt 에 닿으면 다시 부르지 않고 화면만 바꾼다 (ADR-0026). 반대 방향은 없다
  const status = p.status === 'UPCOMING' && (opened || reached) ? (p.availableSeats > 0 ? 'OPEN' : 'SOLD_OUT') : p.status

  let eyebrow: string | null = null
  let title: React.ReactNode
  let lead: string | null = null
  let ctaLabel: string | null = null
  let ctaOn = false
  let retryClock = false

  if (status === 'UPCOMING') {
    eyebrow = `${formatDateTime(p.openAt)} 예매 오픈`
    // 오늘인지는 기준 시각이 있으면 그 한국 날짜로, 없으면 기기 날짜로만 가른다(문구용 — 켜는 데는 쓰지 않는다)
    const reference = clock.now ?? Date.now()
    const when = kstDay(p.openAt) === kstDay(reference) ? formatTimeOfDay(p.openAt) : formatDateTime(p.openAt)
    ctaLabel = `예매하기 · ${when}에 열려요`
    if (clock.status === 'ready' && clock.now !== null) {
      const text = formatCountdown(Date.parse(p.openAt) - clock.now)
      title = (
        <>
          오픈까지{' '}
          <Box
            component="span"
            role="timer"
            aria-label={`오픈까지 ${text}`}
            sx={{
              display: 'block',
              marginTop: '2px',
              fontSize: `calc(${token('--fs-title')} * 1.75)`,
              lineHeight: 1.12,
              fontWeight: 800,
              letterSpacing: componentSizes.titleTracking,
              color: token('--c-brand-text'),
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {text}
          </Box>
        </>
      )
      lead = `티켓온 기준 ${formatClock(new Date(clock.now).toISOString())} · 폰과 PC가 같은 순간에 열려요`
    } else if (clock.status === 'failed') {
      title = '기준 시각을 맞추지 못했어요'
      retryClock = true
    } else {
      title = '기준 시각을 맞추고 있어요'
    }
  } else if (status === 'OPEN') {
    title = '지금 예매할 수 있어요'
    lead = `${formatDateTime(closeAt)}에 마감돼요`
    ctaLabel = '예매하기'
    ctaOn = true
  } else if (status === 'SOLD_OUT') {
    title = '모든 좌석이 팔렸어요'
    ctaLabel = '매진'
  } else if (status === 'CLOSED') {
    title = '예매가 마감됐어요'
    ctaLabel = '예매 마감'
  } else {
    title = '공연이 취소됐어요'
  }

  const sheetOpen = sections.length >= 2 && zoneSheetState

  return (
    <>
      {back}
      <Page plain hasCta={ctaLabel !== null}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
          <Hero eyebrow={eyebrow} title={title} lead={lead} />
          {retryClock && (
            <Button variant="outlined" onClick={clock.retry} sx={{ alignSelf: 'flex-start' }}>
              다시 맞추기
            </Button>
          )}
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
          <Poster title={p.title} url={(p as { posterUrl?: unknown }).posterUrl} />
          <Box component="p" sx={eyebrowStyle}>{`${p.venue} · ${p.totalSeats.toLocaleString('ko-KR')}석`}</Box>
          <Box component="h2" sx={{ margin: 0, fontSize: token('--fs-h2'), fontWeight: Number(token('--fw-h2')), lineHeight: Number(token('--lh-h2')), textWrap: 'balance' }}>
            {p.title}
          </Box>
          {sections.length > 0 && (
            <Box
              component="p"
              sx={{
                margin: 0,
                fontSize: token('--fs-h2'),
                fontWeight: 700,
                color: token('--c-text-1'),
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {priceRange(Math.min(...sections.map((s) => s.price)), Math.max(...sections.map((s) => s.price)))}
            </Box>
          )}
          <Info icon={<Box component="img" src={calendarArt} alt="" sx={{ width: '22px', height: '22px' }} />}>{formatDateTime(p.startAt)}</Info>
          <Info icon={<Box component="img" src={ticketArt} alt="" sx={{ width: '22px', height: '22px' }} />}>1인 최대 4장</Info>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
          <Steps />
          {sections.length >= 2 && (
            <Box
              component="button"
              type="button"
              onClick={openSheet}
              sx={{ all: 'unset', display: 'block', cursor: 'pointer', width: '100%', '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' } }}
            >
              <Row
                lead={<Box component="img" src={pinArt} alt="" sx={{ width: componentSizes.rowArt, height: componentSizes.rowArt }} />}
                title="구역 위치 보기"
                sub={zoneOverview(sections)}
                trail={
                  <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M9 6l6 6-6 6" />
                  </svg>
                }
              />
            </Box>
          )}
        </Box>
      </Page>

      {ctaLabel !== null && (
        <CtaBar disabled={!ctaOn} onClick={() => navigate(queuePath)}>
          {ctaLabel}
        </CtaBar>
      )}
      {sections.length >= 2 && <ZoneMapSheet open={sheetOpen} venue={p.venue} sections={sections} onClose={closeSheet} />}
    </>
  )
}
