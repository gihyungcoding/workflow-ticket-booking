import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ACCESS_TOKEN_KEY, TokenRejectedError, fetchMe } from '../api/auth'
import { setSessionToken } from '../api/http'
import type { AuthUser } from '../api/auth'

export interface AuthContextValue {
  user: AuthUser | null
  // 저장된 토큰으로 사용자를 복원하는 중 — 헤더가 깜빡이지 않게 구분한다.
  restoring: boolean
  login: (accessToken: string, user: AuthUser) => void
  logout: () => void
}

// Provider 밖에서는 비로그인 상태로 본다 (Provider 없이 렌더하는 기존 테스트 보호).
const AuthContext = createContext<AuthContextValue>({
  user: null,
  restoring: false,
  login: () => {},
  logout: () => {},
})

// 브라우저 저장소는 막혀 있을 수 있다 — 어떤 접근도 앱을 죽이지 않는다.
function readToken(): string | null {
  try {
    const stored = localStorage.getItem(ACCESS_TOKEN_KEY)
    return stored && stored.trim() !== '' ? stored : null
  } catch {
    return null
  }
}

function writeToken(accessToken: string | null) {
  try {
    if (accessToken === null) {
      localStorage.removeItem(ACCESS_TOKEN_KEY)
    } else {
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    }
  } catch {
    // 저장하지 못해도 이번 방문 동안의 로그인 상태는 메모리로 유지한다.
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // 세션의 시작 토큰 — 요청 토큰의 정본으로 바로 알린다. 자식 화면의 첫 요청보다 먼저여야 해서 렌더 중에 정한다
  const [storedToken] = useState(() => {
    const token = readToken()
    setSessionToken(token)
    return token
  })
  // 이 Provider 의 세션 토큰 — 내려갔다 다시 올라올 때(StrictMode 포함) 같은 값을 다시 알린다
  const session = useRef<string | null>(storedToken)
  const adopt = (token: string | null) => {
    session.current = token
    setSessionToken(token)
  }
  useEffect(() => {
    setSessionToken(session.current)
    // 내려가면 세션이 끝난다 — Provider 밖의 요청은 저장소를 읽는다
    return () => setSessionToken(undefined)
  }, [])
  const [user, setUser] = useState<AuthUser | null>(null)
  const [restoring, setRestoring] = useState(storedToken !== null)
  // login/logout 이 일어날 때마다 올린다 — 그 전에 시작한 복원의 결과를 버리는 기준.
  const sessionGeneration = useRef(0)

  useEffect(() => {
    if (storedToken === null) return
    let cancelled = false
    const startedAt = sessionGeneration.current
    // 취소됐거나 복원 도중 세션이 바뀌었으면 이 응답은 상태·토큰 어디에도 영향을 주지 않는다.
    const stale = () => cancelled || sessionGeneration.current !== startedAt
    fetchMe(storedToken)
      .then((restored) => {
        if (!stale()) setUser(restored)
      })
      .catch((error: unknown) => {
        if (stale()) return
        // 복원에 실패하면 화면은 비로그인이다 — 요청 토큰도 같이 비운다.
        // 저장된 토큰은 401(무효라는 증거)일 때만 지운다 — 일시 오류면 다음 방문에 다시 복원한다.
        adopt(null)
        if (error instanceof TokenRejectedError) writeToken(null)
      })
      .finally(() => {
        if (!stale()) setRestoring(false)
      })
    return () => {
      cancelled = true
    }
  }, [storedToken])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      restoring,
      login: (accessToken, loggedIn) => {
        sessionGeneration.current += 1
        adopt(accessToken)
        writeToken(accessToken)
        setUser(loggedIn)
        setRestoring(false)
      },
      logout: () => {
        sessionGeneration.current += 1
        adopt(null)
        writeToken(null)
        setUser(null)
      },
    }),
    [user, restoring],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext)
}
