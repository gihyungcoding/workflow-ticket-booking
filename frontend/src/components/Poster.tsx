import Box from '@mui/material/Box'
import { useState } from 'react'
import ticketArt from '../assets/fluent/ticket.svg'
import { componentSizes, token } from '../theme/tokens'

export interface PosterProps {
  title: string
  url: unknown
  /** 칸 비율 — 상세 `4 / 3`, 카드 `1 / 1` (시안 .photo) */
  aspectRatio: string
  marginBottom?: string
}

/** 포스터 — 없거나 불러오지 못하면 같은 크기의 회색 면과 아이콘 (시안 .photo) */
export function Poster({ title, url, aspectRatio, marginBottom }: PosterProps) {
  const [broken, setBroken] = useState(false)
  // http·https 만 그린다 — javascript:·data: 같은 스킴은 포스터 없음으로 본다(서버는 https 만 받는다 — ADR-0028)
  const usable = !broken && typeof url === 'string' && /^https?:\/\//i.test(url.trim())
  const alt = `${title} 포스터`
  return (
    <Box
      data-testid="poster"
      sx={{
        position: 'relative',
        overflow: 'hidden',
        borderRadius: token('--r-m'),
        backgroundColor: token('--c-bg'),
        aspectRatio,
        marginBottom,
      }}
    >
      {usable ? (
        <Box
          component="img"
          src={url as string}
          alt={alt}
          onError={() => setBroken(true)}
          sx={{ display: 'block', width: '100%', height: '100%', objectFit: 'cover' }}
        />
      ) : (
        <Box role="img" aria-label={alt} sx={{ width: '100%', height: '100%', display: 'grid', placeItems: 'center' }}>
          <Box component="img" src={ticketArt} alt="" sx={{ width: componentSizes.waitArt, height: componentSizes.waitArt, opacity: 0.45 }} />
        </Box>
      )}
    </Box>
  )
}
