import Box from '@mui/material/Box'
import { memo } from 'react'
import { Link } from 'react-router-dom'
import type { Performance } from '../api/performances'
import { token } from '../theme/tokens'
import { formatDateTime, formatWhen } from '../utils/datetime'
import { Poster } from './Poster'
import { StatusBadge } from './StatusBadge'

export interface PerformanceCardProps {
  performance: Performance
  /** 한국 날짜 `kstDay`(기준 시각 또는 기기) — 「오늘」 판정. 문자열이라 시각이 흘러도 카드는 다시 그려지지 않는다 */
  today: string
}

// 시안 .p-card 의 글자 — 줄 수를 넘으면 말줄임(기능 문서 §2-5 「긴 공연명」)
const clamp = (lines: number) => ({ display: '-webkit-box', WebkitLineClamp: lines, WebkitBoxOrient: 'vertical', overflow: 'hidden', overflowWrap: 'anywhere' }) as const

/** 상태별 한 줄 — 예매 가능은 남은 좌석, 예정은 오픈 일시, 그 밖은 장소만 */
function metaLine(p: Performance, today: string): string {
  if (p.status === 'OPEN') return `${p.venue} · 남은 좌석 ${p.availableSeats}석`
  if (p.status === 'UPCOMING') return `${p.venue} · ${formatWhen(p.openAt, today)} 오픈`
  return p.venue
}

/** 시안 .p-card — 포스터·배지·공연명·시작 일시(항상 날짜+시각)·장소 줄. 카드 전체가 상세 링크다 */
export const PerformanceCard = memo(function PerformanceCard({ performance: p, today }: PerformanceCardProps) {
  return (
    <Box
      component={Link}
      to={`/performances/${p.id}`}
      sx={{ display: 'flex', flexDirection: 'column', gap: `calc(${token('--stack-s')} / 2)`, minWidth: 0, color: 'inherit', textDecoration: 'none' }}
    >
      <Poster title={p.title} url={p.posterUrl} aspectRatio="1 / 1" marginBottom={`calc(${token('--stack-s')} / 2)`} />
      <StatusBadge status={p.status} />
      <Box component="p" sx={{ margin: 0, fontSize: token('--fs-caption'), fontWeight: 700, lineHeight: Number(token('--lh-caption')), color: token('--c-text-1'), ...clamp(2) }}>
        {p.title}
      </Box>
      <Box component="p" sx={{ margin: 0, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2'), ...clamp(2) }}>
        {formatDateTime(p.startAt)}
      </Box>
      <Box component="p" sx={{ margin: 0, fontSize: token('--fs-caption'), lineHeight: Number(token('--lh-caption')), color: token('--c-text-3'), overflowWrap: 'anywhere' }}>
        {metaLine(p, today)}
      </Box>
    </Box>
  )
})
