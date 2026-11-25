import type {
  AccountStatus,
  AuthStatus,
  CancelType,
  ItemCondition,
  ItemStatus,
  ModerationActionType,
  NotificationType,
  OrderStatus,
  ReportStatus,
  ReportTargetType
} from '@/types'

export type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

export interface SelectOption {
  value: string
  label: string
}

export const conditionOptions: SelectOption[] = [
  { value: 'NEW', label: '全新' },
  { value: 'LIKE_NEW', label: '几乎全新' },
  { value: 'GOOD', label: '轻微使用痕迹' },
  { value: 'FAIR', label: '明显使用痕迹' }
]

export const conditionLabels: Record<string, string> = Object.fromEntries(
  conditionOptions.map((o) => [o.value, o.label])
)

export const itemStatusLabels: Record<ItemStatus, string> = {
  DRAFT: '草稿',
  PENDING: '待审核',
  REJECTED: '审核驳回',
  ON_SALE: '在售',
  LOCKED: '已锁定',
  SOLD: '已售出',
  OFF_SHELF: '已下架',
  VIOLATED: '违规下架'
}

export const itemStatusTag: Record<ItemStatus, TagType> = {
  DRAFT: 'info',
  PENDING: 'warning',
  REJECTED: 'danger',
  ON_SALE: 'success',
  LOCKED: 'warning',
  SOLD: 'info',
  OFF_SHELF: 'info',
  VIOLATED: 'danger'
}

export const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING_SELLER: '待卖家确认',
  DELIVERING: '待线下交付',
  BUYER_CONFIRMED: '买家已确认',
  SELLER_CONFIRMED: '卖家已确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  DISPUTE: '争议中',
  MANAGEMENT_CLOSED: '管理关闭'
}

export const orderStatusTag: Record<OrderStatus, TagType> = {
  PENDING_SELLER: 'warning',
  DELIVERING: 'primary',
  BUYER_CONFIRMED: 'primary',
  SELLER_CONFIRMED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info',
  DISPUTE: 'danger',
  MANAGEMENT_CLOSED: 'danger'
}

export const cancelTypeLabels: Record<CancelType, string> = {
  BUYER_CANCEL: '买家取消',
  SELLER_REJECT: '卖家拒绝',
  TIMEOUT: '超时未确认',
  NOT_SELECTED: '未中选',
  ADMIN_CLOSE: '管理员关闭'
}

export const reportStatusLabels: Record<ReportStatus, string> = {
  SUBMITTED: '待处理',
  PROCESSING: '处理中',
  SUPPLEMENT: '待补充证据',
  UPHELD: '举报成立',
  REJECTED: '举报不成立',
  PENDING_APPEAL: '申诉中',
  CLOSED: '已关闭'
}

export const reportStatusTag: Record<ReportStatus, TagType> = {
  SUBMITTED: 'warning',
  PROCESSING: 'primary',
  SUPPLEMENT: 'warning',
  UPHELD: 'danger',
  REJECTED: 'info',
  PENDING_APPEAL: 'warning',
  CLOSED: 'info'
}

export const reportTargetTypeLabels: Record<ReportTargetType, string> = {
  ITEM: '商品',
  USER: '用户',
  MESSAGE: '消息'
}

export const reportCategoryOptions: SelectOption[] = [
  { value: 'PROHIBITED', label: '违禁物品' },
  { value: 'FAKE', label: '虚假信息' },
  { value: 'FRAUD', label: '欺诈行为' },
  { value: 'ABUSE', label: '辱骂骚扰' },
  { value: 'SPAM', label: '广告刷屏' },
  { value: 'OTHER', label: '其他违规' }
]

export const reportCategoryLabels: Record<string, string> = Object.fromEntries(
  reportCategoryOptions.map((o) => [o.value, o.label])
)

export const accountStatusLabels: Record<AccountStatus, string> = {
  NORMAL: '正常',
  LIMITED: '限权',
  BANNED: '封禁'
}

export const accountStatusTag: Record<AccountStatus, TagType> = {
  NORMAL: 'success',
  LIMITED: 'warning',
  BANNED: 'danger'
}

export const authStatusLabels: Record<AuthStatus, string> = {
  UNVERIFIED: '未认证',
  PENDING: '认证审核中',
  VERIFIED: '已认证',
  REJECTED: '认证驳回'
}

export const authStatusTag: Record<AuthStatus, TagType> = {
  UNVERIFIED: 'info',
  PENDING: 'warning',
  VERIFIED: 'success',
  REJECTED: 'danger'
}

export const actionTypeLabels: Record<ModerationActionType, string> = {
  TAKEDOWN: '违规下架',
  WARN: '警告',
  LIMIT: '限权',
  BAN: '封禁',
  UNBAN: '解封'
}

export const notificationTypeLabels: Record<NotificationType, string> = {
  ITEM: '商品',
  ORDER: '订单',
  REPORT: '举报',
  REVIEW: '评价',
  ACCOUNT: '账户',
  SYSTEM: '系统'
}

export const areas = [
  '东区宿舍',
  '西区宿舍',
  '南区宿舍',
  '北区宿舍',
  '图书馆',
  '第一食堂',
  '第二食堂',
  '体育馆',
  '教学楼',
  '校门口'
]

export const sortOptions: SelectOption[] = [
  { value: 'latest', label: '最新发布' },
  { value: 'price_asc', label: '价格从低到高' },
  { value: 'price_desc', label: '价格从高到低' }
]

export const cancelReasonPresets = [
  '已与卖家另行约定',
  '不想购买了',
  '商品信息有误',
  '交付时间无法协调',
  '其他原因'
]

export function labelOf(map: Record<string, string>, key?: string | null): string {
  if (!key) return '—'
  return map[key] || key
}
