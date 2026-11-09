import Box from '@mui/material/Box'
import { apiUrl } from '../api/http'
import { SOCIAL_PROVIDERS, rememberProvider } from '../auth/socialProvider'
import { token } from '../theme/tokens'

// 공급자 브랜드 색(카카오 노랑 등)은 쓰지 않는다 — 색 리터럴은 테마 밖에 둘 수 없다 (DESIGN-002). 모양은 시안 login 의 .social .btn.
// 인가 주소는 백엔드가 가진다 — fetch 가 아니라 브라우저가 전체 페이지로 이동한다.
// 누를 때 제공자를 남겨 콜백 화면이 "카카오 계정을 확인하고 있어요" 처럼 보이게 한다 — 기록하지 못해도 이동은 막지 않는다.
export function SocialLoginButtons() {
  return (
    <Box role="group" aria-label="소셜 로그인" sx={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: token('--stack-s') }}>
      {SOCIAL_PROVIDERS.map((provider) => (
        <Box
          key={provider.id}
          component="a"
          href={apiUrl(`/oauth2/authorization/${provider.id}`)}
          onClick={() => rememberProvider(provider.id)}
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: token('--h-btn'),
            borderRadius: token('--r-m'),
            backgroundColor: token('--c-surface'),
            boxShadow: `inset 0 0 0 1px ${token('--c-line-strong')}`,
            color: token('--c-text-1'),
            fontSize: token('--fs-sub'),
            fontWeight: Number(token('--fw-strong')),
            textDecoration: 'none',
            '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
          }}
        >
          {provider.name}
        </Box>
      ))}
    </Box>
  )
}
