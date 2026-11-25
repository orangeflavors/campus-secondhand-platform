import { get, post, put } from '@/utils/request'
import type {
  HandoverLogVO,
  OrderStatus,
  OrderVO,
  ReviewVO
} from '@/types'

export interface CreateOrderPayload {
  itemId: number
  remark?: string
}

export const orderApi = {
  create: (data: CreateOrderPayload) => post<OrderVO>('/orders', data),
  list: (params: { role?: 'BUYER' | 'SELLER'; status?: OrderStatus | ''; page?: number; size?: number }) =>
    get<OrderVO[]>('/orders', params as Record<string, unknown>),
  detail: (id: number) => get<OrderVO>(`/orders/${id}`),
  accept: (id: number) => post<OrderVO>(`/orders/${id}/accept`),
  reject: (id: number, reason: string) => post<OrderVO>(`/orders/${id}/reject`, { reason }),
  cancel: (id: number, reason: string) => post<OrderVO>(`/orders/${id}/cancel`, { reason }),
  updateHandover: (
    id: number,
    data: { handoverTime?: string | null; area?: string; note?: string }
  ) => put<OrderVO>(`/orders/${id}/handover`, data),
  confirm: (id: number) => post<OrderVO>(`/orders/${id}/confirm`),
  dispute: (id: number, reason: string) => post<OrderVO>(`/orders/${id}/dispute`, { reason }),
  handoverLogs: (id: number) => get<HandoverLogVO[]>(`/orders/${id}/handover-logs`),
  createReview: (id: number, data: { rating: number; content: string }) =>
    post<ReviewVO>(`/orders/${id}/reviews`, data),
  reviews: (id: number) => get<ReviewVO[]>(`/orders/${id}/reviews`)
}
