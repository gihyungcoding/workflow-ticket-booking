// 토큰은 localStorage 의 이 키에 저장한다 (ADR-0015). auth.ts 가 다시 내보낸다 — auth.ts 가 이 파일을 import 하므로 여기 둔다.
export const ACCESS_TOKEN_KEY = 'accessToken'

// 환경변수는 호출 시점에 읽는다 — 테스트에서 vi.stubEnv 로 바꿀 수 있게.
export function apiUrl(path: string): string {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').trim().replace(/\/+$/, '')
  return `${base}${path}`
}

// 요청 토큰의 정본은 화면의 로그인 세션이다 (AuthProvider 가 알린다). undefined 는 세션이 아직 정해지지 않은 것 —
// 그때만 저장소를 읽는다. 저장에 실패했거나 다른 탭이 저장소를 바꿔도 이 탭의 요청은 화면과 같다 (TASK-024 RC-1).
let sessionToken: string | null | undefined

/** AuthProvider 전용 — 로그인·로그아웃·복원 결과를 요청 토큰으로 알린다. undefined 는 세션이 끝났다(Provider 가 내려감) */
export function setSessionToken(token: string | null | undefined) {
  sessionToken = token
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

function currentToken(): string | null {
  if (sessionToken === undefined) return storedToken()
  const trimmed = sessionToken?.trim()
  return trimmed ? trimmed : null
}

// 쓰기는 JWT 역할·소유권으로 보호한다 (ADR-0029). 저장된 토큰이 있으면 메서드와 무관하게 싣는다 —
// 무효 토큰은 서버가 익명으로 보므로 공개 조회가 막히지 않는다. 호출부가 실은 Authorization 은 그대로 둔다.
export function request(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = headers.has('Authorization') ? null : currentToken()
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
