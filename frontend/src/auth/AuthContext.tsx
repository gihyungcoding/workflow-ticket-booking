import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ACCESS_TOKEN_KEY, TokenRejectedError, fetchMe } from '../api/auth'
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
  const [storedToken] = useState(readToken)
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
        // 401 만 토큰이 무효라는 증거다. 일시 오류로 사용자를 로그아웃시키지 않는다.
        if (!stale() && error instanceof TokenRejectedError) writeToken(null)
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
        writeToken(accessToken)
        setUser(loggedIn)
        setRestoring(false)
      },
      logout: () => {
        sessionGeneration.current += 1
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
