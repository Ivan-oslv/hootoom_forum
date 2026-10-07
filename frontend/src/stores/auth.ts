import { defineStore } from 'pinia'
import type { CurrentUser } from '~/types/auth'

interface AuthState {
  accessToken: string | null
  user: CurrentUser | null
  expiresAt: number | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ accessToken: null, user: null, expiresAt: null }),
  getters: {
    isAuthenticated: state => Boolean(state.accessToken && state.expiresAt && state.expiresAt > Date.now()),
  },
  actions: {
    setSession(accessToken: string, expiresIn: number, user: CurrentUser) {
      // Access Token 只存在 Pinia 运行时内存，禁止持久化插件写入 Web Storage。
      this.accessToken = accessToken
      this.expiresAt = Date.now() + expiresIn * 1000
      this.user = user
    },
    clearSession() {
      this.$reset()
    },
  },
})
