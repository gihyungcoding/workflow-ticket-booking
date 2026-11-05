import Box from '@mui/material/Box'
import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import { token } from '../../theme/tokens'

export interface SheetProps {
  open: boolean
  onClose?: () => void
  children?: ReactNode
}

/** 시안 .dim + .sheet — 아래에서 올라오는 시트. 바깥(어두운 막)·Esc 로 닫는다 */
export function Sheet({ open, onClose, children }: SheetProps) {
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (!open) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current?.()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open])

  if (!open) return null
  return (
    <>
      <Box
        data-testid="sheet-backdrop"
        onClick={() => onCloseRef.current?.()}
        sx={{ position: 'fixed', inset: 0, zIndex: 20, backgroundColor: token('--c-overlay') }}
      />
      <Box
        role="dialog"
        aria-modal="true"
        sx={{
          position: 'fixed',
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 21,
          backgroundColor: token('--c-surface'),
          '--c-fill': token('--c-bg'),
          boxShadow: token('--shadow-2'),
          borderRadius: `${token('--r-l')} ${token('--r-l')} 0 0`,
          padding: `${token('--block-pad')} ${token('--page-x')} calc(${token('--block-pad')} + env(safe-area-inset-bottom))`,
          display: 'flex',
          flexDirection: 'column',
          gap: token('--stack-m'),
        }}
      >
        {children}
      </Box>
    </>
  )
}
