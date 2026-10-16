// 서버 쓰기 가드가 막는 메서드와 같은 집합 (ADR-0016 — 조회 GET·HEAD·OPTIONS 는 열려 있다).
const WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

// 환경변수는 호출 시점에 읽는다 — 테스트에서 vi.stubEnv 로 바꿀 수 있게.
export function apiUrl(path: string): string {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').trim().replace(/\/+$/, '')
  return `${base}${path}`
}

export function request(path: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()
  const writeKey = (import.meta.env.VITE_WRITE_KEY ?? '').trim()
  if (!WRITE_METHODS.has(method) || !writeKey) {
    return fetch(apiUrl(path), init)
  }
  const headers = new Headers(init.headers)
  headers.set('X-Write-Key', writeKey)
  return fetch(apiUrl(path), { ...init, headers })
}
