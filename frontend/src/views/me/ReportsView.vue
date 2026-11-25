<template>
  <div class="me-page reports-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">我的举报</h1>
        <p class="page-subtitle">查看你提交的举报进度、处理结果与规则依据。</p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="loadReports">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </header>

    <div class="surface filter-bar">
      <div class="filter-info">
        <span class="filter-label">状态筛选</span>
        <el-select v-model="statusFilter" class="status-select">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
      <span class="muted small">共 {{ total }} 条举报</span>
    </div>

    <div v-loading="loading" class="reports-panel">
      <div v-if="pagedReports.length" class="report-list">
        <article v-for="report in pagedReports" :key="report.id" class="report-card">
          <header class="report-head">
            <span class="report-no">
              <el-icon><Flag /></el-icon>
              {{ report.reportNo }}
            </span>
            <StatusTag kind="report" :value="report.status" />
          </header>

          <div class="report-body">
            <div class="report-main">
              <h3 class="report-target">
                <el-tag type="info" effect="plain" size="small">
                  {{ report.targetTypeText || reportTargetTypeLabels[report.targetType] }}
                </el-tag>
                <span>{{ report.targetSummary || `对象 #${report.targetId}` }}</span>
              </h3>
              <p class="report-desc">{{ truncate(report.description, 140) }}</p>
              <div class="report-meta">
                <span>类别：{{ report.categoryText || labelOf(reportCategoryLabels, report.category) }}</span>
                <span>提交：{{ formatDateTime(report.createdAt) }}</span>
              </div>
              <div v-if="report.handlingResult || report.ruleBasis" class="report-handling">
                <p v-if="report.handlingResult">
                  <span class="handling-label">处理结果：</span>{{ truncate(report.handlingResult, 80) }}
                </p>
                <p v-if="report.ruleBasis">
                  <span class="handling-label">规则依据：</span>{{ truncate(report.ruleBasis, 80) }}
                </p>
              </div>
            </div>
            <div class="report-side">
              <el-button size="small" type="primary" plain @click="openDetail(report)">详情</el-button>
            </div>
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

    <AppPagination v-model:page="page" :size="pageSize" :total="total" />

    <el-dialog
      v-model="detailVisible"
      title="举报详情"
      width="min(640px, 92vw)"
      :close-on-click-modal="false"
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailReport">
          <div class="detail-head">
            <div>
              <p class="detail-no">举报号 {{ detailReport.reportNo }}</p>
              <p class="muted small">提交于 {{ formatDateTime(detailReport.createdAt) }}</p>
            </div>
            <StatusTag kind="report" :value="detailReport.status" />
          </div>

          <dl class="detail-grid">
            <div class="detail-row">
              <dt>举报对象</dt>
              <dd>
                {{ detailReport.targetTypeText || reportTargetTypeLabels[detailReport.targetType] }}
              </dd>
            </div>
            <div class="detail-row">
              <dt>对象摘要</dt>
              <dd>{{ detailReport.targetSummary || `#${detailReport.targetId}` }}</dd>
            </div>
            <div class="detail-row">
              <dt>举报类别</dt>
              <dd>{{ detailReport.categoryText || labelOf(reportCategoryLabels, detailReport.category) }}</dd>
            </div>
            <div class="detail-row">
              <dt>当前状态</dt>
              <dd>{{ detailReport.statusText || reportStatusLabels[detailReport.status] }}</dd>
            </div>
            <div class="detail-row">
              <dt>处理人</dt>
              <dd>{{ detailReport.handlerName || '—' }}</dd>
            </div>
            <div class="detail-row">
              <dt>处理时间</dt>
              <dd>{{ formatDateTime(detailReport.handledAt) }}</dd>
            </div>
          </dl>

          <section class="detail-section">
            <h4>举报说明</h4>
            <p>{{ detailReport.description }}</p>
          </section>

          <section v-if="detailReport.handlingResult" class="detail-section">
            <h4>处理结果</h4>
            <p>{{ detailReport.handlingResult }}</p>
          </section>

          <section v-if="detailReport.actionSummary" class="detail-section">
            <h4>处置措施</h4>
            <p>{{ detailReport.actionSummary }}</p>
          </section>

          <section v-if="detailReport.ruleBasis" class="detail-section">
            <h4>规则依据</h4>
            <p>{{ detailReport.ruleBasis }}</p>
          </section>

          <section v-if="detailReport.appealReason" class="detail-section">
            <h4>申诉说明</h4>
            <p>{{ detailReport.appealReason }}</p>
          </section>

          <p v-if="!detailReport.handlingResult && !detailReport.ruleBasis" class="detail-note">
            平台尚未给出处理结论，请耐心等待或留意通知中心的处理通知。
          </p>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { reportApi } from '@/api/social'
import {
  labelOf,
  reportCategoryLabels,
  reportStatusLabels,
  reportTargetTypeLabels
} from '@/utils/dict'
import { formatDateTime, truncate } from '@/utils/format'
import type { ReportStatus, ReportVO } from '@/types'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()

const FETCH_SIZE = 50
const PAGE_SIZE = 8

const reports = ref<ReportVO[]>([])
const loading = ref(false)
const statusFilter = ref<ReportStatus | 'ALL'>('ALL')
const page = ref(1)
const pageSize = PAGE_SIZE

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailReport = ref<ReportVO | null>(null)

const statusOptions: { value: ReportStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: '全部状态' },
  ...(Object.entries(reportStatusLabels) as [ReportStatus, string][]).map(([value, label]) => ({
    value,
    label
  }))
]

const filteredReports = computed(() =>
  statusFilter.value === 'ALL'
    ? reports.value
    : reports.value.filter((report) => report.status === statusFilter.value)
)

const total = computed(() => filteredReports.value.length)

const pagedReports = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredReports.value.slice(start, start + pageSize)
})

const hasReports = computed(() => reports.value.length > 0)

const emptyDescription = computed(() =>
  hasReports.value ? '当前筛选状态下没有举报记录' : '你还没有提交过举报，遇到违规商品或行为可以在详情页发起举报'
)

const emptyActionText = computed(() => (hasReports.value ? '查看全部举报' : '去市场逛逛'))

/**
 * /reports/mine 的 reportApi.mine 仅暴露分页参数，没有 status 参数。
 * 为保证状态筛选与分页总数一致，这里拉取全部分页后在本地筛选与分页。
 */
async function loadReports(): Promise<void> {
  loading.value = true
  try {
    const first = await reportApi.mine({ page: 1, size: FETCH_SIZE })
    const records = [...first.records]
    let fetched = first.records.length
    let current = 2
    while (fetched < first.total && current <= 100) {
      const next = await reportApi.mine({ page: current, size: FETCH_SIZE })
      if (!next.records.length) break
      records.push(...next.records)
      fetched += next.records.length
      current += 1
    }
    reports.value = records
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留当前数据 */
  } finally {
    loading.value = false
  }
}

async function openDetail(report: ReportVO): Promise<void> {
  detailReport.value = report
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailReport.value = await reportApi.detail(report.id)
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留列表中的摘要数据 */
  } finally {
    detailLoading.value = false
  }
}

function handleEmptyAction(): void {
  if (hasReports.value) {
    statusFilter.value = 'ALL'
  } else {
    router.push('/')
  }
}

watch(statusFilter, () => {
  page.value = 1
})

watch(total, (value) => {
  const maxPage = Math.max(1, Math.ceil(value / pageSize))
  if (page.value > maxPage) {
    page.value = maxPage
  }
})

onMounted(loadReports)
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

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.filter-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 13px;
  color: var(--ink-700);
}

.status-select {
  width: 160px;
}

.reports-panel {
  min-height: 220px;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-card {
  padding: 16px 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.report-card:hover {
  border-color: #cbe2d6;
  box-shadow: 0 8px 22px rgba(31, 107, 74, 0.08);
}

.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
}

.report-no {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--ink-500);
  letter-spacing: 0.3px;
}

.report-body {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding-top: 12px;
}

.report-main {
  flex: 1;
  min-width: 0;
}

.report-target {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
  word-break: break-word;
}

.report-desc {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  word-break: break-word;
}

.report-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--ink-500);
}

.report-handling {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--brand-50);
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-700);
}

.report-handling p {
  margin: 0;
}

.report-handling p + p {
  margin-top: 4px;
}

.handling-label {
  color: var(--ink-500);
}

.report-side {
  flex-shrink: 0;
}

.detail-body {
  min-height: 160px;
  max-height: 64vh;
  overflow-y: auto;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.detail-no {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-900);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0 0 18px;
}

.detail-row {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
}

.detail-row dt {
  margin-bottom: 5px;
  font-size: 12px;
  color: var(--ink-500);
}

.detail-row dd {
  margin: 0;
  font-size: 13px;
  color: var(--ink-900);
  word-break: break-word;
}

.detail-section {
  padding: 12px 14px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: var(--brand-50);
}

.detail-section h4 {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-700);
}

.detail-section p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-note {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-500);
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
  }

  .filter-info {
    justify-content: space-between;
  }

  .status-select {
    flex: 1;
    max-width: 180px;
  }

  .report-body {
    flex-direction: column;
  }

  .report-side {
    width: 100%;
    text-align: right;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
