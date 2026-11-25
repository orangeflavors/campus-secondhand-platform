<template>
  <div class="admin-page">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">用户管理</h1>
        <p class="page-desc">查询校园用户资料，查看账户状态与历史处置，执行警告、限权、封禁或解封。</p>
      </div>
      <div class="head-actions">
        <el-tag type="success" effect="plain" round>当前列表 {{ total }} 位用户</el-tag>
      </div>
    </div>

    <div class="surface filter-surface">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            class="keyword-input"
            placeholder="用户名 / 昵称 / 学号"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="账户状态">
          <el-select
            v-model="filters.accountStatus"
            clearable
            placeholder="全部"
            class="filter-select"
          >
            <el-option
              v-for="option in accountOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select
            v-model="filters.authStatus"
            clearable
            placeholder="全部"
            class="filter-select"
          >
            <el-option
              v-for="option in authOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="surface table-surface">
      <div class="toolbar table-toolbar">
        <span class="muted small">管理员账户不参与普通交易，处置前请核对用户身份与历史记录。</span>
        <div class="spacer" />
        <el-button text :loading="loading" @click="loadUsers">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>

      <el-table v-loading="loading" :data="users" row-key="id" class="user-table">
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <UserAvatar :size="34" :nickname="row.nickname" :src="row.avatarUrl" />
          </template>
        </el-table-column>

        <el-table-column label="用户名" width="132">
          <template #default="{ row }">
            <span class="username">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column label="昵称" min-width="140">
          <template #default="{ row }">
            <div class="nickname-cell">
              <span class="nickname" :title="row.nickname">{{ row.nickname }}</span>
              <el-tag v-if="row.role === 'ADMIN'" size="small" type="danger" effect="plain">
                管理员
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="学院" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.college || '—' }}</template>
        </el-table-column>

        <el-table-column label="认证状态" width="112">
          <template #default="{ row }">
            <StatusTag kind="auth" :value="row.authStatus" />
          </template>
        </el-table-column>

        <el-table-column label="账户状态" width="138">
          <template #default="{ row }">
            <div class="status-cell">
              <StatusTag kind="account" :value="row.accountStatus" />
              <span
                v-if="row.accountStatus === 'LIMITED' && row.limitedUntil"
                class="muted small limit-text"
              >
                至 {{ formatDate(row.limitedUntil) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.role !== 'ADMIN'"
              size="small"
              type="warning"
              link
              @click="openSanction(row)"
            >
              处置
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState description="当前筛选条件下暂无用户" :image-size="90" />
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
      size="min(640px, 94vw)"
      @closed="onDetailClosed"
    >
      <div v-if="detailUser" v-loading="detailLoading" class="drawer-body">
        <div class="drawer-head">
          <UserAvatar :size="56" :nickname="detailUser.nickname" :src="detailUser.avatarUrl" />
          <div class="drawer-head-main">
            <div class="drawer-title-row">
              <h3 class="drawer-title">{{ detailUser.nickname }}</h3>
              <StatusTag kind="auth" :value="detailUser.authStatus" />
              <StatusTag kind="account" :value="detailUser.accountStatus" />
            </div>
            <p class="muted drawer-meta">
              @{{ detailUser.username }} · 用户 #{{ detailUser.id }} ·
              {{ detailUser.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </p>
            <p v-if="detailUser.statusReason" class="status-reason">
              状态说明：{{ detailUser.statusReason }}
            </p>
          </div>
          <el-button text circle class="drawer-close" @click="detailVisible = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <section class="drawer-section">
          <h4 class="section-heading">账户资料</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户 ID">#{{ detailUser.id }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              {{ detailUser.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-descriptions-item>
            <el-descriptions-item label="学院">
              {{ detailUser.college || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ detailUser.phone || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="学号">
              {{ detailUser.studentNo || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ formatDateTime(detailUser.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="认证状态">
              <StatusTag kind="auth" :value="detailUser.authStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="账户状态">
              <StatusTag kind="account" :value="detailUser.accountStatus" />
            </el-descriptions-item>
            <el-descriptions-item label="限权到期">
              {{ formatDateTime(detailUser.limitedUntil) }}
            </el-descriptions-item>
            <el-descriptions-item label="状态说明">
              {{ detailUser.statusReason || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="drawer-section">
          <h4 class="section-heading">数据统计</h4>
          <div class="stat-grid">
            <div class="stat-cell">
              <strong>{{ detailUser.stats?.publishedCount ?? 0 }}</strong>
              <span>发布商品</span>
            </div>
            <div class="stat-cell">
              <strong>{{ detailUser.stats?.soldCount ?? 0 }}</strong>
              <span>完成成交</span>
            </div>
            <div class="stat-cell">
              <strong>{{ publicRating?.reviewCount ?? detailUser.stats?.reviewCount ?? 0 }}</strong>
              <span>收到评价</span>
            </div>
            <div class="stat-cell">
              <strong>
                {{ Number(publicRating?.avgRating ?? detailUser.stats?.avgRating ?? 0).toFixed(1) }}
              </strong>
              <span>平均评分</span>
            </div>
          </div>
        </section>

        <section class="drawer-section">
          <div class="section-head-row">
            <h4 class="section-heading">历史处置</h4>
            <span class="muted small">共 {{ history.length }} 条</span>
          </div>
          <div v-loading="historyLoading" class="history-wrap">
            <el-timeline v-if="history.length" class="action-timeline">
              <el-timeline-item
                v-for="action in history"
                :key="action.id"
                :timestamp="formatDateTime(action.createdAt)"
                placement="top"
                :type="timelineType(action.actionType)"
              >
                <div class="action-card">
                  <div class="action-head">
                    <el-tag size="small" :type="timelineType(action.actionType)" effect="light" round>
                      {{ action.actionTypeText || actionTypeLabels[action.actionType] }}
                    </el-tag>
                    <span class="muted small">操作人：{{ action.operatorName || '—' }}</span>
                  </div>
                  <p class="action-reason">{{ action.reason }}</p>
                  <p v-if="action.ruleBasis" class="muted small action-line">
                    规则依据：{{ action.ruleBasis }}
                  </p>
                  <p class="muted small action-line">
                    生效时间：{{ formatDateTime(action.effectiveFrom) }}
                    <template v-if="action.effectiveTo">
                      · 到期：{{ formatDateTime(action.effectiveTo) }}
                    </template>
                    <template v-else>· 长期</template>
                  </p>
                </div>
              </el-timeline-item>
            </el-timeline>
            <EmptyState v-else description="暂无历史处置记录" :image-size="70" />
          </div>
        </section>
      </div>

      <template #footer>
        <div v-if="detailUser" class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <div class="spacer" />
          <el-button
            v-if="detailUser.role !== 'ADMIN'"
            type="warning"
            :loading="detailLoading"
            @click="sanctionFromDetail"
          >
            处置该用户
          </el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="sanctionVisible"
      title="账户处置"
      width="min(600px, 94vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onSanctionClosed"
    >
      <div v-if="sanctionTarget" class="sanction-target">
        <UserAvatar :size="40" :nickname="sanctionTarget.nickname" :src="sanctionTarget.avatarUrl" />
        <div class="sanction-target-main">
          <div class="sanction-target-name">
            <strong>{{ sanctionTarget.nickname }}</strong>
            <span class="muted small">@{{ sanctionTarget.username }}</span>
            <StatusTag kind="account" :value="sanctionTarget.accountStatus" />
          </div>
          <span class="muted small">
            当前状态：{{ accountStatusLabels[sanctionTarget.accountStatus] }}，处置结果将通知用户并写入审计日志。
          </span>
        </div>
      </div>

      <el-form
        ref="sanctionFormRef"
        :model="sanctionForm"
        :rules="sanctionRules"
        label-position="top"
      >
        <el-form-item label="处置动作" prop="actionType">
          <el-radio-group
            :model-value="sanctionForm.actionType"
            class="action-list"
            @update:model-value="onSanctionActionChange"
          >
            <el-radio v-for="option in sanctionOptions" :key="option.value" :value="option.value" border>
              <span class="option-title">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处置理由" prop="reason">
          <el-input
            v-model="sanctionForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写违规事实与处置理由，至少 2 个字"
          />
        </el-form-item>

        <el-form-item label="规则依据" prop="ruleBasis">
          <el-input
            v-model="sanctionForm.ruleBasis"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="选填，如《校园集市用户行为规范》第 4 条"
          />
        </el-form-item>

        <el-form-item v-if="needDuration" label="处置时长（天）" prop="durationDays">
          <el-input-number
            v-model="sanctionForm.durationDays"
            :min="1"
            :max="3650"
            :step="1"
            step-strictly
            controls-position="right"
            :placeholder="durationPlaceholder"
            class="duration-input"
          />
          <span class="field-hint">{{ durationHint }}</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="sanctionVisible = false">取消</el-button>
        <el-button type="warning" :loading="sanctionSubmitting" @click="submitSanction">
          确认处置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules
} from 'element-plus'
import { adminUserApi } from '@/api/admin'
import { authApi } from '@/api/auth'
import type {
  AccountStatus,
  AdminUserVO,
  AuthStatus,
  ModerationActionType,
  ModerationActionVO
} from '@/types'
import { formatDate, formatDateTime } from '@/utils/format'
import {
  accountStatusLabels,
  actionTypeLabels,
  authStatusLabels
} from '@/utils/dict'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'

type TimelineType = 'success' | 'warning' | 'danger' | 'info'

interface UserFilters {
  keyword: string
  accountStatus: string
  authStatus: string
}

interface SanctionForm {
  actionType: string
  reason: string
  ruleBasis: string
  durationDays: number | undefined
}

const accountStatusValues: AccountStatus[] = ['NORMAL', 'LIMITED', 'BANNED']
const authStatusValues: AuthStatus[] = ['UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED']

const accountOptions = accountStatusValues.map((value) => ({
  value,
  label: accountStatusLabels[value]
}))

const authOptions = authStatusValues.map((value) => ({
  value,
  label: authStatusLabels[value]
}))

const filters = reactive<UserFilters>({ keyword: '', accountStatus: '', authStatus: '' })
const users = ref<AdminUserVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
let requestSeq = 0

async function loadUsers(): Promise<void> {
  const seq = (requestSeq += 1)
  loading.value = true
  try {
    const result = await adminUserApi.list({
      keyword: filters.keyword.trim() || undefined,
      accountStatus: filters.accountStatus || undefined,
      authStatus: filters.authStatus || undefined,
      page: page.value,
      size: pageSize
    })
    if (seq !== requestSeq) return
    users.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误由 request 拦截器统一提示，页面保留已有数据 */
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void loadUsers()
}

function handleReset(): void {
  filters.keyword = ''
  filters.accountStatus = ''
  filters.authStatus = ''
  page.value = 1
  void loadUsers()
}

function handlePageChange(value: number): void {
  page.value = value
  void loadUsers()
}

/* ------------------------------- 详情抽屉 ------------------------------- */

const detailVisible = ref(false)
const detailUser = ref<AdminUserVO | null>(null)
const detailLoading = ref(false)
const history = ref<ModerationActionVO[]>([])
const historyLoading = ref(false)
const publicRating = ref<{ reviewCount: number; avgRating: number } | null>(null)
let detailSeq = 0

async function loadDetail(userId: number): Promise<void> {
  const seq = (detailSeq += 1)
  detailLoading.value = true
  historyLoading.value = true
  try {
    // AdminUserVO.stats 暂未包含评价统计，补充公开资料中的评分数据用于展示
    const [userResult, historyResult, profileResult] = await Promise.allSettled([
      adminUserApi.detail(userId),
      adminUserApi.actions(userId),
      authApi.publicProfile(userId)
    ] as const)
    if (seq !== detailSeq) return
    if (userResult.status === 'fulfilled') detailUser.value = userResult.value
    if (historyResult.status === 'fulfilled') history.value = historyResult.value
    if (profileResult.status === 'fulfilled') {
      publicRating.value = {
        reviewCount: profileResult.value.reviewCount,
        avgRating: profileResult.value.avgRating
      }
    }
  } finally {
    if (seq === detailSeq) {
      detailLoading.value = false
      historyLoading.value = false
    }
  }
}

function openDetail(row: AdminUserVO): void {
  detailUser.value = row
  history.value = []
  publicRating.value = null
  detailVisible.value = true
  void loadDetail(row.id)
}

function onDetailClosed(): void {
  detailSeq += 1
  detailUser.value = null
  history.value = []
  publicRating.value = null
  detailLoading.value = false
  historyLoading.value = false
}

function timelineType(action: ModerationActionType): TimelineType {
  switch (action) {
    case 'BAN':
    case 'TAKEDOWN':
      return 'danger'
    case 'LIMIT':
    case 'WARN':
      return 'warning'
    case 'UNBAN':
      return 'success'
    default:
      return 'info'
  }
}

/* -------------------------------- 账户处置 ------------------------------- */

const sanctionOptions: {
  value: ModerationActionType
  label: string
  desc: string
}[] = [
  { value: 'WARN', label: '警告', desc: '发送警告通知并记录违规行为，账户权限不变' },
  { value: 'LIMIT', label: '限权', desc: '限制发布商品与下单，可设置处置时长' },
  { value: 'BAN', label: '封禁', desc: '禁止全部写操作，可设置处置时长或长期封禁' },
  { value: 'UNBAN', label: '解封', desc: '解除限权或封禁，恢复账户正常状态' }
]

const sanctionVisible = ref(false)
const sanctionTarget = ref<AdminUserVO | null>(null)
const sanctionFormRef = ref<FormInstance>()
const sanctionSubmitting = ref(false)
const sanctionForm = reactive<SanctionForm>({
  actionType: 'WARN',
  reason: '',
  ruleBasis: '',
  durationDays: undefined
})

const sanctionRules: FormRules = {
  reason: [
    { required: true, message: '请填写处置理由', trigger: 'blur' },
    { min: 2, max: 500, message: '处置理由需为 2-500 个字符', trigger: 'blur' }
  ],
  ruleBasis: [{ max: 500, message: '规则依据不能超过 500 个字符', trigger: 'blur' }]
}

const needDuration = computed(
  () => sanctionForm.actionType === 'LIMIT' || sanctionForm.actionType === 'BAN'
)

const durationPlaceholder = computed(() =>
  sanctionForm.actionType === 'LIMIT' ? '留空默认限权 7 天' : '留空表示长期封禁'
)

const durationHint = computed(() =>
  sanctionForm.actionType === 'LIMIT'
    ? '留空时服务端默认限权 7 天；可填写 1-3650 天。'
    : '留空表示长期封禁；可填写 1-3650 天。'
)

watch(
  () => sanctionForm.actionType,
  (value) => {
    if (value !== 'LIMIT' && value !== 'BAN') {
      sanctionForm.durationDays = undefined
    }
  }
)

function isSanctionAction(value: string): value is ModerationActionType {
  return sanctionOptions.some((option) => option.value === value)
}

function onSanctionActionChange(value: string | number | boolean | undefined): void {
  if (typeof value === 'string') sanctionForm.actionType = value
}

function defaultSanctionAction(user: AdminUserVO): ModerationActionType {
  if (user.accountStatus === 'BANNED') return 'UNBAN'
  return 'WARN'
}

function openSanction(user: AdminUserVO): void {
  if (user.role === 'ADMIN') {
    ElMessage.warning('管理员账户不可执行账户处置')
    return
  }
  sanctionTarget.value = user
  sanctionForm.actionType = defaultSanctionAction(user)
  sanctionForm.reason = ''
  sanctionForm.ruleBasis = ''
  sanctionForm.durationDays = undefined
  sanctionVisible.value = true
  void nextTick(() => sanctionFormRef.value?.clearValidate())
}

function sanctionFromDetail(): void {
  if (detailUser.value) openSanction(detailUser.value)
}

function onSanctionClosed(): void {
  sanctionTarget.value = null
  sanctionForm.actionType = 'WARN'
  sanctionForm.reason = ''
  sanctionForm.ruleBasis = ''
  sanctionForm.durationDays = undefined
  sanctionSubmitting.value = false
  void nextTick(() => sanctionFormRef.value?.clearValidate())
}

async function refreshAfterSanction(userId: number): Promise<void> {
  if (users.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  await loadUsers()
  if (detailVisible.value && detailUser.value?.id === userId) {
    await loadDetail(userId)
  }
}

async function submitSanction(): Promise<void> {
  const target = sanctionTarget.value
  if (!target || !sanctionFormRef.value || sanctionSubmitting.value) return
  const actionType = sanctionForm.actionType
  if (!isSanctionAction(actionType)) {
    ElMessage.warning('请选择处置动作')
    return
  }
  const valid = await sanctionFormRef.value.validate().catch(() => false)
  if (!valid) return
  const reason = sanctionForm.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('处置理由至少需要 2 个字符')
    return
  }
  const duration = sanctionForm.durationDays
  if (needDuration.value && typeof duration === 'number' && duration < 1) {
    ElMessage.warning('处置时长至少为 1 天，或留空由服务端按默认策略处理')
    return
  }
  sanctionSubmitting.value = true
  if (actionType === 'BAN') {
    try {
      await ElMessageBox.confirm(
        `确认封禁用户「${target.nickname}」吗？封禁将禁止该用户全部写操作。`,
        '封禁确认',
        { confirmButtonText: '确认封禁', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      sanctionSubmitting.value = false
      return
    }
  }
  try {
    await adminUserApi.sanction(target.id, {
      actionType,
      reason,
      ruleBasis: sanctionForm.ruleBasis.trim() || undefined,
      durationDays: needDuration.value && typeof duration === 'number' ? duration : undefined
    })
    ElMessage.success(`已执行「${actionTypeLabels[actionType]}」处置`)
    sanctionVisible.value = false
    await refreshAfterSanction(target.id)
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    sanctionSubmitting.value = false
  }
}

onMounted(() => {
  void loadUsers()
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
  padding-bottom: 4px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 8px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.keyword-input {
  width: 260px;
}

.filter-select {
  width: 136px;
}

.table-toolbar {
  margin-bottom: 14px;
}

.user-table {
  width: 100%;
}

.username {
  font-size: 13px;
  color: var(--ink-700);
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.nickname-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.nickname {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.limit-text {
  white-space: nowrap;
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

.drawer-head-main {
  min-width: 0;
}

.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.drawer-title {
  margin: 0;
  font-size: 18px;
  color: var(--ink-900);
}

.drawer-meta {
  margin: 8px 0 0;
  font-size: 12px;
}

.status-reason {
  margin: 8px 0 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff8e8;
  font-size: 12px;
  line-height: 1.6;
  color: #8a5a00;
  word-break: break-word;
}

.drawer-close {
  position: absolute;
  top: 0;
  right: -4px;
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

.section-head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 14px 6px;
  border-radius: 10px;
  background: var(--brand-50);
}

.stat-cell strong {
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  font-size: 22px;
  color: var(--brand-700);
}

.stat-cell span {
  font-size: 11px;
  color: var(--ink-500);
}

.history-wrap {
  min-height: 90px;
}

.action-timeline {
  padding-left: 2px;
}

.action-card {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdfc;
}

.action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.action-reason {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  word-break: break-word;
}

.action-line {
  margin: 4px 0 0;
  line-height: 1.6;
}

.drawer-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sanction-target {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdfc;
}

.sanction-target-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.sanction-target-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.action-list :deep(.el-radio) {
  margin-right: 0;
  height: auto;
  min-height: 46px;
  padding: 8px 12px;
  display: flex;
  align-items: flex-start;
}

.action-list :deep(.el-radio__label) {
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

.duration-input {
  width: 180px;
}

.field-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--ink-500);
}

:deep(.user-table .el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 768px) {
  .page-head {
    align-items: flex-start;
  }

  .keyword-input,
  .filter-select {
    width: 100%;
  }

  .filter-form :deep(.el-form-item) {
    display: block;
    width: 100%;
  }

  .filter-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field-hint {
    display: block;
    margin: 6px 0 0;
  }
}
</style>
