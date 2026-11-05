import type { ReactNode } from 'react'
import { notImplemented } from './skeleton'

export type BadgeTone = 'brand' | 'info' | 'danger' | 'warning' | 'success' | 'neutral'

export interface BadgeProps {
  tone: BadgeTone
  children?: ReactNode
}

export function Badge(_props: BadgeProps): never {
  return notImplemented()
}
