export interface UserProfile {
  id: string
  username: string
  email: string
  emailVerifiedAt: string | null
  nickname: string
  avatarKey: string
  bio: string | null
  version: number
}

export interface UpdateUserProfileInput {
  nickname: string
  bio: string | null
  avatarKey: string
  version: number
}
