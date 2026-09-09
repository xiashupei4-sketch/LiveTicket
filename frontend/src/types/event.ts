export interface EventVO {
  id: number
  title: string
  artist: string
  category: string
  cityCode: string
  venueName: string
  address: string
  coverUrl: string
  startTime: string
  endTime: string
  saleStartTime: string
  saleEndTime: string
  status: number
  heatScore: number
  minPrice?: number | null
}

export interface EventDetailVO extends EventVO {
  description: string
}

export interface TicketSkuVO {
  id: number
  eventId: number
  skuName: string
  price: number
  totalStock: number
  availableStock: number
  perUserLimit: number
  status: number
}

export interface NearbyEventVO extends EventVO {
  distanceKm: number
}
