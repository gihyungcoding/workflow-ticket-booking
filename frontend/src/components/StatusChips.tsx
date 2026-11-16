import Box from '@mui/material/Box'
import type { PerformanceStatus } from '../api/performances'
import { componentSizes, token } from '../theme/tokens'

/** 상태 칩 — 시안 home.html 의 순서·이름. 전체는 status 없음(null) */
export const STATUS_CHIPS: ReadonlyArray<{ label: string; status: PerformanceStatus | null }> = [
  { label: '전체', status: null },
  { label: '예정', status: 'UPCOMING' },
  { label: '예매 가능', status: 'OPEN' },
  { label: '매진', status: 'SOLD_OUT' },
  { label: '마감', status: 'CLOSED' },
  { label: '취소', status: 'CANCELLED' },
]

export interface StatusChipsProps {
  value: PerformanceStatus | null
  onChange: (status: PerformanceStatus | null) => void
}

/** 시안 .chips·.chip — 걸러 보기. 고른 칩은 먹색 채움(브랜드 색 없음), 가로로 넘겨 본다 */
export function StatusChips({ value, onChange }: StatusChipsProps) {
  return (
    <Box
      role="group"
      aria-label="상태로 걸러 보기"
      sx={{
        display: 'flex',
        gap: token('--stack-s'),
        overflowX: 'auto',
        scrollbarWidth: 'none',
        // 블록 안에서 가장자리까지 번지게 한다(시안 .block .chips)
        margin: `0 calc(${token('--block-pad')} * -1)`,
        padding: `0 ${token('--block-pad')}`,
      }}
    >
      {STATUS_CHIPS.map(({ label, status }) => {
        const selected = status === value
        return (
          <Box
            key={label}
            component="button"
            type="button"
            aria-pressed={selected}
            onClick={() => onChange(status)}
            sx={{
              flex: 'none',
              display: 'inline-flex',
              alignItems: 'center',
              minHeight: componentSizes.touch,
              padding: `0 ${token('--stack-m')}`,
              border: 0,
              borderRadius: token('--r-pill'),
              cursor: 'pointer',
              fontFamily: 'inherit',
              fontSize: token('--fs-sub'),
              fontWeight: Number(token('--fw-strong')),
              backgroundColor: token(selected ? '--c-text-1' : '--c-surface'),
              color: token(selected ? '--c-surface' : '--c-text-2'),
              boxShadow: selected ? 'none' : `inset 0 0 0 1px ${token('--c-line-strong')}`,
              '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
            }}
          >
            {label}
          </Box>
        )
      })}
    </Box>
  )
}
