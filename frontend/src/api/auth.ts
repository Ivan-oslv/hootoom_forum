import type { AuthSession, LoginInput, RegisterInput } from '~/types/auth'

export function useAuthApi() {
  const { request } = useApiClient()
  return {
    login: (input: LoginInput) => request<AuthSession>('/auth/login', { method: 'POST', body: input }),
    register: (input: RegisterInput) => request<{ userId: string, status: string, message: string }>(
      '/auth/register', { method: 'POST', body: input },
    ),
    confirmEmail: (token: string) => request<{ status: string, message: string }>(
      '/auth/email-verifications/confirm', { method: 'POST', body: { token } },
    ),
    resendVerification: (email: string) => request<{ status: string, message: string }>(
      '/auth/email-verifications', { method: 'POST', body: { email } },
    ),
    requestPasswordReset: (email: string) => request<{ message: string }>(
      '/auth/password-resets', { method: 'POST', body: { email } },
    ),
    confirmPasswordReset: (token: string, newPassword: string) => request<{ message: string }>(
      '/auth/password-resets/confirm', { method: 'POST', body: { token, newPassword } },
    ),
  }
}
