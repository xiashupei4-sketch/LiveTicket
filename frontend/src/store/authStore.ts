import { create } from 'zustand'
import { TOKEN_KEY, USER_KEY } from '../api/http'
import type { User } from '../types/auth'

interface AuthState {
  accessToken: string | null
  user: User | null
  isAuthenticated: boolean
  setAuth: (token: string, user: User) => void
  logout: () => void
  hydrate: () => void
}

export const useAuthStore = create<AuthState>(set => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,

  setAuth: (token, user) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    set({ accessToken: token, user, isAuthenticated: true })
  },

  logout: () => {
    set({ accessToken: null, user: null, isAuthenticated: false })
  },

  hydrate: () => {
    const token = localStorage.getItem(TOKEN_KEY)
    const rawUser = localStorage.getItem(USER_KEY)
    if (token && rawUser) {
      try {
        const user = JSON.parse(rawUser) as User
        set({ accessToken: token, user, isAuthenticated: true })
      } catch {
        set({ accessToken: null, user: null, isAuthenticated: false })
      }
    }
  }
}))
