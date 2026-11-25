/** 后端统一响应包装：code=0 表示成功 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/* ------------------------------ 基础枚举 ------------------------------ */

export type Role = 'USER' | 'ADMIN'
export type AccountStatus = 'NORMAL' | 'LIMITED' | 'BANNED'
export type AuthStatus = 'UNVERIFIED' | 'PENDING' | 'VERIFIED' | 'REJECTED'
export type ContactVisibility = 'PUBLIC' | 'AUTHENTICATED' | 'PRIVATE'
export type ItemCondition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR'
export type ItemStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'REJECTED'
  | 'ON_SALE'
  | 'LOCKED'
  | 'SOLD'
  | 'OFF_SHELF'
  | 'VIOLATED'
export type OrderStatus =
  | 'PENDING_SELLER'
  | 'DELIVERING'
  | 'BUYER_CONFIRMED'
  | 'SELLER_CONFIRMED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTE'
  | 'MANAGEMENT_CLOSED'
export type CancelType = 'BUYER_CANCEL' | 'SELLER_REJECT' | 'TIMEOUT' | 'NOT_SELECTED' | 'ADMIN_CLOSE'
export type ReportStatus =
  | 'SUBMITTED'
  | 'PROCESSING'
  | 'SUPPLEMENT'
  | 'UPHELD'
  | 'REJECTED'
  | 'PENDING_APPEAL'
  | 'CLOSED'
export type ReportTargetType = 'ITEM' | 'USER' | 'MESSAGE'
export type ModerationActionType = 'TAKEDOWN' | 'WARN' | 'LIMIT' | 'BAN' | 'UNBAN'
export type NotificationType = 'ITEM' | 'ORDER' | 'REPORT' | 'REVIEW' | 'ACCOUNT' | 'SYSTEM'

/* ------------------------------ 用户相关 ------------------------------ */

export interface UserStats {
  publishedCount: number
  soldCount: number
  reviewCount: number
  avgRating: number
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  college?: string | null
  phone?: string | null
  email?: string | null
  studentNo?: string | null
  contactVisibility: ContactVisibility
  role: Role
  accountStatus: AccountStatus
  authStatus: AuthStatus
  authMethod?: string | null
  authTime?: string | null
  limitedUntil?: string | null
  statusReason?: string | null
  createdAt: string
  stats?: UserStats
}

export interface PublicProfileVO {
  id: number
  nickname: string
  avatarUrl?: string | null
  college?: string | null
  authStatus: AuthStatus
  memberSince?: string | null
  avgRating: number
  reviewCount: number
  soldCount: number
  contactPhone?: string | null
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: UserVO
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
  college?: string
  phone?: string
  email?: string
  studentNo?: string
}

export interface ProfileUpdatePayload {
  nickname?: string
  phone?: string
  email?: string
  college?: string
  avatarUrl?: string
  contactVisibility?: ContactVisibility
}

export interface CampusAuthPayload {
  studentNo: string
  realName: string
  college: string
}

/* ------------------------------ 元数据 ------------------------------ */

export interface CategoryVO {
  id: number
  name: string
  code: string
}

export interface OptionVO {
  value: string
  label: string
}

export interface MetaVO {
  categories: CategoryVO[]
  conditions: OptionVO[]
  areas: string[]
  reportCategories: OptionVO[]
}

/* ------------------------------ 商品 ------------------------------ */

export interface ItemCardVO {
  id: number
  title: string
  coverUrl?: string | null
  price: number
  condition: ItemCondition
  conditionText: string
  categoryId: number
  categoryName: string
  handoverArea?: string | null
  status: ItemStatus
  statusText: string
  sellerId: number
  sellerNickname: string
  sellerAuthStatus: AuthStatus
  listedAt?: string | null
  createdAt: string
  viewCount: number
  favoriteCount: number
}

export interface ItemDetailVO extends ItemCardVO {
  description: string
  images: string[]
  isFavorite: boolean
  isOwner: boolean
  canOrder: boolean
  cannotOrderReason?: string | null
  rejectReason?: string | null
  seller: PublicProfileVO
  riskNotice: string
  activeOrderId?: number | null
}

export interface ItemSearchParams {
  keyword?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  condition?: ItemCondition
  area?: string
  sort?: 'latest' | 'price_asc' | 'price_desc'
  page?: number
  size?: number
}

export interface ItemUpsertPayload {
  title: string
  categoryId: number | null
  condition: ItemCondition | ''
  description: string
  price: number | null
  handoverArea: string
  images: string[]
}

export interface FavoriteStateVO {
  favorited: boolean
  count: number
}

/* ------------------------------ 订单 ------------------------------ */

export interface OrderActions {
  canAccept: boolean
  canReject: boolean
  canCancel: boolean
  canConfirm: boolean
  canDispute: boolean
  canReview: boolean
  canEditHandover: boolean
}

export interface HandoverVO {
  handoverTime?: string | null
  area?: string | null
  note?: string | null
  buyerConfirmedAt?: string | null
  sellerConfirmedAt?: string | null
  updatedAt?: string | null
}

export interface OrderItemBrief {
  id: number
  title: string
  coverUrl?: string | null
  price: number
}

export interface UserBrief {
  id: number
  nickname: string
  avatarUrl?: string | null
}

export interface OrderVO {
  id: number
  orderNo: string
  status: OrderStatus
  statusText: string
  item: OrderItemBrief
  buyer: UserBrief
  seller: UserBrief
  myRole: 'BUYER' | 'SELLER'
  requestRemark?: string | null
  cancelType?: CancelType | null
  cancelTypeText?: string | null
  cancelReason?: string | null
  disputeReason?: string | null
  createdAt: string
  acceptedAt?: string | null
  expireAt?: string | null
  buyerConfirmedAt?: string | null
  sellerConfirmedAt?: string | null
  completedAt?: string | null
  handover?: HandoverVO | null
  reviews: ReviewVO[]
  myReview?: ReviewVO | null
  actions: OrderActions
}

export interface ReviewVO {
  id: number
  orderId: number
  authorId: number
  authorNickname: string
  authorAvatarUrl?: string | null
  targetId: number
  targetNickname: string
  rating: number
  content?: string | null
  createdAt: string
}

export interface HandoverLogVO {
  id: number
  changedBy: number
  changedByName: string
  summary: string
  createdAt: string
}

/* ------------------------------ 咨询 ------------------------------ */

export interface ConversationVO {
  id: number
  itemId: number
  itemTitle: string
  itemCoverUrl?: string | null
  itemPrice: number
  otherUserId: number
  otherNickname: string
  otherAvatarUrl?: string | null
  otherAuthStatus: AuthStatus
  lastMessage?: string | null
  lastMessageAt?: string | null
  createdAt: string
}

export interface MessageVO {
  id: number
  conversationId: number
  senderId: number
  senderNickname: string
  senderAvatarUrl?: string | null
  content: string
  mine: boolean
  createdAt: string
}

/* ------------------------------ 举报与通知 ------------------------------ */

export interface ReportVO {
  id: number
  reportNo: string
  targetType: ReportTargetType
  targetId: number
  targetTypeText: string
  targetSummary?: string | null
  category: string
  categoryText: string
  description: string
  status: ReportStatus
  statusText: string
  handlingResult?: string | null
  ruleBasis?: string | null
  actionSummary?: string | null
  appealReason?: string | null
  handlerName?: string | null
  reporterNickname?: string | null
  createdAt: string
  handledAt?: string | null
}

export interface CreateReportPayload {
  targetType: ReportTargetType
  targetId: number
  category: string
  description: string
}

export interface NotificationVO {
  id: number
  type: NotificationType
  typeText: string
  title: string
  content: string
  relatedType?: string | null
  relatedId?: number | null
  read: boolean
  createdAt: string
}

/* ------------------------------ 管理端 ------------------------------ */

export interface AdminItemVO {
  id: number
  title: string
  coverUrl?: string | null
  price: number
  categoryName: string
  sellerId: number
  sellerNickname: string
  sellerAuthStatus: AuthStatus
  status: ItemStatus
  statusText: string
  rejectReason?: string | null
  description?: string | null
  createdAt: string
  auditAt?: string | null
  listedAt?: string | null
}

export interface AdminUserVO {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  college?: string | null
  role: Role
  authStatus: AuthStatus
  accountStatus: AccountStatus
  statusReason?: string | null
  limitedUntil?: string | null
  phone?: string | null
  studentNo?: string | null
  createdAt: string
  stats: UserStats
}

export interface ModerationActionVO {
  id: number
  targetType: string
  targetId: number
  actionType: ModerationActionType
  actionTypeText: string
  reason: string
  ruleBasis?: string | null
  durationDays?: number | null
  operatorName: string
  effectiveFrom: string
  effectiveTo?: string | null
  createdAt: string
}

export interface AuditLogVO {
  id: number
  operatorName?: string | null
  action: string
  actionText: string
  targetType?: string | null
  targetId?: number | null
  reason?: string | null
  beforeValue?: string | null
  afterValue?: string | null
  createdAt: string
}

export interface StatusCount {
  status: string
  label: string
  count: number
}

export interface NameCount {
  name: string
  count: number
}

export interface TrendPoint {
  date: string
  published: number
  completed: number
}

export interface StatsVO {
  from: string
  to: string
  items: {
    published: number
    onSale: number
    sold: number
    pendingAudit: number
    byCategory: NameCount[]
  }
  orders: {
    total: number
    completed: number
    cancelled: number
    active: number
    completionRate: number
  }
  reviews: {
    count: number
    avgRating: number
  }
  reports: {
    total: number
    byStatus: StatusCount[]
  }
  actions: {
    total: number
    byType: StatusCount[]
  }
  trend: TrendPoint[]
}
