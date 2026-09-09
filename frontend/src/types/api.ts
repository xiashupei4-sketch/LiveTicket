export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
  hasNext: boolean
}
