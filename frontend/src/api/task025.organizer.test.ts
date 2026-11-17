import { getMyPerformances } from './organizer'
import { LIST_KEY, LIST_PATH, callsTo, listUrls, orgBody, serve } from '../test/task025'

// TASK-025 시나리오 SC-14 — 내 공연 목록 호출 인자.
// SoT: workflow_design/05_scenario/SCENARIO_TASK-025.md
// 거부 행은 「요청이 한 번도 나가지 않았다」를 먼저 보고, 그다음 정상 호출이 성공하는 것을 본다 —
// 구현이 없어 던지는 것만으로 「거부된다」가 우연히 통과하지 않게 한다(TASK-021 SC-28 행 l 의 교훈).

type Row = { row: string; filter: unknown; page: unknown; query?: string }

const ROWS: Row[] = [
  { row: 'a', filter: 'XYZ', page: undefined },
  { row: 'b 소문자', filter: 'all', page: undefined },
  { row: 'c 빈 문자열', filter: '', page: undefined },
  { row: 'd 뒤 공백', filter: 'ALL ', page: undefined },
  { row: 'e 뒤 개행', filter: 'DRAFT\n', page: undefined },
  { row: 'f page -1', filter: 'ALL', page: -1 },
  { row: 'g page 1.5', filter: 'ALL', page: 1.5 },
  { row: 'h page NaN', filter: 'ALL', page: Number.NaN },
  { row: 'i page Infinity', filter: 'ALL', page: Number.POSITIVE_INFINITY },
  { row: "j page '1'", filter: 'ALL', page: '1' },
  { row: 'k page 2**53', filter: 'ALL', page: Number.MAX_SAFE_INTEGER + 1 },
  { row: 'l page 1e400', filter: 'ALL', page: 1e400 },
  { row: 'm1 생략', filter: undefined, page: undefined, query: '' },
  { row: 'm2 ALL·0', filter: 'ALL', page: 0, query: '' },
  { row: 'n DRAFT·1', filter: 'DRAFT', page: 1, query: '?filter=DRAFT&page=1' },
  { row: 'o ALL·2', filter: 'ALL', page: 2, query: '?page=2' },
  { row: 'p OPEN', filter: 'OPEN', page: undefined, query: '?filter=OPEN' },
]

describe('TASK-025 F5·F6·F7·F10 — 호출 인자', () => {
  test.each(ROWS)('SC-14 (error) 호출 인자가 틀리면 요청을 보내지 않고 거부한다 — $row', async ({ filter, page, query }) => {
    // Given 주최자 로그인 상태(요청이 나갈 수 있는 상태)
    serve({ list: () => new Response(JSON.stringify(orgBody([])), { status: 200 }) })

    if (query !== undefined) {
      // When 올바른 인자로 부른다
      await getMyPerformances(filter as never, page as never)
      // Then 요청 주소는 표의 것이다
      expect(listUrls()).toEqual([`${LIST_PATH}${query}`])
      return
    }

    // When 틀린 인자로 부른다
    const call = getMyPerformances(filter as never, page as never)

    // Then 거부된다
    await expect(call).rejects.toThrow()
    // And fetch 가 한 번도 불리지 않았다
    expect(callsTo(LIST_KEY)).toHaveLength(0)
    // And (구현이 없어 던지는 것과 구별) 정상 인자로는 성공하고 요청이 1번 나간다
    await expect(getMyPerformances('ALL')).resolves.toMatchObject({ items: [] })
    expect(callsTo(LIST_KEY)).toHaveLength(1)
  })
})
