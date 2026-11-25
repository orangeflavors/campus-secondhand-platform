<template>
  <div class="app-shell">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <span class="logo">集</span>
          <span class="brand-text">
            <strong>校园集市</strong>
            <small>可信 · 校内 · 闲置流转</small>
          </span>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-link" :class="{ active: route.name === 'market' }">市场</router-link>
          <router-link to="/messages" class="nav-link" :class="{ active: route.name === 'messages' }">消息</router-link>
          <router-link to="/me/orders" class="nav-link" :class="{ active: String(route.name).includes('order') }">订单</router-link>
        </nav>

        <div class="header-search">
          <el-input
            v-model="keyword"
            placeholder="搜索教材、数码、生活用品…"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="header-actions">
          <el-button type="primary" round class="publish-btn" @click="goPublish">
            <el-icon><Plus /></el-icon>
            <span>发布闲置</span>
          </el-button>

          <el-tooltip content="通知中心" placement="bottom">
            <el-badge :value="unread" :hidden="unread === 0" :max="99" class="bell">
              <el-button circle text @click="goNotifications">
                <el-icon :size="19"><Bell /></el-icon>
              </el-button>
            </el-badge>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-entry">
              <UserAvatar :nickname="store.user?.nickname" :src="store.user?.avatarUrl" :size="34" />
              <span class="nickname">{{ store.user?.nickname || '同学' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="items" :icon="Goods">我的商品</el-dropdown-item>
                <el-dropdown-item command="favorites" :icon="Star">我的收藏</el-dropdown-item>
                <el-dropdown-item command="orders" :icon="List">我的订单</el-dropdown-item>
                <el-dropdown-item command="notifications" :icon="Bell">通知中心</el-dropdown-item>
                <el-dropdown-item v-if="store.isAdmin" command="admin" :icon="Setting" divided>
                  管理后台
                </el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div v-if="store.user && !store.isVerified" class="verify-banner">
      <div class="banner-inner">
        <el-icon><WarningFilled /></el-icon>
        <span>你尚未完成校园身份认证，暂不能浏览商品与参与交易。</span>
        <el-button size="small" type="primary" round @click="router.push('/verify')">立即认证</el-button>
      </div>
    </div>

    <div v-else-if="store.user && store.accountStatus !== 'NORMAL'" class="status-banner">
      <div class="banner-inner">
        <el-icon><CircleCloseFilled /></el-icon>
        <span>
          你的账户当前为「{{ store.accountStatus === 'BANNED' ? '封禁' : '限权' }}」状态：
          {{ store.user.statusReason || '部分交易操作已被限制' }}
        </span>
        <span v-if="store.user.limitedUntil" class="muted small">（至 {{ formatDateTime(store.user.limitedUntil) }}）</span>
      </div>
    </div>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="footer-inner">
        <span>校园集市 · 课程教学模拟项目</span>
        <span class="divider">|</span>
        <span>平台不提供在线支付与物流，请在校内线下完成验货与交付</span>
        <span class="divider">|</span>
        <span>遇到可疑交易请及时举报</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Goods, List, Setting, Star, SwitchButton, User } from '@element-plus/icons-vue'
import { notificationApi } from '@/api/social'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'
import UserAvatar from '@/components/UserAvatar.vue'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const keyword = ref('')
const unread = ref(0)
let timer: number | undefined

async function loadUnread() {
  if (!store.isLoggedIn) return
  try {
    const res = await notificationApi.unreadCount()
    unread.value = res.count
  } catch {
    /* 通知数量失败时静默处理 */
  }
}

function doSearch() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

function goPublish() {
  router.push('/publish')
}

function goNotifications() {
  router.push('/me/notifications')
}

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/me/profile')
      break
    case 'items':
      router.push('/me/items')
      break
    case 'favorites':
      router.push('/me/favorites')
      break
    case 'orders':
      router.push('/me/orders')
      break
    case 'notifications':
      router.push('/me/notifications')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      store.logout()
      router.push('/login')
      break
  }
}

onMounted(() => {
  loadUnread()
  timer = window.setInterval(loadUnread, 60000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(8px);
}

.header-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 20px;
  height: 66px;
  display: flex;
  align-items: center;
  gap: 22px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: inherit;
  flex-shrink: 0;
}

.logo {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, var(--brand-500), var(--brand-700));
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 14px rgba(47, 130, 89, 0.28);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-text strong {
  font-size: 17px;
  letter-spacing: 1px;
  color: var(--ink-900);
}

.brand-text small {
  font-size: 10px;
  color: var(--ink-500);
  letter-spacing: 0.5px;
}

.nav {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.nav-link {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--ink-700);
}

.nav-link:hover {
  background: var(--brand-50);
  color: var(--brand-700);
}

.nav-link.active {
  background: var(--brand-100);
  color: var(--brand-700);
  font-weight: 600;
}

.header-search {
  flex: 1;
  max-width: 360px;
  margin-left: auto;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 20px;
  transition: background 0.15s ease;
}

.user-entry:hover {
  background: var(--brand-50);
}

.nickname {
  font-size: 13px;
  color: var(--ink-700);
  max-width: 80px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.verify-banner,
.status-banner {
  background: #fff8e6;
  border-bottom: 1px solid #f5e3b3;
}

.status-banner {
  background: #fdecec;
  border-bottom-color: #f5c2c2;
}

.banner-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 9px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #7a5a13;
}

.status-banner .banner-inner {
  color: #a33a3a;
}

.main {
  flex: 1;
}

.footer {
  border-top: 1px solid var(--line);
  background: #fff;
}

.footer-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 18px 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 12px;
  color: var(--ink-500);
}

.divider {
  color: var(--ink-300);
}

@media (max-width: 960px) {
  .nav,
  .header-search {
    display: none;
  }

  .header-inner {
    gap: 12px;
  }
}

@media (max-width: 640px) {
  .brand-text small,
  .nickname {
    display: none;
  }

  .publish-btn span {
    display: none;
  }
}
</style>
