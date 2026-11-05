import type { ReactNode } from 'react'
import { notImplemented } from './skeleton'

export interface RowProps {
  lead?: ReactNode
  title: ReactNode
  sub?: ReactNode
  trail?: ReactNode
}

export function Row(_props: RowProps): never {
  return notImplemented()
}
