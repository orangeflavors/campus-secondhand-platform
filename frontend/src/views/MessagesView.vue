<template>
  <PageContainer title="我的消息" subtitle="与买家或卖家沟通交易细节 · 平台不会以任何名义要求你提前转账">
    <div class="messages-shell" :class="{ 'has-active': selectedId !== null }">
      <aside class="conversation-pane">
        <div class="pane-head">
          <div class="pane-title">
            <strong>会话</strong>
            <span class="muted small">{{ conversations.length }} 个</span>
          </div>
          <el-button text circle :loading="conversationLoading" title="刷新会话" @click="loadConversations()">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>

        <div v-if="conversationLoading && !conversations.length" class="conversation-loading">
          <el-skeleton animated :rows="5" />
        </div>

        <div v-else-if="conversationError && !conversations.length" class="conversation-empty">
          <EmptyState description="会话加载失败，请稍后重试" action-text="重新加载" :image-size="90" @action="loadConversations()" />
        </div>

        <div v-else-if="!conversations.length" class="conversation-empty">
          <EmptyState description="还没有会话，去逛逛商品吧" action-text="去逛市场" :image-size="90" @action="router.push('/')" />
        </div>

        <div v-else class="conversation-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.id"
            type="button"
            class="conversation-item"
            :class="{ active: conversation.id === selectedId }"
            @click="selectConversation(conversation.id)"
          >
            <el-image
              :src="conversation.itemCoverUrl || '/images/placeholder.svg'"
              fit="cover"
              class="conversation-thumb"
            >
              <template #error>
                <div class="thumb-fallback"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>

            <div class="conversation-main">
              <div class="conversation-top">
                <span class="conversation-name">{{ conversation.otherNickname }}</span>
                <span class="conversation-time">{{ conversationTime(conversation) }}</span>
              </div>
              <div class="conversation-item-title">{{ conversation.itemTitle }}</div>
              <div class="conversation-last">{{ conversation.lastMessage || '开始和对方聊聊这件商品吧' }}</div>
            </div>
          </button>
        </div>
      </aside>

      <section class="chat-pane">
        <template v-if="activeConversation">
          <header class="chat-head">
            <el-button class="mobile-back" text circle title="返回会话列表" @click="backToList">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <UserAvatar
              :nickname="activeConversation.otherNickname"
              :src="activeConversation.otherAvatarUrl"
              :size="40"
            />
            <div class="chat-person">
              <div class="chat-name-row">
                <strong>{{ activeConversation.otherNickname }}</strong>
                <StatusTag kind="auth" :value="activeConversation.otherAuthStatus" />
              </div>
              <div class="chat-item-line">
                正在咨询：
                <router-link :to="`/items/${activeConversation.itemId}`">{{ activeConversation.itemTitle }}</router-link>
                <span class="price-text">{{ formatPrice(activeConversation.itemPrice) }}</span>
              </div>
            </div>
            <el-button text class="view-item-button" @click="viewItem(activeConversation.itemId)">
              查看商品
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </header>

          <div ref="messageListRef" class="message-list">
            <div v-if="messageLoading" class="message-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载消息中…</span>
            </div>
            <div v-else-if="messageError" class="message-empty">
              <EmptyState description="消息加载失败，请重试" action-text="重新加载" :image-size="80" @action="reloadMessages" />
            </div>
            <div v-else-if="!messages.length" class="message-empty">
              <EmptyState description="还没有消息，发条消息打个招呼吧" :image-size="88" />
            </div>
            <template v-else>
              <div v-for="message in messages" :key="message.id" class="message-row" :class="{ mine: message.mine }">
                <UserAvatar
                  v-if="!message.mine"
                  :nickname="message.senderNickname"
                  :src="message.senderAvatarUrl"
                  :size="34"
                />
                <div class="bubble-wrap">
                  <div class="bubble">{{ message.content }}</div>
                  <span class="message-time">{{ formatDateTime(message.createdAt) }}</span>
                </div>
                <UserAvatar
                  v-if="message.mine"
                  :nickname="message.senderNickname"
                  :src="message.senderAvatarUrl"
                  :size="34"
                />
              </div>
            </template>
          </div>

          <footer class="composer">
            <div class="composer-tip">Enter 发送，Shift + Enter 换行</div>
            <el-input
              v-model="draft"
              type="textarea"
              :rows="3"
              resize="none"
              maxlength="1000"
              show-word-limit
              placeholder="输入消息，和对方确认商品细节与交付时间…"
              @keydown="handleComposerKeydown"
            />
            <div class="composer-actions">
              <span class="muted small">请在校内公共区域当面交易，勿提前转账</span>
              <el-button
                type="primary"
                round
                :loading="sending"
                :disabled="!draft.trim()"
                @click="sendMessage"
              >
                <el-icon v-if="!sending"><Promotion /></el-icon>
                发送
              </el-button>
            </div>
          </footer>
        </template>

        <div v-else class="chat-placeholder">
          <EmptyState description="选择左侧会话，开始沟通交易细节" :image-size="110" />
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw, type LocationQueryValue } from 'vue-router'
import type { ConversationVO, MessageVO } from '@/types'
import { conversationApi } from '@/api/social'
import { formatDate, formatDateTime, formatPrice, fromNow } from '@/utils/format'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import StatusTag from '@/components/StatusTag.vue'

const route = useRoute()
const router = useRouter()

const conversations = ref<ConversationVO[]>([])
const conversationLoading = ref(true)
const conversationError = ref(false)

const selectedId = ref<number | null>(null)
const messages = ref<MessageVO[]>([])
const messageLoading = ref(false)
const messageError = ref(false)
const lastMessageId = ref(0)
const draft = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

const activeConversation = computed(
  () => conversations.value.find((conversation) => conversation.id === selectedId.value) ?? null
)

let messageSeq = 0
let pollTimer: number | undefined
let polling = false
let pollPausedUntil = 0

function parseConversationId(value: LocationQueryValue | LocationQueryValue[] | undefined): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  if (!raw || !/^\d+$/.test(raw)) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function conversationTime(conversation: ConversationVO): string {
  return conversation.lastMessageAt ? fromNow(conversation.lastMessageAt) : formatDate(conversation.createdAt)
}

function scrollToBottom(smooth = false): void {
  void nextTick(() => {
    const container = messageListRef.value
    if (!container) return
    container.scrollTo({
      top: container.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto'
    })
  })
}

function updateQuery(conversationId: number): void {
  if (parseConversationId(route.query.conversation) === conversationId) return
  void router.replace({ query: { ...route.query, conversation: String(conversationId) } })
}

function queryWithoutConversation(): LocationQueryRaw {
  const query: LocationQueryRaw = {}
  Object.keys(route.query).forEach((key) => {
    if (key === 'conversation') return
    const value = route.query[key]
    if (value !== undefined) query[key] = value
  })
  return query
}

function clearConversationQuery(): void {
  if (!route.query.conversation) return
  void router.replace({ query: queryWithoutConversation() })
}

async function loadConversations(showLoading = true): Promise<void> {
  if (showLoading) conversationLoading.value = true
  conversationError.value = false
  try {
    const list = await conversationApi.list()
    conversations.value = list
    if (selectedId.value !== null && !list.some((conversation) => conversation.id === selectedId.value)) {
      selectedId.value = null
      messages.value = []
      lastMessageId.value = 0
      messageError.value = false
    }
    if (selectedId.value === null) {
      const requested = parseConversationId(route.query.conversation)
      if (requested !== null && list.some((conversation) => conversation.id === requested)) {
        void selectConversation(requested, false)
      }
    }
  } catch {
    if (showLoading) conversationError.value = true
  } finally {
    if (showLoading) conversationLoading.value = false
  }
}

async function loadMessages(conversationId: number): Promise<void> {
  const seq = ++messageSeq
  messageLoading.value = true
  messageError.value = false
  try {
    const list = await conversationApi.messages(conversationId)
    if (seq !== messageSeq || selectedId.value !== conversationId) return
    const sorted = [...list].sort((left, right) => left.id - right.id)
    messages.value = sorted
    lastMessageId.value = sorted.length ? sorted[sorted.length - 1].id : 0
    scrollToBottom()
  } catch {
    if (seq === messageSeq && selectedId.value === conversationId) {
      messages.value = []
      lastMessageId.value = 0
      messageError.value = true
    }
  } finally {
    if (seq === messageSeq) messageLoading.value = false
  }
}

async function selectConversation(conversationId: number, syncQuery = true): Promise<void> {
  if (selectedId.value === conversationId) return
  selectedId.value = conversationId
  messages.value = []
  lastMessageId.value = 0
  messageError.value = false
  pollPausedUntil = 0
  draft.value = ''
  if (syncQuery) updateQuery(conversationId)
  await loadMessages(conversationId)
}

function reloadMessages(): void {
  if (selectedId.value === null) return
  void loadMessages(selectedId.value)
}

function backToList(): void {
  messageSeq += 1
  selectedId.value = null
  messages.value = []
  lastMessageId.value = 0
  messageError.value = false
  draft.value = ''
  clearConversationQuery()
}

function viewItem(itemId: number): void {
  void router.push(`/items/${itemId}`)
}

function updateConversationPreview(conversationId: number, latest: MessageVO): void {
  const index = conversations.value.findIndex((conversation) => conversation.id === conversationId)
  if (index < 0) return
  const current = conversations.value[index]
  if (!current) return
  const updated: ConversationVO = {
    ...current,
    lastMessage: latest.content,
    lastMessageAt: latest.createdAt
  }
  conversations.value = [updated, ...conversations.value.filter((conversation) => conversation.id !== conversationId)]
}

async function pollNewMessages(): Promise<void> {
  if (polling || selectedId.value === null || document.hidden || Date.now() < pollPausedUntil) return
  const conversationId = selectedId.value
  polling = true
  try {
    const incoming = await conversationApi.messages(conversationId, lastMessageId.value || undefined)
    if (selectedId.value !== conversationId) return

    const existingIds = new Set(messages.value.map((message) => message.id))
    const additions = incoming
      .filter((message) => !existingIds.has(message.id))
      .sort((left, right) => left.id - right.id)
    if (!additions.length) return

    messages.value = [...messages.value, ...additions]
    const latest = additions[additions.length - 1]
    if (latest) {
      lastMessageId.value = Math.max(lastMessageId.value, latest.id)
      updateConversationPreview(conversationId, latest)
    }

    const container = messageListRef.value
    const nearBottom = !container || container.scrollHeight - container.scrollTop - container.clientHeight < 160
    if (nearBottom) scrollToBottom()
  } catch {
    /* 轮询失败不打断当前会话，短暂退避后继续 */
    pollPausedUntil = Date.now() + 30000
  } finally {
    polling = false
  }
}

async function sendMessage(): Promise<void> {
  const conversationId = selectedId.value
  const content = draft.value.trim()
  if (conversationId === null || !content || sending.value) return

  sending.value = true
  try {
    const message = await conversationApi.send(conversationId, content)
    if (selectedId.value === conversationId) {
      if (!messages.value.some((item) => item.id === message.id)) {
        messages.value = [...messages.value, message]
      }
      lastMessageId.value = Math.max(lastMessageId.value, message.id)
      draft.value = ''
      updateConversationPreview(conversationId, message)
      scrollToBottom(true)
    }
  } catch {
    /* 拦截器已统一提示，保留输入内容 */
  } finally {
    sending.value = false
  }
}

function handleComposerKeydown(event: KeyboardEvent | Event): void {
  if (!(event instanceof KeyboardEvent)) return
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return
  event.preventDefault()
  void sendMessage()
}

watch(
  () => route.query.conversation,
  (value) => {
    const conversationId = parseConversationId(value)
    if (conversationId === null) {
      if (selectedId.value !== null) {
        messageSeq += 1
        selectedId.value = null
        messages.value = []
        lastMessageId.value = 0
        messageError.value = false
      }
      return
    }
    if (conversationId === selectedId.value) return
    if (conversationLoading.value) return
    if (!conversations.value.some((conversation) => conversation.id === conversationId)) {
      clearConversationQuery()
      return
    }
    void selectConversation(conversationId, false)
  }
)

onMounted(async () => {
  await loadConversations()
  const requested = parseConversationId(route.query.conversation)
  if (requested !== null && !conversationError.value) {
    if (conversations.value.some((conversation) => conversation.id === requested)) {
      await selectConversation(requested, false)
    } else {
      clearConversationQuery()
    }
  }
  pollTimer = window.setInterval(() => {
    void pollNewMessages()
  }, 5000)
})

onUnmounted(() => {
  messageSeq += 1
  if (pollTimer !== undefined) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
})
</script>

<style scoped>
.messages-shell {
  display: grid;
  grid-template-columns: 328px minmax(0, 1fr);
  height: calc(100vh - 230px);
  min-height: 520px;
  max-height: 780px;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
}

.conversation-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-right: 1px solid var(--line);
  background: #fbfdfc;
}

.pane-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px 12px 18px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}

.pane-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.pane-title strong {
  font-size: 15px;
  color: var(--ink-900);
}

.conversation-loading {
  padding: 18px;
}

.conversation-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  gap: 11px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-bottom: 1px solid var(--line);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease;
}

.conversation-item:hover {
  background: #f3faf6;
}

.conversation-item.active {
  background: var(--brand-50);
  box-shadow: inset 3px 0 0 var(--brand-500);
}

.conversation-thumb {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: var(--brand-100);
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-300);
  background: var(--brand-50);
}

.conversation-main {
  flex: 1;
  min-width: 0;
}

.conversation-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conversation-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.conversation-time {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--ink-300);
}

.conversation-item-title {
  margin-top: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 11px;
  color: var(--brand-600);
}

.conversation-last {
  margin-top: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--ink-500);
}

.chat-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.chat-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--line);
  background: #fff;
}

.mobile-back {
  display: none;
}

.chat-person {
  flex: 1;
  min-width: 0;
}

.chat-name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
}

.chat-name-row strong {
  font-size: 15px;
  color: var(--ink-900);
}

.chat-item-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-500);
}

.chat-item-line a {
  max-width: 360px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.price-text {
  color: #d9480f;
  font-weight: 700;
  font-size: 13px;
}

.view-item-button {
  flex-shrink: 0;
  color: var(--brand-600);
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 20px;
  background:
    radial-gradient(circle at 8% 10%, rgba(58, 156, 108, 0.05), transparent 30%),
    #fafcfb;
}

.message-loading,
.message-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--ink-500);
  font-size: 13px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.message-row.mine {
  flex-direction: row-reverse;
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 72%;
}

.message-row.mine .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: 4px 14px 14px 14px;
  background: #fff;
  color: var(--ink-900);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 2px 6px rgba(31, 107, 74, 0.03);
}

.message-row.mine .bubble {
  border-color: var(--brand-600);
  border-radius: 14px 4px 14px 14px;
  background: var(--brand-600);
  color: #fff;
}

.message-time {
  font-size: 10px;
  color: var(--ink-300);
}

.composer {
  padding: 12px 18px 14px;
  border-top: 1px solid var(--line);
  background: #fff;
}

.composer-tip {
  margin-bottom: 6px;
  text-align: right;
  font-size: 10px;
  color: var(--ink-300);
}

.composer-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 960px) {
  .messages-shell {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .chat-item-line a {
    max-width: 200px;
  }
}

@media (max-width: 768px) {
  .messages-shell {
    grid-template-columns: 1fr;
    height: calc(100vh - 205px);
    min-height: 480px;
  }

  .conversation-pane {
    border-right: none;
  }

  .chat-pane {
    display: none;
  }

  .messages-shell.has-active .conversation-pane {
    display: none;
  }

  .messages-shell.has-active .chat-pane {
    display: flex;
  }

  .mobile-back {
    display: inline-flex;
  }

  .view-item-button {
    display: none;
  }

  .chat-head {
    padding: 10px 12px;
  }

  .message-list {
    padding: 14px 12px;
  }

  .bubble-wrap {
    max-width: 82%;
  }

  .composer {
    padding: 10px 12px 12px;
  }

  .composer-actions .muted {
    display: none;
  }

  .composer-actions .el-button {
    width: 100%;
  }
}
</style>
