<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const store = useUserStore()

onMounted(() => {
  if (store.isLoggedIn && !store.user) {
    store.fetchMe().catch(() => {
      /* 由路由守卫处理登录失效 */
    })
  }
})
</script>
