import type { ReactElement } from 'react'
import { Hero, Row, AppBar } from './index'
import { renderWithTheme } from '../../test/task017'

// TASK-020 시나리오 SC-42 — 표시 값의 공백 판정이 화면 어디서나 같다 (ADR-0027, Phase 4 리뷰 RC-1).
// 범위: AppBar title · Hero eyebrow·lead · Row sub. 공백이면 그 줄·요소를 그리지 않는다 — 값이 없는 것과 같은 모습이다.
// SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md

const BLANKS: Array<[string, string, string]> = [
  ['a', "''", ''],
  ['b', "'   '", '   '],
  ['c', '전각 공백', '　'],
  ['d', 'NBSP', ' '],
  ['e', '제로폭 공백', '​'],
  ['f', 'NEL', '\u0085'],
]
const LETTERS: Array<[string, string, string]> = [
  ['g', 'U+FEFF', '﻿'],
  ['h', 'U+200D', '‍'],
  ['h', 'U+2800', '⠀'],
  ['h', 'U+3164', 'ㅤ'],
]

type Target = [string, (value: string | undefined) => ReactElement]
const TARGETS: Target[] = [
  ['AppBar title', (v) => <AppBar title={v} onBack={() => {}} />],
  ['Hero eyebrow', (v) => <Hero eyebrow={v} title="제목" />],
  ['Hero lead', (v) => <Hero title="제목" lead={v} />],
  ['Row sub', (v) => <Row title="제목" sub={v} />],
]

const html = (el: ReactElement) => renderWithTheme(el).container.innerHTML

const ROWS: Array<[string, string, string, boolean]> = [
  ...BLANKS.map(([row, label, value]): [string, string, string, boolean] => [row, label, value, true]),
  ...LETTERS.map(([row, label, value]): [string, string, string, boolean] => [row, label, value, false]),
  ['i', '앞뒤 공백 + 글자 \' 공연 \'', ' 공연 ', false],
]

describe('TASK-020 SC-42 공백 판정은 하나다', () => {
  test.each(TARGETS.flatMap(([name, make]) => ROWS.map(([row, label, value, blank]) => [name, row, label, value, blank, make] as const)))(
    'SC-42 (boundary) 표시 값의 공백 판정이 화면 어디서나 같다 — %s %s %s',
    (_name, _row, _label, value, blank, make) => {
      // Given 부품이 표의 값을 받는다 / When 그린다
      const shown = html(make(value))
      const without = html(make(undefined))
      // Then 공백 행은 값을 주지 않았을 때와 같은 모습이다 (빈 h1·p·div 가 없다)
      if (blank) expect(shown).toBe(without)
      // And 글자가 섞인 행은 그 글자가 그대로 보인다
      else {
        expect(shown).not.toBe(without)
        expect(shown).toContain(value.trim() || value)
      }
    },
  )
})
