import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Windows 上 Node 常把 localhost 解析为 ::1，若 JVM 只监听 127.0.0.1 会 ECONNREFUSED
const backend = 'http://127.0.0.1:8081'
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
  'coupon',
  'wallet',
  'chat',
  'notice',
  'ai',
  'oss',
  's',
]

/** 与 dev 一致；`vite preview` 默认不带 proxy，会导致 /order、/coupon 等走静态服务返回 404 */
function createApiProxy() {
  const map = Object.fromEntries(
    apiProxyPrefixes
      .filter((p) => p !== 'note' && p !== 's')
      .map((p) => [`/${p}`, { target: backend, changeOrigin: true }])
  )
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
}

const apiProxy = createApiProxy()

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: apiProxy,
  },
  preview: {
    proxy: apiProxy,
  },
})
