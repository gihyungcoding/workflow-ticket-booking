// Red 단계 컴파일용 스켈레톤 — 본문은 Phase 3 에서 채운다.
import type { ReactNode } from 'react'
import type { AuthUser } from '../api/auth'

export interface AuthContextValue {
  user: AuthUser | null
  restoring: boolean
  login: (accessToken: string, user: AuthUser) => void
  logout: () => void
}

export function AuthProvider(_props: { children: ReactNode }): ReactNode {
  throw new Error('not implemented')
}

export function useAuth(): AuthContextValue {
  throw new Error('not implemented')
}
