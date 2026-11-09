import Box from '@mui/material/Box'
import { useId } from 'react'
import { componentSizes, token } from '../../theme/tokens'
import { isBlank } from './text'

export interface SegmentedOption<T extends string> {
  value: T
  label: string
}

export interface SegmentedProps<T extends string> {
  label: string
  options: readonly SegmentedOption<T>[]
  value: T | null
  onChange: (value: T) => void
  error?: string | null
}

/** 시안 .seg·.seg-item — 하나를 고르는 칸. 고른 칸만 흰 칸으로 떠 있다(브랜드 색 없음) */
export function Segmented<T extends string>({ label, options, value, onChange, error }: SegmentedProps<T>) {
  const labelId = useId()
  const errorId = `${labelId}-error`
  const hasError = !isBlank(error)
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
      <Box component="p" id={labelId} sx={{ margin: 0, fontSize: token('--fs-sub'), fontWeight: Number(token('--fw-strong')), color: token('--c-text-2') }}>
        {label}
      </Box>
      <Box
        role="radiogroup"
        aria-labelledby={labelId}
        aria-describedby={hasError ? errorId : undefined}
        sx={{ display: 'flex', backgroundColor: 'var(--c-track)', borderRadius: token('--r-m'), padding: componentSizes.rowGap }}
      >
        {options.map((option) => {
          const selected = option.value === value
          return (
            <Box
              key={option.value}
              component="button"
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option.value)}
              sx={{
                flex: 1,
                minHeight: componentSizes.touch,
                border: 0,
                borderRadius: token('--r-s'),
                fontFamily: 'inherit',
                fontSize: token('--fs-sub'),
                fontWeight: Number(token('--fw-strong')),
                cursor: 'pointer',
                backgroundColor: selected ? token('--c-surface') : 'transparent',
                color: selected ? token('--c-text-1') : token('--c-text-2'),
                boxShadow: selected ? token('--shadow-1') : 'none',
                '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
              }}
            >
              {option.label}
            </Box>
          )
        })}
      </Box>
      {hasError && (
        <Box component="p" id={errorId} data-field-error="" sx={{ margin: 0, fontSize: token('--fs-sub'), color: token('--c-danger') }}>
          {error}
        </Box>
      )}
    </Box>
  )
}
