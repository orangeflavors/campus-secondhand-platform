<template>
  <div class="page-container detail-page">
    <nav class="breadcrumb">
      <router-link to="/">校园集市</router-link>
      <el-icon><ArrowRight /></el-icon>
      <span>商品详情</span>
    </nav>

    <div v-if="loading" class="surface detail-loading">
      <el-skeleton animated>
        <template #template>
          <div class="loading-grid">
            <el-skeleton-item variant="image" class="loading-image" />
            <div class="loading-info">
              <el-skeleton-item variant="h1" style="width: 72%" />
              <el-skeleton-item variant="text" style="width: 30%" />
              <el-skeleton-item variant="text" style="width: 58%" />
              <el-skeleton-item variant="text" style="width: 46%" />
              <el-skeleton-item variant="image" style="width: 100%; height: 92px; margin-top: 18px" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <section v-else-if="loadError || !item" class="surface detail-missing">
      <EmptyState
        description="商品不存在、已下架，或你暂时无权查看"
        action-text="返回市场"
        @action="router.push('/')"
      />
    </section>

    <template v-else>
      <section class="surface detail-card">
        <div class="gallery">
          <div class="main-image">
            <el-image
              :src="mainImage"
              fit="contain"
              :preview-src-list="galleryImages"
              :initial-index="activeIndex"
              preview-teleported
              class="main-image-content"
            >
              <template #error>
                <div class="image-fallback">
                  <el-icon><Picture /></el-icon>
                  <span>暂无图片</span>
                </div>
              </template>
            </el-image>
            <StatusTag kind="item" :value="item.status" effect="dark" class="image-status" />
            <span v-if="itemImages.length" class="image-counter">{{ activeIndex + 1 }} / {{ itemImages.length }}</span>
          </div>

          <div v-if="itemImages.length > 1" class="thumb-list">
            <button
              v-for="(image, index) in itemImages"
              :key="`${image}-${index}`"
              type="button"
              class="thumb-button"
              :class="{ active: index === activeIndex }"
              :aria-label="`查看第 ${index + 1} 张图片`"
              @click="activeIndex = index"
            >
              <el-image :src="image" fit="cover" class="thumb-image">
                <template #error>
                  <div class="thumb-fallback"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
            </button>
          </div>
        </div>

        <div class="info-panel">
          <div class="title-row">
            <h1 class="item-title">{{ item.title }}</h1>
            <StatusTag kind="item" :value="item.status" />
          </div>

          <div class="price-row">
            <span class="price-text" :class="{ 'is-free': item.price === 0 }">{{ formatPrice(item.price) }}</span>
            <span class="stats">
              <span><el-icon><View /></el-icon>{{ item.viewCount }} 浏览</span>
              <span><el-icon><Star /></el-icon>{{ item.favoriteCount }} 收藏</span>
            </span>
          </div>

          <div class="meta-row">
            <span class="meta-item">
              <el-icon><CollectionTag /></el-icon>
              {{ item.conditionText || labelOf(conditionLabels, item.condition) }}
            </span>
            <span class="meta-item">
              <el-icon><Menu /></el-icon>
              {{ item.categoryName }}
            </span>
            <span class="meta-item">
              <el-icon><Location /></el-icon>
              {{ item.handoverArea || '交付区域面议' }}
            </span>
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatDateTime(item.listedAt || item.createdAt) }}
            </span>
          </div>

          <div class="seller-card">
            <UserAvatar :nickname="item.seller.nickname" :src="item.seller.avatarUrl" :size="50" />
            <div class="seller-main">
              <div class="seller-name-row">
                <strong>{{ item.seller.nickname }}</strong>
                <el-tag v-if="item.isOwner" size="small" round type="info">我发布的</el-tag>
                <StatusTag v-if="item.seller.authStatus === 'VERIFIED'" kind="auth" value="VERIFIED" />
              </div>
              <div class="seller-sub">
                <span>{{ item.seller.college || '未填写学院' }}</span>
                <span v-if="item.seller.memberSince">加入于 {{ formatDate(item.seller.memberSince) }}</span>
                <span>成交 {{ item.seller.soldCount }} 笔</span>
              </div>
              <div class="seller-rating">
                <StarRating :value="item.seller.avgRating || 0" show-text />
                <span class="muted small">{{ item.seller.reviewCount }} 条评价</span>
              </div>
            </div>
          </div>

          <div v-if="item.isOwner" class="owner-panel">
            <el-alert
              v-if="item.status === 'REJECTED' && item.rejectReason"
              type="error"
              :closable="false"
              show-icon
              class="owner-alert"
            >
              <template #title>审核驳回原因</template>
              {{ item.rejectReason }}
            </el-alert>
            <el-alert
              v-else-if="ownerStatusHint"
              :type="item.status === 'ON_SALE' ? 'success' : 'info'"
              :closable="false"
              show-icon
              class="owner-alert"
            >
              <template #title>商品状态提示</template>
              {{ ownerStatusHint }}
            </el-alert>

            <div class="owner-actions">
              <el-button v-if="canEditItem" type="primary" plain round @click="goEdit">
                <el-icon><Edit /></el-icon>
                编辑商品
              </el-button>
              <el-button
                v-if="canSubmitReview"
                type="success"
                round
                :loading="submitLoading"
                @click="handleSubmitReview"
              >
                <el-icon><DocumentChecked /></el-icon>
                提交审核
              </el-button>
              <el-button
                v-if="canOffShelf"
                type="warning"
                plain
                round
                :loading="offShelfLoading"
                @click="handleOffShelf"
              >
                下架商品
              </el-button>
              <el-button v-if="item.activeOrderId" round @click="goOrder(item.activeOrderId)">
                查看进行中订单
              </el-button>
            </div>
          </div>

          <div v-else class="buyer-panel">
            <RiskNotice :text="item.riskNotice" class="buyer-risk" />

            <div class="buyer-actions">
              <el-button
                type="primary"
                size="large"
                round
                :disabled="!item.canOrder"
                :loading="orderSubmitting"
                class="purchase-button"
                @click="openPurchaseDialog"
              >
                <el-icon><ShoppingCart /></el-icon>
                申请购买
              </el-button>
              <el-button size="large" round :loading="consultLoading" @click="handleConsult">
                <el-icon><ChatDotRound /></el-icon>
                咨询卖家
              </el-button>
              <el-button
                size="large"
                round
                :loading="favoriteLoading"
                class="favorite-button"
                @click="toggleFavorite"
              >
                <el-icon v-if="item.isFavorite"><StarFilled /></el-icon>
                <el-icon v-else><Star /></el-icon>
                {{ item.isFavorite ? '已收藏' : '收藏' }}
              </el-button>
            </div>

            <p v-if="!item.canOrder" class="cannot-order">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ item.cannotOrderReason || '当前暂不可购买该商品' }}</span>
            </p>
            <p v-else class="order-hint">
              支持当面验货、当面付款；平台不代管资金，请勿提前转账。
            </p>

            <router-link
              v-if="item.activeOrderId"
              :to="`/me/orders/${item.activeOrderId}`"
              class="active-order-link"
            >
              你与该商品有一条进行中的订单，点击查看订单详情 →
            </router-link>

            <div class="minor-actions">
              <el-button text type="danger" @click="openReportDialog">
                <el-icon><Flag /></el-icon>
                举报商品
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <div class="detail-bottom">
        <section class="surface description-card">
          <h2 class="section-title"><span class="bar"></span>商品描述</h2>
          <p class="description-text">{{ item.description || '卖家暂未填写商品描述。' }}</p>

          <div class="description-meta">
            <span>分类：{{ item.categoryName }}</span>
            <span>成色：{{ item.conditionText || labelOf(conditionLabels, item.condition) }}</span>
            <span>交付区域：{{ item.handoverArea || '面议' }}</span>
            <span>发布时间：{{ formatDateTime(item.listedAt || item.createdAt) }}</span>
          </div>
        </section>

        <aside class="detail-side">
          <section class="surface safety-card">
            <h2 class="section-title"><span class="bar"></span>交易安全</h2>
            <RiskNotice :text="item.riskNotice" />
            <ul class="safety-list">
              <li>约在图书馆、食堂等校内公共区域当面交易。</li>
              <li>验货后再付款，不向陌生人提前转账。</li>
              <li>发现虚假信息或诈骗行为，请及时举报。</li>
            </ul>
          </section>
        </aside>
      </div>

      <section class="surface reviews-card">
        <div class="reviews-head">
          <h2 class="section-title"><span class="bar"></span>卖家收到的评价</h2>
          <div class="reviews-summary">
            <StarRating :value="item.seller.avgRating || 0" show-text />
            <span class="muted small">{{ item.seller.reviewCount }} 条评价</span>
          </div>
        </div>

        <div v-if="reviewLoading" class="review-loading">
          <el-skeleton animated :rows="3" />
        </div>
        <EmptyState v-else-if="!sellerReviews.length" description="卖家暂无收到的评价" :image-size="80" />
        <div v-else class="review-list">
          <article v-for="review in sellerReviews" :key="review.id" class="review-item">
            <UserAvatar :nickname="review.authorNickname" :src="review.authorAvatarUrl" :size="38" />
            <div class="review-main">
              <div class="review-head">
                <strong>{{ review.authorNickname }}</strong>
                <StarRating :value="review.rating" />
                <span class="muted small">{{ formatDateTime(review.createdAt) }}</span>
              </div>
              <p class="review-content">{{ review.content || '该用户未填写评价内容。' }}</p>
              <span class="review-target muted small">评价对象：{{ review.targetNickname }}</span>
            </div>
          </article>
          <p v-if="sellerReviewTotal > sellerReviews.length" class="review-more muted small">
            仅展示最近 {{ sellerReviews.length }} 条评价，共 {{ sellerReviewTotal }} 条。
          </p>
        </div>
      </section>
    </template>

    <el-dialog
      v-model="purchaseVisible"
      title="申请购买"
      width="min(520px, 94vw)"
      :close-on-click-modal="false"
      append-to-body
    >
      <div v-if="item" class="dialog-item-summary">
        <el-image :src="item.coverUrl || '/images/placeholder.svg'" fit="cover" class="dialog-item-cover">
          <template #error>
            <div class="thumb-fallback"><el-icon><Picture /></el-icon></div>
          </template>
        </el-image>
        <div class="dialog-item-info">
          <strong>{{ item.title }}</strong>
          <span class="price-text">{{ formatPrice(item.price) }}</span>
          <span class="muted small">卖家：{{ item.seller.nickname }}</span>
        </div>
      </div>

      <RiskNotice :text="item?.riskNotice || undefined" class="dialog-risk" />

      <el-form label-position="top" class="dialog-form">
        <el-form-item label="给卖家的备注（选填）">
          <el-input
            v-model="purchaseRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="例如：希望明天中午在图书馆门口交易，可以吗？"
          />
        </el-form-item>
      </el-form>

      <p class="dialog-tip muted small">
        提交后生成待确认请求，卖家接受后商品会被锁定；请留意订单页面的确认时效。
      </p>

      <template #footer>
        <el-button @click="purchaseVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderSubmitting" @click="submitPurchase">提交购买请求</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reportVisible"
      title="举报商品"
      width="min(480px, 94vw)"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="举报类别" required>
          <el-select v-model="reportForm.category" placeholder="请选择举报类别" class="full-width">
            <el-option
              v-for="option in reportCategoryOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="举报说明" required>
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="请描述具体的违规情况，例如虚假描述、违禁物品、诱导转账等。"
          />
        </el-form-item>
      </el-form>
      <p class="dialog-tip muted small">
        平台会根据举报内容进行核实。请勿恶意举报，举报信息仅用于平台处理与反馈。
      </p>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" :loading="reportSubmitting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ItemDetailVO, ItemStatus, ReviewVO } from '@/types'
import { itemApi } from '@/api/item'
import { favoriteApi, conversationApi, reportApi } from '@/api/social'
import { orderApi } from '@/api/order'
import { authApi } from '@/api/auth'
import {
  conditionLabels,
  labelOf,
  reportCategoryOptions
} from '@/utils/dict'
import { formatDate, formatDateTime, formatPrice } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import StarRating from '@/components/StarRating.vue'
import RiskNotice from '@/components/RiskNotice.vue'

interface ReportForm {
  category: string
  description: string
}

const route = useRoute()
const router = useRouter()

const item = ref<ItemDetailVO | null>(null)
const loading = ref(true)
const loadError = ref(false)
const activeIndex = ref(0)

const sellerReviews = ref<ReviewVO[]>([])
const sellerReviewTotal = ref(0)
const reviewLoading = ref(false)

const favoriteLoading = ref(false)
const consultLoading = ref(false)
const submitLoading = ref(false)
const offShelfLoading = ref(false)

const purchaseVisible = ref(false)
const purchaseRemark = ref('')
const orderSubmitting = ref(false)

const reportVisible = ref(false)
const reportSubmitting = ref(false)
const reportForm = reactive<ReportForm>({ category: '', description: '' })

let detailSeq = 0
let reviewSeq = 0

const itemImages = computed<string[]>(() => item.value?.images ?? [])

const galleryImages = computed<string[]>(() => {
  if (!itemImages.value.length) return ['/images/placeholder.svg']
  return itemImages.value
})

const mainImage = computed(() => galleryImages.value[activeIndex.value] || '/images/placeholder.svg')

const canEditItem = computed(() => {
  if (!item.value?.isOwner) return false
  return ['DRAFT', 'REJECTED', 'OFF_SHELF', 'ON_SALE'].includes(item.value.status)
})

const canSubmitReview = computed(() => {
  if (!item.value?.isOwner) return false
  return ['DRAFT', 'REJECTED', 'OFF_SHELF'].includes(item.value.status)
})

const canOffShelf = computed(() => item.value?.isOwner === true && item.value.status === 'ON_SALE')

const ownerStatusHint = computed(() => {
  const current = item.value
  if (!current) return ''
  const hints: Partial<Record<ItemStatus, string>> = {
    DRAFT: '商品还是草稿状态，完善信息后可提交审核。',
    PENDING: '商品正在审核中，审核通过后会自动上架，请耐心等待。',
    REJECTED: '审核未通过，请根据驳回原因修改后重新提交审核。',
    ON_SALE: '商品正在出售中。修改标题、分类、价格、描述、成色等关键信息后会自动重新进入审核。',
    LOCKED: '商品已被进行中的订单锁定，暂时无法编辑或下架。',
    SOLD: '商品已售出，感谢你让闲置继续流转。',
    OFF_SHELF: '商品已下架，可修改后重新提交审核。',
    VIOLATED: '商品因违规被下架，如有疑问请联系平台处理。'
  }
  return hints[current.status] || ''
})

function parseItemId(value: string | string[]): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  if (!raw || !/^\d+$/.test(raw)) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

async function loadSellerReviews(sellerId: number): Promise<void> {
  const seq = ++reviewSeq
  reviewLoading.value = true
  try {
    const result = await authApi.userReviews(sellerId, { page: 1, size: 10 })
    if (seq !== reviewSeq) return
    sellerReviews.value = result.records
    sellerReviewTotal.value = result.total
  } catch {
    if (seq === reviewSeq) {
      sellerReviews.value = []
      sellerReviewTotal.value = 0
    }
  } finally {
    if (seq === reviewSeq) reviewLoading.value = false
  }
}

async function loadDetail(): Promise<void> {
  const id = parseItemId(route.params.id)
  const seq = ++detailSeq
  reviewSeq += 1

  loading.value = true
  loadError.value = false
  item.value = null
  sellerReviews.value = []
  sellerReviewTotal.value = 0
  activeIndex.value = 0
  reviewLoading.value = false

  if (id === null) {
    loading.value = false
    loadError.value = true
    return
  }

  try {
    const detail = await itemApi.detail(id)
    if (seq !== detailSeq) return
    item.value = detail
    activeIndex.value = 0
    void loadSellerReviews(detail.seller.id)
  } catch {
    if (seq === detailSeq) loadError.value = true
  } finally {
    if (seq === detailSeq) loading.value = false
  }
}

async function toggleFavorite(): Promise<void> {
  const current = item.value
  if (!current || favoriteLoading.value) return

  const targetItemId = current.id
  const shouldFavorite = !current.isFavorite
  favoriteLoading.value = true
  try {
    const state = shouldFavorite
      ? await favoriteApi.add(targetItemId)
      : await favoriteApi.remove(targetItemId)
    if (item.value && item.value.id === targetItemId) {
      item.value.isFavorite = state.favorited
      item.value.favoriteCount = state.count
    }
    ElMessage.success(shouldFavorite ? '已加入收藏' : '已取消收藏')
  } catch {
    /* 拦截器已统一提示，保持当前收藏状态 */
  } finally {
    favoriteLoading.value = false
  }
}

async function handleConsult(): Promise<void> {
  const current = item.value
  if (!current || consultLoading.value) return
  consultLoading.value = true
  try {
    const conversation = await conversationApi.create(current.id)
    await router.push({ path: '/messages', query: { conversation: String(conversation.id) } })
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    consultLoading.value = false
  }
}

function openPurchaseDialog(): void {
  if (!item.value?.canOrder) return
  purchaseRemark.value = ''
  purchaseVisible.value = true
}

async function submitPurchase(): Promise<void> {
  const current = item.value
  if (!current || orderSubmitting.value) return
  orderSubmitting.value = true
  try {
    const order = await orderApi.create({
      itemId: current.id,
      remark: purchaseRemark.value.trim() || undefined
    })
    purchaseVisible.value = false
    ElMessage.success('购买请求已提交，等待卖家确认')
    await router.push(`/me/orders/${order.id}`)
  } catch {
    /* 拦截器已统一提示，保持弹窗与输入内容 */
  } finally {
    orderSubmitting.value = false
  }
}

function goEdit(): void {
  const current = item.value
  if (!current) return
  void router.push({ name: 'item-edit', params: { id: current.id } })
}

function goOrder(orderId: number): void {
  void router.push(`/me/orders/${orderId}`)
}

async function handleSubmitReview(): Promise<void> {
  const current = item.value
  if (!current || submitLoading.value) return
  submitLoading.value = true
  try {
    await itemApi.submit(current.id)
    ElMessage.success('已提交审核，请等待平台审核')
    await loadDetail()
  } catch {
    /* 拦截器已统一提示，保持当前状态 */
  } finally {
    submitLoading.value = false
  }
}

async function handleOffShelf(): Promise<void> {
  const current = item.value
  if (!current || offShelfLoading.value) return
  try {
    await ElMessageBox.confirm(
      '下架后商品将不再展示；如有进行中的订单，平台可能不允许下架。你可以在「我的商品」中重新编辑并提交审核。',
      '确认下架商品',
      {
        type: 'warning',
        confirmButtonText: '确认下架',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  offShelfLoading.value = true
  try {
    await itemApi.offShelf(current.id)
    ElMessage.success('商品已下架')
    await loadDetail()
  } catch {
    /* 拦截器已统一提示，保持当前状态 */
  } finally {
    offShelfLoading.value = false
  }
}

function openReportDialog(): void {
  reportForm.category = ''
  reportForm.description = ''
  reportVisible.value = true
}

async function submitReport(): Promise<void> {
  const current = item.value
  if (!current || reportSubmitting.value) return
  const description = reportForm.description.trim()
  if (!reportForm.category) {
    ElMessage.warning('请选择举报类别')
    return
  }
  if (description.length < 2) {
    ElMessage.warning('请填写至少 2 个字的举报说明')
    return
  }

  reportSubmitting.value = true
  try {
    await reportApi.create({
      targetType: 'ITEM',
      targetId: current.id,
      category: reportForm.category,
      description
    })
    reportVisible.value = false
    ElMessage.success('举报已提交，平台会尽快核实处理')
  } catch {
    /* 拦截器已统一提示，保持弹窗与输入内容 */
  } finally {
    reportSubmitting.value = false
  }
}

watch(() => route.params.id, loadDetail, { immediate: true })
</script>

<style scoped>
.detail-page {
  padding-top: 18px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--ink-500);
}

.breadcrumb .el-icon {
  font-size: 12px;
  color: var(--ink-300);
}

.detail-loading {
  padding: 24px;
}

.loading-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.85fr);
  gap: 28px;
}

.loading-image {
  width: 100%;
  height: 400px;
  border-radius: 14px;
}

.loading-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 10px;
}

.detail-missing {
  padding: 30px;
}

.detail-card {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 0.98fr);
  gap: 30px;
  padding: 24px;
}

.gallery {
  min-width: 0;
}

.main-image {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: linear-gradient(135deg, var(--brand-50), #fbfdfc);
  aspect-ratio: 1 / 1;
}

.main-image-content {
  display: block;
  width: 100%;
  height: 100%;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-300);
  font-size: 13px;
}

.image-fallback .el-icon {
  font-size: 54px;
}

.image-status {
  position: absolute;
  top: 12px;
  left: 12px;
}

.image-counter {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 9px;
  border-radius: 12px;
  background: rgba(28, 43, 36, 0.55);
  color: #fff;
  font-size: 11px;
  backdrop-filter: blur(3px);
}

.thumb-list {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.thumb-button {
  flex: 0 0 70px;
  width: 70px;
  height: 70px;
  padding: 0;
  overflow: hidden;
  border: 2px solid transparent;
  border-radius: 10px;
  background: var(--brand-50);
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.16s ease;
}

.thumb-button:hover,
.thumb-button.active {
  border-color: var(--brand-500);
  opacity: 1;
}

.thumb-image {
  display: block;
  width: 100%;
  height: 100%;
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-300);
  background: var(--brand-50);
}

.info-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.item-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.4;
  font-weight: 700;
  color: var(--ink-900);
  word-break: break-word;
}

.price-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff7f0, #fffdfb);
  border: 1px solid #f7e6d6;
}

.price-text {
  color: #d9480f;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.price-text.is-free {
  color: var(--brand-600);
  font-size: 23px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 12px;
  color: var(--ink-500);
}

.stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 8px;
  background: var(--brand-50);
  color: var(--ink-700);
  font-size: 12px;
}

.meta-item .el-icon {
  color: var(--brand-500);
}

.seller-card {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-top: 20px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fbfdfc;
}

.seller-main {
  flex: 1;
  min-width: 0;
}

.seller-name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
}

.seller-name-row strong {
  font-size: 15px;
  color: var(--ink-900);
}

.seller-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--ink-500);
}

.seller-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
}

.owner-panel,
.buyer-panel {
  margin-top: 18px;
}

.owner-alert {
  margin-bottom: 12px;
  border-radius: 10px;
}

.owner-actions,
.buyer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.buyer-actions {
  align-items: center;
}

.buyer-actions .el-button + .el-button {
  margin-left: 0;
}

.purchase-button {
  min-width: 136px;
}

.favorite-button {
  color: var(--brand-700);
}

.buyer-risk {
  margin-bottom: 14px;
}

.cannot-order {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 12px 0 0;
  padding: 9px 12px;
  border-radius: 8px;
  background: #fff8e6;
  color: #8a5b11;
  font-size: 12px;
  line-height: 1.6;
}

.cannot-order .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.order-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--ink-500);
}

.active-order-link {
  display: inline-block;
  margin-top: 10px;
  font-size: 13px;
}

.minor-actions {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--line);
}

.detail-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  margin-top: 18px;
  align-items: start;
}

.description-card {
  min-height: 240px;
}

.description-text {
  margin: 0;
  min-height: 130px;
  color: var(--ink-700);
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

.description-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
  font-size: 12px;
  color: var(--ink-500);
}

.safety-list {
  margin: 14px 0 0;
  padding-left: 18px;
  color: var(--ink-700);
  font-size: 12px;
  line-height: 1.9;
}

.reviews-card {
  margin-top: 18px;
}

.reviews-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.reviews-head .section-title {
  margin-bottom: 0;
}

.reviews-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-loading {
  padding: 8px 0;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
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
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}

.review-more {
  margin: 4px 0 0;
}

.dialog-item-summary {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: var(--brand-50);
}

.dialog-item-cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #fff;
}

.dialog-item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.dialog-item-info strong {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.dialog-risk {
  margin-bottom: 14px;
}

.dialog-tip {
  margin: 6px 0 0;
  line-height: 1.7;
}

.full-width {
  width: 100%;
}

@media (max-width: 960px) {
  .detail-card {
    grid-template-columns: 1fr;
    gap: 22px;
  }

  .main-image {
    aspect-ratio: 4 / 3;
  }

  .detail-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .detail-page {
    padding-top: 12px;
  }

  .detail-card {
    padding: 14px;
  }

  .item-title {
    font-size: 20px;
  }

  .price-text {
    font-size: 24px;
  }

  .buyer-actions,
  .owner-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .buyer-actions .el-button,
  .owner-actions .el-button {
    width: 100%;
  }

  .favorite-button {
    width: 100%;
  }

  .loading-grid {
    grid-template-columns: 1fr;
  }

  .loading-image {
    height: 250px;
  }

  .seller-card {
    align-items: flex-start;
  }

  .thumb-button {
    flex-basis: 60px;
    width: 60px;
    height: 60px;
  }

  .reviews-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
