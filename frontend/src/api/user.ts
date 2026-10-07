import type { UpdateUserProfileInput, UserProfile } from '~/types/user'

export function useUserApi() {
  const { request } = useApiClient()
  return {
    getMe: () => request<UserProfile>('/users/me'),
    updateMe: (input: UpdateUserProfileInput) => request<UserProfile>('/users/me', { method: 'PUT', body: input }),
  }
}
