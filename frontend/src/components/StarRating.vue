<template>
  <span class="star-rating">
    <el-rate :model-value="value" disabled allow-half size="small" />
    <span v-if="showText" class="score">{{ value.toFixed(1) }}</span>
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ value: number; showText?: boolean }>(), { showText: false })
</script>

<style scoped>
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  vertical-align: middle;
}

.score {
  font-size: 13px;
  color: var(--ink-500);
}

:deep(.el-rate) {
  height: 18px;
}
</style>
