import type { ReactNode } from 'react'
import { notImplemented } from './skeleton'

export interface SheetProps {
  open: boolean
  onClose?: () => void
  children?: ReactNode
}

export function Sheet(_props: SheetProps): never {
  return notImplemented()
}
