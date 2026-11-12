import { ACCESS_TOKEN_KEY, request } from './http'
import { throwIfApiError } from './performances'
import { SOCIAL_PROVIDERS } from '../auth/socialProvider'
import type { SocialProviderCode } from '../auth/socialProvider'

// 토큰은 localStorage 의 이 키에 저장한다 (ADR-0015). 상수는 http.ts 에 있다 — 순환 import 를 피한다.
export { ACCESS_TOKEN_KEY }

export type UserRole = 'AUDIENCE' | 'ORGANIZER'

export interface AuthUser {
  id: number
  email: string
  name: string
  role: UserRole
  // 티켓온 아이디 — 일행에게 보내 표를 받는다 (TASK-016)
  handle: string
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
export const PENDING_SIGNUP_EXPIRED = 'PENDING_SIGNUP_EXPIRED'
export const SOCIAL_ACCOUNT_ALREADY_LINKED = 'SOCIAL_ACCOUNT_ALREADY_LINKED'

const ROLES: readonly unknown[] = ['AUDIENCE', 'ORGANIZER'] satisfies UserRole[]

const isFilled = (value: unknown): value is string => typeof value === 'string' && value.trim() !== ''

// 사용자 응답 규칙 — 로그인·가입 완료·복원이 같다 (SCENARIO_TASK-018 제가 정한 값 6). 값은 걷지 않고 그대로 쓴다.
function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== 'object' || value === null) return false
  const user = value as Record<string, unknown>
  return isFilled(user.name) && typeof user.email === 'string' && ROLES.includes(user.role) && isFilled(user.handle)
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

// 로그인 응답(accessToken + user)을 읽는다 — 상태가 200 이어도 본문이 틀리면 실패다.
async function readLoginResult(response: Response): Promise<LoginResult> {
  const body = await response.json().catch(() => null)
  const accessToken = body?.accessToken
  if (!isFilled(accessToken) || !isAuthUser(body?.user)) {
    throw new Error('로그인 응답이 올바르지 않습니다')
  }
  return { accessToken, user: body.user }
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
  return readLoginResult(response)
}

// 소셜 최초 가입 — 신원은 인증 토큰이 아니라 pendingToken 이 증명한다.
export async function completeSocialSignUp(pendingToken: string, role: UserRole): Promise<LoginResult> {
  const response = await request('/api/auth/social/complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pendingToken, role }),
  })
  if (!response.ok) {
    await throwIfApiError(response)
  }
  return readLoginResult(response)
}

export interface PendingSignup {
  provider: SocialProviderCode
  email: string
  name: string
  // 만료 시각(epoch ms) — 화면은 이 순간부터 만료 상태다. 판정은 서버가 최종이다
  expiresAt: number
}

// ISO-8601 시각 + 오프셋(Z 또는 ±hh:mm). 오프셋이 없으면 로컬 시각으로 읽히므로 받지 않는다.
// 소수 초는 Java Instant 처럼 9자리까지 올 수 있다 — 밀리초까지만 쓴다(버림).
const INSTANT = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(?:Z|([+-])(\d{2}):(\d{2}))$/

// Date.parse 는 2월 30일을 3월 2일로, 24:00 을 다음 날로 넘겨 읽는다 — 칸마다 범위를 보고 달력으로 되돌려 대조한다.
function parseInstant(value: unknown): number | null {
  if (typeof value !== 'string') return null
  const m = INSTANT.exec(value)
  if (!m) return null
  const [year, month, day, hour, minute, second] = m.slice(1, 7).map(Number)
  const millis = Number((m[7] ?? '').slice(0, 3).padEnd(3, '0'))
  const offsetHour = Number(m[9] ?? 0)
  const offsetMinute = Number(m[10] ?? 0)
  if (hour > 23 || minute > 59 || second > 59 || offsetHour > 23 || offsetMinute > 59) return null
  const local = Date.UTC(year, month - 1, day, hour, minute, second, millis)
  const calendar = new Date(local)
  if (calendar.getUTCFullYear() !== year || calendar.getUTCMonth() !== month - 1 || calendar.getUTCDate() !== day) return null
  const sign = m[8] === '-' ? -1 : 1
  return local - sign * (offsetHour * 60 + offsetMinute) * 60_000
}

// 소셜 가입 대기 정보 — 토큰이 쿼리에 남지 않게 POST 로 보낸다 (ADR-0021 과 같은 이유).
export async function fetchPendingSignup(pendingToken: string): Promise<PendingSignup> {
  const response = await request('/api/auth/social/pending', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pendingToken }),
  })
  if (!response.ok) {
    await throwIfApiError(response)
  }
  const body = await response.json().catch(() => null)
  const provider = SOCIAL_PROVIDERS.find((p) => p.code === body?.provider)?.code
  const expiresAt = parseInstant(body?.expiresAt)
  if (!provider || !isFilled(body?.email) || !isFilled(body?.name) || expiresAt === null) {
    throw new Error('가입 대기 정보 응답이 올바르지 않습니다')
  }
  return { provider, email: body.email, name: body.name, expiresAt }
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
