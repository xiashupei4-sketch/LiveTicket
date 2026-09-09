import http from './http'
import type { ApiResponse, PageResponse } from '../types/api'
import type { EventVO, EventDetailVO, NearbyEventVO } from '../types/event'

export function listEvents(params: {
  page?: number
  pageSize?: number
  keyword?: string
  cityCode?: string
  category?: string
}): Promise<ApiResponse<PageResponse<EventVO>>> {
  return http.get('/events', { params }) as Promise<ApiResponse<PageResponse<EventVO>>>
}

export function listHotEvents(limit = 10): Promise<ApiResponse<EventVO[]>> {
  return http.get('/events/hot', { params: { limit } }) as Promise<ApiResponse<EventVO[]>>
}

export function listCityEvents(cityCode: string, page = 1, pageSize = 20): Promise<ApiResponse<PageResponse<EventVO>>> {
  return http.get('/events/city', { params: { cityCode, page, pageSize } }) as Promise<ApiResponse<PageResponse<EventVO>>>
}

export function listNearbyEvents(params: {
  longitude: number
  latitude: number
  radiusKm: number
  pageSize?: number
}): Promise<ApiResponse<NearbyEventVO[]>> {
  return http.get('/events/nearby', { params }) as Promise<ApiResponse<NearbyEventVO[]>>
}

export function getEventDetail(eventId: number | string): Promise<ApiResponse<EventDetailVO>> {
  return http.get(`/events/${eventId}`) as Promise<ApiResponse<EventDetailVO>>
}
