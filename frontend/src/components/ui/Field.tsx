import type { InputHTMLAttributes } from 'react'
import { notImplemented } from './skeleton'

export interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'children'> {
  label?: string | null
  error?: string | null
}

export function Field(_props: FieldProps): never {
  return notImplemented()
}
