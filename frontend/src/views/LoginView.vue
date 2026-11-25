<template>
  <div class="auth-page">
    <div class="auth-brand">
      <div class="brand-head">
        <span class="logo">集</span>
        <div>
          <h1>校园集市</h1>
          <p>校园二手交易与物品流转平台</p>
        </div>
      </div>
      <ul class="highlights">
        <li>
          <el-icon><CircleCheckFilled /></el-icon>
          <div>
            <strong>校内认证才可交易</strong>
            <span>学号认证通过后才能浏览与发布，减少校外风险。</span>
          </div>
        </li>
        <li>
          <el-icon><CircleCheckFilled /></el-icon>
          <div>
            <strong>待确认请求不锁商品</strong>
            <span>卖家接受后才锁定商品，同一商品最多一笔有效订单。</span>
          </div>
        </li>
        <li>
          <el-icon><CircleCheckFilled /></el-icon>
          <div>
            <strong>双方确认才算完成</strong>
            <span>线下交付记录由双方分别确认，形成可追踪的交易闭环。</span>
          </div>
        </li>
      </ul>
      <div class="risk">
        <el-icon><WarningFilled /></el-icon>
        平台不提供在线支付与物流，请在校内公共区域当面验货、当面付款。
      </div>
    </div>

    <div class="auth-panel">
      <div class="auth-card">
        <h2>欢迎回来</h2>
        <p class="sub">登录后即可浏览校内闲置物品</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <div class="form-links">
            <router-link to="/forgot">忘记密码？</router-link>
          </div>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">登录</el-button>
        </el-form>

        <div class="auth-foot">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>

        <el-divider content-position="center"><span class="small muted">演示账号（点击填入）</span></el-divider>
        <div class="demo-accounts">
          <el-tag v-for="account in demoAccounts" :key="account.username" class="demo-tag" @click="fill(account)">
            {{ account.label }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

interface DemoAccount {
  label: string
  username: string
  password: string
}

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts: DemoAccount[] = [
  { label: '卖家 seller01', username: 'seller01', password: '123456' },
  { label: '买家 buyer01', username: 'buyer01', password: '123456' },
  { label: '管理员 admin', username: 'admin', password: 'admin123' },
  { label: '未认证 newbie01', username: 'newbie01', password: '123456' }
]

function fill(account: DemoAccount) {
  form.username = account.username
  form.password = account.password
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const user = await store.login({ username: form.username.trim(), password: form.password })
    ElMessage.success(`欢迎回来，${user.nickname}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect) {
      router.replace(redirect)
    } else if (user.role === 'ADMIN') {
      router.replace('/admin/dashboard')
    } else {
      router.replace('/')
    }
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: var(--bg);
}

.auth-brand {
  padding: 64px 56px;
  background: linear-gradient(150deg, #1f6b4a 0%, #2f8259 55%, #3a9c6c 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 36px;
  position: relative;
  overflow: hidden;
}

.auth-brand::after {
  content: '';
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  right: -140px;
  bottom: -160px;
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
}

.brand-head h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 2px;
}

.brand-head p {
  margin: 6px 0 0;
  opacity: 0.8;
  font-size: 13px;
}

.highlights {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
  max-width: 430px;
}

.highlights li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.highlights .el-icon {
  font-size: 20px;
  margin-top: 2px;
  opacity: 0.9;
}

.highlights strong {
  display: block;
  font-size: 15px;
  margin-bottom: 4px;
}

.highlights span {
  font-size: 13px;
  opacity: 0.78;
  line-height: 1.6;
}

.risk {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  max-width: 460px;
}

.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 34px 32px 28px;
  box-shadow: 0 20px 50px rgba(31, 107, 74, 0.08);
}

.auth-card h2 {
  margin: 0;
  font-size: 24px;
  color: var(--ink-900);
}

.sub {
  margin: 8px 0 24px;
  font-size: 13px;
  color: var(--ink-500);
}

.form-links {
  display: flex;
  justify-content: flex-end;
  margin: -6px 0 16px;
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 44px;
  font-size: 15px;
}

.auth-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--ink-500);
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
  transition: transform 0.15s ease;
}

.demo-tag:hover {
  transform: translateY(-1px);
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-brand {
    display: none;
  }
}
</style>
