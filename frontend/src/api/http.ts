// 토큰은 localStorage 의 이 키에 저장한다 (ADR-0015). auth.ts 가 다시 내보낸다 — auth.ts 가 이 파일을 import 하므로 여기 둔다.
export const ACCESS_TOKEN_KEY = 'accessToken'

// 환경변수는 호출 시점에 읽는다 — 테스트에서 vi.stubEnv 로 바꿀 수 있게.
export function apiUrl(path: string): string {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').trim().replace(/\/+$/, '')
  return `${base}${path}`
}

// 저장소가 막혀 있어도 요청은 나간다 — 헤더만 없다. 비었거나 공백만이면 토큰이 없는 것이다.
function storedToken(): string | null {
  try {
    const stored = localStorage.getItem(ACCESS_TOKEN_KEY)?.trim()
    return stored ? stored : null
  } catch {
    return null
  }
}

// 쓰기는 JWT 역할·소유권으로 보호한다 (ADR-0029). 저장된 토큰이 있으면 메서드와 무관하게 싣는다 —
// 무효 토큰은 서버가 익명으로 보므로 공개 조회가 막히지 않는다. 호출부가 실은 Authorization 은 그대로 둔다.
export function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = headers.has('Authorization') ? null : storedToken()
  if (token === null) {
    return fetch(apiUrl(path), init)
  }
  try {
    headers.set('Authorization', `Bearer ${token}`)
  } catch {
    // 헤더에 실을 수 없는 값(NUL·줄바꿈·Latin-1 밖 글자) — 토큰 없이 보낸다
    return fetch(apiUrl(path), init)
  }
  return fetch(apiUrl(path), { ...init, headers })
}
