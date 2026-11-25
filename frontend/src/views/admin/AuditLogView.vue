<template>
  <div class="admin-page">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">审计日志</h1>
        <p class="page-desc">查看管理员关键操作记录，核对操作人、对象、理由与前后值。</p>
      </div>
      <div class="head-actions">
        <el-tag type="info" effect="plain" round>只读记录</el-tag>
      </div>
    </div>

    <div class="surface filter-surface">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="动作筛选">
          <el-input
            v-model="actionKeyword"
            clearable
            class="action-input"
            placeholder="输入动作编码或对象类型，如 ITEM_APPROVE"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item class="readonly-item">
          <span class="readonly-hint">
            <el-icon><Lock /></el-icon>
            审计日志仅支持查询，不提供修改或删除入口。
          </span>
        </el-form-item>
      </el-form>
    </div>

    <div class="surface table-surface">
      <div class="toolbar table-toolbar">
        <span class="muted small">展开行可查看变更前后的原始值（JSON 格式）。</span>
        <div class="spacer" />
        <span class="muted small">共 {{ total }} 条记录</span>
        <el-button text :loading="loading" @click="loadLogs">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
      </div>

      <el-table v-loading="loading" :data="logs" row-key="id" class="audit-log-table">
        <el-table-column type="expand" width="48">
          <template #default="{ row }">
            <div class="expand-panel">
              <div class="value-block">
                <span class="value-label">变更前</span>
                <pre class="json-block">{{ prettyValue(row.beforeValue) }}</pre>
              </div>
              <div class="value-block">
                <span class="value-label">变更后</span>
                <pre class="json-block">{{ prettyValue(row.afterValue) }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="155">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column label="操作人" width="120">
          <template #default="{ row }">{{ row.operatorName || '系统' }}</template>
        </el-table-column>

        <el-table-column label="动作" min-width="180">
          <template #default="{ row }">
            <div class="action-cell">
              <span class="action-text">{{ row.actionText || row.action }}</span>
              <span v-if="row.actionText && row.action" class="action-code">{{ row.action }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="对象" width="150">
          <template #default="{ row }">
            <span class="target-text">{{ targetText(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="理由" min-width="240">
          <template #default="{ row }">
            <span :title="row.reason || ''">{{ row.reason || '—' }}</span>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState description="当前筛选条件下暂无审计日志" :image-size="90" />
        </template>
      </el-table>

      <AppPagination
        v-model:page="page"
        :size="pageSize"
        :total="total"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminAuditApi } from '@/api/admin'
import type { AuditLogVO } from '@/types'
import { formatDateTime } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'

const actionKeyword = ref('')
const logs = ref<AuditLogVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
let requestSeq = 0

async function loadLogs(): Promise<void> {
  const seq = (requestSeq += 1)
  loading.value = true
  try {
    const result = await adminAuditApi.list({
      action: actionKeyword.value.trim() || undefined,
      page: page.value,
      size: pageSize
    })
    if (seq !== requestSeq) return
    logs.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误由 request 拦截器统一提示，页面保留已有数据 */
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void loadLogs()
}

function handleReset(): void {
  actionKeyword.value = ''
  page.value = 1
  void loadLogs()
}

function handlePageChange(value: number): void {
  page.value = value
  void loadLogs()
}

function targetText(log: AuditLogVO): string {
  const type = log.targetType || '—'
  return log.targetId !== null && log.targetId !== undefined ? `${type} #${log.targetId}` : type
}

function prettyValue(value?: string | null): string {
  if (!value) return '未记录'
  try {
    const parsed: unknown = JSON.parse(value)
    if (typeof parsed === 'string') return parsed
    return JSON.stringify(parsed, null, 2) ?? value
  } catch {
    return value
  }
}

onMounted(() => {
  void loadLogs()
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

.action-input {
  width: 320px;
}

.readonly-item {
  margin-left: auto;
}

.readonly-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--ink-500);
}

.table-toolbar {
  margin-bottom: 14px;
  gap: 8px;
}

.audit-log-table {
  width: 100%;
}

.action-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
}

.action-code {
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, monospace;
  font-size: 11px;
  color: var(--ink-300);
}

.target-text {
  font-size: 12px;
  color: var(--ink-700);
}

.expand-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 4px 18px 12px;
}

.value-block {
  min-width: 0;
}

.value-label {
  display: inline-block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-500);
}

.json-block {
  margin: 0;
  padding: 12px;
  max-height: 260px;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #f7faf8;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-700);
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.audit-log-table .el-table__inner-wrapper::before) {
  display: none;
}

@media (max-width: 768px) {
  .page-head {
    align-items: flex-start;
  }

  .action-input {
    width: 100%;
  }

  .filter-form :deep(.el-form-item) {
    display: block;
    width: 100%;
  }

  .filter-form :deep(.el-form-item__content) {
    width: 100%;
  }

  .readonly-item {
    margin-left: 0;
  }

  .expand-panel {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
