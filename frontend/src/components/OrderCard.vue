<template>
  <div class="order-card">
    <div class="order-head">
      <span class="order-no">订单号 {{ order.orderNo }}</span>
      <StatusTag kind="order" :value="order.status" />
    </div>
    <router-link :to="`/me/orders/${order.id}`" class="order-body">
      <el-image :src="order.item.coverUrl || '/images/placeholder.svg'" fit="cover" class="thumb">
        <template #error>
          <div class="thumb-fallback"><el-icon><Picture /></el-icon></div>
        </template>
      </el-image>
      <div class="info">
        <h4 :title="order.item.title">{{ order.item.title }}</h4>
        <p class="muted small">{{ order.statusText }}</p>
        <p class="muted small">
          {{ order.myRole === 'BUYER' ? '卖家' : '买家' }}：{{ otherName }} ·
          {{ formatDateTime(order.createdAt) }}
        </p>
      </div>
      <div class="amount">
        <span class="price" :class="{ 'is-free': order.item.price === 0 }">
          {{ order.item.price === 0 ? '免费' : Number(order.item.price).toFixed(2) }}
        </span>
        <span class="role">{{ order.myRole === 'BUYER' ? '我买到的' : '我卖出的' }}</span>
      </div>
    </router-link>
    <div v-if="$slots.actions" class="order-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OrderVO } from '@/types'
import { formatDateTime } from '@/utils/format'
import StatusTag from './StatusTag.vue'

const props = defineProps<{ order: OrderVO }>()

const otherName = computed(() =>
  props.order.myRole === 'BUYER' ? props.order.seller.nickname : props.order.buyer.nickname
)
</script>

<style scoped>
.order-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 16px 18px;
  transition: box-shadow 0.18s ease;
}

.order-card:hover {
  box-shadow: 0 8px 22px rgba(31, 107, 74, 0.08);
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line);
}

.order-no {
  font-size: 12px;
  color: var(--ink-500);
  letter-spacing: 0.3px;
}

.order-body {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 14px 0 4px;
  color: inherit;
}

.thumb {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--brand-50);
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-300);
}

.info {
  flex: 1;
  min-width: 0;
}

.info h4 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.info p {
  margin: 4px 0;
}

.amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.amount .price {
  font-size: 17px;
  color: #d9480f;
  font-weight: 700;
}

.amount .price.is-free {
  color: var(--brand-600);
}

.role {
  font-size: 11px;
  color: var(--ink-500);
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
  margin-top: 8px;
}
</style>
