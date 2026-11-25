<template>
  <div class="admin-page">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">争议订单</h1>
        <p class="page-desc">查看处于争议中的订单，核实争议原因后作出裁决。</p>
      </div>
      <div class="head-actions">
        <el-tag type="danger" effect="plain" round>争议订单 {{ total }} 单</el-tag>
      </div>
    </div>

    <div class="surface table-surface">
      <div class="toolbar table-toolbar">
        <span class="muted small">
          仅展示状态为「争议中」的订单，裁决结果将通知买卖双方。
        </span>
        <div class="spacer" />
        <el-button text :loading="loading" @click="loadDisputes">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>

      <el-table v-loading="loading" :data="orders" row-key="id" class="dispute-table">
        <el-table-column label="订单号" width="152">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="商品" min-width="250">
          <template #default="{ row }">
            <div class="item-cell">
              <el-image
                :src="row.item.coverUrl || undefined"
                fit="cover"
                class="cover-thumb"
                :preview-src-list="row.item.coverUrl ? [row.item.coverUrl] : []"
                preview-teleported
              >
                <template #error>
                  <div class="thumb-fallback">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="item-info">
                <span class="item-title" :title="row.item.title">{{ row.item.title }}</span>
                <span class="price-text">{{ formatPrice(row.item.price) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="买家" width="134">
          <template #default="{ row }">
            <div class="user-cell">
              <UserAvatar :size="26" :nickname="row.buyer.nickname" :src="row.buyer.avatarUrl" />
              <span class="user-name" :title="row.buyer.nickname">{{ row.buyer.nickname }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="卖家" width="134">
          <template #default="{ row }">
            <div class="user-cell">
              <UserAvatar :size="26" :nickname="row.seller.nickname" :src="row.seller.avatarUrl" />
              <span class="user-name" :title="row.seller.nickname">{{ row.seller.nickname }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="争议原因" min-width="220">
          <template #default="{ row }">
            <el-tooltip
              :content="row.disputeReason || '未填写争议原因'"
              placement="top"
              :disabled="!row.disputeReason"
            >
              <span class="dispute-reason">{{ truncate(row.disputeReason, 40) || '—' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="状态" width="108">
          <template #default="{ row }">
            <StatusTag kind="order" :value="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="92" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openResolve(row)">
              裁决
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState description="暂无争议订单" :image-size="90" />
        </template>
      </el-table>

      <AppPagination
        v-model:page="page"
        :size="pageSize"
        :total="total"
        @change="handlePageChange"
      />
    </div>

    <el-dialog
      v-model="resolveVisible"
      title="裁决争议订单"
      width="min(560px, 94vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onResolveClosed"
    >
      <div v-if="resolveTarget" class="order-brief">
        <el-image
          :src="resolveTarget.item.coverUrl || undefined"
          fit="cover"
          class="brief-cover"
        >
          <template #error>
            <div class="thumb-fallback">
              <el-icon><Picture /></el-icon>
            </div>
          </template>
        </el-image>
        <div class="brief-main">
          <strong>{{ resolveTarget.item.title }}</strong>
          <span class="muted small">订单号 {{ resolveTarget.orderNo }}</span>
          <span class="muted small">
            买家 {{ resolveTarget.buyer.nickname }} · 卖家 {{ resolveTarget.seller.nickname }}
          </span>
        </div>
      </div>

      <p class="dialog-tip">
        请选择裁决结果并填写理由。理由将记录到订单与审计日志，并通知买卖双方。
      </p>

      <el-form
        ref="resolveFormRef"
        :model="resolveForm"
        :rules="resolveRules"
        label-position="top"
      >
        <el-form-item label="裁决结果" prop="outcome">
          <el-radio-group
            :model-value="resolveForm.outcome"
            class="option-list"
            @update:model-value="onOutcomeChange"
          >
            <el-radio value="COMPLETE" border>
              <span class="option-title">完成订单</span>
              <span class="option-desc">确认交易完成，按平台规则完成订单结算</span>
            </el-radio>
            <el-radio value="CLOSE" border>
              <span class="option-title">管理关闭</span>
              <span class="option-desc">终止本次交易，按平台规则处理商品与订单状态</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="裁决理由" prop="reason">
          <el-input
            v-model="resolveForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写裁决依据与理由，至少 2 个字"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResolve">
          提交裁决
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminOrderApi } from '@/api/admin'
import type { OrderVO } from '@/types'
import { formatDateTime, formatPrice, truncate } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'

interface ResolveForm {
  outcome: string
  reason: string
}

const orders = ref<OrderVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
let requestSeq = 0

async function loadDisputes(): Promise<void> {
  const seq = (requestSeq += 1)
  loading.value = true
  try {
    const result = await adminOrderApi.disputes({ page: page.value, size: pageSize })
    if (seq !== requestSeq) return
    orders.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误由 request 拦截器统一提示，页面保留已有数据 */
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function handlePageChange(value: number): void {
  page.value = value
  void loadDisputes()
}

async function refreshAfterResolve(): Promise<void> {
  if (orders.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  await loadDisputes()
}

const resolveVisible = ref(false)
const resolveTarget = ref<OrderVO | null>(null)
const resolveFormRef = ref<FormInstance>()
const resolveForm = reactive<ResolveForm>({ outcome: 'COMPLETE', reason: '' })
const submitting = ref(false)

const resolveRules: FormRules = {
  reason: [
    { required: true, message: '请填写裁决理由', trigger: 'blur' },
    { min: 2, max: 500, message: '裁决理由需为 2-500 个字符', trigger: 'blur' }
  ]
}

function onOutcomeChange(value: string | number | boolean | undefined): void {
  if (value === 'COMPLETE' || value === 'CLOSE') resolveForm.outcome = value
}

function openResolve(row: OrderVO): void {
  resolveTarget.value = row
  resolveForm.outcome = 'COMPLETE'
  resolveForm.reason = ''
  resolveVisible.value = true
  void nextTick(() => resolveFormRef.value?.clearValidate())
}

function onResolveClosed(): void {
  resolveTarget.value = null
  resolveForm.outcome = 'COMPLETE'
  resolveForm.reason = ''
  submitting.value = false
  void nextTick(() => resolveFormRef.value?.clearValidate())
}

async function submitResolve(): Promise<void> {
  const target = resolveTarget.value
  if (!target || !resolveFormRef.value || submitting.value) return
  const outcome = resolveForm.outcome
  if (outcome !== 'COMPLETE' && outcome !== 'CLOSE') {
    ElMessage.warning('请选择裁决结果')
    return
  }
  const valid = await resolveFormRef.value.validate().catch(() => false)
  if (!valid) return
  const reason = resolveForm.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('裁决理由至少需要 2 个字符')
    return
  }
  submitting.value = true
  try {
    await adminOrderApi.resolve(target.id, {
      outcome,
      reason
    })
    ElMessage.success('裁决已提交，订单状态已更新')
    resolveVisible.value = false
    await refreshAfterResolve()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadDisputes()
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

.table-surface {
  min-width: 0;
}

.table-toolbar {
  margin-bottom: 14px;
}

.dispute-table {
  width: 100%;
}

.order-no {
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  font-size: 13px;
  color: var(--ink-700);
}

.item-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.cover-thumb {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--brand-50);
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-50);
  color: var(--ink-300);
  font-size: 18px;
}

.item-info {
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

.price-text {
  color: #d9480f;
  font-weight: 600;
  font-size: 13px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  color: var(--ink-700);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dispute-reason {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.55;
  color: var(--ink-700);
  word-break: break-word;
}

.order-brief {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdfc;
}

.brief-cover {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--brand-50);
}

.brief-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.brief-main strong {
  font-size: 13px;
  color: var(--ink-900);
}

.dialog-tip {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-500);
}

.option-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.option-list :deep(.el-radio) {
  margin-right: 0;
  height: auto;
  min-height: 48px;
  padding: 9px 12px;
  display: flex;
  align-items: flex-start;
}

.option-list :deep(.el-radio__label) {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.5;
  white-space: normal;
}

.option-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
}

.option-desc {
  font-size: 12px;
  color: var(--ink-500);
}

:deep(.dispute-table .el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 768px) {
  .page-head {
    align-items: flex-start;
  }
}
</style>
