export interface OrderVO {
  orderNo: string
  userId: number
  eventId: number
  eventTitle: string
  eventCoverUrl: string
  eventStartTime: string
  venueName: string
  ticketSkuId: number
  skuName: string
  quantity: number
  unitPrice: number
  totalAmount: number
  status: number
  source: number
  createdAt: string
  paidAt?: string | null
  cancelledAt?: string | null
}
