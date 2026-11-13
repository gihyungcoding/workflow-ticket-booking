import type { SectionSummary } from '../api/performances'

export interface ZoneMapSheetProps {
  open: boolean
  venue: string
  sections: SectionSummary[]
  onClose: () => void
}

export function ZoneMapSheet(_props: ZoneMapSheetProps): never {
  throw new Error('Not implemented')
}
