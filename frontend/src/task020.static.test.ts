// SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md — SC-31
// 앱 tsconfig 에 node 타입이 없어(TASK-015 deploy.test.ts·task017.static.test.ts 와 같은 사정) node 모듈은 타입 없이 동적으로 불러온다.

/** 저장소 루트 — vitest 의 URL 해석은 /@fs 접두를 붙이므로 문자열로 자른다 */
const REPO = decodeURIComponent(import.meta.url.replace('file://', '').split('/frontend/')[0])
const CHILD_PROCESS: string = 'node:child_process'

interface ConstraintResult {
  id: string
  violations?: { file?: string; path?: string }[]
}

describe('TASK-020 정적 검사', () => {
  test('SC-31 (regression) 디자인·배포 제약이 이번 변경으로 새로 어겨지지 않는다', async () => {
    // Given 이 태스크의 변경이 frontend/src 에 반영돼 있다
    const { execFileSync } = (await import(/* @vite-ignore */ CHILD_PROCESS)) as {
      execFileSync: (cmd: string, args: string[], options: { cwd: string; encoding: 'utf8' }) => string
    }

    // When python3 scripts/check_architecture.py --json 을 실행한다
    const output = execFileSync('python3', ['scripts/check_architecture.py', '--json'], { cwd: REPO, encoding: 'utf8' })
    const report = JSON.parse(output) as { results: ConstraintResult[]; no_target: string[] }
    const byId = (id: string) => report.results.find((r) => r.id === id)
    const files = (id: string) => (byId(id)?.violations ?? []).map((v) => v.file ?? v.path ?? '')

    // Then DESIGN-001·DESIGN-002·DEPLOY-001 위반이 0건이다
    expect(files('DESIGN-001')).toEqual([])
    expect(files('DESIGN-002')).toEqual([])
    expect(files('DEPLOY-001')).toEqual([])
    // And DESIGN-003 위반 파일은 PerformanceEditPage·PerformanceListPage 둘뿐이고 theme·components 아래 위반은 0건이다
    expect(files('DESIGN-003').sort()).toEqual([
      'frontend/src/pages/PerformanceEditPage.tsx',
      'frontend/src/pages/PerformanceListPage.tsx',
    ])
    expect(files('DESIGN-003').filter((f) => /frontend\/src\/(theme|components)\//.test(f))).toEqual([])
    // And no_target 에 DESIGN-001·002·003 이 없다
    for (const id of ['DESIGN-001', 'DESIGN-002', 'DESIGN-003']) expect(report.no_target).not.toContain(id)
  })
})
