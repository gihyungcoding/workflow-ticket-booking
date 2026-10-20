// TASK-015 시나리오 SC-10, SC-11. SoT: workflow_design/05_scenario/SCENARIO_TASK-015.md
// Vite 의 import.meta.glob 으로 프로젝트 파일을 읽는다 (앱 tsconfig 에 node 타입이 없다).

describe('배포 조건', () => {
  test('test_sc10_빌드_입력에_index_html_이_있고_404_html_은_없다', () => {
    // Given 정적 자산 폴더(frontend/public)와 진입 문서(frontend/index.html)가 있다
    const indexHtml = import.meta.glob('/index.html', { query: '?raw', eager: true })
    const notFoundHtml = import.meta.glob('/public/404.html', { query: '?raw', eager: true })

    // When SPA 조건을 확인한다
    const indexFiles = Object.keys(indexHtml)
    const notFoundFiles = Object.keys(notFoundHtml)

    // Then frontend/index.html 이 존재한다
    expect(indexFiles).toEqual(['/index.html'])
    // And frontend/public/404.html 이 존재하지 않는다
    expect(notFoundFiles).toEqual([])
  })

  test('test_sc11_소스에_배포_주소가_하드코딩돼_있지_않다', () => {
    // Given frontend/src 의 .ts·.tsx 파일 전체
    const sources = import.meta.glob('/src/**/*.{ts,tsx}', {
      query: '?raw',
      import: 'default',
      eager: true,
    }) as Record<string, string>
    const hosts = ['onrender\\.com', 'pages\\.dev', 'up\\.railway\\.app', 'fly\\.dev', 'vercel\\.app', 'netlify\\.app']
    const forbidden = new RegExp(`https?://[A-Za-z0-9.-]+\\.(${hosts.join('|')})`)

    // When DEPLOY-001 의 금지 패턴으로 검색한다
    const offenders = Object.entries(sources)
      .filter(([, content]) => forbidden.test(content))
      .map(([path]) => path)

    // Then 일치하는 파일이 0개이다
    expect(Object.keys(sources).length).toBeGreaterThan(10)
    expect(offenders).toEqual([])
  })
})
