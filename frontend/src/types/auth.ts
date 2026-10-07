export interface CurrentUser {
  id: string
  username: string
  nickname: string
  avatarKey: string
}

export interface AuthSession {
  accessToken: string
  expiresIn: number
  user: CurrentUser
}

export interface LoginInput { identifier: string, password: string, deviceName?: string }
export interface RegisterInput {
  username: string
  email: string
  password: string
  policyVersion: string
  policyAccepted: boolean
}
