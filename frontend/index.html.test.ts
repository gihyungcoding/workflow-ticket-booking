import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const html = readFileSync(resolve(here, './index.html'), 'utf-8')

describe('index.html', () => {
  test('SC-05 (happy) index.html이 한국어 lang·실제 title·웹폰트 로드 지시를 갖는다', () => {
    // Given
    // frontend/index.html 파일이 있다

    // When
    // 그 문서의 <html> 태그·<title>·<link> 목록을 확인한다
    const linkTags = html.match(/<link\b[^>]*>/g) ?? []
    // TASK-017: 서체가 Pretendard 한 가지로 바뀌어 Google Fonts 대신 Pretendard CSS 를 본다 (design.md §4, ADR-0022)
    const fontLink = linkTags.find(
      (tag) => tag.includes('orioncactus/pretendard') && tag.includes('rel="stylesheet"'),
    )

    // Then
    // lang 속성이 'ko' 다
    expect(html).toContain('lang="ko"')

    // <title> 내용이 'frontend' 가 아니다
    expect(html).not.toContain('<title>frontend</title>')

    // Pretendard CSS 를 가리키는 <link rel="stylesheet"> 가 있다
    expect(fontLink).toBeDefined()
    expect(fontLink).toContain('rel="stylesheet"')

    // 그 <link> 는 정적 서브셋이 아닌 전체 정적 CSS 다 (font-display:swap 은 그 CSS 안에 선언돼 있다 — URL 로는 확인할 수 없다)
    expect(fontLink).toContain('/dist/web/static/pretendard.min.css')
  })
})
