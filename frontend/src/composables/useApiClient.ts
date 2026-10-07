import type { ApiResponse } from '~/types/api'
import type { AuthSession } from '~/types/auth'
import { readCookie } from '~/utils/cookies'

// 模块级 Promise 合并所有 API 实例触发的并发刷新请求。
let refreshPromise: Promise<boolean> | null = null

export function useApiClient() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  async function refreshSession(): Promise<boolean> {
    if (import.meta.server) return false
    if (!refreshPromise) {
      refreshPromise = (async () => {
        try {
          const csrf = readCookie('HOOTOOM_XSRF')
          if (!csrf) return false
          const response = await $fetch<ApiResponse<AuthSession>>('/auth/refresh', {
            baseURL: config.public.apiBase,
            method: 'POST',
            credentials: 'include',
            headers: { 'X-CSRF-Token': csrf },
          })
          authStore.setSession(response.data.accessToken, response.data.expiresIn, response.data.user)
          return true
        }
        catch {
          authStore.clearSession()
          return false
        }
        finally {
          refreshPromise = null
        }
      })()
    }
    return refreshPromise
  }

  async function request<T>(
    path: string,
    options: Record<string, unknown> = {},
    canRetry = true,
  ): Promise<T> {
    const headers = new Headers(options.headers as HeadersInit | undefined)
    if (authStore.accessToken) headers.set('Authorization', `Bearer ${authStore.accessToken}`)
    try {
      const response = await $fetch<ApiResponse<T>>(path, {
        ...options,
        baseURL: config.public.apiBase,
        credentials: 'include',
        headers,
      } as never)
      return response.data
    }
    catch (error: unknown) {
      const statusCode = typeof error === 'object' && error !== null && 'statusCode' in error
        ? error.statusCode
        : undefined
      if (statusCode === 401 && canRetry && !path.startsWith('/auth/')) {
        const refreshed = await refreshSession()
        if (refreshed) return request<T>(path, options, false)
      }
      throw error
    }
  }

  return { request, refreshSession }
}
