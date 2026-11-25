<template>
  <div class="order-page">
    <div class="order-back">
      <router-link to="/me/orders">
        <el-icon><ArrowLeft /></el-icon>
        返回我的订单
      </router-link>
    </div>

    <div v-if="loading" class="surface order-loading">
      <el-skeleton animated :rows="8" />
    </div>

    <section v-else-if="loadError || !order" class="surface order-missing">
      <EmptyState
        description="订单不存在，或你无权查看该订单"
        action-text="返回我的订单"
        @action="router.push('/me/orders')"
      />
    </section>

    <template v-else>
      <section class="surface order-header">
        <div class="order-title-row">
          <div class="order-title-main">
            <div class="order-no">订单号 {{ order.orderNo }}</div>
            <h1 class="order-title">{{ order.item.title }}</h1>
            <div class="order-meta">
              <span>创建于 {{ formatDateTime(order.createdAt) }}</span>
              <span>我的身份：{{ order.myRole === 'BUYER' ? '买家' : '卖家' }}</span>
              <span v-if="order.acceptedAt">卖家接受于 {{ formatDateTime(order.acceptedAt) }}</span>
              <span v-if="order.completedAt">完成于 {{ formatDateTime(order.completedAt) }}</span>
            </div>
          </div>
          <StatusTag kind="order" :value="order.status" effect="dark" class="order-status" />
        </div>

        <el-alert
          v-if="order.status === 'PENDING_SELLER'"
          type="warning"
          :closable="false"
          show-icon
          class="status-alert"
        >
          <template #title>等待卖家确认 · {{ remainingText }}</template>
          <div>
            卖家需在 {{ formatDateTime(order.expireAt) }} 前确认该购买请求；超时后请求会自动取消。你也可在操作区主动取消。
          </div>
        </el-alert>

        <el-alert
          v-if="abnormalInfo"
          :type="abnormalInfo.type"
          :closable="false"
          show-icon
          class="status-alert"
        >
          <template #title>{{ abnormalInfo.title }}</template>
          <div>{{ abnormalInfo.description }}</div>
        </el-alert>

        <el-steps
          v-if="showSteps"
          :active="activeStep"
          finish-status="success"
          :process-status="processStatus"
          align-center
          class="order-steps"
        >
          <el-step title="提交请求" :description="stepDescriptions[0]" />
          <el-step title="卖家接受" :description="stepDescriptions[1]" />
          <el-step title="双方确认" :description="stepDescriptions[2]" />
          <el-step title="完成" :description="stepDescriptions[3]" />
        </el-steps>
      </section>

      <div class="order-grid">
        <div class="order-main">
          <section class="surface item-summary-card">
            <h2 class="section-title"><span class="bar"></span>商品信息</h2>
            <router-link :to="`/items/${order.item.id}`" class="order-item-link">
              <el-image
                :src="order.item.coverUrl || '/images/placeholder.svg'"
                fit="cover"
                class="order-item-cover"
              >
                <template #error>
                  <div class="thumb-fallback"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="order-item-info">
                <strong>{{ order.item.title }}</strong>
                <span class="price-text">{{ formatPrice(order.item.price) }}</span>
                <span class="muted small">查看商品详情 →</span>
              </div>
            </router-link>
          </section>

          <section class="surface parties-card">
            <h2 class="section-title"><span class="bar"></span>交易双方</h2>
            <div class="parties-row">
              <div class="party-card">
                <UserAvatar :nickname="order.buyer.nickname" :src="order.buyer.avatarUrl" :size="46" />
                <div class="party-info">
                  <div class="party-name">
                    <strong>{{ order.buyer.nickname }}</strong>
                    <el-tag v-if="order.myRole === 'BUYER'" size="small" round type="success">我</el-tag>
                  </div>
                  <span class="muted small">买家</span>
                </div>
              </div>

              <el-icon class="exchange-icon"><Right /></el-icon>

              <div class="party-card">
                <UserAvatar :nickname="order.seller.nickname" :src="order.seller.avatarUrl" :size="46" />
                <div class="party-info">
                  <div class="party-name">
                    <strong>{{ order.seller.nickname }}</strong>
                    <el-tag v-if="order.myRole === 'SELLER'" size="small" round type="success">我</el-tag>
                  </div>
                  <span class="muted small">卖家</span>
                </div>
              </div>
            </div>

            <div v-if="order.requestRemark" class="request-remark">
              <span class="muted small">买家请求备注</span>
              <p>{{ order.requestRemark }}</p>
            </div>
          </section>

          <section class="surface handover-card">
            <div class="section-head">
              <h2 class="section-title"><span class="bar"></span>交付约定</h2>
              <el-button
                v-if="order.actions?.canEditHandover"
                type="primary"
                plain
                round
                size="small"
                @click="openHandoverDialog"
              >
                <el-icon><Edit /></el-icon>
                修改约定
              </el-button>
            </div>

            <div class="handover-grid">
              <div class="handover-field">
                <span class="handover-label">约定时间</span>
                <strong>{{ formatDateTime(order.handover?.handoverTime) }}</strong>
              </div>
              <div class="handover-field">
                <span class="handover-label">交付区域</span>
                <strong>{{ order.handover?.area || '未约定' }}</strong>
              </div>
              <div class="handover-field handover-full">
                <span class="handover-label">交付备注</span>
                <strong class="handover-note">{{ order.handover?.note || '暂无备注' }}</strong>
              </div>
            </div>

            <p v-if="order.handover?.updatedAt" class="handover-updated muted small">
              交付约定更新于 {{ formatDateTime(order.handover.updatedAt) }}
            </p>

            <div class="confirm-row">
              <div class="confirm-item" :class="{ done: !!order.buyerConfirmedAt }">
                <el-icon><CircleCheckFilled /></el-icon>
                <span>买家确认：{{ order.buyerConfirmedAt ? formatDateTime(order.buyerConfirmedAt) : '待确认' }}</span>
              </div>
              <div class="confirm-item" :class="{ done: !!order.sellerConfirmedAt }">
                <el-icon><CircleCheckFilled /></el-icon>
                <span>卖家确认：{{ order.sellerConfirmedAt ? formatDateTime(order.sellerConfirmedAt) : '待确认' }}</span>
              </div>
            </div>

            <p
              v-if="order.status === 'DELIVERING' && !order.handover?.handoverTime && !order.handover?.area"
              class="handover-empty-tip muted small"
            >
              尚未约定交付信息，建议先通过消息与对方沟通时间、地点，再填写交付约定。
            </p>
          </section>

          <section ref="reviewSectionRef" class="surface reviews-card">
            <div class="section-head">
              <h2 class="section-title"><span class="bar"></span>订单评价</h2>
              <span class="muted small">双方各可评价一次</span>
            </div>

            <div v-if="order.reviews?.length" class="review-list">
              <article v-for="review in order.reviews" :key="review.id" class="review-item">
                <UserAvatar
                  :nickname="review.authorNickname"
                  :src="review.authorAvatarUrl"
                  :size="38"
                />
                <div class="review-main">
                  <div class="review-head">
                    <strong>{{ review.authorNickname }}</strong>
                    <el-tag size="small" round effect="plain">{{ reviewRole(review) }}</el-tag>
                    <StarRating :value="review.rating" />
                    <span class="muted small">{{ formatDateTime(review.createdAt) }}</span>
                  </div>
                  <p class="review-content">{{ review.content || '该用户未填写评价内容。' }}</p>
                  <span class="muted small">评价对象：{{ review.targetNickname }}</span>
                </div>
              </article>
            </div>
            <EmptyState
              v-else-if="!order.actions?.canReview"
              description="双方暂未提交评价"
              :image-size="80"
            />

            <div v-if="order.actions?.canReview && !order.myReview" class="review-form">
              <h3 class="review-form-title">评价{{ order.myRole === 'BUYER' ? '卖家' : '买家' }}</h3>
              <div class="rating-row">
                <span class="field-label">评分</span>
                <el-rate v-model="reviewForm.rating" show-text :texts="ratingTexts" />
              </div>
              <el-input
                v-model="reviewForm.content"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="分享本次交易的体验（选填）"
              />
              <div class="review-submit">
                <el-button type="primary" round :loading="reviewSubmitting" @click="submitReview">
                  提交评价
                </el-button>
              </div>
            </div>

            <el-alert
              v-else-if="order.myReview"
              type="success"
              :closable="false"
              show-icon
              class="my-review-alert"
            >
              <template #title>你已提交评价</template>
              <div class="my-review-body">
                <StarRating :value="order.myReview.rating" />
                <span>{{ order.myReview.content || '未填写评价内容' }}</span>
              </div>
            </el-alert>
          </section>
        </div>

        <aside class="order-side">
          <section class="surface action-card">
            <h2 class="section-title"><span class="bar"></span>可执行操作</h2>
            <div class="action-list">
              <el-button
                v-if="order.actions?.canAccept"
                type="primary"
                :loading="actionLoading === 'accept'"
                :disabled="!!actionLoading"
                @click="handleAccept"
              >
                接受请求
              </el-button>
              <el-button
                v-if="order.actions?.canReject"
                type="danger"
                plain
                :disabled="!!actionLoading"
                @click="openReasonDialog('reject')"
              >
                拒绝请求
              </el-button>
              <el-button
                v-if="order.actions?.canCancel"
                type="warning"
                plain
                :disabled="!!actionLoading"
                @click="openReasonDialog('cancel')"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.actions?.canConfirm"
                type="primary"
                :loading="confirmSubmitting"
                :disabled="!!actionLoading"
                @click="openConfirmDialog"
              >
                确认交付
              </el-button>
              <el-button
                v-if="order.actions?.canDispute"
                type="danger"
                plain
                :disabled="!!actionLoading"
                @click="openReasonDialog('dispute')"
              >
                提交争议
              </el-button>
              <el-button
                v-if="order.actions?.canReview"
                type="success"
                plain
                @click="scrollToReview"
              >
                去评价
              </el-button>
              <p v-if="!hasActions" class="muted small action-empty">当前状态下暂无可执行操作。</p>
            </div>

            <div class="action-note">
              <el-icon><WarningFilled /></el-icon>
              <span>平台不代管资金，请当面验货后再付款。</span>
            </div>
          </section>

          <section class="surface logs-card">
            <button type="button" class="logs-toggle" @click="toggleLogs">
              <span class="logs-title">
                <span class="bar" />
                交付变更记录
              </span>
              <el-icon class="logs-arrow" :class="{ rotated: logsExpanded }"><ArrowDown /></el-icon>
            </button>
            <div v-show="logsExpanded" class="logs-body">
              <div v-if="logsLoading" class="logs-status muted small">
                <el-icon class="is-loading"><Loading /></el-icon>
                加载中…
              </div>
              <div v-else-if="logsError" class="logs-status">
                <span class="muted small">变更记录加载失败</span>
                <el-button link type="primary" @click="loadHandoverLogs">重新加载</el-button>
              </div>
              <div v-else-if="!handoverLogs.length" class="logs-status muted small">
                暂无交付变更记录
              </div>
              <ul v-else class="log-list">
                <li v-for="log in handoverLogs" :key="log.id" class="log-item">
                  <span class="log-dot" />
                  <div class="log-content">
                    <div class="log-summary">{{ log.summary }}</div>
                    <div class="muted small">{{ log.changedByName }} · {{ formatDateTime(log.createdAt) }}</div>
                  </div>
                </li>
              </ul>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <el-dialog
      v-model="reasonDialogVisible"
      :title="reasonDialogTitle"
      width="min(480px, 94vw)"
      :close-on-click-modal="false"
      append-to-body
    >
      <p class="dialog-hint">{{ reasonDialogHint }}</p>
      <div v-if="reasonAction === 'cancel'" class="preset-list">
        <el-button
          v-for="preset in cancelReasonPresets"
          :key="preset"
          size="small"
          round
          :type="reasonText === preset ? 'primary' : 'default'"
          @click="reasonText = preset"
        >
          {{ preset }}
        </el-button>
      </div>
      <el-input
        v-model="reasonText"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        :placeholder="reasonPlaceholder"
      />
      <p class="dialog-tip muted small">理由至少 2 个字、最多 200 个字，将同步给对方并记录在订单中。</p>
      <template #footer>
        <el-button @click="reasonDialogVisible = false">返回</el-button>
        <el-button
          :type="reasonAction === 'cancel' ? 'warning' : 'danger'"
          :loading="reasonSubmitting"
          @click="submitReason"
        >
          {{ reasonConfirmText }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="confirmDialogVisible"
      title="确认交付"
      width="min(520px, 94vw)"
      :close-on-click-modal="false"
      append-to-body
    >
      <RiskNotice />
      <p class="confirm-description">
        请确认你已当面验货并完成实物交付。双方都确认后，订单将标记为「已完成」，商品标记为「已售出」。
      </p>
      <el-checkbox v-model="confirmChecked">
        我已当面验货并完成交付，确认无误
      </el-checkbox>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="confirmSubmitting"
          :disabled="!confirmChecked"
          @click="submitConfirm"
        >
          确认交付
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="handoverDialogVisible"
      title="修改交付约定"
      width="min(520px, 94vw)"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="约定交付时间">
          <el-date-picker
            v-model="handoverForm.handoverTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            placeholder="选择交付时间"
            clearable
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="交付区域">
          <el-select
            v-model="handoverForm.area"
            filterable
            allow-create
            default-first-option
            clearable
            :value-on-clear="''"
            placeholder="选择或输入校内交付区域"
            class="full-width"
          >
            <el-option v-for="areaItem in areaOptions" :key="areaItem" :label="areaItem" :value="areaItem" />
          </el-select>
        </el-form-item>
        <el-form-item label="交付备注">
          <el-input
            v-model="handoverForm.note"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="例如：在东区宿舍门口，到了电话联系"
          />
        </el-form-item>
      </el-form>
      <p class="dialog-tip muted small">保存后将通知对方，并记录本次交付变更。</p>
      <template #footer>
        <el-button @click="handoverDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handoverSubmitting" @click="submitHandover">保存约定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { HandoverLogVO, OrderVO, ReviewVO } from '@/types'
import { orderApi } from '@/api/order'
import { areas as fallbackAreas, cancelReasonPresets } from '@/utils/dict'
import { formatDateTime, formatPrice, remainText } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import StarRating from '@/components/StarRating.vue'
import RiskNotice from '@/components/RiskNotice.vue'

type ReasonAction = 'reject' | 'cancel' | 'dispute'
type AlertType = 'info' | 'warning' | 'error'

interface AbnormalInfo {
  type: AlertType
  title: string
  description: string
}

interface HandoverForm {
  handoverTime: string | null
  area: string
  note: string
}

const route = useRoute()
const router = useRouter()

const order = ref<OrderVO | null>(null)
const loading = ref(true)
const loadError = ref(false)

const clock = ref(0)
let clockTimer: number | undefined

const actionLoading = ref<'' | 'accept'>('')
const confirmDialogVisible = ref(false)
const confirmChecked = ref(false)
const confirmSubmitting = ref(false)

const reasonDialogVisible = ref(false)
const reasonAction = ref<ReasonAction>('reject')
const reasonText = ref('')
const reasonSubmitting = ref(false)

const handoverDialogVisible = ref(false)
const handoverSubmitting = ref(false)
const handoverForm = reactive<HandoverForm>({
  handoverTime: null,
  area: '',
  note: ''
})

const reviewSectionRef = ref<HTMLElement | null>(null)
const reviewForm = reactive({ rating: 0, content: '' })
const reviewSubmitting = ref(false)
const ratingTexts = ['很差', '较差', '一般', '满意', '非常满意']

const logsExpanded = ref(false)
const logsLoading = ref(false)
const logsLoaded = ref(false)
const logsError = ref(false)
const handoverLogs = ref<HandoverLogVO[]>([])

let detailSeq = 0
let logsSeq = 0

const remainingText = computed(() => {
  void clock.value
  if (order.value?.status !== 'PENDING_SELLER') return ''
  return remainText(order.value?.expireAt) || '等待卖家确认'
})

const isClosedAbnormally = computed(() =>
  ['CANCELLED', 'DISPUTE', 'MANAGEMENT_CLOSED'].includes(order.value?.status ?? '')
)

const showSteps = computed(() => {
  if (!order.value) return false
  return !isClosedAbnormally.value
})

const activeStep = computed(() => {
  switch (order.value?.status) {
    case 'PENDING_SELLER':
      return 1
    case 'DELIVERING':
    case 'BUYER_CONFIRMED':
    case 'SELLER_CONFIRMED':
      return 2
    case 'COMPLETED':
      return 4
    default:
      return 0
  }
})

const processStatus = computed<'process' | 'success'>(() =>
  order.value?.status === 'COMPLETED' ? 'success' : 'process'
)

const stepDescriptions = computed<string[]>(() => {
  const current = order.value
  if (!current) return ['', '', '', '']
  return [
    `已于 ${formatDateTime(current.createdAt)} 提交请求`,
    current.acceptedAt ? `卖家已于 ${formatDateTime(current.acceptedAt)} 接受` : '等待卖家确认',
    confirmationDescription(current),
    current.completedAt ? `完成于 ${formatDateTime(current.completedAt)}` : '双方确认后完成'
  ]
})

const abnormalInfo = computed<AbnormalInfo | null>(() => {
  const current = order.value
  if (!current) return null
  if (current.status === 'CANCELLED') {
    const detail = [current.cancelTypeText, current.cancelReason].filter(Boolean).join('：')
    return {
      type: 'info',
      title: '订单已取消',
      description: detail || '该订单已取消，商品已恢复可售状态。'
    }
  }
  if (current.status === 'DISPUTE') {
    return {
      type: 'error',
      title: '订单争议中',
      description: current.disputeReason
        ? `争议原因：${current.disputeReason}。商品将保持锁定，等待平台管理员处理。`
        : '该订单已进入争议处理，商品保持锁定，等待平台管理员处理。'
    }
  }
  if (current.status === 'MANAGEMENT_CLOSED') {
    return {
      type: 'error',
      title: '订单已由平台关闭',
      description: current.cancelReason || '管理员已关闭该订单，如有疑问请联系平台。'
    }
  }
  return null
})

const hasActions = computed(() => {
  const actions = order.value?.actions
  if (!actions) return false
  return (
    actions.canAccept ||
    actions.canReject ||
    actions.canCancel ||
    actions.canConfirm ||
    actions.canDispute ||
    actions.canReview
  )
})

const areaOptions = computed(() => {
  const result = new Set<string>(fallbackAreas)
  const currentArea = order.value?.handover?.area
  if (currentArea) result.add(currentArea)
  if (handoverForm.area) result.add(handoverForm.area)
  return Array.from(result)
})

const reasonDialogTitle = computed(() => {
  switch (reasonAction.value) {
    case 'reject':
      return '拒绝购买请求'
    case 'cancel':
      return '取消订单'
    default:
      return '提交争议'
  }
})

const reasonDialogHint = computed(() => {
  switch (reasonAction.value) {
    case 'reject':
      return '拒绝后该购买请求会取消，商品仍保持可售。请说明拒绝原因。'
    case 'cancel':
      return order.value?.status === 'PENDING_SELLER'
        ? '取消后该购买请求会失效。若商品无人确认，平台会将商品恢复在售。'
        : '双方都尚未确认交付时可取消订单，取消后商品恢复在售。'
    default:
      return '提交争议后订单进入争议状态，商品保持锁定，请说明具体情况并等待平台处理。'
  }
})

const reasonPlaceholder = computed(() => {
  switch (reasonAction.value) {
    case 'reject':
      return '请填写拒绝原因（2-200 字）'
    case 'cancel':
      return '请填写取消原因，或选择上方常用原因'
    default:
      return '请描述争议原因与经过（2-200 字）'
  }
})

const reasonConfirmText = computed(() => {
  switch (reasonAction.value) {
    case 'reject':
      return '确认拒绝'
    case 'cancel':
      return '确认取消'
    default:
      return '确认提交争议'
  }
})

function parseOrderId(value: string | string[]): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  if (!raw || !/^\d+$/.test(raw)) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function confirmationDescription(current: OrderVO): string {
  const buyerConfirmed = current.buyerConfirmedAt
  const sellerConfirmed = current.sellerConfirmedAt
  if (buyerConfirmed && sellerConfirmed) return '双方均已确认交付'
  if (buyerConfirmed) return `买家已于 ${formatDateTime(buyerConfirmed)} 确认，待卖家确认`
  if (sellerConfirmed) return `卖家已于 ${formatDateTime(sellerConfirmed)} 确认，待买家确认`
  return '待双方确认交付'
}

function resetLogs(): void {
  logsSeq += 1
  logsExpanded.value = false
  logsLoaded.value = false
  logsLoading.value = false
  logsError.value = false
  handoverLogs.value = []
}

async function loadOrder(silent = false): Promise<void> {
  const id = parseOrderId(route.params.id)
  const seq = ++detailSeq

  if (!silent) {
    loading.value = true
    loadError.value = false
    order.value = null
    reasonDialogVisible.value = false
    confirmDialogVisible.value = false
    handoverDialogVisible.value = false
    reviewForm.rating = 0
    reviewForm.content = ''
    resetLogs()
  }

  if (id === null) {
    if (!silent) {
      loading.value = false
      loadError.value = true
    }
    return
  }

  try {
    const detail = await orderApi.detail(id)
    if (seq !== detailSeq) return
    order.value = detail
  } catch {
    if (!silent && seq === detailSeq) loadError.value = true
  } finally {
    if (!silent && seq === detailSeq) loading.value = false
  }
}

async function handleAccept(): Promise<void> {
  const current = order.value
  if (!current || actionLoading.value) return

  try {
    await ElMessageBox.confirm(
      '接受后商品会被锁定，同一商品的其他待确认请求将自动取消。请与买家约定交付时间与地点，并完成线下交易。',
      '接受购买请求',
      {
        type: 'warning',
        confirmButtonText: '确认接受',
        cancelButtonText: '再想想'
      }
    )
  } catch {
    return
  }

  actionLoading.value = 'accept'
  try {
    await orderApi.accept(current.id)
    ElMessage.success('已接受请求，请与买家约定交付')
    await loadOrder(true)
  } catch {
    /* 拦截器已统一提示，保持当前订单状态 */
  } finally {
    actionLoading.value = ''
  }
}

function openReasonDialog(action: ReasonAction): void {
  reasonAction.value = action
  reasonText.value = ''
  reasonDialogVisible.value = true
}

async function submitReason(): Promise<void> {
  const current = order.value
  if (!current || reasonSubmitting.value) return

  const reason = reasonText.value.trim()
  if (reason.length < 2) {
    ElMessage.warning('请填写至少 2 个字的理由')
    return
  }
  if (reason.length > 200) {
    ElMessage.warning('理由不能超过 200 个字')
    return
  }

  reasonSubmitting.value = true
  try {
    if (reasonAction.value === 'reject') {
      await orderApi.reject(current.id, reason)
      ElMessage.success('已拒绝该购买请求')
    } else if (reasonAction.value === 'cancel') {
      await orderApi.cancel(current.id, reason)
      ElMessage.success('订单已取消')
    } else {
      await orderApi.dispute(current.id, reason)
      ElMessage.success('争议已提交，平台会尽快处理')
    }
    reasonDialogVisible.value = false
    await loadOrder(true)
  } catch {
    /* 拦截器已统一提示，保留弹窗与已填写理由 */
  } finally {
    reasonSubmitting.value = false
  }
}

function openConfirmDialog(): void {
  confirmChecked.value = false
  confirmDialogVisible.value = true
}

async function submitConfirm(): Promise<void> {
  const current = order.value
  if (!current || confirmSubmitting.value || !confirmChecked.value) return

  confirmSubmitting.value = true
  try {
    const result = await orderApi.confirm(current.id)
    confirmDialogVisible.value = false
    ElMessage.success(
      result.status === 'COMPLETED' ? '双方均已确认，订单已完成' : '已确认交付，等待对方确认'
    )
    await loadOrder(true)
  } catch {
    /* 拦截器已统一提示，保留弹窗与勾选状态 */
  } finally {
    confirmSubmitting.value = false
  }
}

function openHandoverDialog(): void {
  const current = order.value
  if (!current) return
  handoverForm.handoverTime = current.handover?.handoverTime || null
  handoverForm.area = current.handover?.area || ''
  handoverForm.note = current.handover?.note || ''
  handoverDialogVisible.value = true
}

async function submitHandover(): Promise<void> {
  const current = order.value
  if (!current || handoverSubmitting.value) return

  const area = (handoverForm.area || '').trim()
  const note = (handoverForm.note || '').trim()
  if (!handoverForm.handoverTime && !area && !note) {
    ElMessage.warning('请至少填写时间、区域或备注中的一项')
    return
  }
  if (area.length > 50) {
    ElMessage.warning('交付区域不能超过 50 个字')
    return
  }
  if (note.length > 500) {
    ElMessage.warning('交付备注不能超过 500 个字')
    return
  }

  handoverSubmitting.value = true
  try {
    await orderApi.updateHandover(current.id, {
      handoverTime: handoverForm.handoverTime || null,
      area,
      note
    })
    handoverDialogVisible.value = false
    ElMessage.success('交付约定已更新')
    await loadOrder(true)
    logsLoaded.value = false
    logsError.value = false
    handoverLogs.value = []
    if (logsExpanded.value) void loadHandoverLogs()
  } catch {
    /* 拦截器已统一提示，保留弹窗与输入内容 */
  } finally {
    handoverSubmitting.value = false
  }
}

function reviewRole(review: ReviewVO): string {
  const current = order.value
  if (!current) return '评价'
  return review.authorId === current.buyer.id ? '买家评价' : '卖家评价'
}

function scrollToReview(): void {
  reviewSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function submitReview(): Promise<void> {
  const current = order.value
  if (!current || reviewSubmitting.value) return
  if (!reviewForm.rating) {
    ElMessage.warning('请先选择评分')
    return
  }

  reviewSubmitting.value = true
  try {
    await orderApi.createReview(current.id, {
      rating: reviewForm.rating,
      content: reviewForm.content.trim()
    })
    ElMessage.success('评价已提交，感谢你的反馈')
    reviewForm.rating = 0
    reviewForm.content = ''
    await loadOrder(true)
  } catch {
    /* 拦截器已统一提示，保留已填写评价 */
  } finally {
    reviewSubmitting.value = false
  }
}

async function loadHandoverLogs(): Promise<void> {
  const current = order.value
  if (!current || logsLoading.value) return
  const seq = ++logsSeq
  logsLoading.value = true
  logsError.value = false
  try {
    const logs = await orderApi.handoverLogs(current.id)
    if (seq !== logsSeq) return
    handoverLogs.value = logs
    logsLoaded.value = true
  } catch {
    if (seq === logsSeq) logsError.value = true
  } finally {
    if (seq === logsSeq) logsLoading.value = false
  }
}

async function toggleLogs(): Promise<void> {
  if (logsExpanded.value) {
    logsExpanded.value = false
    return
  }
  logsExpanded.value = true
  if (!logsLoaded.value && !logsLoading.value) await loadHandoverLogs()
}

watch(() => route.params.id, () => {
  void loadOrder()
}, { immediate: true })

onMounted(() => {
  clockTimer = window.setInterval(() => {
    clock.value += 1
  }, 30000)
})

onUnmounted(() => {
  if (clockTimer !== undefined) {
    window.clearInterval(clockTimer)
    clockTimer = undefined
  }
})
</script>

<style scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-back {
  margin-bottom: -4px;
}

.order-back a {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--ink-500);
}

.order-back a:hover {
  color: var(--brand-600);
}

.order-loading,
.order-missing {
  padding: 24px;
}

.order-header {
  padding: 20px 22px;
}

.order-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.order-title-main {
  min-width: 0;
}

.order-no {
  font-size: 12px;
  color: var(--ink-500);
  letter-spacing: 0.3px;
}

.order-title {
  margin: 6px 0 0;
  font-size: 21px;
  line-height: 1.4;
  color: var(--ink-900);
  word-break: break-word;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--ink-500);
}

.order-status {
  flex-shrink: 0;
  margin-top: 2px;
}

.status-alert {
  margin-top: 16px;
  border-radius: 10px;
}

.order-steps {
  margin-top: 24px;
  padding: 4px 0 0;
}

.order-steps :deep(.el-step__description) {
  font-size: 11px;
  line-height: 1.6;
  padding: 0 8px;
}

.order-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 16px;
  align-items: start;
}

.order-main,
.order-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.order-item-link {
  display: flex;
  align-items: center;
  gap: 14px;
  color: inherit;
}

.order-item-cover {
  width: 86px;
  height: 86px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 10px;
  background: var(--brand-50);
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-300);
}

.order-item-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.order-item-info strong {
  font-size: 15px;
  color: var(--ink-900);
  word-break: break-word;
}

.price-text {
  color: #d9480f;
  font-size: 19px;
  font-weight: 700;
}

.parties-card .section-title {
  margin-bottom: 14px;
}

.parties-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.party-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdfc;
}

.party-info {
  min-width: 0;
}

.party-name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.party-name strong {
  font-size: 14px;
  color: var(--ink-900);
}

.exchange-icon {
  flex-shrink: 0;
  color: var(--ink-300);
}

.request-remark {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--brand-50);
}

.request-remark p {
  margin: 5px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.section-head .section-title {
  margin-bottom: 0;
}

.handover-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.handover-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 11px 12px;
  border-radius: 9px;
  background: var(--brand-50);
}

.handover-full {
  grid-column: 1 / -1;
}

.handover-label {
  font-size: 11px;
  color: var(--ink-500);
}

.handover-field strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
}

.handover-note {
  white-space: pre-wrap;
  word-break: break-word;
  font-weight: 400 !important;
  line-height: 1.7;
}

.handover-updated {
  margin: 9px 0 0;
}

.confirm-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}

.confirm-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-500);
}

.confirm-item .el-icon {
  font-size: 15px;
  color: var(--ink-300);
}

.confirm-item.done {
  color: var(--brand-700);
}

.confirm-item.done .el-icon {
  color: var(--brand-500);
}

.handover-empty-tip {
  margin: 12px 0 0;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-item {
  display: flex;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--line);
}

.review-item:last-of-type {
  padding-bottom: 0;
  border-bottom: none;
}

.review-main {
  flex: 1;
  min-width: 0;
}

.review-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.review-head strong {
  font-size: 13px;
  color: var(--ink-900);
}

.review-content {
  margin: 7px 0 4px;
  color: var(--ink-700);
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.review-form {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdfc;
}

.review-form-title {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--ink-900);
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.field-label {
  font-size: 12px;
  color: var(--ink-500);
}

.review-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.my-review-alert {
  margin-top: 14px;
  border-radius: 10px;
}

.my-review-body {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.action-card .section-title {
  margin-bottom: 14px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.action-list .el-button {
  width: 100%;
  margin-left: 0;
}

.action-empty {
  margin: 0;
  text-align: center;
}

.action-note {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
  font-size: 11px;
  line-height: 1.6;
  color: var(--ink-500);
}

.action-note .el-icon {
  margin-top: 2px;
  color: var(--el-color-warning);
  flex-shrink: 0;
}

.logs-card {
  padding: 0;
  overflow: hidden;
}

.logs-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 18px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.logs-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-900);
}

.logs-title .bar {
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: var(--brand-500);
}

.logs-arrow {
  color: var(--ink-500);
  transition: transform 0.2s ease;
}

.logs-arrow.rotated {
  transform: rotate(180deg);
}

.logs-body {
  padding: 0 18px 16px;
  border-top: 1px dashed var(--line);
}

.logs-status {
  display: flex;
  align-items: center;
  gap: 7px;
  padding-top: 12px;
}

.log-list {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.log-item {
  position: relative;
  display: flex;
  gap: 10px;
  padding-bottom: 14px;
}

.log-item:last-child {
  padding-bottom: 0;
}

.log-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 12px;
  bottom: 0;
  width: 1px;
  background: var(--line);
}

.log-dot {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  width: 9px;
  height: 9px;
  margin-top: 4px;
  border-radius: 50%;
  background: var(--brand-500);
  box-shadow: 0 0 0 3px var(--brand-100);
}

.log-content {
  min-width: 0;
}

.log-summary {
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-700);
  word-break: break-word;
}

.dialog-hint {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
}

.dialog-tip {
  margin: 8px 0 0;
  line-height: 1.6;
}

.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.confirm-description {
  margin: 12px 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-700);
}

.full-width {
  width: 100%;
}

@media (max-width: 900px) {
  .order-grid {
    grid-template-columns: 1fr;
  }

  .order-side {
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: start;
  }
}

@media (max-width: 768px) {
  .order-header {
    padding: 16px;
  }

  .order-title-row {
    flex-direction: column;
    gap: 8px;
  }

  .order-title {
    font-size: 18px;
  }

  .order-status {
    align-self: flex-start;
  }

  .order-steps :deep(.el-step__title) {
    font-size: 12px;
  }

  .order-steps :deep(.el-step__description) {
    display: none;
  }

  .parties-row {
    flex-direction: column;
    align-items: stretch;
  }

  .exchange-icon {
    align-self: center;
    transform: rotate(90deg);
  }

  .handover-grid {
    grid-template-columns: 1fr;
  }

  .order-side {
    display: flex;
  }

  .action-list {
    flex-direction: column;
  }

  .review-head {
    align-items: flex-start;
  }
}
</style>
