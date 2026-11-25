<template>
  <router-link :to="`/items/${item.id}`" class="item-card" :class="{ 'is-inactive': item.status !== 'ON_SALE' }">
    <div class="cover">
      <el-image :src="item.coverUrl || '/images/placeholder.svg'" fit="cover" class="cover-img">
        <template #error>
          <div class="cover-fallback"><el-icon><Picture /></el-icon></div>
        </template>
      </el-image>
      <StatusTag v-if="item.status !== 'ON_SALE'" kind="item" :value="item.status" effect="dark" class="status" />
      <span class="condition">{{ item.conditionText }}</span>
    </div>
    <div class="body">
      <h3 class="title" :title="item.title">{{ item.title }}</h3>
      <div class="meta">
        <span>{{ item.categoryName }}</span>
        <span v-if="item.handoverArea">· {{ item.handoverArea }}</span>
      </div>
      <div class="footer">
        <span class="price" :class="{ 'is-free': item.price === 0 }">{{ priceText }}</span>
        <span class="seller">
          <el-icon><User /></el-icon>
          {{ item.sellerNickname }}
          <el-tooltip v-if="item.sellerAuthStatus === 'VERIFIED'" content="已完成校园认证" placement="top">
            <el-icon class="verified"><CircleCheckFilled /></el-icon>
          </el-tooltip>
        </span>
      </div>
      <div class="sub">
        <span>{{ item.viewCount }} 浏览</span>
        <span>{{ item.favoriteCount }} 收藏</span>
        <span>{{ formatDate(item.listedAt || item.createdAt) }}</span>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ItemCardVO } from '@/types'
import { formatDate } from '@/utils/format'
import StatusTag from './StatusTag.vue'

const props = defineProps<{ item: ItemCardVO }>()

const priceText = computed(() =>
  props.item.price === 0 ? '免费' : Number(props.item.price).toFixed(2)
)
</script>

<style scoped>
.item-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  color: inherit;
}

.item-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(31, 107, 74, 0.12);
  border-color: #cbe2d6;
}

.item-card.is-inactive {
  opacity: 0.82;
}

.cover {
  position: relative;
  aspect-ratio: 4 / 3;
  background: var(--brand-50);
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-300);
  font-size: 36px;
}

.status {
  position: absolute;
  top: 10px;
  left: 10px;
}

.condition {
  position: absolute;
  right: 10px;
  bottom: 10px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--brand-700);
  backdrop-filter: blur(2px);
}

.body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.title {
  margin: 0;
  font-size: 15px;
  line-height: 1.4;
  font-weight: 600;
  color: var(--ink-900);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.meta {
  font-size: 12px;
  color: var(--ink-500);
  display: flex;
  gap: 4px;
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.price {
  color: #d9480f;
  font-size: 19px;
  font-weight: 700;
}

.price.is-free {
  color: var(--brand-600);
  font-size: 17px;
}

.seller {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--ink-500);
  max-width: 55%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.verified {
  color: var(--brand-500);
}

.sub {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--ink-300);
}
</style>
