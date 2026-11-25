<template>
  <div class="auth-page">
    <div class="auth-panel">
      <div class="auth-card">
        <router-link to="/login" class="back">
          <el-icon><ArrowLeft /></el-icon>
          返回登录
        </router-link>
        <h2>注册校园账号</h2>
        <p class="sub">注册后需完成校园身份认证才能浏览与交易</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="4-20 位字母、数字或下划线" clearable />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="2-20 个字符，将展示给其他同学" clearable />
          </el-form-item>
          <div class="two-col">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
            </el-form-item>
          </div>
          <el-form-item label="学院" prop="college">
            <el-input v-model="form.college" placeholder="例如：计算机学院" clearable />
          </el-form-item>
          <div class="two-col">
            <el-form-item label="手机号（选填）" prop="phone">
              <el-input v-model="form.phone" placeholder="用于找回凭据" clearable />
            </el-form-item>
            <el-form-item label="学号（选填）" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="8-12 位数字" clearable />
            </el-form-item>
          </div>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">注册并登录</el-button>
        </el-form>

        <div class="auth-foot">
          已有账号？
          <router-link to="/login">直接登录</router-link>
        </div>
      </div>
    </div>

    <div class="auth-brand">
      <div class="brand-head">
        <span class="logo">集</span>
        <div>
          <h1>校园集市</h1>
          <p>把闲置交给需要的同学</p>
        </div>
      </div>
      <ul class="steps">
        <li><span>1</span> 注册账号，填写基本信息</li>
        <li><span>2</span> 完成校园身份认证</li>
        <li><span>3</span> 发布、搜索并线下交易</li>
      </ul>
      <div class="risk">
        <el-icon><WarningFilled /></el-icon>
        请勿使用他人学号认证，平台仅保存必要的认证结果。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  college: '',
  phone: '',
  studentNo: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '用户名需为 4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需为 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  studentNo: [{ pattern: /^$|^\d{8,12}$/, message: '学号需为 8-12 位数字', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await store.register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim(),
      college: form.college.trim() || undefined,
      phone: form.phone.trim() || undefined,
      studentNo: form.studentNo.trim() || undefined
    })
    ElMessage.success('注册成功，请完成校园身份认证')
    router.replace('/verify')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  background: var(--bg);
}

.auth-brand {
  padding: 64px 56px;
  background: linear-gradient(150deg, #1f6b4a 0%, #2f8259 55%, #3a9c6c 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 34px;
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
  left: -150px;
  top: -150px;
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

.steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  font-size: 14px;
}

.steps li {
  display: flex;
  align-items: center;
  gap: 12px;
}

.steps span {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
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
  max-width: 420px;
}

.auth-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 30px 32px 26px;
  box-shadow: 0 20px 50px rgba(31, 107, 74, 0.08);
}

.back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--ink-500);
  margin-bottom: 14px;
}

.auth-card h2 {
  margin: 0;
  font-size: 24px;
}

.sub {
  margin: 8px 0 20px;
  font-size: 13px;
  color: var(--ink-500);
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 4px;
}

.auth-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--ink-500);
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-brand {
    display: none;
  }

  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
