import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'redbook_jwt'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')

  const isLogin = computed(() => !!token.value)

  function setToken(t) {
    token.value = t || ''
    if (t) localStorage.setItem(TOKEN_KEY, t)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function clearToken() {
    setToken('')
  }

  return { token, isLogin, setToken, clearToken }
})
