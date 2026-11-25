<template>
  <div class="admin-page dashboard-page">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">运营统计</h1>
        <p class="page-desc">按时间区间查看平台发布、成交、评价与治理概览。</p>
      </div>
      <div class="head-actions">
        <el-date-picker
          :model-value="dateRange"
          type="daterange"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :shortcuts="rangeShortcuts"
          :disabled-date="disableFutureDate"
          :clearable="false"
          class="range-picker"
          @update:model-value="handleRangeUpdate"
          @change="handleRangeChange"
        />
        <el-button type="primary" :loading="loading" @click="loadStats">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>
    </div>

    <div v-if="stats" v-loading="loading" class="dashboard-body">
      <p class="range-hint">
        统计区间：{{ formatDate(stats.from) }} 至 {{ formatDate(stats.to) }}
        <span class="muted">（默认近 30 天）</span>
      </p>

      <section class="metric-grid">
        <article
          v-for="card in metricCards"
          :key="card.key"
          class="metric-card"
          :class="`tone-${card.tone}`"
        >
          <div class="metric-icon">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="metric-content">
            <span class="metric-label">{{ card.label }}</span>
            <strong class="metric-value">
              {{ card.value }}<small v-if="card.unit">{{ card.unit }}</small>
            </strong>
            <span class="metric-sub">{{ card.sub }}</span>
          </div>
        </article>
      </section>

      <section class="chart-grid">
        <div class="surface chart-card">
          <div class="card-head">
            <div>
              <h3 class="card-title">发布与成交趋势</h3>
              <p class="card-sub">按日统计的发布量与成交量</p>
            </div>
            <el-tag v-if="stats.trend.length" type="success" effect="plain" round size="small">
              {{ stats.trend.length }} 天
            </el-tag>
          </div>
          <div v-if="hasTrend" ref="trendChartRef" class="chart" />
          <div v-else class="chart-empty">
            <EmptyState description="所选区间暂无趋势数据" :image-size="80" />
          </div>
        </div>

        <div class="surface chart-card pie-card">
          <div class="card-head">
            <div>
              <h3 class="card-title">举报状态分布</h3>
              <p class="card-sub">区间内举报的处理状态占比</p>
            </div>
            <el-tag type="warning" effect="plain" round size="small">
              共 {{ formatNumber(stats.reports.total) }} 条
            </el-tag>
          </div>
          <div v-if="hasReportData" ref="reportChartRef" class="chart" />
          <div v-else class="chart-empty">
            <EmptyState description="所选区间暂无举报数据" :image-size="80" />
          </div>
        </div>
      </section>

      <section class="bottom-grid">
        <div class="surface category-card">
          <div class="card-head">
            <div>
              <h3 class="card-title">分类发布分布</h3>
              <p class="card-sub">区间内各分类的发布量与占比</p>
            </div>
            <span class="muted small">共 {{ formatNumber(categoryTotal) }} 件</span>
          </div>
          <el-table
            v-if="categoryRows.length"
            :data="categoryRows"
            size="small"
            class="category-table"
          >
            <el-table-column type="index" label="#" width="46" />
            <el-table-column prop="name" label="分类" min-width="120" show-overflow-tooltip />
            <el-table-column label="发布量" width="90" align="right">
              <template #default="{ row }">{{ formatNumber(row.count) }}</template>
            </el-table-column>
            <el-table-column label="占比" min-width="180">
              <template #default="{ row }">
                <div class="ratio-cell">
                  <el-progress
                    :percentage="row.percent"
                    :show-text="false"
                    :stroke-width="6"
                    color="#3a9c6c"
                    class="ratio-progress"
                  />
                  <span class="ratio-text">{{ row.percent.toFixed(1) }}%</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <EmptyState v-else description="所选区间暂无分类发布数据" :image-size="80" />
        </div>

        <div class="right-stack">
          <div class="surface orders-panel">
            <div class="card-head">
              <div>
                <h3 class="card-title">订单概览</h3>
                <p class="card-sub">区间订单状态汇总</p>
              </div>
              <el-icon class="head-icon"><ShoppingCart /></el-icon>
            </div>
            <div class="orders-grid">
              <div v-for="entry in orderOverview" :key="entry.label" class="order-cell">
                <strong>{{ entry.value }}</strong>
                <span>{{ entry.label }}</span>
              </div>
            </div>
            <div class="progress-block">
              <div class="progress-label">
                <span>成交率</span>
                <strong>{{ formatPercent(stats.orders.completionRate) }}</strong>
              </div>
              <el-progress
                :percentage="completionPercent"
                :show-text="false"
                :stroke-width="8"
                color="#2f8259"
              />
              <p class="muted small progress-note">
                订单总数 {{ formatNumber(stats.orders.total) }} 单，已完成
                {{ formatNumber(stats.orders.completed) }} 单
              </p>
            </div>
          </div>

          <div class="surface actions-panel">
            <div class="card-head">
              <div>
                <h3 class="card-title">处置动作分布</h3>
                <p class="card-sub">按动作类型统计管理员处置</p>
              </div>
              <span class="muted small">共 {{ formatNumber(stats.actions.total) }} 次</span>
            </div>
            <div v-if="actionRows.length" class="dist-list">
              <div v-for="item in actionRows" :key="item.status" class="dist-row">
                <div class="dist-head">
                  <span>{{ item.label || labelOf(actionTypeLabels, item.status) }}</span>
                  <strong>{{ formatNumber(item.count) }}</strong>
                </div>
                <div class="dist-bar">
                  <span :style="{ width: `${actionShare(item.count)}%` }" />
                </div>
              </div>
            </div>
            <EmptyState v-else description="所选区间暂无处置动作" :image-size="72" />
          </div>
        </div>
      </section>
    </div>

    <div v-else-if="loading" class="surface loading-surface">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else class="surface">
      <EmptyState
        description="暂无统计数据，请稍后重试"
        action-text="重新加载"
        @action="loadStats"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { adminStatsApi } from '@/api/admin'
import type { StatsVO, StatusCount } from '@/types'
import { formatDate } from '@/utils/format'
import { actionTypeLabels, labelOf, reportStatusLabels } from '@/utils/dict'
import EmptyState from '@/components/EmptyState.vue'

type MetricTone = 'green' | 'blue' | 'amber' | 'gold' | 'red' | 'violet'

interface MetricCard {
  key: string
  label: string
  value: string
  unit: string
  sub: string
  icon: string
  tone: MetricTone
}

interface CategoryRow {
  name: string
  count: number
  percent: number
}

interface ShortcutItem {
  text: string
  value: () => [Date, Date]
}

const PIE_COLORS = ['#f59e0b', '#3b82f6', '#8b5cf6', '#ef4444', '#10b981', '#0ea5e9', '#94a3b8']

function defaultRange(): [string, string] {
  return [dayjs().subtract(29, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
}

const loading = ref(true)
const stats = ref<StatsVO | null>(null)
const dateRange = ref<[string, string]>(defaultRange())

const trendChartRef = ref<HTMLElement | null>(null)
const reportChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let reportChart: echarts.ECharts | null = null
let requestSeq = 0

const rangeShortcuts: ShortcutItem[] = [
  {
    text: '近 7 天',
    value: () => [dayjs().subtract(6, 'day').toDate(), dayjs().toDate()]
  },
  {
    text: '近 30 天',
    value: () => [dayjs().subtract(29, 'day').toDate(), dayjs().toDate()]
  },
  {
    text: '近 90 天',
    value: () => [dayjs().subtract(89, 'day').toDate(), dayjs().toDate()]
  }
]

function parseRangeValue(value: unknown): [string, string] | null {
  if (!Array.isArray(value)) return null
  const list = value as unknown[]
  if (list.length !== 2) return null
  const first = list[0]
  const second = list[1]
  if (first instanceof Date && second instanceof Date) {
    return [dayjs(first).format('YYYY-MM-DD'), dayjs(second).format('YYYY-MM-DD')]
  }
  if (typeof first === 'string' && typeof second === 'string' && first && second) {
    return [first, second]
  }
  return null
}

function disableFutureDate(date: Date): boolean {
  return dayjs(date).isAfter(dayjs(), 'day')
}

function handleRangeUpdate(value: unknown): void {
  const range = parseRangeValue(value)
  if (range) dateRange.value = range
}

function handleRangeChange(value: unknown): void {
  const range = parseRangeValue(value)
  dateRange.value = range ?? defaultRange()
  void loadStats()
}

async function loadStats(): Promise<void> {
  const seq = (requestSeq += 1)
  loading.value = true
  try {
    const [from, to] = dateRange.value
    const result = await adminStatsApi.get({ from, to })
    if (seq !== requestSeq) return
    stats.value = result
  } catch {
    /* 接口错误由 request 拦截器统一提示，页面保留已有数据 */
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function formatNumber(value?: number | null): string {
  return Number(value ?? 0).toLocaleString('zh-CN')
}

function ratePercent(value?: number | null): number {
  const raw = Number(value ?? 0)
  if (!Number.isFinite(raw)) return 0
  return raw
}

function formatPercent(value?: number | null): string {
  return `${ratePercent(value).toFixed(1)}%`
}

function clampPercent(value: number): number {
  return Math.min(100, Math.max(0, Math.round(value)))
}

function formatTrendDate(date: string): string {
  const parsed = dayjs(date)
  return parsed.isValid() ? parsed.format('MM-DD') : date
}

const hasTrend = computed(() => (stats.value?.trend.length ?? 0) > 0)

const reportStatusRows = computed<StatusCount[]>(() =>
  (stats.value?.reports.byStatus ?? []).filter((item) => item.count > 0)
)

const hasReportData = computed(() => reportStatusRows.value.length > 0)

const categoryRows = computed<CategoryRow[]>(() => {
  const list = stats.value?.items.byCategory ?? []
  const total = list.reduce((sum, item) => sum + item.count, 0)
  return list.map((item) => ({
    name: item.name,
    count: item.count,
    percent: total > 0 ? Math.round((item.count / total) * 1000) / 10 : 0
  }))
})

const categoryTotal = computed(() =>
  (stats.value?.items.byCategory ?? []).reduce((sum, item) => sum + item.count, 0)
)

const actionRows = computed<StatusCount[]>(() =>
  (stats.value?.actions.byType ?? []).filter((item) => item.count > 0)
)

const actionTotal = computed(() => {
  const sum = actionRows.value.reduce((total, item) => total + item.count, 0)
  return sum > 0 ? sum : (stats.value?.actions.total ?? 0)
})

function actionShare(count: number): number {
  if (actionTotal.value <= 0) return 0
  return clampPercent((count / actionTotal.value) * 100)
}

const activeReportCount = computed(() =>
  reportStatusRows.value
    .filter((item) =>
      ['SUBMITTED', 'PROCESSING', 'SUPPLEMENT', 'PENDING_APPEAL'].includes(item.status)
    )
    .reduce((sum, item) => sum + item.count, 0)
)

const metricCards = computed<MetricCard[]>(() => {
  const data = stats.value
  if (!data) return []
  return [
    {
      key: 'published',
      label: '发布量',
      value: formatNumber(data.items.published),
      unit: '件',
      sub: `当前在售 ${formatNumber(data.items.onSale)} 件 · 待审核 ${formatNumber(data.items.pendingAudit)} 件`,
      icon: 'Sell',
      tone: 'green'
    },
    {
      key: 'completed',
      label: '成交量',
      value: formatNumber(data.orders.completed),
      unit: '单',
      sub: `区间订单 ${formatNumber(data.orders.total)} 单 · 进行中 ${formatNumber(data.orders.active)} 单`,
      icon: 'ShoppingCart',
      tone: 'blue'
    },
    {
      key: 'rate',
      label: '成交率',
      value: formatPercent(data.orders.completionRate),
      unit: '',
      sub: `已取消 ${formatNumber(data.orders.cancelled)} 单`,
      icon: 'TrendCharts',
      tone: 'amber'
    },
    {
      key: 'rating',
      label: '评价均分',
      value: Number(data.reviews.avgRating ?? 0).toFixed(1),
      unit: '分',
      sub: `共 ${formatNumber(data.reviews.count)} 条评价`,
      icon: 'StarFilled',
      tone: 'gold'
    },
    {
      key: 'reports',
      label: '举报总数',
      value: formatNumber(data.reports.total),
      unit: '条',
      sub: `处理中或待处理 ${formatNumber(activeReportCount.value)} 条`,
      icon: 'Flag',
      tone: 'red'
    },
    {
      key: 'actions',
      label: '处置动作数',
      value: formatNumber(data.actions.total),
      unit: '次',
      sub: '区间内管理员处置记录',
      icon: 'SetUp',
      tone: 'violet'
    }
  ]
})

const orderOverview = computed(() => {
  const orders = stats.value?.orders
  return [
    { label: '订单总数', value: formatNumber(orders?.total) },
    { label: '已完成', value: formatNumber(orders?.completed) },
    { label: '已取消', value: formatNumber(orders?.cancelled) },
    { label: '进行中', value: formatNumber(orders?.active) }
  ]
})

const completionPercent = computed(() => clampPercent(ratePercent(stats.value?.orders.completionRate)))

function ensureTrendChart(): echarts.ECharts | null {
  if (!trendChartRef.value) return null
  if (!trendChart || trendChart.isDisposed()) {
    trendChart = echarts.init(trendChartRef.value)
  }
  return trendChart
}

function ensureReportChart(): echarts.ECharts | null {
  if (!reportChartRef.value) return null
  if (!reportChart || reportChart.isDisposed()) {
    reportChart = echarts.init(reportChartRef.value)
  }
  return reportChart
}

function renderTrendChart(): void {
  const data = stats.value?.trend ?? []
  if (!data.length) {
    trendChart?.dispose()
    trendChart = null
    return
  }
  const chart = ensureTrendChart()
  if (!chart) return
  const option: echarts.EChartsOption = {
    color: ['#2f8259', '#f59e0b'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' }
    },
    legend: {
      right: 4,
      top: 0,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 6,
      textStyle: { color: '#6b7f75', fontSize: 12 }
    },
    grid: { left: 8, right: 16, top: 44, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((point) => formatTrendDate(point.date)),
      axisLine: { lineStyle: { color: '#dfe9e3' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a9a91', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#8a9a91', fontSize: 11 },
      splitLine: { lineStyle: { color: '#edf3ef', type: 'dashed' } }
    },
    series: [
      {
        name: '发布量',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: data.map((point) => point.published),
        lineStyle: { width: 2.5, color: '#2f8259' },
        itemStyle: { color: '#2f8259' },
        areaStyle: { color: 'rgba(47, 130, 89, 0.10)' }
      },
      {
        name: '成交量',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: data.map((point) => point.completed),
        lineStyle: { width: 2.5, color: '#f59e0b' },
        itemStyle: { color: '#f59e0b' },
        areaStyle: { color: 'rgba(245, 158, 11, 0.08)' }
      }
    ]
  }
  chart.setOption(option, true)
}

function renderReportChart(): void {
  const rows = reportStatusRows.value
  if (!rows.length) {
    reportChart?.dispose()
    reportChart = null
    return
  }
  const chart = ensureReportChart()
  if (!chart) return
  const option: echarts.EChartsOption = {
    color: PIE_COLORS,
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 条（{d}%）'
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#6b7f75', fontSize: 11 }
    },
    series: [
      {
        name: '举报状态',
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold',
            color: '#1c2b24',
            formatter: '{b}\n{c} 条'
          }
        },
        data: rows.map((item) => ({
          name: item.label || labelOf(reportStatusLabels, item.status),
          value: item.count
        }))
      }
    ]
  }
  chart.setOption(option, true)
}

function renderCharts(): void {
  renderTrendChart()
  renderReportChart()
}

function handleResize(): void {
  trendChart?.resize()
  reportChart?.resize()
}

watch(stats, async () => {
  await nextTick()
  renderCharts()
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
  void loadStats()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  reportChart?.dispose()
  trendChart = null
  reportChart = null
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

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.range-picker {
  width: 280px;
  max-width: 100%;
}

.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.range-hint {
  margin: -4px 0 0;
  font-size: 12px;
  color: var(--ink-500);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.metric-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 22px rgba(31, 107, 74, 0.07);
}

.metric-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--tone);
}

.tone-green {
  --tone: #2f8259;
  --tone-soft: #e6f4ec;
}

.tone-blue {
  --tone: #3b82f6;
  --tone-soft: #eaf2ff;
}

.tone-amber {
  --tone: #d97706;
  --tone-soft: #fff4e2;
}

.tone-gold {
  --tone: #ca8a04;
  --tone-soft: #fdf7dc;
}

.tone-red {
  --tone: #dc2626;
  --tone-soft: #fdecec;
}

.tone-violet {
  --tone: #7c3aed;
  --tone-soft: #f1ebff;
}

.metric-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: var(--tone);
  background: var(--tone-soft);
}

.metric-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.metric-label {
  font-size: 12px;
  color: var(--ink-500);
}

.metric-value {
  margin-top: 2px;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  font-size: 23px;
  line-height: 1.25;
  color: var(--ink-900);
}

.metric-value small {
  margin-left: 2px;
  font-size: 11px;
  font-weight: 400;
  color: var(--ink-500);
}

.metric-sub {
  margin-top: 4px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--ink-300);
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 1fr);
  gap: 18px;
}

.chart-card {
  display: flex;
  flex-direction: column;
  min-height: 352px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-900);
}

.card-sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-500);
}

.chart {
  width: 100%;
  height: 290px;
  flex: 1;
}

.chart-empty {
  height: 290px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(300px, 1fr);
  gap: 18px;
}

.category-card {
  min-width: 0;
}

.category-table {
  width: 100%;
}

.ratio-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ratio-progress {
  flex: 1;
  min-width: 70px;
}

.ratio-text {
  width: 46px;
  text-align: right;
  font-size: 12px;
  color: var(--ink-500);
}

.right-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.orders-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.head-icon {
  color: var(--brand-500);
  font-size: 20px;
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: var(--brand-50);
}

.order-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.order-cell strong {
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
  font-size: 19px;
  color: var(--ink-900);
}

.order-cell span {
  font-size: 11px;
  color: var(--ink-500);
}

.progress-block {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-500);
}

.progress-label strong {
  color: var(--brand-600);
  font-size: 14px;
}

.progress-note {
  margin: 0;
}

.actions-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dist-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dist-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.dist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-700);
}

.dist-head strong {
  color: var(--ink-900);
}

.dist-bar {
  height: 6px;
  border-radius: 3px;
  background: #eef3f0;
  overflow: hidden;
}

.dist-bar span {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--brand-500), var(--brand-600));
}

.loading-surface {
  min-height: 320px;
}

:deep(.category-table .el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 1400px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .chart-grid,
  .bottom-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .head-actions {
    width: 100%;
  }

  .range-picker {
    width: 100%;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .metric-card {
    padding: 12px;
  }

  .metric-icon {
    width: 34px;
    height: 34px;
    font-size: 17px;
  }

  .metric-value {
    font-size: 20px;
  }

  .orders-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-card {
    min-height: 320px;
  }
}
</style>
