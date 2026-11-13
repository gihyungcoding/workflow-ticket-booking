import type { SectionSummary } from '../api/performances'

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

export function groupZones(_sections: SectionSummary[]): ZoneBundle[] {
  throw new Error('Not implemented')
}
