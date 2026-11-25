<template>
  <el-tag :type="tagType" :effect="effect" size="small" round class="status-tag">{{ text }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  accountStatusLabels,
  accountStatusTag,
  authStatusLabels,
  authStatusTag,
  itemStatusLabels,
  itemStatusTag,
  orderStatusLabels,
  orderStatusTag,
  reportStatusLabels,
  reportStatusTag,
  type TagType
} from '@/utils/dict'

const props = withDefaults(
  defineProps<{
    kind: 'item' | 'order' | 'report' | 'account' | 'auth'
    value: string
    effect?: 'light' | 'dark' | 'plain'
    text?: string
  }>(),
  { effect: 'light' }
)

const text = computed(() => {
  if (props.text) return props.text
  switch (props.kind) {
    case 'item':
      return itemStatusLabels[props.value as keyof typeof itemStatusLabels] || props.value
    case 'order':
      return orderStatusLabels[props.value as keyof typeof orderStatusLabels] || props.value
    case 'report':
      return reportStatusLabels[props.value as keyof typeof reportStatusLabels] || props.value
    case 'account':
      return accountStatusLabels[props.value as keyof typeof accountStatusLabels] || props.value
    case 'auth':
      return authStatusLabels[props.value as keyof typeof authStatusLabels] || props.value
    default:
      return props.value
  }
})

const tagType = computed<TagType>(() => {
  switch (props.kind) {
    case 'item':
      return itemStatusTag[props.value as keyof typeof itemStatusTag] || 'info'
    case 'order':
      return orderStatusTag[props.value as keyof typeof orderStatusTag] || 'info'
    case 'report':
      return reportStatusTag[props.value as keyof typeof reportStatusTag] || 'info'
    case 'account':
      return accountStatusTag[props.value as keyof typeof accountStatusTag] || 'info'
    case 'auth':
      return authStatusTag[props.value as keyof typeof authStatusTag] || 'info'
    default:
      return 'info'
  }
})
</script>
