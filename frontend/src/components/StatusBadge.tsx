import type { PerformanceStatus } from '../api/performances'
import { Badge } from './ui'
import type { BadgeTone } from './ui'

// 같은 뜻은 어느 화면에서나 같은 말·같은 색 (design.md §5)
const STATUS_BADGE: Record<PerformanceStatus, [string, BadgeTone]> = {
  OPEN: ['예매 가능', 'brand'],
  UPCOMING: ['예정', 'info'],
  SOLD_OUT: ['매진', 'danger'],
  CLOSED: ['마감', 'neutral'],
  CANCELLED: ['공연 취소', 'neutral'],
}

export function StatusBadge({ status }: { status: PerformanceStatus }) {
  const badge = Object.hasOwn(STATUS_BADGE, status) ? STATUS_BADGE[status] : undefined
  if (!badge) return null
  return <Badge tone={badge[1]}>{badge[0]}</Badge>
}
