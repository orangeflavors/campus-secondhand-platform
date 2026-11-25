import { get, post } from '@/utils/request'
import type {
  AdminItemVO,
  AdminUserVO,
  AuditLogVO,
  ItemStatus,
  ModerationActionType,
  ModerationActionVO,
  OrderVO,
  PageResult,
  ReportStatus,
  ReportVO,
  StatsVO
} from '@/types'

export const adminItemApi = {
  list: (params: {
    status?: ItemStatus | ''
    keyword?: string
    page?: number
    size?: number
  }) => get<PageResult<AdminItemVO>>('/admin/items', params as Record<string, unknown>),
  approve: (id: number) => post<AdminItemVO>(`/admin/items/${id}/approve`),
  reject: (id: number, reason: string) => post<AdminItemVO>(`/admin/items/${id}/reject`, { reason }),
  takedown: (id: number, data: { reason: string; ruleBasis?: string }) =>
    post<AdminItemVO>(`/admin/items/${id}/takedown`, data)
}

export const adminReportApi = {
  list: (params: { status?: ReportStatus | ''; page?: number; size?: number }) =>
    get<PageResult<ReportVO>>('/admin/reports', params as Record<string, unknown>),
  claim: (id: number) => post<ReportVO>(`/admin/reports/${id}/claim`),
  handle: (
    id: number,
    data: {
      result: 'UPHELD' | 'REJECTED' | 'SUPPLEMENT'
      ruleBasis?: string
      reason: string
      actionType?: ModerationActionType
      durationDays?: number
    }
  ) => post<ReportVO>(`/admin/reports/${id}/handle`, data),
  close: (id: number) => post<ReportVO>(`/admin/reports/${id}/close`),
  resolveAppeal: (id: number, data: { uphold: boolean; reason: string }) =>
    post<ReportVO>(`/admin/reports/${id}/appeal/resolve`, data)
}

export const adminUserApi = {
  list: (params: {
    keyword?: string
    accountStatus?: string
    authStatus?: string
    page?: number
    size?: number
  }) => get<PageResult<AdminUserVO>>('/admin/users', params as Record<string, unknown>),
  detail: (id: number) => get<AdminUserVO>(`/admin/users/${id}`),
  sanction: (
    id: number,
    data: {
      actionType: ModerationActionType
      reason: string
      ruleBasis?: string
      durationDays?: number
    }
  ) => post<ModerationActionVO>(`/admin/users/${id}/sanction`, data),
  actions: (id: number) => get<ModerationActionVO[]>(`/admin/users/${id}/actions`)
}

export const adminOrderApi = {
  disputes: (params: { page?: number; size?: number }) =>
    get<PageResult<OrderVO>>('/admin/orders', { status: 'DISPUTE', ...params }),
  resolve: (id: number, data: { outcome: 'COMPLETE' | 'CLOSE'; reason: string }) =>
    post<OrderVO>(`/admin/orders/${id}/resolve`, data)
}

export const adminStatsApi = {
  get: (params: { from?: string; to?: string }) => get<StatsVO>('/admin/stats', params)
}

export const adminAuditApi = {
  list: (params: { action?: string; page?: number; size?: number }) =>
    get<PageResult<AuditLogVO>>('/admin/audit-logs', params as Record<string, unknown>)
}
