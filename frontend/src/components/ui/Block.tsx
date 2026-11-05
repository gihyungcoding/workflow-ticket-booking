import type { ReactNode } from 'react'
import { notImplemented } from './skeleton'

export interface BlockProps {
  title?: string
  note?: ReactNode
  list?: boolean
  children?: ReactNode
}

export function Block(_props: BlockProps): never {
  return notImplemented()
}
