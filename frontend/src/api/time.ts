export interface ServerTime {
  now: string
}

export async function getServerTime(): Promise<ServerTime> {
  throw new Error('Not implemented')
}
