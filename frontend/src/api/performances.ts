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
  const response = await fetch('/api/performances')
  if (!response.ok) {
    throw new Error(`공연 목록 조회 실패: ${response.status}`)
  }
  return response.json()
}

export async function getPerformance(id: string): Promise<Performance> {
  const response = await fetch(`/api/performances/${id}`)
  if (response.status === 404) {
    throw new PerformanceNotFoundError(`공연을 찾을 수 없습니다: ${id}`)
  }
  if (!response.ok) {
    throw new Error(`공연 상세 조회 실패: ${response.status}`)
  }
  return response.json()
}
