import Chip from '@mui/material/Chip'
import type { ChipProps } from '@mui/material/Chip'
import type { PerformanceStatus } from '../api/performances'

const STATUS_LABEL: Record<PerformanceStatus, string> = {
  UPCOMING: '예매예정',
  OPEN: '예매가능',
  SOLD_OUT: '매진',
  CLOSED: '예매마감',
  CANCELLED: '공연취소',
}

const STATUS_COLOR: Record<PerformanceStatus, ChipProps['color']> = {
  UPCOMING: 'info',
  OPEN: 'success',
  SOLD_OUT: 'warning',
  CLOSED: 'default',
  CANCELLED: 'error',
}

export function StatusBadge({ status }: { status: PerformanceStatus }) {
  return <Chip label={STATUS_LABEL[status]} color={STATUS_COLOR[status]} size="small" />
}
