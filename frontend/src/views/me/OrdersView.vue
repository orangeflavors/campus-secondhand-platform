<template>
  <div class="me-page orders-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">我的订单</h1>
        <p class="page-subtitle">查看购买与售出记录，按可执行的操作处理待确认请求与线下交付。</p>
      </div>
      <div class="head-actions">
        <span v-if="!loading" class="count-pill">共 {{ orders.length }} 笔</span>
        <el-button :loading="loading" @click="loadOrders">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </header>

    <div class="surface filter-bar">
      <el-tabs v-model="activeRole" class="role-tabs">
        <el-tab-pane name="BUYER">
          <template #label>
            <span class="tab-label"><el-icon><List /></el-icon>我买到的</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="SELLER">
          <template #label>
            <span class="tab-label"><el-icon><Goods /></el-icon>我卖出的</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="filter-actions">
        <el-select v-model="statusFilter" class="status-select" placeholder="全部状态">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="orders-panel">
      <div v-if="orders.length" class="orders-list">
        <OrderCard v-for="order in orders" :key="order.id" :order="order">
          <template #actions>
            <el-button
              v-if="order.actions.canAccept"
              size="small"
              type="primary"
              :loading="actionId === order.id"
              @click="acceptOrder(order)"
            >
              接受
            </el-button>
            <el-button
              v-if="order.actions.canReject"
              size="small"
              :disabled="actionId === order.id"
              @click="openReasonDialog(order, 'reject')"
            >
              拒绝
            </el-button>
            <el-button
              v-if="order.actions.canCancel"
              size="small"
              :disabled="actionId === order.id"
              @click="openReasonDialog(order, 'cancel')"
            >
              取消订单
            </el-button>
            <el-button
              v-if="order.actions.canConfirm"
              size="small"
              type="success"
              :loading="actionId === order.id"
              @click="confirmOrder(order)"
            >
              确认交付
            </el-button>
            <el-button
              v-if="order.actions.canReview"
              size="small"
              type="warning"
              plain
              @click="goDetail(order)"
            >
              去评价
            </el-button>
            <el-button size="small" plain @click="goDetail(order)">查看详情</el-button>
          </template>
        </OrderCard>
      </div>

      <EmptyState
        v-else-if="!loading"
        :description="emptyDescription"
        action-text="刷新列表"
        :image-size="100"
        @action="loadOrders"
      />
    </div>

    <el-dialog
      v-model="reasonDialogVisible"
      :title="reasonDialogTitle"
      width="min(480px, 92vw)"
      :close-on-click-modal="false"
    >
      <p class="dialog-hint">{{ reasonDialogHint }}</p>
      <div v-if="reasonMode === 'cancel'" class="preset-row">
        <el-button
          v-for="preset in cancelReasonPresets"
          :key="preset"
          size="small"
          round
          @click="actionReason = preset"
        >
          {{ preset }}
        </el-button>
      </div>
      <el-input
        v-model="actionReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        :placeholder="reasonMode === 'reject' ? '请说明拒绝原因（2-200 字）' : '请说明取消原因（2-200 字）'"
      />
      <p class="dialog-tip">理由需为 2-200 字，提交后将通知对方。</p>
      <template #footer>
        <el-button @click="reasonDialogVisible = false">再想想</el-button>
        <el-button type="primary" :loading="reasonSubmitting" @click="submitReason">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'
import { cancelReasonPresets, orderStatusLabels } from '@/utils/dict'
import type { OrderStatus, OrderVO } from '@/types'
import EmptyState from '@/components/EmptyState.vue'
import OrderCard from '@/components/OrderCard.vue'

type OrderRole = 'BUYER' | 'SELLER'
type ReasonMode = 'reject' | 'cancel'

const router = useRouter()

const activeRole = ref<string>('BUYER')
const statusFilter = ref<OrderStatus | ''>('')
const orders = ref<OrderVO[]>([])
const loading = ref(false)
const actionId = ref<number | null>(null)

const reasonDialogVisible = ref(false)
const reasonMode = ref<ReasonMode>('reject')
const reasonTarget = ref<OrderVO | null>(null)
const actionReason = ref('')
const reasonSubmitting = ref(false)

const statusOptions: { value: OrderStatus | ''; label: string }[] = [
  { value: '', label: '全部状态' },
  ...(Object.entries(orderStatusLabels) as [OrderStatus, string][]).map(([value, label]) => ({
    value,
    label
  }))
]

const currentRole = computed<OrderRole>(() => (activeRole.value === 'SELLER' ? 'SELLER' : 'BUYER'))

const emptyDescription = computed(() => {
  const actionText = currentRole.value === 'SELLER' ? '卖出' : '买到'
  return statusFilter.value ? `当前状态下暂无${actionText}的订单` : `还没有${actionText}的订单`
})

const reasonDialogTitle = computed(() =>
  reasonMode.value === 'reject' ? '拒绝购买请求' : '取消订单'
)

const reasonDialogHint = computed(() =>
  reasonMode.value === 'reject'
    ? '拒绝后该购买请求将被取消，并通知买家。'
    : '取消后订单将关闭，商品状态以服务端处理结果为准。'
)

async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    orders.value = await orderApi.list({
      role: currentRole.value,
      status: statusFilter.value || undefined
    })
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留当前列表 */
  } finally {
    loading.value = false
  }
}

async function runAction(
  orderId: number,
  action: () => Promise<OrderVO>,
  successMessage: string
): Promise<void> {
  actionId.value = orderId
  try {
    await action()
    ElMessage.success(successMessage)
    await loadOrders()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    actionId.value = null
  }
}

async function acceptOrder(order: OrderVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认接受订单 ${order.orderNo} 的购买请求吗？接受后商品将被锁定，其他待确认请求会自动取消。`,
      '接受购买请求',
      { type: 'warning', confirmButtonText: '确认接受', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  await runAction(order.id, () => orderApi.accept(order.id), '已接受该购买请求')
}

async function confirmOrder(order: OrderVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '请确认你已当面验货并完成线下交付。双方都确认后，订单才会标记为已完成。',
      '确认交付',
      { type: 'warning', confirmButtonText: '确认已交付', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await runAction(order.id, () => orderApi.confirm(order.id), '已确认交付')
}

function openReasonDialog(order: OrderVO, mode: ReasonMode): void {
  reasonTarget.value = order
  reasonMode.value = mode
  actionReason.value = ''
  reasonDialogVisible.value = true
}

async function submitReason(): Promise<void> {
  const target = reasonTarget.value
  if (!target) return
  const reason = actionReason.value.trim()
  if (reason.length < 2 || reason.length > 200) {
    ElMessage.warning('请填写 2-200 字的理由')
    return
  }

  reasonSubmitting.value = true
  try {
    if (reasonMode.value === 'reject') {
      await orderApi.reject(target.id, reason)
      ElMessage.success('已拒绝该购买请求')
    } else {
      await orderApi.cancel(target.id, reason)
      ElMessage.success('订单已取消')
    }
    reasonDialogVisible.value = false
    await loadOrders()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    reasonSubmitting.value = false
  }
}

function goDetail(order: OrderVO): void {
  router.push(`/me/orders/${order.id}`)
}

watch([activeRole, statusFilter], () => {
  loadOrders()
})

onMounted(loadOrders)
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
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.count-pill {
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--brand-100);
  font-size: 12px;
  color: var(--brand-700);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 16px;
}

.role-tabs {
  flex: 1;
  min-width: 0;
}

.role-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.role-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.status-select {
  width: 158px;
}

.orders-panel {
  min-height: 220px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.orders-list :deep(.order-actions) {
  flex-wrap: wrap;
}

.orders-list :deep(.order-actions .el-button) {
  margin-left: 0;
}

.dialog-hint {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-500);
}

.preset-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.preset-row .el-button {
  margin-left: 0;
}

.dialog-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--ink-300);
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .head-actions {
    justify-content: flex-end;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    padding: 10px 12px;
  }

  .filter-actions {
    justify-content: flex-end;
  }

  .status-select {
    width: 100%;
  }

  .filter-actions :deep(.el-select) {
    flex: 1;
  }
}
</style>
