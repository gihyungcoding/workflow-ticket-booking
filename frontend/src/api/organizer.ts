import { isBlankDisplay } from '../components/ui/text'
import { isInstant } from '../utils/datetime'
import { request } from './http'
import { ForbiddenRoleError, STATUSES, UnauthorizedError, isSafeCount } from './performances'
import type { PerformanceStatus } from './performances'

/** 백엔드 domain/PerformanceFilter.java 와 같은 이름 — 칩 순서가 아니라 서버가 받는 값이다 */
export const PERFORMANCE_FILTERS = ['ALL', 'DRAFT', 'UPCOMING', 'OPEN', 'SOLD_OUT', 'CLOSED', 'CANCELLED'] as const
export type PerformanceFilter = (typeof PERFORMANCE_FILTERS)[number]

export interface OrganizerPerformance {
  id: number
  title: string
  venue: string
  startAt: string
  openAt: string
  totalSeats: number
  availableSeats: number
  status: PerformanceStatus
  /** 없으면 키가 없다 */
  posterUrl?: string
  published: boolean
}

export interface OrganizerPerformanceList {
  items: OrganizerPerformance[]
  totalElements: number
}

/**
 * 목록 원소가 화면이 쓰는 계약을 지키는지 본다 — 어기면 null.
 * id 는 양의 안전 정수(링크에 끼운다), title·venue 는 공백이 아닌 문자열(ADR-0027), startAt·openAt 은 ISO 순간,
 * status 는 다섯 값, 좌석 수는 0 이상 안전 정수이고 남은 좌석이 총석을 넘지 않는다, published 는 불리언.
 * closeAt·sections·editable·visibilityChangeable·모르는 키는 이 화면이 쓰지 않으므로 계약이 아니다.
 */
function parseItem(value: unknown): OrganizerPerformance | null {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null
  const p = value as Record<string, unknown>
  if (!isSafeCount(p.id) || p.id < 1) return null
  if (isBlankDisplay(p.title) || isBlankDisplay(p.venue)) return null
  if (!isInstant(p.startAt) || !isInstant(p.openAt)) return null
  if (typeof p.status !== 'string' || !STATUSES.includes(p.status)) return null
  if (!isSafeCount(p.totalSeats) || !isSafeCount(p.availableSeats) || p.availableSeats > p.totalSeats) return null
  if (typeof p.published !== 'boolean') return null
  return {
    id: p.id,
    title: p.title as string,
    venue: p.venue as string,
    startAt: p.startAt,
    openAt: p.openAt,
    totalSeats: p.totalSeats,
    availableSeats: p.availableSeats,
    status: p.status as PerformanceStatus,
    published: p.published,
    ...(typeof p.posterUrl === 'string' ? { posterUrl: p.posterUrl } : {}),
  }
}

/** 원소 하나라도 계약을 어기면 전체를 오류로 본다 — 건너뛰면 totalElements 와 불러온 개수가 어긋나 「더 보기」가 영영 남는다 */
function parseList(value: unknown): OrganizerPerformanceList | null {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null
  const body = value as Record<string, unknown>
  if (!Array.isArray(body.items) || !isSafeCount(body.totalElements)) return null
  const items: OrganizerPerformance[] = []
  for (const element of body.items) {
    const item = parseItem(element)
    if (item === null) return null
    items.push(item)
  }
  return { items, totalElements: body.totalElements }
}

/**
 * 내 공연 한 쪽. filter 가 ALL 이면, page 가 0 이면 쿼리에 싣지 않는다 — 인자 없는 호출은 `/api/organizer/performances` 다.
 * 정의 밖 filter 와 0 이상 안전 정수가 아닌 page 는 요청 전에 거부한다(화면이 만드는 값이라 버그다).
 * 401 은 UnauthorizedError, 403 은 ForbiddenRoleError — 본문의 code 가 아니라 상태로 가른다(TASK-024).
 */
export async function getMyPerformances(filter: PerformanceFilter = 'ALL', page?: number): Promise<OrganizerPerformanceList> {
  if (!PERFORMANCE_FILTERS.includes(filter)) throw new Error(`내 공연 조회 인자 오류: filter ${String(filter)}`)
  if (page !== undefined && !isSafeCount(page)) throw new Error(`내 공연 조회 인자 오류: page ${String(page)}`)
  const query = [filter === 'ALL' ? '' : `filter=${filter}`, page ? `page=${page}` : ''].filter(Boolean).join('&')
  const response = await request(`/api/organizer/performances${query ? `?${query}` : ''}`)
  if (response.status === 401) throw new UnauthorizedError('로그인이 필요합니다')
  if (response.status === 403) throw new ForbiddenRoleError('주최자만 이용할 수 있습니다')
  if (!response.ok) throw new Error(`내 공연 조회 실패: ${response.status}`)
  const list = parseList(await response.json())
  if (list === null) throw new Error('내 공연 응답이 올바르지 않습니다')
  return list
}
