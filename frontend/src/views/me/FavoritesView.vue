<template>
  <div class="me-page favorites-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">我的收藏</h1>
        <p class="page-subtitle">收藏感兴趣的闲置，方便随时回来查看状态与价格。</p>
      </div>
      <span v-if="total > 0" class="count-pill">共 {{ total }} 件</span>
    </header>

    <div v-loading="loading" class="favorites-panel">
      <div v-if="items.length" class="grid-cards favorites-grid">
        <div v-for="item in items" :key="item.id" class="favorite-cell">
          <ItemCard :item="item" />
          <el-button
            class="remove-btn"
            size="small"
            round
            :loading="removingId === item.id"
            @click="removeFavorite(item)"
          >
            <el-icon v-if="removingId !== item.id"><StarFilled /></el-icon>
            取消收藏
          </el-button>
        </div>
      </div>

      <EmptyState
        v-else-if="!loading"
        description="还没有收藏任何商品，去市场逛逛吧"
        action-text="去市场逛逛"
        :image-size="110"
        @action="goMarket"
      />
    </div>

    <AppPagination v-model:page="page" :size="pageSize" :total="total" @change="loadFavorites" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/item'
import { favoriteApi } from '@/api/social'
import type { ItemCardVO } from '@/types'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import ItemCard from '@/components/ItemCard.vue'

const router = useRouter()

const items = ref<ItemCardVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const loading = ref(false)
const removingId = ref<number | null>(null)

async function loadFavorites(): Promise<void> {
  loading.value = true
  try {
    const result = await itemApi.favorites({ page: page.value, size: pageSize })
    items.value = result.records
    total.value = result.total
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留当前列表 */
  } finally {
    loading.value = false
  }
}

async function removeFavorite(item: ItemCardVO): Promise<void> {
  removingId.value = item.id
  try {
    await favoriteApi.remove(item.id)
    ElMessage.success('已取消收藏')
    if (items.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadFavorites()
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    removingId.value = null
  }
}

function goMarket(): void {
  router.push('/')
}

onMounted(loadFavorites)
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

.count-pill {
  flex-shrink: 0;
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--brand-100);
  font-size: 12px;
  color: var(--brand-700);
}

.favorites-panel {
  min-height: 240px;
}

.favorite-cell {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.favorite-cell :deep(.item-card) {
  flex: 1;
}

.remove-btn {
  align-self: center;
  color: var(--ink-500);
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .favorite-cell {
    gap: 8px;
  }

  .remove-btn {
    width: 100%;
  }
}
</style>
