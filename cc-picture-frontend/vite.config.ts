import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
  // TODO 如果后端代码无法修改，还可以通过前端代理服务器来解决跨域问题
  // server: {
  //   proxy: {
  //     '/api': 'http://localhost:8123',
  //   }
  // },
})
