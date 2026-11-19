import type { UserRole } from '../api/auth'

/** 로그인·가입 직후 기본 목적지 — 주최자는 내 공연, 그 밖에는 홈 (organizer-flow §4) */
export function homePathOf(role: UserRole): string {
  return role === 'ORGANIZER' ? '/organizer' : '/'
}
