import type { SectionSummary } from '../api/performances'
import { isBlankDisplay } from '../components/ui/text'

export interface ZoneBundle {
  /** 묶음 이름 */
  area: string
  /** 묶음 설명(없으면 undefined) */
  note?: string
  /** 묶음에 든 등급, stageOrder 순 */
  grades: string[]
  minPrice: number
  maxPrice: number
}

/** 무대에서 가까운 순 — 같은 값은 응답 순서를 지킨다 */
export function sortByStage(sections: SectionSummary[]): SectionSummary[] {
  return sections
    .map((section, index) => ({ section, index }))
    .sort((a, b) => (a.section.stageOrder ?? 0) - (b.section.stageOrder ?? 0) || a.index - b.index)
    .map(({ section }) => section)
}

/** stageOrder 순으로 이어진 같은 area 를 한 묶음으로 */
export function groupZones(sections: SectionSummary[]): ZoneBundle[] {
  const bundles: ZoneBundle[] = []
  for (const section of sortByStage(sections)) {
    const area = section.area ?? section.grade
    const last = bundles[bundles.length - 1]
    if (last && last.area === area) {
      last.grades.push(section.grade)
      last.minPrice = Math.min(last.minPrice, section.price)
      last.maxPrice = Math.max(last.maxPrice, section.price)
      if (last.note === undefined && !isBlankDisplay(section.areaNote)) last.note = section.areaNote
    } else {
      bundles.push({
        area,
        note: isBlankDisplay(section.areaNote) ? undefined : section.areaNote,
        grades: [section.grade],
        minPrice: section.price,
        maxPrice: section.price,
      })
    }
  }
  return bundles
}

/** 「플로어 F1~F4」 — 등급이 둘 이상이면 처음~끝을 붙인다 */
export function bundleTitle(bundle: ZoneBundle): string {
  return bundle.grades.length >= 2 ? `${bundle.area} ${bundle.grades[0]}~${bundle.grades[bundle.grades.length - 1]}` : bundle.area
}

const won = (price: number) => `${price.toLocaleString('ko-KR')}원`

/** 「110,000~150,000원」 — 한 가격이면 「150,000원」 */
export function priceRange(minPrice: number, maxPrice: number): string {
  return minPrice === maxPrice ? won(minPrice) : `${won(minPrice).slice(0, -1)}~${won(maxPrice)}`
}

/** 「구역 위치 보기」 행의 아랫줄 — 첫 묶음의 세부 구역이 둘 이상이면 그 앞·뒤, 아니면 전체 구역의 앞·뒤 */
export function zoneOverview(sections: SectionSummary[]): string {
  const bundles = groupZones(sections)
  const first = bundles[0]
  if (first && first.grades.length >= 2) {
    return `${first.grades[0]} 무대 바로 앞 → ${first.grades[first.grades.length - 1]} 맨 뒤`
  }
  const sorted = sortByStage(sections)
  return `${sorted[0].grade} 무대 바로 앞 → ${sorted[sorted.length - 1].grade} 맨 뒤`
}
