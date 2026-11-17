import type { PerformanceStatus } from './performances'

// Phase 2b 컴파일용 스켈레톤 — 타입 선언과 시그니처만 있고 본문은 구현이 아니다. 로직은 Phase 3 에서 채운다.

/** 백엔드 domain/PerformanceFilter.java 와 같은 이름 */
export type PerformanceFilter = 'ALL' | 'DRAFT' | 'UPCOMING' | 'OPEN' | 'SOLD_OUT' | 'CLOSED' | 'CANCELLED'

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

export async function getMyPerformances(_filter?: PerformanceFilter, _page?: number): Promise<OrganizerPerformanceList> {
  throw new Error('Not implemented')
}
