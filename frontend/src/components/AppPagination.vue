<template>
  <div v-if="total > 0" class="pagination-row">
    <el-pagination
      background
      :current-page="page"
      :page-size="size"
      :total="total"
      :pager-count="7"
      layout="prev, pager, next, total"
      @current-change="onChange"
    />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ page: number; size: number; total: number }>()
const emit = defineEmits<{ 'update:page': [value: number]; change: [value: number] }>()

function onChange(value: number) {
  if (value === props.page) return
  emit('update:page', value)
  emit('change', value)
}
</script>
