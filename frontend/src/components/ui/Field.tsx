import Box from '@mui/material/Box'
import { useId } from 'react'
import type { InputHTMLAttributes } from 'react'
import { componentSizes, token } from '../../theme/tokens'
import { isBlank } from './text'

export interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'children'> {
  label?: string | null
  error?: string | null
}

/** 시안 .field·.label·.input·.field-msg — 오류는 색만으로 전하지 않고 아이콘·문구를 함께 */
export function Field({ label, error, id, ...inputProps }: FieldProps) {
  const autoId = useId()
  const inputId = id ?? autoId
  const messageId = `${inputId}-error`
  const hasError = !isBlank(error)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
      {!isBlank(label) && (
        <Box
          component="label"
          htmlFor={inputId}
          sx={{ fontSize: token('--fs-sub'), fontWeight: Number(token('--fw-strong')), color: token('--c-text-2'), overflowWrap: 'anywhere' }}
        >
          {label}
        </Box>
      )}
      <Box
        component="input"
        id={inputId}
        aria-invalid={hasError ? true : undefined}
        aria-describedby={hasError ? messageId : undefined}
        {...inputProps}
        sx={{
          height: token('--h-btn'),
          padding: `0 ${token('--stack-m')}`,
          borderRadius: token('--r-m'),
          borderWidth: '1px',
          borderStyle: 'solid',
          borderColor: hasError ? token('--c-danger') : 'transparent',
          backgroundColor: hasError ? token('--c-danger-tint') : 'var(--c-fill)',
          fontFamily: 'inherit',
          fontSize: token('--fs-body'),
          color: 'inherit',
          '&:focus': hasError ? { outline: 'none' } : { outline: 'none', borderColor: token('--c-focus'), backgroundColor: token('--c-surface') },
        }}
      />
      {hasError && (
        <Box
          component="p"
          id={messageId}
          data-field-error=""
          sx={{
            margin: 0,
            display: 'flex',
            alignItems: 'center',
            gap: componentSizes.rowGap,
            fontSize: token('--fs-sub'),
            color: token('--c-danger'),
            overflowWrap: 'anywhere',
          }}
        >
          <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" style={{ flex: 'none' }}>
            <circle cx="12" cy="12" r="9" />
            <path d="M12 8v5M12 16h.01" />
          </svg>
          {error}
        </Box>
      )}
    </Box>
  )
}
