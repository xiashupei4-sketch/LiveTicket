import http from './http'
import type { ApiResponse } from '../types/api'

export interface SeckillResult {
  status: 'PROCESSING' | 'SUCCESS' | 'FAILED'
  orderNo?: string | null
  reason?: string | null
}

export function seckill(ticketSkuId: number | string): Promise<ApiResponse<SeckillResult>> {
  return http.post(`/seckill/${ticketSkuId}`) as Promise<ApiResponse<SeckillResult>>
}

export function getSeckillResult(ticketSkuId: number | string): Promise<ApiResponse<SeckillResult>> {
  return http.get(`/seckill/${ticketSkuId}/result`) as Promise<ApiResponse<SeckillResult>>
}
