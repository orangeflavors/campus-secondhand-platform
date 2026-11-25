<template>
  <div class="page-container user-layout">
    <aside class="side">
      <div class="user-card">
        <UserAvatar :nickname="store.user?.nickname" :src="store.user?.avatarUrl" :size="52" />
        <div class="user-meta">
          <strong>{{ store.user?.nickname }}</strong>
          <div class="tags">
            <StatusTag kind="auth" :value="store.user?.authStatus || 'UNVERIFIED'" />
            <StatusTag
              v-if="store.user && store.user.accountStatus !== 'NORMAL'"
              kind="account"
              :value="store.user.accountStatus"
            />
          </div>
        </div>
      </div>

      <el-menu :default-active="activePath" router class="side-menu">
        <el-menu-item index="/me/profile">
          <el-icon><User /></el-icon>
          <span>个人资料</span>
        </el-menu-item>
        <el-menu-item index="/verify">
          <el-icon><Stamp /></el-icon>
          <span>校园认证</span>
        </el-menu-item>
        <el-menu-item index="/me/items">
          <el-icon><Goods /></el-icon>
          <span>我的商品</span>
        </el-menu-item>
        <el-menu-item index="/me/favorites">
          <el-icon><Star /></el-icon>
          <span>我的收藏</span>
        </el-menu-item>
        <el-menu-item index="/me/orders">
          <el-icon><List /></el-icon>
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="/me/reports">
          <el-icon><Flag /></el-icon>
          <span>我的举报</span>
        </el-menu-item>
        <el-menu-item index="/me/notifications">
          <el-icon><Bell /></el-icon>
          <span>通知中心</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="content">
      <router-view />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import UserAvatar from '@/components/UserAvatar.vue'
import StatusTag from '@/components/StatusTag.vue'

const store = useUserStore()
const route = useRoute()

const activePath = computed(() => {
  if (route.path.startsWith('/me/orders')) return '/me/orders'
  return route.path
})
</script>

<style scoped>
.user-layout {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.side {
  position: sticky;
  top: 88px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  background: linear-gradient(135deg, var(--brand-50), #fff);
  border-bottom: 1px solid var(--line);
}

.user-meta {
  min-width: 0;
}

.user-meta strong {
  display: block;
  font-size: 15px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tags {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.side-menu {
  border-right: none;
  padding: 6px;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 8px;
  height: 42px;
  line-height: 42px;
  margin-bottom: 2px;
}

.content {
  min-width: 0;
}

@media (max-width: 860px) {
  .user-layout {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
  }

  .side-menu {
    display: flex;
    overflow-x: auto;
    padding: 4px;
  }

  .side-menu :deep(.el-menu-item) {
    flex-shrink: 0;
    height: 38px;
    line-height: 38px;
  }

  .user-card {
    display: none;
  }
}
</style>
