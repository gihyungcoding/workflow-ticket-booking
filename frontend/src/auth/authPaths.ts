import { matchPath } from 'react-router-dom'

// 인증 화면 경로 — 판별은 라우터와 같은 규칙(matchPath: 끝 슬래시·대소문자 무시)으로 한다.
// 문자열 정확 일치는 /login/·/Login 을 다른 화면으로 봤다 (TASK-018 Phase 4 RC-2).
export const AUTH_PATHS = ['/login', '/signup', '/oauth2/redirect', '/oauth2/role', '/joined']

/** pathname 이 paths 중 하나와 같은 화면인가 — 쿼리·해시는 호출부가 떼고 넘긴다 */
export function isOneOf(paths: readonly string[], pathname: string): boolean {
  return paths.some((path) => matchPath(path, pathname) !== null)
}
