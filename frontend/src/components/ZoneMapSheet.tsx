import Box from '@mui/material/Box'
import type { SectionSummary } from '../api/performances'
import { componentSizes, token } from '../theme/tokens'
import { bundleTitle, groupZones, priceRange } from '../utils/zones'
import { Row, Sheet } from './ui'
import { ZoneMap } from './ZoneMap'

// 시안 zone-map.html 의 .sheet — 구역 위치. 묶음이 5개 이상이면 배치도를 그리지 않고 행만 보인다.
const MAX_MAP_BUNDLES = 4
// 견본 SVG 좌표(viewBox 20×20 안의 값)
const SWATCH_INSET = 2
const SWATCH_SIDE = 16
const SWATCH_RADIUS = 4

export interface ZoneMapSheetProps {
  open: boolean
  venue: string
  sections: SectionSummary[]
  onClose: () => void
}

/** 묶음 행 앞의 견본 — 배치도 도형과 같은 색(첫 묶음 플로어, 다음 고리, 그 바깥 고리) */
function Swatch({ index }: { index: number }) {
  const style =
    index === 0
      ? { fill: token('--c-brand-tint'), stroke: token('--c-brand-line') }
      : index === 1
        ? { fill: token('--c-bg'), stroke: token('--c-line-strong') }
        : { fill: token('--c-surface'), stroke: token('--c-line-strong'), strokeDasharray: '4 3' }
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" style={{ width: componentSizes.iconSmall, height: componentSizes.iconSmall }}>
      <rect x={SWATCH_INSET} y={SWATCH_INSET} width={SWATCH_SIDE} height={SWATCH_SIDE} rx={SWATCH_RADIUS} style={{ ...style, strokeWidth: 1.5 }} />
    </svg>
  )
}

export function ZoneMapSheet({ open, venue, sections, onClose }: ZoneMapSheetProps) {
  const bundles = groupZones(sections)
  return (
    <Sheet open={open} onClose={onClose} label="구역 위치">
      <Box
        component="span"
        aria-hidden="true"
        sx={{
          alignSelf: 'center',
          width: '36px',
          height: '4px',
          borderRadius: token('--r-pill'),
          backgroundColor: token('--c-line-strong'),
          marginTop: `calc(${token('--stack-s')} * -1)`,
        }}
      />
      <Box component="p" sx={{ margin: 0, fontSize: token('--fs-h2'), fontWeight: Number(token('--fw-h2')), lineHeight: Number(token('--lh-h2')) }}>
        구역 위치
      </Box>
      {bundles.length > 0 && bundles.length <= MAX_MAP_BUNDLES && <ZoneMap venue={venue} bundles={bundles} />}
      <Box>
        {bundles.map((bundle, index) => (
          <Row
            key={`${bundle.area}-${index}`}
            lead={<Swatch index={index} />}
            title={bundleTitle(bundle)}
            sub={bundle.note}
            trail={
              <Box component="span" sx={{ color: token('--c-text-1'), fontWeight: 700 }}>
                {priceRange(bundle.minPrice, bundle.maxPrice)}
              </Box>
            }
          />
        ))}
      </Box>
      <Box
        component="button"
        type="button"
        onClick={onClose}
        sx={{
          width: '100%',
          height: token('--h-btn'),
          padding: `0 ${token('--block-pad')}`,
          border: 0,
          borderRadius: token('--r-m'),
          fontFamily: 'inherit',
          fontSize: token('--fs-strong'),
          fontWeight: Number(token('--fw-strong')),
          cursor: 'pointer',
          backgroundColor: token('--c-brand'),
          color: token('--c-on-brand'),
          '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
        }}
      >
        확인
      </Box>
    </Sheet>
  )
}
