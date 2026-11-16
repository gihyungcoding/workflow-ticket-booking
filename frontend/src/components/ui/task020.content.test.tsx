import type { ReactElement } from 'react'
import { Block, Row } from './index'
import { renderWithTheme } from '../../test/task017'

// TASK-020 Phase 3 자체 리뷰(RC-1 변종) — 표시 문구의 「없음」 판정은 한 곳(hasContent)이다.
// Block title·note 와 Row lead·trail 도 공백 문자열이면 값이 없는 것과 같은 모습이어야 한다 (SC-42 와 같은 규칙).
const html = (el: ReactElement) => renderWithTheme(el).container.innerHTML
const BLANKS = ['', '   ', '　', ' ', '​', '\u0085']

describe('TASK-020 hasContent 호출부', () => {
  test.each(BLANKS.map((v) => [JSON.stringify(v), v]))('공백이면 값이 없는 것과 같다 — %s', (_label, value) => {
    expect(html(<Block title={value}>내용</Block>)).toBe(html(<Block>내용</Block>))
    expect(html(<Block note={value}>내용</Block>)).toBe(html(<Block>내용</Block>))
    expect(html(<Row title="제목" lead={value} />)).toBe(html(<Row title="제목" />))
    expect(html(<Row title="제목" trail={value} />)).toBe(html(<Row title="제목" />))
  })

  test('글자가 있으면 그린다', () => {
    expect(html(<Block title="제목" note="설명">내용</Block>)).toContain('설명')
    expect(html(<Row title="제목" lead="앞" trail="뒤" />)).toContain('뒤')
  })
})
