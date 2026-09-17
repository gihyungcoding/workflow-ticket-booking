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

async function throwIfApiError(response: Response): Promise<never> {
  const body = await response.json().catch(() => null)
  if (body && typeof body.code === 'string') {
    throw new ApiError(body.code, typeof body.message === 'string' ? body.message : '')
  }
  throw new Error(`요청 실패: ${response.status}`)
}

export async function registerPerformance(
  input: RegisterPerformanceInput,
): Promise<Performance> {
  const response = await fetch('/api/performances', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    await throwIfApiError(response)
  }
  return response.json()
}

export async function updatePerformance(
  id: string,
  input: UpdatePerformanceInput,
): Promise<Performance> {
  const response = await fetch(`/api/performances/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    await throwIfApiError(response)
  }
  return response.json()
}

export async function cancelPerformance(id: string): Promise<Performance> {
  const response = await fetch(`/api/performances/${id}/cancel`, { method: 'POST' })
  if (!response.ok) {
    await throwIfApiError(response)
  }
  return response.json()
}
