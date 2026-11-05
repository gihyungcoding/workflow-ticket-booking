import indexHtml from '../index.html?raw'

// SoT: workflow_design/05_scenario/SCENARIO_TASK-017.md
// 앱 tsconfig 에 node 타입이 없어(TASK-015 deploy.test.ts 와 같은 사정) node 모듈은 타입 없이 동적으로 불러온다.

/** 저장소 루트 — vitest 의 URL 해석은 /@fs 접두를 붙이므로 문자열로 자른다 */
const REPO = decodeURIComponent(import.meta.url.replace('file://', '').split('/frontend/')[0])
const CHILD_PROCESS: string = 'node:child_process'
const PRETENDARD = 'https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css'

interface ConstraintResult {
  id: string
  violations?: { file?: string; path?: string }[]
}

describe('TASK-017 정적 검사', () => {
  test('TASK-017 SC-10 (happy) 테마 밖 색·매직 치수가 없다', async () => {
    // Given / When check_architecture.py --json 을 실행한다
    const { execFileSync } = (await import(/* @vite-ignore */ CHILD_PROCESS)) as {
      execFileSync: (cmd: string, args: string[], options: { cwd: string; encoding: 'utf8' }) => string
    }
    const output = execFileSync('python3', ['scripts/check_architecture.py', '--json'], { cwd: REPO, encoding: 'utf8' })
    const report = JSON.parse(output) as { results: ConstraintResult[]; no_target: string[] }
    const byId = (id: string) => report.results.find((r) => r.id === id)
    const files = (id: string) => (byId(id)?.violations ?? []).map((v) => v.file ?? v.path ?? '')

    // Then DESIGN-001·002 위반 0건
    expect(files('DESIGN-001')).toEqual([])
    expect(files('DESIGN-002')).toEqual([])
    // And DESIGN-003 은 기존 3건뿐이고 theme·components 위반 0건
    expect(files('DESIGN-003').sort()).toEqual([
      'frontend/src/pages/PerformanceDetailPage.tsx',
      'frontend/src/pages/PerformanceEditPage.tsx',
      'frontend/src/pages/PerformanceListPage.tsx',
    ])
    expect(files('DESIGN-003').filter((f) => /frontend\/src\/(theme|components)\//.test(f))).toEqual([])
    // And 세 제약 모두 no_target 에 없다
    expect(report.no_target).not.toContain('DESIGN-001')
    expect(report.no_target).not.toContain('DESIGN-002')
    expect(report.no_target).not.toContain('DESIGN-003')
  })

  test('TASK-017 SC-11 (happy) Pretendard 를 불러오고 옛 서체를 불러오지 않는다', () => {
    // Given / When index.html 의 스타일시트 링크를 읽는다
    const html = indexHtml
    const links = [...html.matchAll(/<link[^>]+rel="stylesheet"[^>]*href="([^"]+)"/g)].map((m) => m[1])

    // Then Pretendard 스타일시트를 불러온다
    expect(links).toContain(PRETENDARD)
    // And Noto Serif KR·IBM Plex Sans KR 을 불러오지 않는다
    expect(html).not.toMatch(/Noto\+?\s?Serif|IBM\+?\s?Plex/)
  })
})
