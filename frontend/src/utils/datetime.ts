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

const KST = 'Asia/Seoul'

// 기기 시간대와 무관하게 한국 시간으로 표기한다 — 기준 시각이 한국 시간이다 (기능 문서 §1-3)
const dateTimeFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: KST,
  month: 'numeric',
  day: 'numeric',
  weekday: 'short',
  hour: 'numeric',
  minute: '2-digit',
  hourCycle: 'h12',
})
const clockFormat = new Intl.DateTimeFormat('ko-KR', {
  timeZone: KST,
  hour: 'numeric',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h12',
})
const dayFormat = new Intl.DateTimeFormat('en-CA', { timeZone: KST })

function partsOf(format: Intl.DateTimeFormat, time: number | string): Record<string, string> {
  const parts: Record<string, string> = {}
  for (const part of format.formatToParts(new Date(time))) parts[part.type] = part.value
  // ICU 버전에 따라 「오전/오후」 대신 「AM/PM」 으로 온다 — 표기를 고정한다
  if (parts.dayPeriod !== undefined) parts.dayPeriod = /^(AM|오전)$/i.test(parts.dayPeriod) ? '오전' : '오후'
  return parts
}

/** 한국 시간 "11월 21일(토) 오후 7:00" */
export function formatDateTime(iso: string): string {
  const p = partsOf(dateTimeFormat, iso)
  return `${p.month}월 ${p.day}일(${p.weekday}) ${p.dayPeriod} ${p.hour}:${p.minute}`
}

/** 한국 시간 "오후 5:55:28" */
export function formatClock(iso: string): string {
  const p = partsOf(clockFormat, iso)
  return `${p.dayPeriod} ${p.hour}:${p.minute}:${p.second}`
}

/** 한국 시간 "오후 6:00" — 날짜 없이 */
export function formatTimeOfDay(iso: string): string {
  const p = partsOf(dateTimeFormat, iso)
  return `${p.dayPeriod} ${p.hour}:${p.minute}`
}

/** 한국 날짜 "2026-11-11" — 같은 날인지 비교한다 */
export function kstDay(time: number | string): string {
  return dayFormat.format(new Date(time))
}

/**
 * 오픈 일시 표기 — 한국 날짜가 today(`kstDay`)와 같으면 「오늘 오후 6:00」, 아니면 「11월 14일(토) 오후 2:00」.
 * 머리글과 카드가 같은 비교를 쓴다. today 를 문자열로 받아 카드가 매초 다시 그려지지 않게 한다(날짜가 바뀔 때만 달라진다).
 */
export function formatWhen(iso: string, today: string): string {
  return kstDay(iso) === today ? `오늘 ${formatTimeOfDay(iso)}` : formatDateTime(iso)
}

/** 남은 시간(ms) → "3일 4시간" · "2시간 15분" · "4분 32초" · "32초". 초 이하는 버린다 */
export function formatCountdown(remainingMs: number): string {
  const total = Math.max(0, Math.floor(remainingMs / 1000))
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  if (days >= 1) return `${days}일 ${hours}시간`
  if (hours >= 1) return `${hours}시간 ${minutes}분`
  if (minutes >= 1) return `${minutes}분 ${seconds}초`
  return `${seconds}초`
}

/**
 * 서버가 주는 시각(ISO-8601 순간 — `Z` 또는 `±hh:mm` 이 붙은 `YYYY-MM-DDThh:mm:ss`)인가.
 * `Date.parse` 는 '1'·'2026'·날짜만·오프셋 없는 시각도 받는다 — 오프셋이 없으면 기기 시간대에 따라 다른 순간이 된다.
 */
const INSTANT = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(?:Z|[+-](\d{2}):(\d{2}))$/
export function isInstant(value: unknown): value is string {
  if (typeof value !== 'string') return false
  const m = INSTANT.exec(value)
  if (m === null) return false
  const [year, month, day, hour, minute, second] = m.slice(1, 7).map(Number)
  // Date.parse 는 2026-02-30 을 3월 2일로, T24:00:00 을 다음 날 0시로 읽는다 — 달력에 있는 값인지 되돌려 대조한다
  const date = new Date(0)
  date.setUTCFullYear(year, month - 1, day) // Date.UTC 는 연도 0~99 를 1900 년대로 읽는다
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return false
  if (hour > 23 || minute > 59 || second > 59) return false
  if (m[7] !== undefined && (Number(m[7]) > 23 || Number(m[8]) > 59)) return false
  return !Number.isNaN(Date.parse(value))
}
