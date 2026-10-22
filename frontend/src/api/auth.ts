// Red 단계 컴파일용 스켈레톤 — 본문은 Phase 3 에서 채운다.
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

export async function signUp(_input: SignUpInput): Promise<void> {
  throw new Error('not implemented')
}

export async function login(_input: LoginInput): Promise<LoginResult> {
  throw new Error('not implemented')
}

export async function fetchMe(_accessToken: string): Promise<AuthUser> {
  throw new Error('not implemented')
}
