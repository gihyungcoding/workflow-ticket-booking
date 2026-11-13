export type ServerClockStatus = 'measuring' | 'ready' | 'failed'

export interface ServerClock {
  status: ServerClockStatus
  /** 기준 시각(서버 시각, ms). status 가 ready 일 때만 의미가 있다 */
  now: () => number
  /** 다시 맞추기 */
  retry: () => void
}

/** enabled 가 false 면 측정하지 않는다(예정이 아닌 공연) */
export function useServerClock(_options: { enabled: boolean }): ServerClock {
  throw new Error('Not implemented')
}
