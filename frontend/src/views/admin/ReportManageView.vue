<template>
  <div class="admin-page">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">举报处理</h1>
        <p class="page-desc">领取举报、核查事实并作出处理结论，跟踪申诉复核进度。</p>
      </div>
      <div class="head-actions">
        <el-tag type="warning" effect="plain" round>当前列表共 {{ total }} 条</el-tag>
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
      <div class="toolbar">
        <span class="muted small">按状态筛选举报，点击「详情」查看举报说明与对象快照。</span>
        <div class="spacer" />
        <el-button text :loading="loading" @click="loadReports">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>
    </div>

    <div class="surface table-surface">
      <el-table v-loading="loading" :data="reports" row-key="id" class="report-table">
        <el-table-column prop="reportNo" label="举报号" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="report-no">{{ row.reportNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="举报对象" min-width="230">
          <template #default="{ row }">
            <div class="target-cell">
              <el-tag size="small" type="info" effect="plain" class="target-type">
                {{ row.targetTypeText || labelOf(reportTargetTypeLabels, row.targetType) }}
              </el-tag>
              <span class="target-summary" :title="row.targetSummary || ''">
                {{ truncate(row.targetSummary, 34) || `#${row.targetId}` }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="举报人" width="120">
          <template #default="{ row }">
            {{ row.reporterNickname || '匿名用户' }}
          </template>
        </el-table-column>

        <el-table-column label="举报类别" width="110">
          <template #default="{ row }">
            {{ row.categoryText || labelOf(reportCategoryLabels, row.category) }}
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="状态" width="112">
          <template #default="{ row }">
            <StatusTag kind="report" :value="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="处理人" width="110">
          <template #default="{ row }">{{ row.handlerName || '—' }}</template>
        </el-table-column>

        <el-table-column label="操作" width="304" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUBMITTED'"
              size="small"
              type="primary"
              link
              :loading="busyId === row.id"
              @click="claimReport(row)"
            >
              领取
            </el-button>
            <el-button
              v-if="canHandle(row.status)"
              size="small"
              type="primary"
              link
              @click="openHandle(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_APPEAL'"
              size="small"
              type="warning"
              link
              @click="openAppeal(row)"
            >
              申诉复核
            </el-button>
            <el-button
              v-if="canClose(row.status)"
              size="small"
              type="danger"
              link
              :loading="busyId === row.id"
              @click="closeReport(row)"
            >
              关闭
            </el-button>
            <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState description="当前状态下暂无举报" :image-size="90" />
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
      @closed="onDetailClosed"
    >
      <div v-if="selected" class="drawer-body">
        <div class="drawer-head">
          <div>
            <div class="drawer-title-row">
              <h3 class="drawer-title">举报 {{ selected.reportNo }}</h3>
              <StatusTag kind="report" :value="selected.status" />
            </div>
            <p class="muted drawer-meta">
              {{ selected.targetTypeText || labelOf(reportTargetTypeLabels, selected.targetType) }}
              #{{ selected.targetId }} ·
              {{ formatDateTime(selected.createdAt) }} 提交
            </p>
          </div>
          <el-button text circle class="drawer-close" @click="detailVisible = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <el-descriptions :column="2" border size="small" class="detail-desc">
          <el-descriptions-item label="举报人">
            {{ selected.reporterNickname || '匿名用户' }}
          </el-descriptions-item>
          <el-descriptions-item label="举报类别">
            {{ selected.categoryText || labelOf(reportCategoryLabels, selected.category) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理人">
            {{ selected.handlerName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ formatDateTime(selected.handledAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="对象类型">
            {{ selected.targetTypeText || labelOf(reportTargetTypeLabels, selected.targetType) }}
          </el-descriptions-item>
          <el-descriptions-item label="对象编号">#{{ selected.targetId }}</el-descriptions-item>
        </el-descriptions>

        <section class="drawer-section">
          <h4 class="section-heading">举报说明</h4>
          <p class="description-text">{{ selected.description || '举报人未填写说明' }}</p>
        </section>

        <section class="drawer-section">
          <h4 class="section-heading">对象快照</h4>
          <div class="snapshot-box">
            <p>{{ selected.targetSummary || '暂无对象快照信息' }}</p>
            <span class="muted small">
              快照为举报提交时保存的对象摘要，仅用于核验，不代表对象当前状态。
            </span>
          </div>
        </section>

        <section
          v-if="selected.handlingResult || selected.ruleBasis || selected.actionSummary"
          class="drawer-section"
        >
          <h4 class="section-heading">处理结果</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="处理结论">
              {{ selected.handlingResult || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="规则依据">
              {{ selected.ruleBasis || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="处置动作">
              {{ selected.actionSummary || '—' }}
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <section v-if="selected.appealReason" class="drawer-section">
          <h4 class="section-heading">申诉理由</h4>
          <p class="description-text appeal-text">{{ selected.appealReason }}</p>
        </section>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="handleVisible"
      title="处理举报"
      width="min(600px, 94vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onHandleClosed"
    >
      <p class="dialog-tip">
        处理举报「{{ handleTarget?.reportNo }}」。结论为成立时可同时执行处置动作，处置结果将通知相关用户。
      </p>
      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-position="top"
      >
        <el-form-item label="处理结论" prop="result">
          <el-radio-group
            :model-value="handleForm.result"
            class="option-list"
            @update:model-value="onHandleResultChange"
          >
            <el-radio value="UPHELD" border>
              <span class="option-title">举报成立</span>
              <span class="option-desc">举报事实成立，可执行处置动作</span>
            </el-radio>
            <el-radio value="REJECTED" border>
              <span class="option-title">举报不成立</span>
              <span class="option-desc">证据不足或未发现违规</span>
            </el-radio>
            <el-radio value="SUPPLEMENT" border>
              <span class="option-title">待补充证据</span>
              <span class="option-desc">要求举报人补充说明或证据</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          label="规则依据"
          prop="ruleBasis"
          :required="handleForm.result === 'UPHELD'"
        >
          <el-input
            v-model="handleForm.ruleBasis"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="举报成立时必填，如《校园集市社区规范》第 5 条"
          />
        </el-form-item>

        <el-form-item label="处理理由" prop="reason">
          <el-input
            v-model="handleForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写核查事实与处理理由，至少 2 个字"
          />
        </el-form-item>

        <template v-if="showActionFields">
          <el-form-item label="处置动作" prop="actionType">
            <el-radio-group
              :model-value="handleForm.actionType"
              class="action-options"
              @update:model-value="onHandleActionChange"
            >
              <el-radio
                v-for="option in availableActionOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </el-radio>
            </el-radio-group>
            <span class="field-hint">商品举报仅支持违规下架；用户举报支持警告、限权、封禁。</span>
          </el-form-item>

          <el-form-item v-if="showDuration" label="处置时长（天）" prop="durationDays">
            <el-input-number
              v-model="handleForm.durationDays"
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
        </template>
        <p
          v-else-if="handleForm.result === 'UPHELD' && handleTarget?.targetType === 'MESSAGE'"
          class="muted small message-note"
        >
          消息举报成立后将保留原始证据，不执行商品下架或账户处置。
        </p>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="submitHandle">
          提交处理
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="appealVisible"
      title="申诉复核"
      width="min(560px, 94vw)"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onAppealClosed"
    >
      <p class="dialog-tip">
        举报「{{ appealTarget?.reportNo }}」正在申诉中。请核对原处理依据与申诉理由，选择维持或撤销原处置。
      </p>
      <div v-if="appealTarget?.appealReason" class="appeal-box">
        <span class="appeal-label">申诉理由</span>
        <p>{{ appealTarget.appealReason }}</p>
      </div>
      <el-form
        ref="appealFormRef"
        :model="appealForm"
        :rules="appealRules"
        label-position="top"
      >
        <el-form-item label="复核结论" prop="uphold">
          <el-radio-group
            :model-value="appealForm.uphold"
            class="option-list"
            @update:model-value="onAppealUpholdChange"
          >
            <el-radio value="UPHOLD" border>
              <span class="option-title">维持处置</span>
              <span class="option-desc">申诉理由不成立，维持原处理结论</span>
            </el-radio>
            <el-radio value="REVOKE" border>
              <span class="option-title">撤销处置</span>
              <span class="option-desc">申诉理由成立，撤销原处理结论</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="复核理由" prop="reason">
          <el-input
            v-model="appealForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写复核理由，至少 2 个字"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button type="primary" :loading="appealSubmitting" @click="submitAppeal">
          提交复核
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminReportApi } from '@/api/admin'
import type { ModerationActionType, ReportStatus, ReportVO } from '@/types'
import { formatDateTime, truncate } from '@/utils/format'
import {
  actionTypeLabels,
  labelOf,
  reportCategoryLabels,
  reportTargetTypeLabels
} from '@/utils/dict'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'

type ReportTab = ReportStatus | 'ALL'
type HandleResult = 'UPHELD' | 'REJECTED' | 'SUPPLEMENT'

interface StatusTab {
  value: ReportTab
  label: string
}

interface HandleForm {
  result: string
  ruleBasis: string
  reason: string
  actionType: string
  durationDays: number | undefined
}

interface AppealForm {
  uphold: string
  reason: string
}

const tabs: StatusTab[] = [
  { value: 'ALL', label: '全部' },
  { value: 'SUBMITTED', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'SUPPLEMENT', label: '待补充' },
  { value: 'UPHELD', label: '成立' },
  { value: 'REJECTED', label: '不成立' },
  { value: 'PENDING_APPEAL', label: '申诉中' },
  { value: 'CLOSED', label: '已关闭' }
]

const activeTab = ref<ReportTab>('ALL')
const reports = ref<ReportVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const busyId = ref<number | null>(null)

let requestSeq = 0

function currentStatus(): ReportStatus | '' {
  return activeTab.value === 'ALL' ? '' : activeTab.value
}

function handleTabUpdate(value: string | number): void {
  const next = tabs.find((tab) => tab.value === value)
  if (!next || next.value === activeTab.value) return
  activeTab.value = next.value
  page.value = 1
  void loadReports()
}

async function loadReports(): Promise<void> {
  const seq = (requestSeq += 1)
  loading.value = true
  try {
    const result = await adminReportApi.list({
      status: currentStatus(),
      page: page.value,
      size: pageSize
    })
    if (seq !== requestSeq) return
    reports.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误由 request 拦截器统一提示，页面保留已有数据 */
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function handlePageChange(value: number): void {
  page.value = value
  void loadReports()
}

async function refreshAfterAction(): Promise<void> {
  if (reports.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  await loadReports()
}

function canHandle(status: ReportStatus): boolean {
  return status === 'PROCESSING' || status === 'SUPPLEMENT'
}

function canClose(status: ReportStatus): boolean {
  return status === 'UPHELD' || status === 'REJECTED' || status === 'PENDING_APPEAL'
}

function syncReport(updated: ReportVO): void {
  const index = reports.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) reports.value[index] = updated
  if (selected.value?.id === updated.id) selected.value = updated
}

/* ------------------------------- 详情抽屉 ------------------------------- */

const detailVisible = ref(false)
const selected = ref<ReportVO | null>(null)

function openDetail(row: ReportVO): void {
  selected.value = row
  detailVisible.value = true
}

function onDetailClosed(): void {
  selected.value = null
}

/* -------------------------------- 领取/关闭 ------------------------------- */

async function claimReport(row: ReportVO): Promise<void> {
  if (busyId.value !== null) return
  busyId.value = row.id
  try {
    const updated = await adminReportApi.claim(row.id)
    ElMessage.success('已领取该举报')
    syncReport(updated)
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    busyId.value = null
  }
}

async function closeReport(row: ReportVO): Promise<void> {
  if (busyId.value !== null) return
  try {
    await ElMessageBox.confirm(
      `确认关闭举报「${row.reportNo}」吗？关闭后该举报将不再出现在待处理流程中。`,
      '关闭举报',
      { confirmButtonText: '关闭举报', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  busyId.value = row.id
  try {
    const updated = await adminReportApi.close(row.id)
    ElMessage.success('举报已关闭')
    syncReport(updated)
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    busyId.value = null
  }
}

/* -------------------------------- 处理举报 ------------------------------- */

const handleVisible = ref(false)
const handleTarget = ref<ReportVO | null>(null)
const handleFormRef = ref<FormInstance>()
const handleSubmitting = ref(false)
const handleForm = reactive<HandleForm>({
  result: 'UPHELD',
  ruleBasis: '',
  reason: '',
  actionType: '',
  durationDays: undefined
})

const handleResultValues: HandleResult[] = ['UPHELD', 'REJECTED', 'SUPPLEMENT']
const handleActionOptions: { value: ModerationActionType; label: string }[] = [
  { value: 'TAKEDOWN', label: actionTypeLabels.TAKEDOWN },
  { value: 'WARN', label: actionTypeLabels.WARN },
  { value: 'LIMIT', label: actionTypeLabels.LIMIT },
  { value: 'BAN', label: actionTypeLabels.BAN }
]

const availableActionOptions = computed(() => {
  const target = handleTarget.value
  if (!target) return []
  if (target.targetType === 'ITEM') {
    return handleActionOptions.filter((option) => option.value === 'TAKEDOWN')
  }
  if (target.targetType === 'USER') {
    return handleActionOptions.filter((option) => option.value !== 'TAKEDOWN')
  }
  return []
})

function isHandleResult(value: string): value is HandleResult {
  return handleResultValues.some((item) => item === value)
}

function isAvailableAction(value: string): value is ModerationActionType {
  return availableActionOptions.value.some((option) => option.value === value)
}

function onHandleResultChange(value: string | number | boolean | undefined): void {
  if (typeof value === 'string') handleForm.result = value
}

function onHandleActionChange(value: string | number | boolean | undefined): void {
  if (typeof value === 'string') handleForm.actionType = value
}

const handleRules: FormRules = {
  reason: [
    { required: true, message: '请填写处理理由', trigger: 'blur' },
    { min: 2, max: 500, message: '处理理由需为 2-500 个字符', trigger: 'blur' }
  ],
  ruleBasis: [{ max: 500, message: '规则依据不能超过 500 个字符', trigger: 'blur' }]
}

const showActionFields = computed(
  () => handleForm.result === 'UPHELD' && availableActionOptions.value.length > 0
)
const showDuration = computed(
  () => handleForm.actionType === 'LIMIT' || handleForm.actionType === 'BAN'
)
const durationPlaceholder = computed(() =>
  handleForm.actionType === 'LIMIT' ? '留空默认限权 7 天' : '留空表示长期封禁'
)
const durationHint = computed(() =>
  handleForm.actionType === 'LIMIT'
    ? '留空时服务端默认限权 7 天；可填写 1-3650 天。'
    : '留空表示长期封禁；可填写 1-3650 天。'
)

watch(
  () => handleForm.result,
  (value) => {
    if (value !== 'UPHELD') {
      handleForm.actionType = ''
      handleForm.durationDays = undefined
      return
    }
    if (
      availableActionOptions.value.length > 0 &&
      !isAvailableAction(handleForm.actionType)
    ) {
      handleForm.actionType = defaultActionTypeForTarget()
    }
  }
)

watch(
  () => handleForm.actionType,
  (value) => {
    if (value !== 'LIMIT' && value !== 'BAN') {
      handleForm.durationDays = undefined
    }
  }
)

function defaultActionType(row: ReportVO): string {
  if (row.targetType === 'ITEM') return 'TAKEDOWN'
  if (row.targetType === 'USER') return 'WARN'
  return ''
}

function defaultActionTypeForTarget(): string {
  const target = handleTarget.value
  return target ? defaultActionType(target) : ''
}

function openHandle(row: ReportVO): void {
  handleTarget.value = row
  handleForm.result = 'UPHELD'
  handleForm.ruleBasis = row.ruleBasis || ''
  handleForm.reason = ''
  handleForm.actionType = defaultActionType(row)
  handleForm.durationDays = undefined
  handleVisible.value = true
  void nextTick(() => handleFormRef.value?.clearValidate())
}

function onHandleClosed(): void {
  handleTarget.value = null
  handleSubmitting.value = false
  handleForm.result = 'UPHELD'
  handleForm.ruleBasis = ''
  handleForm.reason = ''
  handleForm.actionType = ''
  handleForm.durationDays = undefined
  void nextTick(() => handleFormRef.value?.clearValidate())
}

async function submitHandle(): Promise<void> {
  const target = handleTarget.value
  if (!target || !handleFormRef.value || handleSubmitting.value) return
  const result = handleForm.result
  if (!isHandleResult(result)) {
    ElMessage.warning('请选择处理结论')
    return
  }
  if (result === 'UPHELD' && !handleForm.ruleBasis.trim()) {
    ElMessage.warning('举报成立时必须填写规则依据')
    return
  }
  if (
    result === 'UPHELD' &&
    availableActionOptions.value.length > 0 &&
    !isAvailableAction(handleForm.actionType)
  ) {
    ElMessage.warning('请选择与举报对象匹配的处置动作')
    return
  }
  const valid = await handleFormRef.value.validate().catch(() => false)
  if (!valid) return
  const reason = handleForm.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('处理理由至少需要 2 个字符')
    return
  }
  const duration = handleForm.durationDays
  if (showDuration.value && typeof duration === 'number' && duration < 1) {
    ElMessage.warning('处置时长至少为 1 天，或留空由服务端按默认策略处理')
    return
  }
  const payload: {
    result: HandleResult
    ruleBasis?: string
    reason: string
    actionType?: ModerationActionType
    durationDays?: number
  } = {
    result,
    reason
  }
  if (handleForm.ruleBasis.trim()) payload.ruleBasis = handleForm.ruleBasis.trim()
  const actionType = handleForm.actionType
  if (result === 'UPHELD' && isAvailableAction(actionType)) {
    payload.actionType = actionType
    if ((actionType === 'LIMIT' || actionType === 'BAN') && typeof duration === 'number') {
      payload.durationDays = duration
    }
  }
  handleSubmitting.value = true
  try {
    const updated = await adminReportApi.handle(target.id, payload)
    ElMessage.success('举报处理完成')
    syncReport(updated)
    handleVisible.value = false
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    handleSubmitting.value = false
  }
}

/* -------------------------------- 申诉复核 ------------------------------- */

const appealVisible = ref(false)
const appealTarget = ref<ReportVO | null>(null)
const appealFormRef = ref<FormInstance>()
const appealForm = reactive<AppealForm>({ uphold: 'UPHOLD', reason: '' })
const appealSubmitting = ref(false)

function onAppealUpholdChange(value: string | number | boolean | undefined): void {
  if (value === 'UPHOLD' || value === 'REVOKE') appealForm.uphold = value
}

const appealRules: FormRules = {
  reason: [
    { required: true, message: '请填写复核理由', trigger: 'blur' },
    { min: 2, max: 500, message: '复核理由需为 2-500 个字符', trigger: 'blur' }
  ]
}

function openAppeal(row: ReportVO): void {
  appealTarget.value = row
  appealForm.uphold = 'UPHOLD'
  appealForm.reason = ''
  appealVisible.value = true
  void nextTick(() => appealFormRef.value?.clearValidate())
}

function onAppealClosed(): void {
  appealTarget.value = null
  appealForm.uphold = 'UPHOLD'
  appealForm.reason = ''
  appealSubmitting.value = false
  void nextTick(() => appealFormRef.value?.clearValidate())
}

async function submitAppeal(): Promise<void> {
  const target = appealTarget.value
  if (!target || !appealFormRef.value || appealSubmitting.value) return
  const valid = await appealFormRef.value.validate().catch(() => false)
  if (!valid) return
  const reason = appealForm.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('复核理由至少需要 2 个字符')
    return
  }
  appealSubmitting.value = true
  try {
    const updated = await adminReportApi.resolveAppeal(target.id, {
      uphold: appealForm.uphold === 'UPHOLD',
      reason
    })
    ElMessage.success('申诉复核已提交')
    syncReport(updated)
    appealVisible.value = false
    await refreshAfterAction()
  } catch {
    /* 接口错误由 request 拦截器统一提示 */
  } finally {
    appealSubmitting.value = false
  }
}

onMounted(() => {
  void loadReports()
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
  padding-bottom: 12px;
}

:deep(.status-tabs .el-tabs__header) {
  margin: 0 0 12px;
}

:deep(.status-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--line);
}

.table-surface {
  min-width: 0;
}

.report-table {
  width: 100%;
}

.report-no {
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  font-size: 13px;
  color: var(--ink-700);
}

.target-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.target-type {
  flex-shrink: 0;
}

.target-summary {
  font-size: 13px;
  color: var(--ink-700);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-body {
  padding: 4px 4px 12px;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 16px;
}

.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.drawer-title {
  margin: 0;
  font-size: 17px;
  color: var(--ink-900);
}

.drawer-meta {
  margin: 8px 0 0;
  font-size: 12px;
}

.drawer-close {
  flex-shrink: 0;
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

.appeal-text {
  background: #fff8e8;
}

.snapshot-box {
  padding: 12px 14px;
  border: 1px dashed #d7e5dd;
  border-radius: 10px;
  background: #fbfdfc;
}

.snapshot-box p {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  word-break: break-word;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
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

.action-options {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
}

.duration-input {
  width: 180px;
}

.field-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--ink-500);
}

.message-note {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--brand-50);
  line-height: 1.6;
}

.appeal-box {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff8e8;
}

.appeal-label {
  font-size: 12px;
  color: var(--ink-500);
}

.appeal-box p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  word-break: break-word;
}

:deep(.report-table .el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 768px) {
  .page-head {
    align-items: flex-start;
  }

  .drawer-head {
    flex-direction: row;
  }

  .field-hint {
    display: block;
    margin: 6px 0 0;
  }
}
</style>
