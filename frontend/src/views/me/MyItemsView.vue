<template>
  <div class="me-page items-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">我的商品</h1>
        <p class="page-subtitle">管理你发布的闲置，追踪审核与交易状态。</p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="loadItems">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="router.push('/publish')">
          <el-icon><Plus /></el-icon>
          发布闲置
        </el-button>
      </div>
    </header>

    <div class="surface tabs-bar">
      <el-tabs v-model="activeStatus" class="status-tabs">
        <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>
    </div>

    <div v-loading="loading" class="items-panel">
      <div v-if="items.length" class="item-list">
        <article v-for="item in items" :key="item.id" class="item-row">
          <router-link :to="`/items/${item.id}`" class="cover">
            <el-image :src="item.coverUrl || '/images/placeholder.svg'" fit="cover" class="cover-img">
              <template #error>
                <div class="cover-fallback"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <span class="condition">{{ item.conditionText }}</span>
          </router-link>

          <div class="item-main">
            <div class="title-line">
              <router-link :to="`/items/${item.id}`" class="item-title" :title="item.title">
                {{ item.title }}
              </router-link>
              <StatusTag kind="item" :value="item.status" />
              <el-tooltip v-if="item.status === 'REJECTED'" content="点击查看审核驳回原因" placement="top">
                <span class="reason-entry" @click="openReason(item)">
                  <el-tag type="danger" effect="plain" size="small">查看驳回原因</el-tag>
                </span>
              </el-tooltip>
            </div>

            <div class="item-meta">
              <span>{{ item.categoryName }}</span>
              <span class="dot">·</span>
              <span>{{ item.conditionText }}</span>
              <template v-if="item.handoverArea">
                <span class="dot">·</span>
                <span>{{ item.handoverArea }}</span>
              </template>
            </div>

            <div class="item-stats">
              <span class="item-price" :class="{ 'is-free': item.price === 0 }">{{ formatPrice(item.price) }}</span>
              <span><el-icon><View /></el-icon>{{ item.viewCount }} 浏览</span>
              <span><el-icon><Star /></el-icon>{{ item.favoriteCount }} 收藏</span>
              <span><el-icon><Clock /></el-icon>发布 {{ formatDateTime(item.listedAt || item.createdAt) }}</span>
            </div>
          </div>

          <div class="item-actions">
            <el-button size="small" @click="router.push(`/items/${item.id}`)">查看</el-button>
            <el-button v-if="canEdit(item)" size="small" @click="router.push(`/items/${item.id}/edit`)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              v-if="canSubmit(item)"
              size="small"
              type="primary"
              :loading="actingId === item.id"
              @click="submitItem(item)"
            >
              提交审核
            </el-button>
            <el-button v-if="canOffShelf(item)" size="small" :loading="actingId === item.id" @click="offShelfItem(item)">
              下架
            </el-button>
            <el-button
              v-if="canDelete(item)"
              size="small"
              type="danger"
              plain
              :loading="actingId === item.id"
              @click="deleteItem(item)"
            >
              删除
            </el-button>
          </div>
        </article>
      </div>

      <EmptyState
        v-else-if="!loading"
        :description="emptyDescription"
        :action-text="emptyActionText"
        :image-size="100"
        @action="handleEmptyAction"
      />
    </div>

    <AppPagination v-model:page="page" :size="pageSize" :total="total" @change="loadItems" />

    <el-dialog v-model="reasonVisible" title="审核驳回原因" width="min(520px, 92vw)">
      <div v-loading="reasonLoading" class="reason-body">
        <p class="reason-title">{{ reasonItem?.title || '' }}</p>
        <p class="reason-text">{{ rejectReason || '平台未提供具体的驳回说明，请参考站内通知或联系管理员。' }}</p>
      </div>
      <template #footer>
        <el-button @click="reasonVisible = false">关闭</el-button>
        <el-button v-if="reasonItem && canEdit(reasonItem)" type="primary" @click="goEditFromReason">
          去修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { itemApi } from '@/api/item'
import { itemStatusLabels } from '@/utils/dict'
import { formatDateTime, formatPrice } from '@/utils/format'
import type { ItemCardVO, ItemStatus } from '@/types'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()

const STATUS_ORDER: ItemStatus[] = [
  'DRAFT',
  'PENDING',
  'ON_SALE',
  'LOCKED',
  'SOLD',
  'REJECTED',
  'OFF_SHELF',
  'VIOLATED'
]

const EDITABLE_STATUSES: ItemStatus[] = ['DRAFT', 'REJECTED', 'OFF_SHELF', 'ON_SALE']
const SUBMITTABLE_STATUSES: ItemStatus[] = ['DRAFT', 'REJECTED', 'OFF_SHELF']

const statusTabs: { value: string; label: string }[] = [
  { value: 'ALL', label: '全部' },
  ...STATUS_ORDER.map((status) => ({ value: status, label: itemStatusLabels[status] }))
]

const activeStatus = ref('ALL')
const items = ref<ItemCardVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 8
const loading = ref(false)
const actingId = ref<number | null>(null)

const reasonVisible = ref(false)
const reasonLoading = ref(false)
const reasonItem = ref<ItemCardVO | null>(null)
const rejectReason = ref('')

const currentTabLabel = computed(() => statusTabs.find((tab) => tab.value === activeStatus.value)?.label || '全部')

const emptyDescription = computed(() =>
  activeStatus.value === 'ALL'
    ? '你还没有发布过商品，快去发布第一件闲置吧'
    : `「${currentTabLabel.value}」状态下暂无商品`
)

const emptyActionText = computed(() => (activeStatus.value === 'ALL' ? '去发布闲置' : '查看全部商品'))

function isItemStatus(value: string): value is ItemStatus {
  return STATUS_ORDER.includes(value as ItemStatus)
}

function canEdit(item: ItemCardVO): boolean {
  return EDITABLE_STATUSES.includes(item.status)
}

function canSubmit(item: ItemCardVO): boolean {
  return SUBMITTABLE_STATUSES.includes(item.status)
}

function canOffShelf(item: ItemCardVO): boolean {
  return item.status === 'ON_SALE'
}

function canDelete(item: ItemCardVO): boolean {
  return SUBMITTABLE_STATUSES.includes(item.status)
}

async function loadItems(): Promise<void> {
  loading.value = true
  try {
    const statusValue = activeStatus.value
    const result = await itemApi.mine({
      status: isItemStatus(statusValue) ? statusValue : undefined,
      page: page.value,
      size: pageSize
    })
    items.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留当前列表 */
  } finally {
    loading.value = false
  }
}

async function submitItem(item: ItemCardVO): Promise<void> {
  actingId.value = item.id
  try {
    await itemApi.submit(item.id)
    ElMessage.success('已提交审核，请等待平台审核')
    await loadItems()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    actingId.value = null
  }
}

async function offShelfItem(item: ItemCardVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定下架「${item.title}」吗？下架后买家将无法看到该商品，你可以修改后重新提交审核。`,
      '下架商品',
      { type: 'warning', confirmButtonText: '确认下架', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  actingId.value = item.id
  try {
    await itemApi.offShelf(item.id)
    ElMessage.success('商品已下架')
    await loadItems()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    actingId.value = null
  }
}

async function deleteItem(item: ItemCardVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除商品「${item.title}」吗？删除后不可恢复，请确保该商品没有进行中的订单。`,
      '删除商品',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  actingId.value = item.id
  try {
    await itemApi.remove(item.id)
    ElMessage.success('商品已删除')
    if (items.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadItems()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    actingId.value = null
  }
}

async function openReason(item: ItemCardVO): Promise<void> {
  reasonItem.value = item
  rejectReason.value = ''
  reasonVisible.value = true
  reasonLoading.value = true
  try {
    const detail = await itemApi.detail(item.id)
    rejectReason.value =
      detail.rejectReason || '平台未提供具体的驳回说明，请参考站内通知或联系管理员。'
  } catch {
    rejectReason.value = '驳回原因加载失败，请稍后重试。'
  } finally {
    reasonLoading.value = false
  }
}

function goEditFromReason(): void {
  const item = reasonItem.value
  if (!item) return
  reasonVisible.value = false
  router.push(`/items/${item.id}/edit`)
}

function handleEmptyAction(): void {
  if (activeStatus.value === 'ALL') {
    router.push('/publish')
  } else {
    activeStatus.value = 'ALL'
  }
}

watch(activeStatus, () => {
  page.value = 1
  loadItems()
})

onMounted(loadItems)
</script>

<style scoped>
.me-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-900);
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ink-500);
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tabs-bar {
  padding: 4px 16px 0;
}

.status-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.status-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}

.items-panel {
  min-height: 200px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-row {
  display: flex;
  gap: 16px;
  padding: 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.item-row:hover {
  border-color: #cbe2d6;
  box-shadow: 0 8px 22px rgba(31, 107, 74, 0.08);
}

.cover {
  position: relative;
  width: 132px;
  height: 99px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 10px;
  background: var(--brand-50);
}

.cover-img {
  display: block;
  width: 100%;
  height: 100%;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 28px;
  color: var(--ink-300);
}

.condition {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 1px 7px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.9);
  font-size: 11px;
  color: var(--brand-700);
}

.item-main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 7px;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.item-title {
  max-width: 100%;
  overflow: hidden;
  color: var(--ink-900);
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.reason-entry {
  cursor: pointer;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-500);
}

.dot {
  color: var(--ink-300);
}

.item-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: auto;
  font-size: 12px;
  color: var(--ink-500);
}

.item-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.item-price {
  color: #d9480f;
  font-size: 17px;
  font-weight: 700;
}

.item-price.is-free {
  color: var(--brand-600);
}

.item-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.item-actions .el-button {
  min-width: 84px;
  margin-left: 0;
}

.reason-body {
  min-height: 80px;
}

.reason-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.reason-text {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--brand-50);
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-700);
  word-break: break-word;
}

@media (max-width: 860px) {
  .item-row {
    flex-wrap: wrap;
  }

  .item-actions {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-end;
    width: 100%;
    padding-top: 10px;
    border-top: 1px dashed var(--line);
  }

  .item-actions .el-button {
    flex: 1;
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .head-actions {
    justify-content: flex-end;
  }

  .cover {
    width: 104px;
    height: 78px;
  }

  .item-price {
    font-size: 16px;
  }

  .tabs-bar {
    padding: 4px 10px 0;
  }
}
</style>
