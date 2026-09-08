import type { PerformanceStatus } from '../api/performances'

export function StatusBadge({ status }: { status: PerformanceStatus }): never {
  void status
  throw new Error('Not implemented')
}
