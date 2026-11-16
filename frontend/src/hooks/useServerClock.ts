import { useCallback, useEffect, useRef, useState } from 'react'
import { getServerTime } from '../api/time'

export type ServerClockStatus = 'idle' | 'measuring' | 'ready' | 'failed'

export interface ServerClock {
  status: ServerClockStatus
  /** 기준 시각(서버 시각, ms). status 가 ready 일 때만 값이 있다 */
  now: number | null
  /** 다시 맞추기 */
  retry: () => void
}

// setTimeout 은 2^31-1ms(약 24.8일)를 넘기면 바로 실행된다 — 먼 시각은 나눠서 기다린다
const MAX_TIMER_MS = 2_147_483_647
const SAMPLES = 3
// 표본 하나가 이 시간 안에 오지 않으면 실패로 보고 요청을 끊는다 — 서버가 응답을 붙들어도 안내·재시도로 간다(TASK-020 SC-40)
const SAMPLE_TIMEOUT_MS = 5000

/**
 * 서버 시각과 기기 시각의 차이(오프셋)로 기준 시각을 센다 (ADR-0024) — 오프셋 계산은 이 훅 하나에 둔다.
 * 왕복이 가장 짧은 표본의 오프셋을 쓴다. 측정 도중 화면이 바뀌거나 재측정이 겹치면 오래된 결과는 버린다(ADR-0018).
 * deadline(ms)이 있으면 그 순간에 맞춰 한 번 더 그리게 한다 — 1초 주기에 기대면 기기마다 최대 1초가 어긋난다.
 */
export function useServerClock(options: { enabled: boolean; deadline?: number }): ServerClock {
  const { enabled, deadline } = options
  const [offset, setOffset] = useState<number | null>(null)
  const [failed, setFailed] = useState(false)
  const [, bump] = useState(0)
  const generation = useRef(0)
  const offsetRef = useRef<number | null>(null)
  const inFlight = useRef<AbortController | null>(null)

  // 진행 중인 측정을 오래된 것으로 만든다 — 결과는 버리고 요청도 끊는다
  const invalidate = useCallback(() => {
    generation.current += 1
    inFlight.current?.abort()
    inFlight.current = null
  }, [])

  const measure = useCallback(async (showProgress: boolean) => {
    invalidate()
    const mine = generation.current
    if (showProgress) setFailed(false)
    let best: { rtt: number; offset: number } | null = null
    for (let i = 0; i < SAMPLES; i += 1) {
      if (mine !== generation.current) return
      const sentAt = Date.now()
      const controller = new AbortController()
      inFlight.current = controller
      const timer = setTimeout(() => controller.abort(), SAMPLE_TIMEOUT_MS)
      try {
        const { now } = await getServerTime(controller.signal)
        const receivedAt = Date.now()
        const rtt = receivedAt - sentAt
        const sample = { rtt, offset: Date.parse(now) + rtt / 2 - receivedAt }
        if (best === null || sample.rtt < best.rtt) best = sample
      } catch {
        // 이 표본은 버린다 — 성공한 표본만으로 센다 (시간 제한·중단도 여기로 온다)
      } finally {
        clearTimeout(timer)
        if (inFlight.current === controller) inFlight.current = null
      }
    }
    if (mine !== generation.current) return
    if (best !== null) {
      offsetRef.current = best.offset
      setOffset(best.offset)
      setFailed(false)
      // 오프셋이 그대로여도 기준 시각은 그 사이 흘렀다 — 다시 그리게 해 1초 주기를 기다리지 않는다
      bump((n) => n + 1)
    } else if (offsetRef.current === null) {
      // 처음부터 못 맞췄다 — 기기 시계로 대신하지 않는다. 이미 맞춘 적이 있으면 그 오프셋을 유지한다
      setFailed(true)
    }
  }, [invalidate])

  // 측정: 화면에 들어올 때 한 번, 탭이 다시 보일 때마다 다시
  useEffect(() => {
    if (!enabled) return
    void measure(true)
    const onVisible = () => {
      if (document.visibilityState !== 'visible') return
      bump((n) => n + 1)
      void measure(false)
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      invalidate()
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [enabled, measure, invalidate])

  // 표시: 1초마다, 그리고 deadline 이 되는 순간에 다시 그린다
  useEffect(() => {
    if (!enabled || offset === null) return
    const serverNow = () => Date.now() + offset
    const timers: { interval?: ReturnType<typeof setInterval>; timeout?: ReturnType<typeof setTimeout> } = {}
    const done = () => deadline !== undefined && serverNow() >= deadline
    timers.interval = setInterval(() => {
      bump((n) => n + 1)
      if (done()) clearInterval(timers.interval)
    }, 1000)
    const schedule = () => {
      if (deadline === undefined) return
      const delay = deadline - serverNow()
      if (delay <= 0) return
      timers.timeout = setTimeout(() => {
        bump((n) => n + 1)
        schedule()
      }, Math.min(delay, MAX_TIMER_MS))
    }
    schedule()
    return () => {
      clearInterval(timers.interval)
      clearTimeout(timers.timeout)
    }
  }, [enabled, offset, deadline])

  const retry = useCallback(() => {
    void measure(true)
  }, [measure])

  let status: ServerClockStatus = 'idle'
  if (enabled) status = offset !== null ? 'ready' : failed ? 'failed' : 'measuring'
  return { status, now: enabled && offset !== null ? Date.now() + offset : null, retry }
}
