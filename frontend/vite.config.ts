/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 개발 환경 전용 — 배포 환경의 CORS/리버스 프록시 전략은 별도로 정한다
      // (PLAN_TASK-002.json codebase_analysis.unresolved 참고)
      '/api': 'http://localhost:8080',
      // 소셜 로그인 시작 주소만 백엔드로 보낸다 — /oauth2/redirect 는 프론트 라우트라 /oauth2 전체를 보내면 안 된다.
      '/oauth2/authorization': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    globals: true,
  },
})
