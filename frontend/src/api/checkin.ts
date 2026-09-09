import http from './http'
import type { ApiResponse } from '../types/api'

export interface CheckinCalendar {
  month: string
  checkedDays: number[]
  checkedCount: number
}

export function checkinEvent(eventId: number): Promise<ApiResponse<null>> {
  return http.post(`/events/${eventId}/checkin`) as Promise<ApiResponse<null>>
}

export function getCalendar(month?: string): Promise<ApiResponse<CheckinCalendar>> {
  return http.get('/checkins/calendar', { params: month ? { month } : {} }) as Promise<ApiResponse<CheckinCalendar>>
}

export function getStreak(): Promise<ApiResponse<number>> {
  return http.get('/checkins/streak') as Promise<ApiResponse<number>>
}

export function getEventUv(eventId: number, date?: string): Promise<ApiResponse<{ eventId: number; date: string; uv: number }>> {
  return http.get(`/statistics/events/${eventId}/uv`, { params: date ? { date } : {} }) as Promise<ApiResponse<{ eventId: number; date: string; uv: number }>>
}
