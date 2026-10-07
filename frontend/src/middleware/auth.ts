export default defineNuxtRouteMiddleware(async () => {
  if (import.meta.server) return
  const store = useAuthStore()
  if (store.isAuthenticated) return
  const { refreshSession } = useApiClient()
  if (!await refreshSession()) return navigateTo('/login')
})
