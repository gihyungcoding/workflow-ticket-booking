import { isInstant } from '../utils/datetime'
import { apiUrl } from './http'

export interface ServerTime {
  now: string
}

// 토큰을 싣지 않는다 — Authorization 이 붙으면 교차 출처에서 preflight 가 왕복 표본을 부풀린다(공개 경로, SecurityConfig).
// 응답 본문이 계약을 어기면(now 가 없거나 ISO 순간이 아닌 시각) 이 표본은 실패다. signal 이 중단되면 요청도 끝난다.
export async function getServerTime(signal?: AbortSignal): Promise<ServerTime> {
  const response = await fetch(apiUrl('/api/time'), { cache: 'no-store', signal })
  if (!response.ok) {
    throw new Error(`서버 시각 조회 실패: ${response.status}`)
  }
  const body: unknown = await response.json()
  const now = (body as { now?: unknown } | null)?.now
  if (!isInstant(now)) {
    throw new Error('서버 시각 응답이 올바르지 않습니다')
  }
  return { now }
}
