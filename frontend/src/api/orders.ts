import http from './http'
import type { ApiResponse, PageResponse } from '../types/api'
import type { OrderVO } from '../types/order'

export function createOrder(params: { ticketSkuId: number; quantity: number }): Promise<ApiResponse<OrderVO>> {
  return http.post('/orders', params) as Promise<ApiResponse<OrderVO>>
}

export function listOrders(params: {
  status?: number
  page?: number
  pageSize?: number
}): Promise<ApiResponse<PageResponse<OrderVO>>> {
  return http.get('/orders', { params }) as Promise<ApiResponse<PageResponse<OrderVO>>>
}

export function getOrder(orderNo: string): Promise<ApiResponse<OrderVO>> {
  return http.get(`/orders/${orderNo}`) as Promise<ApiResponse<OrderVO>>
}

export function payOrder(orderNo: string): Promise<ApiResponse<OrderVO>> {
  return http.post(`/orders/${orderNo}/pay`) as Promise<ApiResponse<OrderVO>>
}

export function cancelOrder(orderNo: string): Promise<ApiResponse<OrderVO>> {
  return http.post(`/orders/${orderNo}/cancel`) as Promise<ApiResponse<OrderVO>>
}
