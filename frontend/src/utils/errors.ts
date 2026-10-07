import type { FetchError } from 'ofetch'
import type { ApiErrorBody } from '~/types/api'

export function getErrorMessage(error: unknown): string {
  const body = (error as FetchError<ApiErrorBody>)?.data
  if (body?.details?.length) return body.details.map(item => item.reason).join('；')
  return body?.message || '请求失败，请稍后重试'
}
