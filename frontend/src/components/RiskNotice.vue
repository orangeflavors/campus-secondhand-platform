<template>
  <el-alert type="warning" :closable="false" show-icon class="risk-notice">
    <template #title>
      <strong>{{ title }}</strong>
    </template>
    <div class="risk-body">
      <slot>
        {{ text }}
      </slot>
    </div>
  </el-alert>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    text?: string
  }>(),
  {
    title: '交易风险提示：平台不代管资金',
    text:
      '本平台不提供在线支付、资金托管与物流配送。请在校内公共区域当面验货、当面付款并完成实物交付；不要向陌生人提前转账，谨防诈骗。'
  }
)
</script>

<style scoped>
.risk-notice {
  border-radius: 10px;
}

.risk-body {
  font-size: 12px;
  line-height: 1.7;
  margin-top: 2px;
}
</style>
