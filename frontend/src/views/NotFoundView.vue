<template>
  <div class="not-found">
    <div class="code">404</div>
    <h2>页面走丢了</h2>
    <p class="muted">你访问的页面不存在，或已被移动到其他位置。</p>
    <div class="actions">
      <el-button type="primary" round @click="router.push('/')">回到集市</el-button>
      <el-button round @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  min-height: 70vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 20px;
}

.code {
  font-size: 96px;
  font-weight: 800;
  letter-spacing: 6px;
  color: var(--brand-500);
  opacity: 0.28;
  line-height: 1;
}

h2 {
  margin: 0;
  font-size: 24px;
}

.actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}
</style>
