<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <router-link to="/admin/dashboard" class="admin-brand">
        <span class="logo">管</span>
        <span>
          <strong>校园集市</strong>
          <small>管理后台</small>
        </span>
      </router-link>

      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>运营统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/items">
          <el-icon><Checked /></el-icon>
          <span>商品审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/reports">
          <el-icon><Flag /></el-icon>
          <span>举报处理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Warning /></el-icon>
          <span>争议订单</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/audit-logs">
          <el-icon><Document /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
      </el-menu>

      <div class="admin-side-footer">
        <el-button text @click="router.push('/')">
          <el-icon><Back /></el-icon>
          返回市场
        </el-button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div class="crumb">
          <span class="muted">管理后台</span>
          <span class="sep">/</span>
          <strong>{{ route.meta.title }}</strong>
        </div>
        <div class="admin-user">
          <span class="admin-name">{{ store.user?.nickname }}</span>
          <el-tag size="small" type="danger" effect="plain" round>管理员</el-tag>
          <el-button text @click="logout">退出</el-button>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

function logout() {
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  background: #f3f6f4;
}

.admin-side {
  width: 216px;
  flex-shrink: 0;
  background: #17392c;
  color: #fff;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  color: #fff;
}

.admin-brand .logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--brand-500);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.admin-brand strong {
  display: block;
  font-size: 15px;
  letter-spacing: 1px;
}

.admin-brand small {
  font-size: 11px;
  opacity: 0.65;
}

.admin-menu {
  border-right: none;
  background: transparent;
  padding: 4px 8px;
  flex: 1;
}

.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  margin-bottom: 2px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: var(--brand-500);
  color: #fff;
}

.admin-side-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.admin-side-footer :deep(.el-button) {
  color: rgba(255, 255, 255, 0.8);
}

.admin-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.crumb .sep {
  color: var(--ink-300);
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.admin-content {
  padding: 22px 24px 48px;
}

@media (max-width: 860px) {
  .admin-side {
    width: 64px;
  }

  .admin-brand span:not(.logo),
  .admin-menu span,
  .admin-side-footer {
    display: none;
  }

  .admin-menu :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 !important;
  }
}
</style>
