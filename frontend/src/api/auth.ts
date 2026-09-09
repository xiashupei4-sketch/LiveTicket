import http from './http'
import type { ApiResponse } from '../types/api'
import type { LoginResult } from '../types/auth'

export function register(params: { username: string; password: string; nickname: string }): Promise<ApiResponse<null>> {
  return http.post('/auth/register', params) as Promise<ApiResponse<null>>
}

export function login(params: { username: string; password: string }): Promise<ApiResponse<LoginResult>> {
  return http.post('/auth/login', params) as Promise<ApiResponse<LoginResult>>
}
