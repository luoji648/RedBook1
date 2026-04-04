import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backend = 'http://localhost:8081'
const apiProxyPrefixes = [
  'user',
  'note',
  'like',
  'collect',
  'follow',
  'share',
  'product',
  'cart',
  'order',
  'chat',
  'ai',
  'oss',
  's',
]

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: (() => {
      const map = Object.fromEntries(
        apiProxyPrefixes
          .filter((p) => p !== 'note' && p !== 's')
          .map((p) => [`/${p}`, { target: backend, changeOrigin: true }])
      )
      // `/note` 前缀会误匹配前端路由 GET /note/:id，转给后端后仅有 DELETE /note/{id}，无 GET，导致 405
      map['/note'] = {
        target: backend,
        changeOrigin: true,
        bypass(req) {
          const u = req.url?.split('?')[0] ?? ''
          if (req.method === 'GET' && /^\/note\/\d+$/.test(u)) {
            return req.url
          }
        },
      }
      // 短链代理键为 `/s` 时，前缀匹配会误伤 `/src/*`（如 /src/main.js），被转发到后端导致 404
      map['/s'] = {
        target: backend,
        changeOrigin: true,
        bypass(req) {
          const u = req.url?.split('?')[0] ?? ''
          if (u === '/src' || u.startsWith('/src/')) {
            return req.url
          }
        },
      }
      return map
    })(),
  },
})
