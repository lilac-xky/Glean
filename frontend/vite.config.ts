import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // 新增：本地开发服务器配置
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true, // 允许跨域
        // 如果你的后端接口本身没有 /api 前缀，需要把 /api 替换掉
        // 例如：前端请求 /api/user -> 转发为 http://localhost:8080/user
        rewrite: (path) => path.replace(/^\/api/, ''), 
      },
    },
  },
})
