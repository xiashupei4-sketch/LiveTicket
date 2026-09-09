import http from './http'
import type { ApiResponse } from '../types/api'
import type { TicketSkuVO } from '../types/event'

export function listEventTickets(eventId: number | string): Promise<ApiResponse<TicketSkuVO[]>> {
  return http.get(`/events/${eventId}/tickets`) as Promise<ApiResponse<TicketSkuVO[]>>
}
