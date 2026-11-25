<template>
  <div class="me-page notifications-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">通知中心</h1>
        <p class="page-subtitle">商品审核、订单进展、举报处理与账户状态变更都会在这里提醒你。</p>
      </div>
      <div class="head-actions">
        <span v-if="unreadCount > 0" class="unread-pill">
          {{ unreadCount > 99 ? '99+' : unreadCount }} 条未读
        </span>
        <el-button
          type="primary"
          plain
          :disabled="!canMarkAll"
          :loading="markingAll"
          @click="markAllRead"
        >
          <el-icon v-if="!markingAll"><Check /></el-icon>
          全部已读
        </el-button>
      </div>
    </header>

    <div class="surface filter-bar">
      <div class="switch-group">
        <el-switch v-model="unreadOnly" />
        <span class="switch-label">只看未读</span>
      </div>
      <span class="muted small">共 {{ total }} 条通知</span>
      <div class="spacer"></div>
      <el-button text :loading="loading" @click="loadNotifications">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div v-loading="loading" class="notices-panel">
      <div v-if="notices.length" class="notice-list">
        <article
          v-for="notice in notices"
          :key="notice.id"
          class="notice-item"
          :class="{ 'is-unread': !notice.read }"
          role="button"
          tabindex="0"
          @click="openNotice(notice)"
          @keyup.enter="openNotice(notice)"
        >
          <span class="unread-dot" :class="{ visible: !notice.read }"></span>

          <div class="notice-icon">
            <el-icon :size="20">
              <Goods v-if="notice.type === 'ITEM'" />
              <List v-else-if="notice.type === 'ORDER'" />
              <Flag v-else-if="notice.type === 'REPORT'" />
              <Star v-else-if="notice.type === 'REVIEW'" />
              <User v-else-if="notice.type === 'ACCOUNT'" />
              <Bell v-else />
            </el-icon>
          </div>

          <div class="notice-main">
            <div class="notice-top">
              <el-tag :type="notificationTypeTag[notice.type]" size="small" effect="light" round>
                {{ notice.typeText || notificationTypeLabels[notice.type] }}
              </el-tag>
              <h3 class="notice-title">{{ notice.title }}</h3>
              <span v-if="!notice.read" class="unread-label">未读</span>
            </div>
            <p class="notice-content">{{ notice.content }}</p>
            <div class="notice-meta">
              <span>{{ formatDateTime(notice.createdAt) }}</span>
              <span class="dot">·</span>
              <span>{{ fromNow(notice.createdAt) }}</span>
              <span v-if="targetOf(notice)" class="jump-hint">
                查看详情
                <el-icon><ArrowRight /></el-icon>
              </span>
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

    <AppPagination v-model:page="page" :size="pageSize" :total="total" @change="loadNotifications" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api/social'
import { notificationTypeLabels, type TagType } from '@/utils/dict'
import { formatDateTime, fromNow } from '@/utils/format'
import type { NotificationType, NotificationVO } from '@/types'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const notices = ref<NotificationVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const unreadOnly = ref(false)
const unreadCount = ref(0)
const markingAll = ref(false)

const notificationTypeTag: Record<NotificationType, TagType> = {
  ITEM: 'success',
  ORDER: 'primary',
  REPORT: 'danger',
  REVIEW: 'warning',
  ACCOUNT: 'danger',
  SYSTEM: 'info'
}

const canMarkAll = computed(
  () => unreadCount.value > 0 || notices.value.some((notice) => !notice.read)
)

const emptyDescription = computed(() =>
  unreadOnly.value ? '没有未读通知，所有消息都已处理完毕' : '暂无通知，有新消息时会在这里提醒你'
)

const emptyActionText = computed(() => (unreadOnly.value ? '查看全部通知' : '刷新'))

async function loadNotifications(): Promise<void> {
  loading.value = true
  try {
    const result = await notificationApi.list({
      unread: unreadOnly.value || undefined,
      page: page.value,
      size: pageSize
    })
    notices.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留当前列表 */
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount(): Promise<void> {
  try {
    const result = await notificationApi.unreadCount()
    unreadCount.value = result.count
  } catch {
    /* 未读数失败时静默处理，不打断页面使用 */
  }
}

function targetOf(notice: NotificationVO): string | null {
  switch (notice.relatedType) {
    case 'ORDER':
      return notice.relatedId ? `/me/orders/${notice.relatedId}` : null
    case 'ITEM':
      return notice.relatedId ? `/items/${notice.relatedId}` : null
    case 'REPORT':
      return '/me/reports'
    case 'CONVERSATION':
      return notice.relatedId ? `/messages?conversation=${notice.relatedId}` : null
    default:
      return null
  }
}

async function openNotice(notice: NotificationVO): Promise<void> {
  if (!notice.read) {
    try {
      await notificationApi.read(notice.id)
      notice.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      /* 接口错误已由请求拦截器统一提示，仍继续尝试跳转 */
    }
  }

  const target = targetOf(notice)
  if (target) {
    await router.push(target)
  } else if (unreadOnly.value) {
    if (notices.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadNotifications()
  }
}

async function markAllRead(): Promise<void> {
  markingAll.value = true
  try {
    await notificationApi.readAll()
    ElMessage.success('已将全部通知标记为已读')
    unreadCount.value = 0
    notices.value.forEach((notice) => {
      notice.read = true
    })
    page.value = 1
    await loadNotifications()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    markingAll.value = false
  }
}

function handleEmptyAction(): void {
  if (unreadOnly.value) {
    unreadOnly.value = false
  } else {
    loadNotifications()
  }
}

watch(unreadOnly, () => {
  page.value = 1
  loadNotifications()
})

onMounted(() => {
  loadNotifications()
  loadUnreadCount()
})
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
  gap: 10px;
  flex-wrap: wrap;
}

.unread-pill {
  padding: 5px 12px;
  border-radius: 999px;
  background: #fff2e0;
  font-size: 12px;
  color: #b36b00;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.switch-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-label {
  font-size: 13px;
  color: var(--ink-700);
}

.notices-panel {
  min-height: 240px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 16px 18px 16px 22px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.notice-item:hover,
.notice-item:focus-visible {
  border-color: #cbe2d6;
  box-shadow: 0 8px 22px rgba(31, 107, 74, 0.08);
  outline: none;
}

.notice-item.is-unread {
  background: linear-gradient(90deg, var(--brand-50), #fff 55%);
  border-color: #d5e9de;
}

.unread-dot {
  position: absolute;
  top: 24px;
  left: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: transparent;
}

.unread-dot.visible {
  background: var(--brand-500);
}

.notice-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 12px;
  background: var(--brand-100);
  color: var(--brand-700);
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.notice-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
  word-break: break-word;
}

.is-unread .notice-title {
  color: var(--brand-700);
}

.unread-label {
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--brand-100);
  font-size: 11px;
  color: var(--brand-700);
}

.notice-content {
  margin: 8px 0 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  word-break: break-word;
  white-space: pre-wrap;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--ink-300);
}

.dot {
  color: var(--ink-300);
}

.jump-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  color: var(--brand-600);
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
    flex-wrap: wrap;
    padding: 12px;
  }

  .notices-panel {
    min-height: 180px;
  }

  .notice-item {
    padding: 14px 14px 14px 20px;
  }

  .notice-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .jump-hint {
    margin-left: 0;
  }
}
</style>
