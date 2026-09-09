import http from './http'
import type { ApiResponse } from '../types/api'

export function getEventUv(eventId: number | string, date?: string): Promise<ApiResponse<{ eventId: number; date: string; uv: number }>> {
  return http.get(`/statistics/events/${eventId}/uv`, { params: date ? { date } : {} }) as Promise<ApiResponse<{ eventId: number; date: string; uv: number }>>
}
