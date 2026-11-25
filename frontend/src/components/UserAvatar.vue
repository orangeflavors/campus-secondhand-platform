<template>
  <el-avatar :size="size" :src="src || undefined" :style="{ background: bg }" class="user-avatar">
    {{ initial }}
  </el-avatar>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    nickname?: string | null
    src?: string | null
    size?: number
  }>(),
  { nickname: '同学', size: 36 }
)

const initial = computed(() => (props.nickname || '同学').slice(0, 1))

const palette = ['#2f8259', '#3b82f6', '#8b5cf6', '#f59e0b', '#ef4444', '#0ea5e9', '#14b8a6']

const bg = computed(() => {
  const name = props.nickname || '同学'
  let hash = 0
  for (let i = 0; i < name.length; i += 1) {
    hash = (hash * 31 + name.charCodeAt(i)) % 997
  }
  return palette[hash % palette.length]
})
</script>
