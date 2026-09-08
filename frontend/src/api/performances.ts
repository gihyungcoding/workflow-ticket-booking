export type PerformanceStatus = 'UPCOMING' | 'OPEN' | 'SOLD_OUT' | 'CLOSED' | 'CANCELLED'

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
}

export interface PerformanceListResponse {
  content: Performance[]
  page: number
  size: number
  totalElements: number
}

export class PerformanceNotFoundError extends Error {}

export async function getPerformances(): Promise<PerformanceListResponse> {
  throw new Error('Not implemented')
}

export async function getPerformance(id: string): Promise<Performance> {
  void id
  throw new Error('Not implemented')
}
