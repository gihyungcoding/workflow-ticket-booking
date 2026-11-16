import { isBlankDisplay } from './text'

// TASK-020 Phase 3 자체 리뷰(RC-1)로 추가 — ADR-0027: 공백은 유니코드 공백(Zs·Zl·Zp)·탭·줄바꿈·U+0085·U+200B,
// 서식 문자(U+FEFF·U+200D)·U+2800·U+3164 는 글자로 받는다. 서버 상수 `[^\p{Z}\s​]`(유니코드 모드)와 같은 집합이다.
describe('TASK-020 isBlankDisplay (ADR-0027)', () => {
  test.each([
    ['빈 문자열', ''],
    ['ASCII 공백', '   '],
    ['탭·줄바꿈', '\t\n\r\v\f'],
    ['NBSP', ' '],
    ['전각 공백', '　'],
    ['U+2003', ' '],
    ['줄 구분자 U+2028', ' '],
    ['제로폭 공백 U+200B', '​'],
    ['NEL U+0085', '\u0085'],
    ['여러 공백 종류의 섞임', '  ​　'],
    ['문자열이 아닌 값', null],
  ])('공백으로 본다 — %s', (_label, value) => {
    expect(isBlankDisplay(value)).toBe(true)
  })

  test.each([
    ['BOM U+FEFF(서식 문자)', '﻿'],
    ['U+200D(이모지 연결자)', '‍'],
    ['U+2800(점자 빈칸)', '⠀'],
    ['U+3164(한글 채움)', 'ㅤ'],
    ['글자가 하나 있다', ' a '],
  ])('글자로 본다 — %s', (_label, value) => {
    expect(isBlankDisplay(value)).toBe(false)
  })
})
