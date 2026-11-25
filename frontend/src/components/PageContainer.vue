<template>
  <div class="page-container">
    <div class="page-head">
      <div class="head-text">
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      </div>
      <div class="head-actions">
        <slot name="actions" />
      </div>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>()
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
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
  gap: 8px;
  flex-wrap: wrap;
}
</style>
