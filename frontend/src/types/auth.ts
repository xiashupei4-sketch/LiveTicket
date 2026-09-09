export interface User {
  userId: number
  username: string
  nickname: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
}
