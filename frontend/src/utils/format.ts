import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/** 格式化日期时间：2026-10-15 12:30 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : String(value)
}

export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD') : String(value)
}

export function fromNow(value?: string | null): string {
  if (!value) return '—'
  const d = dayjs(value)
  return d.isValid() ? d.fromNow() : String(value)
}

/** 金额展示：0 元显示为「免费」 */
export function formatPrice(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  if (Number(value) === 0) return '免费'
  return `¥${Number(value).toFixed(2)}`
}

export function formatPriceValue(value?: number | null): string {
  return Number(value ?? 0).toFixed(2)
}

/** 距离超时时间的可读描述 */
export function remainText(expireAt?: string | null): string {
  if (!expireAt) return ''
  const diff = dayjs(expireAt).diff(dayjs(), 'minute')
  if (diff <= 0) return '已超时'
  if (diff < 60) return `剩余 ${diff} 分钟`
  const hours = Math.floor(diff / 60)
  const minutes = diff % 60
  return `剩余 ${hours} 小时${minutes > 0 ? ` ${minutes} 分钟` : ''}`
}

export function truncate(text?: string | null, len = 60): string {
  if (!text) return ''
  return text.length > len ? `${text.slice(0, len)}…` : text
}
