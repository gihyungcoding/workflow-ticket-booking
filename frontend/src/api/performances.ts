import { request } from './http'

export type PerformanceStatus = 'UPCOMING' | 'OPEN' | 'SOLD_OUT' | 'CLOSED' | 'CANCELLED'

export interface SectionSummary {
  grade: string
  price: number
  seatCount: number
}

export interface Performance {
  id: number
  title: string
  venue: string
  startAt: string
  openAt: string
  closeAt: string
  totalSeats: number
  availableSeats: number
  status: PerformanceStatus
  sections?: SectionSummary[]
}

export interface PerformanceListResponse {
  content: Performance[]
  page: number
  size: number
  totalElements: number
}

export interface SectionInput {
  grade: string
  price: number
  rowStart: string
  rowEnd: string
  seatsPerRow: number
}

export interface RegisterPerformanceInput {
  title: string
  venue: string
  startAt: string
  openAt: string
  closeAt: string
  sections: SectionInput[]
}

export interface UpdatePerformanceInput {
  title: string
  venue: string
  startAt: string
  openAt: string
  closeAt: string
}

export class PerformanceNotFoundError extends Error {}

export class ApiError extends Error {
  code: string

  constructor(code: string, message: string) {
    super(message)
    this.code = code
  }
}

// 공연 쓰기의 인증 실패(401) — 토큰이 없거나 무효하다. 화면은 로그아웃하고 로그인으로 보낸다.
export class UnauthorizedError extends Error {}

// 공연 쓰기의 역할 불일치(403) — 주최자가 아니다 (ADR-0029). 화면은 주최자 전용 안내를 보인다.
export class ForbiddenRoleError extends Error {}

export async function getPerformances(): Promise<PerformanceListResponse> {
  const response = await request('/api/performances')
  if (!response.ok) {
    throw new Error(`공연 목록 조회 실패: ${response.status}`)
  }
  return response.json()
}

export async function getPerformance(id: string): Promise<Performance> {
  const response = await request(`/api/performances/${id}`)
  if (response.status === 404) {
    throw new PerformanceNotFoundError(`공연을 찾을 수 없습니다: ${id}`)
  }
  if (!response.ok) {
    throw new Error(`공연 상세 조회 실패: ${response.status}`)
  }
  return response.json()
}

export async function throwIfApiError(response: Response): Promise<never> {
  const body = await response.json().catch(() => null)
  if (body && typeof body.code === 'string') {
    throw new ApiError(body.code, typeof body.message === 'string' ? body.message : '')
  }
  throw new Error(`요청 실패: ${response.status}`)
}

// 공연 쓰기 응답의 오류 — 401/403 은 본문과 무관하게 상태로 가른다(SecurityConfig 가 이 경로에서 내는 403 은 FORBIDDEN_ROLE 뿐이고,
// 프록시가 본문 없이 돌려줘도 같은 처리가 맞다). 그 밖은 지금처럼 코드로.
async function throwWriteError(response: Response): Promise<never> {
  if (response.status === 401) throw new UnauthorizedError('인증이 필요합니다')
  if (response.status === 403) throw new ForbiddenRoleError('주최자만 이용할 수 있습니다')
  return throwIfApiError(response)
}

export async function registerPerformance(
  input: RegisterPerformanceInput,
): Promise<Performance> {
  const response = await request('/api/performances', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    await throwWriteError(response)
  }
  return response.json()
}

export async function updatePerformance(
  id: string,
  input: UpdatePerformanceInput,
): Promise<Performance> {
  const response = await request(`/api/performances/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    await throwWriteError(response)
  }
  return response.json()
}

export async function cancelPerformance(id: string): Promise<Performance> {
  const response = await request(`/api/performances/${id}/cancel`, { method: 'POST' })
  if (!response.ok) {
    await throwWriteError(response)
  }
  return response.json()
}
