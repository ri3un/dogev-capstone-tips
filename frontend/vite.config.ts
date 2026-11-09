import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // 저장소 루트 .env 하나로 씀. 브라우저에는 VITE_ 로 시작하는 값만 들어가서 다른 비밀 키는 안 나감
  envDir: '..',
  server: {
    // 개발할 때 /api 요청을 로컬 백엔드(8080)로 넘깁니다. 배포에서는 Nginx가 같은 일을 합니다.
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080',
      '/login/oauth2': 'http://localhost:8080',
    },
  },
})
