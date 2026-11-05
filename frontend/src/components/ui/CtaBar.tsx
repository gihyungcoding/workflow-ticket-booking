import type { ReactNode } from 'react'
import { notImplemented } from './skeleton'

export interface CtaBarProps {
  children: ReactNode
  onClick?: () => void
  disabled?: boolean
}

export function CtaBar(_props: CtaBarProps): never {
  return notImplemented()
}
