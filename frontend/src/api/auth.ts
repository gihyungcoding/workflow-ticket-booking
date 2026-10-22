import { request } from './http'
import { throwIfApiError } from './performances'

// 토큰은 localStorage 의 이 키에 저장한다 (ADR-0015).
export const ACCESS_TOKEN_KEY = 'accessToken'

export type UserRole = 'AUDIENCE' | 'ORGANIZER'

export interface AuthUser {
  id: number
  email: string
  name: string
  role: UserRole
}

export interface SignUpInput {
  email: string
  password: string
  passwordConfirm: string
  name: string
  role: UserRole
}

export interface LoginInput {
  email: string
  password: string
}

export interface LoginResult {
  accessToken: string
  user: AuthUser
}

// 토큰이 무효하다는 서버의 판정(401). 일시 오류와 구분해 토큰 삭제 여부를 가른다.
export class TokenRejectedError extends Error {}

export const EMAIL_ALREADY_EXISTS = 'EMAIL_ALREADY_EXISTS'
export const INVALID_CREDENTIALS = 'INVALID_CREDENTIALS'

function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== 'object' || value === null) return false
  const user = value as Record<string, unknown>
  return typeof user.name === 'string' && user.name.trim() !== '' && typeof user.email === 'string'
}

// 성공 판정은 본문이 아니라 상태 201 로 한다.
export async function signUp(input: SignUpInput): Promise<void> {
  const response = await request('/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (response.status !== 201) {
    await throwIfApiError(response)
  }
}

export async function login(input: LoginInput): Promise<LoginResult> {
  const response = await request('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    await throwIfApiError(response)
  }
  const body = await response.json().catch(() => null)
  const accessToken = body?.accessToken
  if (typeof accessToken !== 'string' || accessToken.trim() === '' || !isAuthUser(body?.user)) {
    throw new Error('로그인 응답이 올바르지 않습니다')
  }
  return { accessToken, user: body.user }
}

export async function fetchMe(accessToken: string): Promise<AuthUser> {
  const response = await request('/api/auth/me', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (response.status === 401) {
    throw new TokenRejectedError('토큰이 유효하지 않습니다')
  }
  if (!response.ok) {
    throw new Error(`사용자 조회 실패: ${response.status}`)
  }
  const body = await response.json().catch(() => null)
  if (!isAuthUser(body)) {
    throw new Error('사용자 조회 응답이 올바르지 않습니다')
  }
  return body
}
