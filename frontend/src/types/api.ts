export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId?: string
}

export interface ApiErrorDetail { field: string, reason: string }

export interface ApiErrorBody {
  code: string
  message: string
  details?: ApiErrorDetail[]
  requestId?: string
}
