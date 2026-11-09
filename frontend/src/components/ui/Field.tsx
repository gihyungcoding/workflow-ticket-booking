import Box from '@mui/material/Box'
import { useId } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'
import { componentSizes, token } from '../../theme/tokens'
import { isBlank } from './text'

export interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'children'> {
  label?: string | null
  error?: string | null
  /** 칸 아래 안내 — 오류가 나면 같은 자리가 오류로 바뀐다 (시안 .block-note → .field-msg, TASK-018) */
  help?: string | null
  /** 오류 문구 뒤에 붙는 동작 (예: 이미 가입된 이메일 → 로그인하기 링크). 오류가 있을 때만 그린다 */
  errorAction?: ReactNode
}

/** 시안 .field·.label·.input·.field-msg — 오류는 색만으로 전하지 않고 아이콘·문구를 함께 */
export function Field({ label, error, help, errorAction, id, 'aria-describedby': describedBy, 'aria-invalid': ariaInvalid, ...inputProps }: FieldProps) {
  const autoId = useId()
  const inputId = id ?? autoId
  const messageId = `${inputId}-error`
  const helpId = `${inputId}-help`
  const hasError = !isBlank(error)
  // 안내와 오류는 같은 자리다 — 오류가 있으면 안내 대신 오류 하나만
  const hasHelp = !hasError && !isBlank(help)
  // 문구 없이 오류 모양만 (로그인 401 — 두 칸을 함께 표시하고 문구는 한 칸에만, TASK-018)
  const looksInvalid = hasError || ariaInvalid === true || ariaInvalid === 'true'

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
        {...inputProps}
        // 오류 연결은 호출부의 값에 덮이지 않는다 — 호출부 설명(describedby)은 뒤에 이어 붙인다
        aria-invalid={hasError ? true : ariaInvalid}
        aria-describedby={[hasError ? messageId : hasHelp ? helpId : null, describedBy].filter(Boolean).join(' ') || undefined}
        sx={{
          height: token('--h-btn'),
          padding: `0 ${token('--stack-m')}`,
          borderRadius: token('--r-m'),
          borderWidth: '1px',
          borderStyle: 'solid',
          borderColor: looksInvalid ? token('--c-danger') : 'transparent',
          backgroundColor: looksInvalid ? token('--c-danger-tint') : 'var(--c-fill)',
          fontFamily: 'inherit',
          fontSize: token('--fs-body'),
          color: 'inherit',
          '&:focus': looksInvalid ? { outline: 'none' } : { outline: 'none', borderColor: token('--c-focus'), backgroundColor: token('--c-surface') },
        }}
      />
      {hasHelp && (
        <Box
          component="p"
          id={helpId}
          sx={{ margin: 0, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2'), overflowWrap: 'anywhere' }}
        >
          {help}
        </Box>
      )}
      {hasError && (
        <Box
          component="p"
          id={messageId}
          data-field-error=""
          sx={{
            margin: 0,
            display: 'flex',
            flexWrap: 'wrap',
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
          {errorAction}
        </Box>
      )}
    </Box>
  )
}
