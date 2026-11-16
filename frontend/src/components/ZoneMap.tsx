import Box from '@mui/material/Box'
import { token } from '../theme/tokens'
import type { ZoneBundle } from '../utils/zones'

// 시안 zone-map.html 의 배치도 — 무대 + 첫 묶음(등급을 무대 앞에서 뒤로 쌓기) + 나머지 묶음(바깥 고리).
// 좌표는 SVG viewBox 안의 값이다(CSS 치수가 아니다). 시안의 한 공연(고리 2개)에 맞춘 손 좌표를 묶음 수에 따라 일반화했다.
const VIEW_W = 320
const MID = VIEW_W / 2
const STAGE_X = 96
const STAGE_W = 128
const STAGE_H = 26
const TOP = 40
const ROW_H = 34
const ROW_GAP = 4
const RING_INNER = 8
const BAND = 34
const LABEL_SIZE = 13
const RING_LABEL_SIZE = 12
const MAX_LABEL = 12

/** 12자를 넘는 이름은 앞 11자 + 「…」 — 배치도 칸에 들어가게 (전체 이름은 묶음 행과 접근성 이름에 있다) */
export function shortLabel(name: string): string {
  const chars = Array.from(name)
  return chars.length > MAX_LABEL ? `${chars.slice(0, MAX_LABEL - 1).join('')}…` : name
}

/** 배치도의 접근성 이름 — 첫 묶음 등급을 앞에서 뒤로, 바깥 묶음 이름을 안에서 바깥으로 */
export function zoneMapLabel(venue: string, bundles: ZoneBundle[]): string {
  const [first, ...outer] = bundles
  const head = `${venue} 구역 위치. 무대 바로 앞부터 ${first.grades.join(', ')} 순서`
  return outer.length === 0 ? `${head}예요` : `${head}이고, 바깥으로 ${outer.map((b) => b.area).join(', ')} 순서예요`
}

export interface ZoneMapProps {
  venue: string
  /** 4개 이하 — 5개 이상이면 배치도를 그리지 않는다(호출하는 쪽이 판단) */
  bundles: ZoneBundle[]
}

export function ZoneMap({ venue, bundles }: ZoneMapProps) {
  const [floor, ...rings] = bundles
  const count = floor.grades.length
  const floorBottom = TOP + count * (ROW_H + ROW_GAP) - ROW_GAP
  const step = rings.length === 0 ? 0 : Math.min(44, Math.floor(86 / rings.length))

  // 고리 k(1부터): 바깥 왼쪽 x, 안쪽 아래 y, 바깥 아래 y
  const geometry = rings.map((_, i) => {
    const k = i + 1
    const outerLeft = STAGE_X - step * k
    const innerLeft = STAGE_X - step * (k - 1) - RING_INNER
    return { outerLeft, innerLeft }
  })
  let cursor = floorBottom + 6
  const bands = rings.map(() => {
    const innerBottom = cursor
    const outerBottom = innerBottom + BAND
    cursor = outerBottom + RING_INNER
    return { innerBottom, outerBottom }
  })
  const height = rings.length === 0 ? floorBottom + 8 : bands[bands.length - 1].outerBottom + 6

  const ringStyle = (k: number) =>
    k === 1
      ? { fill: token('--c-bg'), stroke: token('--c-line-strong'), strokeWidth: 1.5 }
      : { fill: token('--c-surface'), stroke: token('--c-line-strong'), strokeWidth: 1.5, strokeDasharray: '4 3' }

  return (
    <Box
      component="svg"
      viewBox={`0 0 ${VIEW_W} ${height}`}
      role="img"
      aria-label={zoneMapLabel(venue, bundles)}
      fontFamily="inherit"
      textAnchor="middle"
      sx={{ width: '100%', height: 'auto', maxHeight: '280px', color: token('--c-text-1') }}
    >
      {[...rings].reverse().map((_, reversed) => {
        const i = rings.length - 1 - reversed
        const { outerLeft, innerLeft } = geometry[i]
        const { innerBottom, outerBottom } = bands[i]
        const innerRight = VIEW_W - innerLeft
        const outerRight = VIEW_W - outerLeft
        return (
          <path
            key={`ring-${i}`}
            style={ringStyle(i + 1)}
            d={`M${outerLeft} ${TOP} H${innerLeft} V${innerBottom} H${innerRight} V${TOP} H${outerRight} V${outerBottom} H${outerLeft} Z`}
          />
        )
      })}
      <rect style={{ fill: token('--c-text-1') }} x={STAGE_X} y={6} width={STAGE_W} height={STAGE_H} rx={6} />
      {floor.grades.map((grade, i) => (
        <rect
          key={`floor-${i}`}
          style={{ fill: token('--c-brand-tint'), stroke: token('--c-brand-line'), strokeWidth: 1.5 }}
          x={STAGE_X}
          y={TOP + i * (ROW_H + ROW_GAP)}
          width={STAGE_W}
          height={ROW_H}
          rx={4}
          data-grade={grade}
        />
      ))}
      <text style={{ fill: token('--c-surface'), fontWeight: 700 }} x={MID} y={24} fontSize={LABEL_SIZE}>
        무대
      </text>
      {floor.grades.map((grade, i) => {
        const suffix = count >= 2 ? (i === 0 ? ' · 무대 바로 앞' : i === count - 1 ? ' · 맨 뒤' : '') : ''
        return (
          <text
            key={`floor-label-${i}`}
            style={{ fill: token('--c-brand-tint-text'), fontWeight: 700 }}
            x={MID}
            y={TOP + i * (ROW_H + ROW_GAP) + 22}
            fontSize={LABEL_SIZE}
          >
            {`${shortLabel(grade)}${suffix}`}
          </text>
        )
      })}
      {rings.map((bundle, i) => (
        <text
          key={`ring-label-${i}`}
          style={{ fill: token('--c-text-2'), fontWeight: 600 }}
          x={MID}
          y={bands[i].innerBottom + BAND / 2 + 5}
          fontSize={RING_LABEL_SIZE}
        >
          {shortLabel(bundle.area)}
        </text>
      ))}
    </Box>
  )
}
