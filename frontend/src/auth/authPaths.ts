import { matchRoutes } from 'react-router-dom'

// 인증 화면 경로 — 판별은 라우터가 화면을 고르는 것과 같은 함수(matchRoutes)로 한다:
// 끝 슬래시·대소문자 무시, 퍼센트 인코딩 디코딩(깨진 인코딩은 그대로 둔다).
// 문자열 정확 일치는 /login/·/Login 을, matchPath 는 디코딩을 안 해 /%6Cogin 을 놓쳤다 (TASK-018 Phase 4 RC-2).
export const AUTH_PATHS = ['/login', '/signup', '/oauth2/redirect', '/oauth2/role', '/joined']

/** pathname 이 paths 중 하나와 같은 화면인가 — 쿼리·해시는 호출부가 떼고 넘긴다 */
export function isOneOf(paths: readonly string[], pathname: string): boolean {
  return matchRoutes(paths.map((path) => ({ path })), pathname) !== null
}
