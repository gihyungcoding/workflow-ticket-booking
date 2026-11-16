import { isBlankDisplay } from '../components/ui/text'
import { isInstant } from '../utils/datetime'
import { request } from './http'

export type PerformanceStatus = 'UPCOMING' | 'OPEN' | 'SOLD_OUT' | 'CLOSED' | 'CANCELLED'

export interface SectionSummary {
  grade: string
  price: number
  seatCount: number
  /** 묶음 이름 — 구역 위치를 등록하지 않은 공연은 등급 이름과 같다. 서버는 항상 주지만 화면은 런타임에 검증한다(TASK-020 SC-04) */
  area?: string
  /** 묶음 설명 — 없으면 키가 없다 */
  areaNote?: string
  /** 무대에서 가까운 순 — 위와 같이 런타임에 검증한다 */
  stageOrder?: number
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
  /** 없으면 키가 없다 */
  posterUrl?: string
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

/**
 * 공연 목록 한 쪽. status 가 없으면 전체, page 가 0 이면 첫 쪽이라 둘 다 쿼리에 싣지 않는다 — 인자 없는 호출은 지금처럼 `/api/performances` 다.
 * 정의 밖 status 와 0 이상 정수가 아닌 page 는 요청 전에 거부한다(화면이 만드는 값이라 버그다).
 */
export async function getPerformances(status?: PerformanceStatus | null, page?: number): Promise<PerformanceListResponse> {
  if (status != null && !STATUSES.includes(status)) throw new Error(`공연 목록 조회 인자 오류: status ${String(status)}`)
  if (page !== undefined && !isSafeCount(page)) throw new Error(`공연 목록 조회 인자 오류: page ${String(page)}`)
  const query = [status ? `status=${status}` : '', page ? `page=${page}` : ''].filter(Boolean).join('&')
  const response = await request(`/api/performances${query ? `?${query}` : ''}`)
  if (!response.ok) {
    throw new Error(`공연 목록 조회 실패: ${response.status}`)
  }
  const list = parseList(await response.json())
  if (list === null) throw new Error('공연 목록 응답이 올바르지 않습니다')
  return list
}

/** 홈 머리글 후보 — 200 이면 그 공연, 204 면 후보 없음(null). 본문이 계약을 어기면 던진다 */
export async function getNextOpening(): Promise<Performance | null> {
  const response = await request('/api/performances/next-opening')
  if (response.status === 204) return null
  if (!response.ok) {
    throw new Error(`머리글 후보 조회 실패: ${response.status}`)
  }
  const item = parseListItem(await response.json())
  if (item === null) throw new Error('머리글 후보 응답이 올바르지 않습니다')
  return item
}

export async function getPerformance(id: string): Promise<Performance> {
  const response = await request(`/api/performances/${encodeURIComponent(id)}`)
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

const STATUSES: readonly string[] = ['UPCOMING', 'OPEN', 'SOLD_OUT', 'CLOSED', 'CANCELLED']
const isCount = (value: unknown): value is number => typeof value === 'number' && Number.isInteger(value) && value >= 0

/**
 * 상세 응답이 계약을 지키는지 본다 — 어기면 null (화면은 오류 화면으로 간다, TASK-020 SC-04).
 * sections 가 없거나 비어 있으면 구역 정보가 없는 것이지 위반이 아니다.
 */
export function parseDetail(value: unknown): Performance | null {
  if (typeof value !== 'object' || value === null) return null
  const p = value as Record<string, unknown>
  if (typeof p.status !== 'string' || !STATUSES.includes(p.status)) return null
  if (!isInstant(p.startAt) || !isInstant(p.openAt) || !isInstant(p.closeAt)) return null
  if (!isCount(p.availableSeats) || !isCount(p.totalSeats)) return null
  if (isBlankDisplay(p.title) || isBlankDisplay(p.venue)) return null
  if (p.availableSeats > p.totalSeats) return null
  let sections: SectionSummary[] | undefined
  if (p.sections !== undefined && p.sections !== null) {
    if (!Array.isArray(p.sections)) return null
    for (const element of p.sections) {
      if (typeof element !== 'object' || element === null) return null
      const s = element as Record<string, unknown>
      if (isBlankDisplay(s.grade) || isBlankDisplay(s.area)) return null
      if (!isCount(s.price)) return null
      if (typeof s.stageOrder !== 'number' || !Number.isInteger(s.stageOrder)) return null
    }
    sections = p.sections as SectionSummary[]
  }
  return { ...(value as Performance), sections }
}

// 안전 정수 — 1e400(Infinity)·2^53 넘는 값은 서버가 내지 않는다. 어기면 화면이 오류로 본다.
const isSafeCount = (value: unknown): value is number => typeof value === 'number' && Number.isSafeInteger(value) && value >= 0

/**
 * 목록 원소가 홈이 쓰는 계약을 지키는지 본다 — 어기면 null. id 는 양의 정수(링크 /performances/{id} 에 끼운다),
 * title·venue 는 공백이 아닌 문자열(ADR-0027), startAt·openAt 은 ISO 순간, status 는 다섯 값, availableSeats 는 0 이상 정수.
 * closeAt·totalSeats·sections·그 밖의 키는 홈이 쓰지 않으므로 계약이 아니다(쓰지 않는 값을 위반으로 보면 서버가 바꿀 때 멀쩡한 화면이 오류가 된다).
 */
export function parseListItem(value: unknown): Performance | null {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null
  const p = value as Record<string, unknown>
  if (!isSafeCount(p.id) || p.id < 1) return null
  if (isBlankDisplay(p.title) || isBlankDisplay(p.venue)) return null
  if (!isInstant(p.startAt) || !isInstant(p.openAt)) return null
  if (typeof p.status !== 'string' || !STATUSES.includes(p.status)) return null
  if (!isSafeCount(p.availableSeats)) return null
  return value as Performance
}

/** 목록 응답 — 원소 하나라도 계약을 어기면 전체를 오류로 본다(건너뛰면 totalElements 와 불러온 개수가 어긋나 더 보기가 영영 남는다) */
export function parseList(value: unknown): PerformanceListResponse | null {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null
  const body = value as Record<string, unknown>
  if (!Array.isArray(body.content) || !isSafeCount(body.totalElements)) return null
  const content: Performance[] = []
  for (const element of body.content) {
    const item = parseListItem(element)
    if (item === null) return null
    content.push(item)
  }
  return { ...(value as PerformanceListResponse), content }
}
