import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import router from '../router'

// 生产应留空走同源 + Nginx 反代；填绝对地址时仅用于直连后端（易触发跨域）
const rawBase = import.meta.env.VITE_API_BASE
const baseURL =
  rawBase === undefined || rawBase === null || String(rawBase).trim() === ''
    ? ''
    : String(rawBase).replace(/\/$/, '')

export const apiBase = baseURL

const instance = axios.create({
  baseURL,
  timeout: 120000,
})

instance.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.authorization = `Bearer ${auth.token}`
  }
  return config
})

instance.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.clearToken()
      const route = router.currentRoute.value
      if (route.meta.requiresAuth) {
        router.push({ name: 'login', query: { redirect: route.fullPath } })
      }
      ElMessage.warning('登录已失效，请重新登录')
    }
    return Promise.reject(err)
  }
)

export async function get(url, params) {
  const res = await instance.get(url, { params })
  const body = res.data
  if (!body.success) {
    ElMessage.error(body.errorMsg || '请求失败')
    throw new Error(body.errorMsg || '请求失败')
  }
  return { data: body.data, total: body.total }
}

export async function post(url, data, config) {
  const res = await instance.post(url, data, config)
  const body = res.data
  if (!body.success) {
    ElMessage.error(body.errorMsg || '请求失败')
    throw new Error(body.errorMsg || '请求失败')
  }
  return { data: body.data, total: body.total }
}

export async function put(url, data, config) {
  const res = await instance.put(url, data ?? {}, config)
  const body = res.data
  if (!body.success) {
    ElMessage.error(body.errorMsg || '请求失败')
    throw new Error(body.errorMsg || '请求失败')
  }
  return { data: body.data, total: body.total }
}

export async function del(url) {
  const res = await instance.delete(url)
  const body = res.data
  if (!body.success) {
    ElMessage.error(body.errorMsg || '请求失败')
    throw new Error(body.errorMsg || '请求失败')
  }
  return { data: body.data, total: body.total }
}

export async function postForm(url, params) {
  const res = await instance.post(url, null, { params })
  const body = res.data
  if (!body.success) {
    ElMessage.error(body.errorMsg || '请求失败')
    throw new Error(body.errorMsg || '请求失败')
  }
  return { data: body.data, total: body.total }
}

export default instance
