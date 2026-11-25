import { del, get, post, put } from '@/utils/request'
import type {
  ConversationVO,
  CreateReportPayload,
  FavoriteStateVO,
  MessageVO,
  NotificationVO,
  PageResult,
  ReportVO
} from '@/types'

export const favoriteApi = {
  state: (itemId: number) => get<FavoriteStateVO>(`/items/${itemId}/favorite`),
  add: (itemId: number) => post<FavoriteStateVO>(`/items/${itemId}/favorite`),
  remove: (itemId: number) => del<FavoriteStateVO>(`/items/${itemId}/favorite`)
}

export const conversationApi = {
  create: (itemId: number) => post<ConversationVO>('/conversations', { itemId }),
  list: () => get<ConversationVO[]>('/conversations'),
  detail: (id: number) => get<ConversationVO>(`/conversations/${id}`),
  messages: (id: number, afterId?: number) =>
    get<MessageVO[]>(`/conversations/${id}/messages`, afterId ? { afterId } : {}),
  send: (id: number, content: string) => post<MessageVO>(`/conversations/${id}/messages`, { content })
}

export const reportApi = {
  create: (data: CreateReportPayload) => post<ReportVO>('/reports', data),
  mine: (params: { page?: number; size?: number }) =>
    get<PageResult<ReportVO>>('/reports/mine', params),
  detail: (id: number) => get<ReportVO>(`/reports/${id}`),
  appeal: (id: number, reason: string) => post<ReportVO>(`/reports/${id}/appeal`, { reason })
}

export const notificationApi = {
  list: (params: { unread?: boolean; page?: number; size?: number }) =>
    get<PageResult<NotificationVO>>('/notifications', params as Record<string, unknown>),
  unreadCount: () => get<{ count: number }>('/notifications/unread-count'),
  read: (id: number) => put<void>(`/notifications/${id}/read`),
  readAll: () => put<void>('/notifications/read-all')
}
