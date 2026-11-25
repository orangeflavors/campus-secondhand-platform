<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/login" class="back">
        <el-icon><ArrowLeft /></el-icon>
        返回登录
      </router-link>
      <h2>找回凭据</h2>
      <p class="sub">通过注册时填写的学号校验身份并重置密码</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="注册学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入注册时填写的学号" clearable />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32 位新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submit">重置密码</el-button>
      </el-form>

      <el-alert
        class="hint"
        type="info"
        :closable="false"
        show-icon
        title="教学模拟说明"
        description="当前找回流程仅校验用户名与学号，正式环境应由校园统一身份服务或短信验证完成身份核实。"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  studentNo: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入注册时填写的学号', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authApi.resetPassword({
      username: form.username.trim(),
      studentNo: form.studentNo.trim(),
      newPassword: form.newPassword
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    router.replace('/login')
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
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: radial-gradient(circle at 20% 20%, #e8f5ed, var(--bg) 55%);
}

.auth-card {
  width: 100%;
  max-width: 430px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 32px;
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

h2 {
  margin: 0;
  font-size: 24px;
}

.sub {
  margin: 8px 0 22px;
  font-size: 13px;
  color: var(--ink-500);
}

.submit {
  width: 100%;
  height: 44px;
  font-size: 15px;
}

.hint {
  margin-top: 20px;
  border-radius: 10px;
}
</style>
