<template>
  <div class="admin-page">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">商品审核</h1>
        <p class="page-desc">审核待发布商品，处理违规在售商品，并查看商品完整资料。</p>
      </div>
      <div class="head-actions">
        <el-tag type="warning" effect="plain" round>当前列表共 {{ total }} 件</el-tag>
      </div>
    </div>

    <div class="surface filter-surface">
      <el-tabs
        :model-value="activeTab"
        class="status-tabs"
        @update:model-value="handleTabUpdate"
      >
        <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
      </el-tabs>

      <div class="toolbar search-toolbar">
        <el-input
          v-model="keyword"
          clearable
          class="keyword-input"
          placeholder="搜索商品标题或卖家昵称"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
        <el-button @click="handleReset">重置</el-button>
        <div class="spacer" />
        <el-button text :loading="loading" @click="loadItems">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>
    </div>

    <div class="surface table-surface">
      <el-table
        v-loading="loading"
        :data="items"
        row-key="id"
        class="audit-table"
      >
        <el-table-column label="封面" width="78">
          <template #default="{ row }">
            <el-image
              :src="row.coverUrl || undefined"
              fit="cover"
              class="cover-thumb"
              :preview-src-list="row.coverUrl ? [row.coverUrl] : []"
              preview-teleported
            >
              <template #error>
                <div class="thumb-fallback">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column label="商品" min-width="230">
          <template #default="{ row }">
            <div class="item-cell">
              <span class="item-title" :title="row.title">{{ row.title }}</span>
              <span class="muted small">商品编号 #{{ row.id }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="卖家" min-width="150">
          <template #default="{ row }">
            <div class="seller-cell">
              <UserAvatar :size="26" :nickname="row.sellerNickname" />
              <span class="seller-name" :title="row.sellerNickname">{{ row.sellerNickname }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="price-text">{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="110" show-overflow-tooltip />

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="状态" width="108">
          <template #default="{ row }">
            <StatusTag kind="item" :value="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="258" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="canAudit(row.status)"
              size="small"
              type="success"
              link
              :loading="busyId === row.id"
              @click="approveItem(row)"
            >
              批准
            </el-button>
            <el-button
              v-if="canAudit(row.status)"
              size="small"
              type="danger"
              link
              @click="openReject(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="canTakedown(row.status)"
              size="small"
              type="warning"
              link
              @click="openTakedown(row)"
            >
              违规下架
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState description="当前筛选条件下暂无商品" :image-size="90" />
        </template>
      </el-table>

      <AppPagination
        v-model:page="page"
        :size="pageSize"
        :total="total"
        @change="handlePageChange"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      :with-header="false"
      size="min(600px, 94vw)"
      class="detail-drawer"
      @closed="onDetailClosed"
    >
      <div v-if="selected" v-loading="detailLoading" class="drawer-body">
        <div class="drawer-head">
          <el-image
            :src="selected.coverUrl || undefined"
            fit="cover"
            class="drawer-cover"
          >
            <template #error>
              <div class="thumb-fallback large">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="drawer-head-main">
            <div class="drawer-title-row">
              <h3 class="drawer-title">{{ selected.title }}</h3>
              <StatusTag kind="item" :value="selected.status" />
            </div>
            <p class="muted drawer-meta">
              商品编号 #{{ selected.id }} · {{ selected.categoryName }} ·
              {{ formatDateTime(selected.createdAt) }}
            </p>
            <div class="drawer-price">{{ formatPrice(selected.price) }}</div>
          </div>
          <el-button text circle class="drawer-close" @click="detailVisible = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <el-alert
          v-if="selected.rejectReason || detail?.rejectReason"
          type="error"
          :closable="false"
          show-icon
          title="审核未通过"
          :description="selected.rejectReason || detail?.rejectReason || ''"
          class="reject-alert"
        />

        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="卖家">{{ sellerNickname }}</el-descriptions-item>
          <el-descriptions-item label="卖家认证">
            <StatusTag kind="auth" :value="sellerAuthStatus" />
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(selected.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核时间">
            {{ formatDateTime(selected.auditAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="上架时间">
            {{ formatDateTime(selected.listedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <StatusTag kind="item" :value="selected.status" />
          </el-descriptions-item>
        </el-descriptions>

        <section class="drawer-section">
          <h4 class="section-heading">商品描述</h4>
          <p class="description-text">{{ detailDescription || '卖家暂未填写商品描述' }}</p>
        </section>

        <section class="drawer-section">
          <h4 class="section-heading">商品图片</h4>
          <div v-if="detailImages.length" class="image-grid">
            <el-image
              v-for="(image, index) in detailImages"
              :key="`${image}-${index}`"
              :src="image"
              fit="cover"
              class="gallery-image"
              :preview-src-list="detailImages"
              :initial-index="index"
              preview-teleported
            >
              <template #error>
                <div class="gallery-fallback">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <EmptyState v-else description="暂无商品图片" :image-size="70" />
        </section>

        <section class="drawer-section">
          <h4 class="section-heading">卖家信息</h4>
          <div class="seller-card">
            <UserAvatar :size="44" :nickname="sellerNickname" :src="sellerAvatar" />
            <div class="seller-card-main">
              <div class="seller-card-name">
                <strong>{{ sellerNickname }}</strong>
                <StatusTag kind="auth" :value="sellerAuthStatus" />
              </div>
              <p class="muted small">
                {{ detail?.seller.college || '未填写学院' }}
                <template v-if="detail">
                  · 评分 {{ detail.seller.avgRating.toFixed(1) }} ·
                  {{ detail.seller.reviewCount }} 条评价 · 成交
                  {{ detail.seller.soldCount }} 件
                </template>
              </p>
            </div>
          </div>
        </section>

        <section class="drawer-section">
          <h4 class="section-heading">审核信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="审核状态">
              <StatusTag kind="item" :value="selected.status" />
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDateTime(selected.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="审核时间">
              {{ formatDateTime(selected.auditAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="驳回原因">
              {{ selected.rejectReason || detail?.rejectReason || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </section>
      </div>

      <template #footer>
        <div v-if="selected" class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <div class="spacer" />
          <el-button
            v-if="canTakedown(selected.status)"
            type="warning"
            plain
            @click="takedownFromDetail"
          >
            违规下架
          </el-button>
          <el-button
            v-if="canAudit(selected.status)"
            type="danger"
            plain
            @click="rejectFromDetail"
          >
            驳回
          </el-button>
          <el-button
            v-if="canAudit(selected.status)"
            type="primary"
            :loading="busyId === selected.id"
            @click="approveFromDetail"
          >
            批准上架
          </el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="rejectVisible"
      title="驳回商品"
      width="min(520px, 94vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onRejectClosed"
    >
      <p class="dialog-tip">
        驳回「{{ rejectTarget?.title }}」，卖家将收到驳回原因，可在修改后重新提交审核。
      </p>
      <el-form
        ref="rejectFormRef"
        :model="rejectForm"
        :rules="rejectRules"
        label-position="top"
      >
        <el-form-item label="驳回原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明驳回原因，便于卖家修改"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="takedownVisible"
      title="违规下架商品"
      width="min(560px, 94vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onTakedownClosed"
    >
      <p class="dialog-tip">
        违规下架「{{ takedownTarget?.title }}」。若商品存在活跃订单，订单将转入争议处理并通知买卖双方。
      </p>
      <el-form
        ref="takedownFormRef"
        :model="takedownForm"
        :rules="takedownRules"
        label-position="top"
      >
        <el-form-item label="下架理由" prop="reason">
          <el-input
            v-model="takedownForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请说明违规事实与下架理由"
          />
        </el-form-item>
        <el-form-item label="规则依据" prop="ruleBasis">
          <el-input
            v-model="takedownForm.ruleBasis"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="选填，如《校园集市商品发布规范》第 3 条"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="takedownVisible = false">取消</el-button>
        <el-button type="warning" :loading="takedownSubmitting" @click="submitTakedown">
          确认下架
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminItemApi } from '@/api/admin'
import { itemApi } from '@/api/item'
import type { AdminItemVO, AuthStatus, ItemDetailVO, ItemStatus } from '@/types'
import { formatDateTime, formatPrice } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'

type AuditTab = ItemStatus | 'ALL'

interface StatusTab {
  value: AuditTab
  label: string
}

interface RejectForm {
  reason: string
}

interface TakedownForm {
  reason: string
  ruleBasis: string
}

const tabs: StatusTab[] = [
  { value: 'PENDING', label: '待审核' },
  { value: 'ALL', label: '全部' },
  { value: 'ON_SALE', label: '在售' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'VIOLATED', label: '违规下架' }
]

const activeTab = ref<AuditTab>('PENDING')
const keyword = ref('')
const items = ref<AdminItemVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const busyId = ref<number | null>(null)

let requestSeq = 0

function currentStatus(): ItemStatus | '' {
  return activeTab.value === 'ALL' ? '' : activeTab.value
}

function handleTabUpdate(value: string | number): void {
  const next = tabs.find((tab) => tab.value === value)
  if (!next || next.value === activeTab.value) return
  activeTab.value = next.value
  page.value = 1
  void loadItems()
}

async function loadItems(): Promise<void> {
  const seq = (requestSeq += 1)
  loading.value = true
  try {
    const result = await adminItemApi.list({
      status: currentStatus(),
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: pageSize
    })
    if (seq !== requestSeq) return
    items.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误由 request 拦截器统一提示，页面保留已有数据 */
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void loadItems()
}

function handleReset(): void {
  keyword.value = ''
  page.value = 1
  void loadItems()
}

function handlePageChange(value: number): void {
  page.value = value
  void loadItems()
}

async function refreshAfterAction(): Promise<void> {
  if (items.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  await loadItems()
}

function canAudit(status?: ItemStatus | null): boolean {
  return status === 'PENDING'
}

function canTakedown(status?: ItemStatus | null): boolean {
  return status === 'PENDING' || status === 'ON_SALE' || status === 'LOCKED'
}

/* ------------------------------- 详情抽屉 ------------------------------- */

const detailVisible = ref(false)
const selected = ref<AdminItemVO | null>(null)
const detail = ref<ItemDetailVO | null>(null)
const detailLoading = ref(false)
let detailSeq = 0

async function openDetail(row: AdminItemVO): Promise<void> {
  selected.value = row
  detail.value = null
  detailVisible.value = true
  const seq = (detailSeq += 1)
  detailLoading.value = true
  try {
    const result = await itemApi.detail(row.id)
    if (seq !== detailSeq) return
    detail.value = result
  } catch {
    /* 详情补充接口失败时仍展示审核列表中的基础信息 */
  } finally {
    if (seq === detailSeq) detailLoading.value = false
  }
}

function onDetailClosed(): void {
  detailSeq += 1
  selected.value = null
  detail.value = null
  detailLoading.value = false
}

const detailImages = computed<string[]>(() => {
  const images = detail.value?.images ?? []
  if (images.length) return images
  const cover = selected.value?.coverUrl
  return cover ? [cover] : []
})

const detailDescription = computed(() => detail.value?.description || selected.value?.description || '')

const sellerNickname = computed(
  () => detail.value?.seller.nickname || selected.value?.sellerNickname || '—'
)

const sellerAuthStatus = computed<AuthStatus>(
  () => detail.value?.seller.authStatus || selected.value?.sellerAuthStatus || 'UNVERIFIED'
)

const sellerAvatar = computed<string | null>(() => detail.value?.seller.avatarUrl ?? null)

/* -------------------------------- 审核动作 ------------------------------- */

function syncItem(updated: AdminItemVO): void {
  const index = items.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) items.value[index] = updated
  if (selected.value?.id === updated.id) {
    selected.value = updated
    const currentDetail = detail.value
    if (currentDetail && currentDetail.id === updated.id) {
      detail.value = {
        ...currentDetail,
        title: updated.title,
        price: updated.price,
        status: updated.status,
        statusText: updated.statusText,
        rejectReason: updated.rejectReason
      }
    }
  }
}

async function approveItem(row: AdminItemVO): Promise<void> {
  if (busyId.value !== null) return
  try {
    await ElMessageBox.confirm(
      `确认批准商品「${row.title}」上架吗？批准后商品将直接进入在售状态。`,
      '批准商品',
      { confirmButtonText: '批准上架', cancelButtonText: '取消', type: 'success' }
    )
  } catch {
    return
  }
  busyId.value = row.id
  try {
    const updated = await adminItemApi.approve(row.id)
    ElMessage.success('已批准，商品已上架')
    syncItem(updated)
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    busyId.value = null
  }
}

const rejectVisible = ref(false)
const rejectTarget = ref<AdminItemVO | null>(null)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive<RejectForm>({ reason: '' })
const rejectSubmitting = ref(false)

const rejectRules: FormRules = {
  reason: [
    { required: true, message: '请填写驳回原因', trigger: 'blur' },
    { min: 2, max: 200, message: '驳回原因需为 2-200 个字符', trigger: 'blur' }
  ]
}

function openReject(row: AdminItemVO): void {
  rejectTarget.value = row
  rejectForm.reason = ''
  rejectVisible.value = true
  void nextTick(() => rejectFormRef.value?.clearValidate())
}

function onRejectClosed(): void {
  rejectForm.reason = ''
  rejectTarget.value = null
  rejectSubmitting.value = false
  void nextTick(() => rejectFormRef.value?.clearValidate())
}

async function submitReject(): Promise<void> {
  const target = rejectTarget.value
  if (!target || !rejectFormRef.value || rejectSubmitting.value) return
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return
  const reason = rejectForm.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('驳回原因至少需要 2 个字符')
    return
  }
  rejectSubmitting.value = true
  try {
    const updated = await adminItemApi.reject(target.id, reason)
    ElMessage.success('已驳回该商品并通知卖家')
    syncItem(updated)
    rejectVisible.value = false
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    rejectSubmitting.value = false
  }
}

const takedownVisible = ref(false)
const takedownTarget = ref<AdminItemVO | null>(null)
const takedownFormRef = ref<FormInstance>()
const takedownForm = reactive<TakedownForm>({ reason: '', ruleBasis: '' })
const takedownSubmitting = ref(false)

const takedownRules: FormRules = {
  reason: [
    { required: true, message: '请填写下架理由', trigger: 'blur' },
    { min: 2, max: 500, message: '下架理由需为 2-500 个字符', trigger: 'blur' }
  ],
  ruleBasis: [{ max: 500, message: '规则依据不能超过 500 个字符', trigger: 'blur' }]
}

function openTakedown(row: AdminItemVO): void {
  takedownTarget.value = row
  takedownForm.reason = ''
  takedownForm.ruleBasis = ''
  takedownVisible.value = true
  void nextTick(() => takedownFormRef.value?.clearValidate())
}

function onTakedownClosed(): void {
  takedownForm.reason = ''
  takedownForm.ruleBasis = ''
  takedownTarget.value = null
  takedownSubmitting.value = false
  void nextTick(() => takedownFormRef.value?.clearValidate())
}

async function submitTakedown(): Promise<void> {
  const target = takedownTarget.value
  if (!target || !takedownFormRef.value || takedownSubmitting.value) return
  const valid = await takedownFormRef.value.validate().catch(() => false)
  if (!valid) return
  const reason = takedownForm.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('下架理由至少需要 2 个字符')
    return
  }
  takedownSubmitting.value = true
  try {
    const updated = await adminItemApi.takedown(target.id, {
      reason,
      ruleBasis: takedownForm.ruleBasis.trim() || undefined
    })
    ElMessage.success('商品已违规下架')
    syncItem(updated)
    takedownVisible.value = false
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    takedownSubmitting.value = false
  }
}

function approveFromDetail(): void {
  if (selected.value) void approveItem(selected.value)
}

function rejectFromDetail(): void {
  if (selected.value) openReject(selected.value)
}

function takedownFromDetail(): void {
  if (selected.value) openTakedown(selected.value)
}

onMounted(() => {
  void loadItems()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-900);
}

.page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ink-500);
}

.filter-surface {
  padding-bottom: 16px;
}

.status-tabs {
  margin-bottom: 4px;
}

:deep(.status-tabs .el-tabs__header) {
  margin: 0 0 12px;
}

:deep(.status-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--line);
}

.search-toolbar {
  margin-bottom: 0;
}

.keyword-input {
  width: 280px;
  max-width: 100%;
}

.table-surface {
  min-width: 0;
}

.audit-table {
  width: 100%;
}

.cover-thumb {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  display: block;
  border: 1px solid var(--line);
  background: var(--brand-50);
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-50);
  color: var(--ink-300);
  font-size: 20px;
}

.thumb-fallback.large {
  min-height: 96px;
  font-size: 28px;
}

.item-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seller-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.seller-name {
  font-size: 13px;
  color: var(--ink-700);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-text {
  color: #d9480f;
  font-weight: 600;
  font-size: 14px;
}

.drawer-body {
  padding: 4px 4px 12px;
}

.drawer-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 6px 34px 16px 0;
  border-bottom: 1px solid var(--line);
  margin-bottom: 16px;
}

.drawer-cover {
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: var(--brand-50);
}

.drawer-head-main {
  min-width: 0;
}

.drawer-title-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.drawer-title {
  margin: 0;
  font-size: 17px;
  line-height: 1.45;
  color: var(--ink-900);
}

.drawer-meta {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
}

.drawer-price {
  margin-top: 8px;
  color: #d9480f;
  font-weight: 700;
  font-size: 20px;
}

.drawer-close {
  position: absolute;
  top: 0;
  right: -4px;
}

.reject-alert {
  margin-bottom: 16px;
}

.detail-desc {
  margin-bottom: 18px;
}

.drawer-section {
  margin-bottom: 20px;
}

.section-heading {
  margin: 0 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--brand-500);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
  line-height: 1.2;
}

.description-text {
  margin: 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--brand-50);
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.gallery-image {
  width: 100%;
  height: 96px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--brand-50);
  cursor: zoom-in;
}

.gallery-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-50);
  color: var(--ink-300);
  font-size: 24px;
}

.seller-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdfc;
}

.seller-card-main {
  min-width: 0;
}

.seller-card-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.seller-card-main p {
  margin: 0;
  line-height: 1.6;
}

.drawer-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-tip {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-500);
}

:deep(.audit-table .el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .keyword-input {
    width: 100%;
  }

  .search-toolbar {
    gap: 8px;
  }

  .image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .drawer-head {
    flex-direction: column;
  }

  .drawer-cover {
    width: 100%;
    height: 180px;
  }
}
</style>
