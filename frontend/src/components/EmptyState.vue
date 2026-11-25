<template>
  <el-empty :description="description" :image-size="imageSize">
    <slot>
      <el-button v-if="actionText" type="primary" plain round @click="emit('action')">
        {{ actionText }}
      </el-button>
    </slot>
  </el-empty>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    description?: string
    imageSize?: number
    actionText?: string
  }>(),
  { description: '暂无数据', imageSize: 110 }
)

const emit = defineEmits<{ action: [] }>()
</script>
