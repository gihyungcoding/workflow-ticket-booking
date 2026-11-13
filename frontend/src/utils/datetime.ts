export function toDatetimeLocalInput(iso: string): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function fromDatetimeLocalInput(value: string): string {
  if (!value) {
    return value
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toISOString()
}

/** 한국 시간 "11월 21일(토) 오후 7:00" */
export function formatDateTime(_iso: string): string {
  throw new Error('Not implemented')
}

/** 한국 시간 "오후 5:55:28" */
export function formatClock(_iso: string): string {
  throw new Error('Not implemented')
}

/** 남은 시간(ms) → "3일 4시간" · "2시간 15분" · "4분 32초" · "32초" */
export function formatCountdown(_remainingMs: number): string {
  throw new Error('Not implemented')
}
