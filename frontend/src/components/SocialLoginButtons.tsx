import Button from '@mui/material/Button'
import Divider from '@mui/material/Divider'
import Stack from '@mui/material/Stack'
import { apiUrl } from '../api/http'

// 브랜드 색은 쓰지 않는다 — 색 리터럴은 테마 밖에 둘 수 없고 채도는 「예매가능」 하나다 (design.md §2).
const PROVIDERS = [
  { id: 'kakao', label: '카카오' },
  { id: 'naver', label: '네이버' },
  { id: 'google', label: '구글' },
  { id: 'apple', label: '애플' },
]

// 인가 주소는 백엔드가 가진다 — fetch 가 아니라 브라우저가 전체 페이지로 이동한다.
export function SocialLoginButtons() {
  return (
    <>
      <Divider>또는</Divider>
      <Stack role="group" aria-label="소셜 로그인" direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
        {PROVIDERS.map((provider) => (
          <Button
            key={provider.id}
            component="a"
            href={apiUrl(`/oauth2/authorization/${provider.id}`)}
            variant="outlined"
            sx={{ flex: 1 }}
          >
            {provider.label}
          </Button>
        ))}
      </Stack>
    </>
  )
}
