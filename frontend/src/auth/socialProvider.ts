// 소셜 제공자 — 인가 주소의 id(소문자), 서버 enum(대문자), 화면 이름.
export const SOCIAL_PROVIDERS = [
  { id: 'kakao', code: 'KAKAO', name: '카카오' },
  { id: 'naver', code: 'NAVER', name: '네이버' },
  { id: 'google', code: 'GOOGLE', name: '구글' },
  { id: 'apple', code: 'APPLE', name: '애플' },
] as const

export type SocialProviderId = (typeof SOCIAL_PROVIDERS)[number]['id']
export type SocialProviderCode = (typeof SOCIAL_PROVIDERS)[number]['code']

// 소셜 버튼을 누를 때 남기고 콜백 화면이 읽는다 — 어느 계정을 확인 중인지 보이려고.
const STORAGE_KEY = 'socialProvider'

// 브라우저 저장소는 막혀 있을 수 있다 — 기록하지 못해도 로그인 이동은 막지 않는다.
export function rememberProvider(id: SocialProviderId) {
  try {
    sessionStorage.setItem(STORAGE_KEY, id)
  } catch {
    // 콜백 화면이 '소셜 계정' 으로 보인다
  }
}

/** 기록된 제공자의 화면 이름. 없거나 모르는 값이면 null (값은 정확히 소문자 id 여야 한다). */
export function rememberedProviderName(): string | null {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    return SOCIAL_PROVIDERS.find((p) => p.id === stored)?.name ?? null
  } catch {
    return null
  }
}

/** 서버 enum(대문자) → 화면 이름. 모르는 값은 null. */
export function providerNameOf(code: unknown): string | null {
  return SOCIAL_PROVIDERS.find((p) => p.code === code)?.name ?? null
}
