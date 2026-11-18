import Box from '@mui/material/Box'
import { Link as RouterLink } from 'react-router-dom'
import ticketArt from '../assets/fluent/ticket.svg'
import type { OrganizerPerformance } from '../api/organizer'
import { componentSizes, token } from '../theme/tokens'
import { formatDateTime } from '../utils/datetime'
import { Poster } from './Poster'
import { StatusBadge } from './StatusBadge'
import { Badge } from './ui'

const hasPoster = (url: unknown): url is string => typeof url === 'string' && /^https?:\/\//i.test(url.trim())

/** 시안 organizer.html 의 둘째 줄 — 취소는 「공연 취소」, 비공개·예정은 오픈 일시와 총석, 나머지는 남은 좌석 */
function secondLine(p: OrganizerPerformance) {
  if (p.status === 'CANCELLED') return '공연 취소'
  if (!p.published || p.status === 'UPCOMING') return `${formatDateTime(p.openAt)} 오픈 · ${p.totalSeats}석`
  return (
    <>
      남은 좌석{' '}
      <Box component="span" sx={{ fontVariantNumeric: 'tabular-nums' }}>
        {p.availableSeats}
      </Box>{' '}
      / {p.totalSeats}석
    </>
  )
}

/**
 * 시안 organizer.html 의 `a.row` — 썸네일(또는 자리표시) · 배지 · 공연명 · 일시·장소 · 둘째 줄 · 화살표.
 * 배지는 한 행에 하나다(시안 우선): 비공개(취소 아님)면 「비공개」만, 취소면 없음, 그 외는 상태 배지.
 * 모든 행이 상세로 가는 링크다 — 시안의 취소·마감 비링크 행과 다르다(PLAN unresolved #3).
 */
export function OrganizerPerformanceRow({ performance: p }: { performance: OrganizerPerformance }) {
  const cancelled = p.status === 'CANCELLED'
  const draft = !p.published && !cancelled
  const sub = { margin: 0, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2'), overflowWrap: 'anywhere' } as const
  return (
    <Box
      component={RouterLink}
      to={`/organizer/performances/${p.id}`}
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: token('--stack-m'),
        minHeight: token('--h-row'),
        padding: `${token('--row-py')} 0`,
        color: 'inherit',
        textDecoration: 'none',
        '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
      }}
    >
      {hasPoster(p.posterUrl) ? (
        <Box sx={{ flex: 'none', width: componentSizes.thumbLarge, height: componentSizes.thumbLarge, overflow: 'hidden', borderRadius: token('--r-s') }}>
          <Poster title={p.title} url={p.posterUrl} aspectRatio="1 / 1" />
        </Box>
      ) : (
        <Box sx={{ flex: 'none', width: componentSizes.touch, height: componentSizes.touch, display: 'grid', placeItems: 'center' }}>
          <Box component="img" src={ticketArt} alt="" sx={{ width: componentSizes.rowArt, height: componentSizes.rowArt }} />
        </Box>
      )}
      <Box sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: componentSizes.rowGap }}>
        {draft ? <Badge tone="draft">비공개</Badge> : cancelled ? null : <StatusBadge status={p.status} />}
        <Box
          component="p"
          sx={{
            margin: 0,
            fontSize: token('--fs-strong'),
            fontWeight: Number(token('--fw-strong')),
            lineHeight: Number(token('--lh-strong')),
            overflowWrap: 'anywhere',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {p.title}
        </Box>
        <Box component="p" sx={sub}>{`${formatDateTime(p.startAt)} · ${p.venue}`}</Box>
        <Box component="p" sx={sub}>
          {secondLine(p)}
        </Box>
      </Box>
      <Box component="span" aria-hidden="true" sx={{ flex: 'none', display: 'inline-flex', color: token('--c-text-3') }}>
        <svg width={componentSizes.iconSmall} height={componentSizes.iconSmall} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
          <path d="M9 6l6 6-6 6" />
        </svg>
      </Box>
    </Box>
  )
}
