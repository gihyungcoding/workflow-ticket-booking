import type { ReactNode } from 'react'
import { notImplemented } from './skeleton'

export interface PageProps {
  children?: ReactNode
  hasCta?: boolean
}

export function Page(_props: PageProps): never {
  return notImplemented()
}
