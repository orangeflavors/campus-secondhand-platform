<template>
  <PageContainer title="校园身份认证" subtitle="完成认证后即可浏览、发布与交易校内闲置物品">
    <div class="verify-grid">
      <div class="surface verify-card">
        <template v-if="store.isVerified">
          <div class="success">
            <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
            <h3>你已完成校园身份认证</h3>
            <p class="muted">认证方式：{{ methodText }} · 认证时间：{{ formatDateTime(store.user?.authTime) }}</p>
            <p class="muted">学院：{{ store.user?.college || '—' }}</p>
            <div class="success-actions">
              <el-button type="primary" round @click="goNext">开始逛集市</el-button>
            </div>
          </div>
        </template>

        <template v-else>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="为什么要认证？"
            description="平台仅对已认证的校内用户开放浏览与交易，以降低校外人员带来的交易与内容风险。认证结果只保存状态、方式和时间，不会向其他用户展示你的学号和真实姓名。"
            class="notice"
          />
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent>
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="请输入 8-12 位学号" clearable maxlength="12" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="用于与学号匹配，不会公开展示" clearable />
            </el-form-item>
            <el-form-item label="学院" prop="college">
              <el-input v-model="form.college" placeholder="例如：计算机学院" clearable />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="submit">提交认证</el-button>
          </el-form>
          <p class="tip small muted">
            教学模拟说明：当前版本通过学号格式与姓名填写模拟校园认证服务，正式部署时应接入学校统一身份认证（Q-01）。
          </p>
        </template>
      </div>

      <aside class="side">
        <div class="surface side-card">
          <h4>认证后可用的功能</h4>
          <ul>
            <li><el-icon><View /></el-icon> 浏览与搜索在售商品</li>
            <li><el-icon><Goods /></el-icon> 发布闲置并提交审核</li>
            <li><el-icon><ChatDotRound /></el-icon> 咨询卖家、收藏商品</li>
            <li><el-icon><Tickets /></el-icon> 创建待确认请求并完成交付</li>
            <li><el-icon><Star /></el-icon> 评价交易、举报违规内容</li>
          </ul>
        </div>
        <RiskNotice title="账号安全提示" text="请勿将账号借给他人使用。认证后如更换学号或学院，可联系管理员协助更新。" />
      </aside>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer.vue'
import RiskNotice from '@/components/RiskNotice.vue'
import { formatDateTime } from '@/utils/format'

const store = useUserStore()
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  studentNo: '',
  realName: '',
  college: store.user?.college || ''
})

const rules: FormRules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{8,12}$/, message: '学号需为 8-12 位数字', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度需为 2-20 个字符', trigger: 'blur' }
  ],
  college: [{ required: true, message: '请输入学院', trigger: 'blur' }]
}

const methodText = computed(() => {
  switch (store.user?.authMethod) {
    case 'STUDENT_NO':
      return '学号认证'
    case 'EMAIL':
      return '校园邮箱认证'
    case 'MANUAL':
      return '管理员审核'
    default:
      return '—'
  }
})

function goNext() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  router.replace(redirect || '/')
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await store.campusAuth({
      studentNo: form.studentNo.trim(),
      realName: form.realName.trim(),
      college: form.college.trim()
    })
    ElMessage.success('校园身份认证通过')
    goNext()
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.verify-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.verify-card {
  padding: 26px 28px;
}

.notice {
  margin-bottom: 22px;
  border-radius: 10px;
}

.submit {
  width: 100%;
  height: 44px;
  margin-top: 6px;
}

.tip {
  margin: 16px 0 0;
  line-height: 1.7;
}

.success {
  text-align: center;
  padding: 26px 0;
}

.success-icon {
  font-size: 56px;
  color: var(--brand-500);
}

.success h3 {
  margin: 14px 0 8px;
  font-size: 20px;
}

.success-actions {
  margin-top: 22px;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card h4 {
  margin: 0 0 14px;
  font-size: 15px;
}

.side-card ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13px;
  color: var(--ink-700);
}

.side-card li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-card .el-icon {
  color: var(--brand-500);
}

@media (max-width: 900px) {
  .verify-grid {
    grid-template-columns: 1fr;
  }
}
</style>
