import Box from '@mui/material/Box'
import useMediaQuery from '@mui/material/useMediaQuery'
import { useEffect, useRef, useState } from 'react'
import { TOAST_DURATION_MS, token } from '../../theme/tokens'
import { isBlank } from './text'

export interface ToastProps {
  message?: string | null
  onClose?: () => void
}

/**
 * 시안 .toast — 그 자리에서 바뀐 뒤 안내. 2,500ms 뒤 스스로 사라진다(시간은 고정, prop 없음).
 * 문구가 바뀌면 시간을 새로 세고, 문구가 비면 바로 닫는다.
 */
export function Toast({ message, onClose }: ToastProps) {
  const text = isBlank(message) ? null : message
  // 시간이 다 돼 닫힌 문구 — 문구가 바뀌면 렌더 중에 비운다(effect 안 setState 대신)
  const [expired, setExpired] = useState<string | null>(null)
  const [prevText, setPrevText] = useState(text)
  if (text !== prevText) {
    setPrevText(text)
    setExpired(null)
  }
  const open = useRef(false)
  const onCloseRef = useRef(onClose)
  const reduceMotion = useMediaQuery('(prefers-reduced-motion: reduce)', { noSsr: true })

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (text === null) {
      // 떠 있던 토스트의 문구가 비면 바로 닫는다
      if (open.current) {
        open.current = false
        onCloseRef.current?.()
      }
      return
    }
    open.current = true
    const timer = setTimeout(() => {
      open.current = false
      setExpired(text)
      onCloseRef.current?.()
    }, TOAST_DURATION_MS)
    return () => clearTimeout(timer)
  }, [text])

  const shown = text !== null && expired !== text ? text : null
  if (shown === null) return null
  return (
    <Box
      role="status"
      sx={{
        position: 'fixed',
        left: token('--page-x'),
        right: token('--page-x'),
        top: `calc(${token('--stack-m')} + env(safe-area-inset-top))`,
        zIndex: 30,
        padding: `${token('--stack-m')} ${token('--block-pad')}`,
        borderRadius: token('--r-m'),
        backgroundColor: token('--c-text-1'),
        color: token('--c-surface'),
        fontSize: token('--fs-body'),
        lineHeight: Number(token('--lh-body')),
        boxShadow: token('--shadow-2'),
        overflowWrap: 'anywhere',
        // 안내만 한다 — 아래 화면을 누르는 것을 가리지 않는다 (시안 .toast)
        pointerEvents: 'none',
        transition: reduceMotion ? 'none' : 'opacity .2s, transform .2s',
      }}
    >
      {shown}
    </Box>
  )
}
