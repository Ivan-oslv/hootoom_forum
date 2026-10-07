import type { LoginInput, RegisterInput } from '~/types/auth'
import { useAuthApi } from '~/api/auth'
import { readCookie } from '~/utils/cookies'

export function useAuth() {
  const store = useAuthStore()
  const api = useAuthApi()
  const { request, refreshSession } = useApiClient()

  async function login(input: LoginInput) {
    const session = await api.login(input)
    store.setSession(session.accessToken, session.expiresIn, session.user)
  }

  async function register(input: RegisterInput) {
    return api.register(input)
  }

  async function logout() {
    const csrf = readCookie('HOOTOOM_XSRF')
    if (csrf) {
      await request('/auth/logout', { method: 'POST', headers: { 'X-CSRF-Token': csrf } })
    }
    store.clearSession()
    await navigateTo('/login')
  }

  return { store, login, register, logout, refreshSession }
}
