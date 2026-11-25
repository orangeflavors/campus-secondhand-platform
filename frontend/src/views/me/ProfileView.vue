<template>
  <div v-loading="loading" class="me-page profile-page">
    <header class="page-head">
      <div>
        <h1 class="page-title">个人资料</h1>
        <p class="page-subtitle">完善资料，让同学更放心地与你交易。</p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="refreshAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </header>

    <div v-if="statusAlerts.length" class="alert-stack">
      <el-alert
        v-for="alert in statusAlerts"
        :key="alert.key"
        :type="alert.type"
        :closable="false"
        show-icon
        class="status-alert"
      >
        <template #title>{{ alert.title }}</template>
        <div class="alert-body">
          <p>{{ alert.message }}</p>
          <p v-if="alert.extra" class="alert-extra">{{ alert.extra }}</p>
          <el-button v-if="alert.verify" link type="primary" class="alert-link" @click="router.push('/verify')">
            立即前往认证
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-alert>
    </div>

    <div class="overview-grid">
      <section class="surface auth-card">
        <div class="card-head">
          <div>
            <h2 class="card-title">校园认证</h2>
            <p class="card-subtitle">完成认证后可浏览商品、发布闲置与参与交易。</p>
          </div>
          <StatusTag kind="auth" :value="store.user?.authStatus || 'UNVERIFIED'" />
        </div>

        <template v-if="store.user?.authStatus === 'VERIFIED'">
          <dl class="auth-info">
            <div class="auth-row">
              <dt>认证方式</dt>
              <dd>{{ authMethodText }}</dd>
            </div>
            <div class="auth-row">
              <dt>认证时间</dt>
              <dd>{{ authTimeText }}</dd>
            </div>
          </dl>
          <p class="auth-note">
            <el-icon><CircleCheckFilled /></el-icon>
            校园身份已通过核验，认证信息仅用于平台交易安全。
          </p>
        </template>
        <template v-else>
          <p class="auth-tip">当前状态：{{ authStatusText }}。认证不会公开展示你的学号与真实姓名。</p>
          <el-button type="primary" round @click="router.push('/verify')">
            <el-icon><Stamp /></el-icon>
            去完成认证
          </el-button>
        </template>
      </section>

      <section class="surface stats-card">
        <div class="card-head">
          <div>
            <h2 class="card-title">我的数据</h2>
            <p class="card-subtitle">基于已发布的商品、完成的交易与收到的评价。</p>
          </div>
        </div>
        <div class="stat-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.publishedCount }}</span>
            <span class="stat-label">发布商品</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.soldCount }}</span>
            <span class="stat-label">成交数量</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.reviewCount }}</span>
            <span class="stat-label">收到评价</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ ratingText }}</span>
            <span class="stat-label">平均评分</span>
            <StarRating v-if="stats.reviewCount > 0" :value="stats.avgRating" class="stat-rating" />
          </div>
        </div>
      </section>
    </div>

    <section class="surface form-card">
      <div class="card-head">
        <div>
          <h2 class="card-title">基本资料</h2>
          <p class="card-subtitle">昵称与学院会在商品与订单中展示，联系方式按可见范围展示。</p>
        </div>
        <span class="username">账号：{{ store.user?.username || '—' }}</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
        <el-form-item label="头像">
          <div class="avatar-editor">
            <UserAvatar :nickname="form.nickname" :src="form.avatarUrl" :size="68" />
            <div class="avatar-fields">
              <el-input v-model="form.avatarUrl" placeholder="输入图片地址，或从本地上传" clearable>
                <template #prefix>
                  <el-icon><Picture /></el-icon>
                </template>
              </el-input>
              <div class="upload-row">
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  :on-change="handleAvatarChange"
                  :disabled="uploading"
                >
                  <el-button :loading="uploading">
                    <el-icon v-if="!uploading"><Upload /></el-icon>
                    本地上传
                  </el-button>
                </el-upload>
                <span class="field-hint">支持 png / jpg / jpeg / webp / gif，不超过 5MB</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit placeholder="2-20 个字符" />
          </el-form-item>
          <el-form-item label="学院" prop="college">
            <el-input v-model="form.college" maxlength="50" placeholder="例如：计算机学院" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" maxlength="11" placeholder="用于线下交付联系" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="100" placeholder="example@campus.edu.cn" />
          </el-form-item>
        </div>

        <el-form-item label="联系方式可见范围" prop="contactVisibility">
          <el-radio-group v-model="form.contactVisibility" class="visibility-group">
            <el-radio
              v-for="option in visibilityOptions"
              :key="option.value"
              :value="option.value"
              border
              class="visibility-radio"
            >
              <span class="visibility-text">
                <strong>{{ option.label }}</strong>
                <span>{{ option.hint }}</span>
              </span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <div class="form-actions">
          <el-button :disabled="saving" @click="resetForm">重置</el-button>
          <el-button type="primary" :loading="saving" :disabled="uploading" @click="handleSave">
            <el-icon v-if="!saving"><Check /></el-icon>
            保存资料
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="surface reviews-card">
      <div class="card-head">
        <div>
          <h2 class="card-title">收到的评价</h2>
          <p class="card-subtitle">来自已完成订单的真实评价，共 {{ reviewTotal }} 条。</p>
        </div>
      </div>

      <div v-loading="reviewLoading" class="review-list">
        <article v-for="review in reviews" :key="review.id" class="review-item">
          <UserAvatar :nickname="review.authorNickname" :src="review.authorAvatarUrl" :size="42" />
          <div class="review-main">
            <div class="review-head">
              <strong>{{ review.authorNickname }}</strong>
              <StarRating :value="review.rating" />
              <span class="muted small review-time">{{ formatDateTime(review.createdAt) }}</span>
            </div>
            <p class="review-content">{{ review.content || '对方未填写评价内容' }}</p>
            <router-link class="review-order" :to="`/me/orders/${review.orderId}`">
              订单 #{{ review.orderId }}
            </router-link>
          </div>
        </article>
        <EmptyState
          v-if="!reviewLoading && reviews.length === 0"
          description="还没有收到评价，完成交易后买家或卖家可以互评"
          :image-size="90"
        />
      </div>

      <AppPagination v-model:page="reviewPage" :size="reviewSize" :total="reviewTotal" @change="loadReviews" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { authApi } from '@/api/auth'
import { fileApi } from '@/api/file'
import { useUserStore } from '@/stores/user'
import { accountStatusLabels, authStatusLabels, labelOf } from '@/utils/dict'
import { formatDateTime } from '@/utils/format'
import type { ContactVisibility, ReviewVO, UserVO } from '@/types'
import AppPagination from '@/components/AppPagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import StarRating from '@/components/StarRating.vue'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'

interface ProfileForm {
  nickname: string
  college: string
  phone: string
  email: string
  avatarUrl: string
  contactVisibility: ContactVisibility
}

interface StatusAlert {
  key: string
  type: 'warning' | 'error'
  title: string
  message: string
  extra?: string
  verify?: boolean
}

const router = useRouter()
const store = useUserStore()

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ProfileForm>({
  nickname: '',
  college: '',
  phone: '',
  email: '',
  avatarUrl: '',
  contactVisibility: 'AUTHENTICATED'
})

const reviewLoading = ref(false)
const reviews = ref<ReviewVO[]>([])
const reviewPage = ref(1)
const reviewSize = 5
const reviewTotal = ref(0)

const visibilityOptions: { value: ContactVisibility; label: string; hint: string }[] = [
  { value: 'PUBLIC', label: '公开', hint: '所有登录用户可见' },
  { value: 'AUTHENTICATED', label: '仅认证用户', hint: '仅已完成校园认证的用户可见' },
  { value: 'PRIVATE', label: '私密', hint: '仅自己可见' }
]

const AUTH_METHOD_LABELS: Record<string, string> = {
  STUDENT_NO: '学号认证'
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type ValidateCallback = (error?: string | Error) => void

function validateOptionalPhone(_rule: unknown, value: unknown, callback: ValidateCallback): void {
  const text = typeof value === 'string' ? value.trim() : ''
  if (!text) {
    callback()
    return
  }
  if (!/^1[3-9]\d{9}$/.test(text)) {
    callback('请输入 11 位有效手机号')
    return
  }
  callback()
}

function validateOptionalEmail(_rule: unknown, value: unknown, callback: ValidateCallback): void {
  const text = typeof value === 'string' ? value.trim() : ''
  if (!text) {
    callback()
    return
  }
  if (!EMAIL_PATTERN.test(text)) {
    callback('请输入正确的邮箱地址')
    return
  }
  callback()
}

const rules: FormRules<ProfileForm> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需为 2-20 个字符', trigger: 'blur' }
  ],
  phone: [{ validator: validateOptionalPhone, trigger: 'blur' }],
  email: [{ validator: validateOptionalEmail, trigger: 'blur' }],
  avatarUrl: [{ max: 255, message: '头像地址过长', trigger: 'blur' }]
}

const stats = computed(
  () =>
    store.user?.stats ?? {
      publishedCount: 0,
      soldCount: 0,
      reviewCount: 0,
      avgRating: 0
    }
)

const ratingText = computed(() => (stats.value.reviewCount > 0 ? stats.value.avgRating.toFixed(1) : '—'))
const authStatusText = computed(() => (store.user ? authStatusLabels[store.user.authStatus] : '未认证'))
const authMethodText = computed(() => labelOf(AUTH_METHOD_LABELS, store.user?.authMethod))
const authTimeText = computed(() => formatDateTime(store.user?.authTime))

const statusAlerts = computed<StatusAlert[]>(() => {
  const user = store.user
  if (!user) return []
  const alerts: StatusAlert[] = []

  if (user.accountStatus !== 'NORMAL') {
    alerts.push({
      key: 'account',
      type: user.accountStatus === 'BANNED' ? 'error' : 'warning',
      title: `账户当前为「${accountStatusLabels[user.accountStatus]}」状态`,
      message: user.statusReason || '你的部分账户能力已被平台限制，如有疑问请联系管理员。',
      extra: user.limitedUntil ? `限制截止时间：${formatDateTime(user.limitedUntil)}` : undefined
    })
  }

  if (user.authStatus !== 'VERIFIED') {
    alerts.push({
      key: 'auth',
      type: user.authStatus === 'REJECTED' ? 'error' : 'warning',
      title: `校园认证状态：${authStatusLabels[user.authStatus]}`,
      message: '完成校园认证后才能浏览商品、发布闲置与参与交易，认证信息仅用于平台安全核验。',
      verify: true
    })
  }

  return alerts
})

function fillForm(user: UserVO | null): void {
  if (!user) return
  form.nickname = user.nickname || ''
  form.college = user.college || ''
  form.phone = user.phone || ''
  form.email = user.email || ''
  form.avatarUrl = user.avatarUrl || ''
  form.contactVisibility = user.contactVisibility
}

async function refreshUser(): Promise<void> {
  loading.value = true
  try {
    const user = await store.fetchMe(true)
    fillForm(user)
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    loading.value = false
  }
}

async function loadReviews(): Promise<void> {
  const userId = store.user?.id
  if (!userId) return
  reviewLoading.value = true
  try {
    const result = await authApi.userReviews(userId, { page: reviewPage.value, size: reviewSize })
    reviews.value = result.records
    reviewTotal.value = result.total
  } catch {
    /* 接口错误已由请求拦截器统一提示，保留已有列表 */
  } finally {
    reviewLoading.value = false
  }
}

async function refreshAll(): Promise<void> {
  await refreshUser()
  await loadReviews()
}

function resetForm(): void {
  fillForm(store.user)
  formRef.value?.clearValidate()
}

async function handleAvatarChange(file: UploadFile): Promise<void> {
  const raw = file.raw
  if (!raw) return
  if (!/^image\/(png|jpe?g|webp|gif)$/i.test(raw.type)) {
    ElMessage.warning('仅支持 png / jpg / jpeg / webp / gif 格式的图片')
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const result = await fileApi.upload(raw)
    form.avatarUrl = result.url
    ElMessage.success('头像上传成功，保存资料后生效')
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    uploading.value = false
  }
}

async function handleSave(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const updated = await store.updateProfile({
      nickname: form.nickname.trim(),
      college: form.college.trim(),
      phone: form.phone.trim(),
      email: form.email.trim(),
      avatarUrl: form.avatarUrl.trim(),
      contactVisibility: form.contactVisibility
    })
    fillForm(updated)
    formRef.value.clearValidate()
    ElMessage.success('个人资料已保存')
  } catch {
    /* 接口错误已由请求拦截器统一提示 */
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  fillForm(store.user)
  await refreshUser()
  await loadReviews()
})
</script>

<style scoped>
.me-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-900);
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--ink-500);
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.alert-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-alert {
  border-radius: 10px;
}

.alert-body p {
  margin: 2px 0;
  font-size: 13px;
  line-height: 1.7;
}

.alert-extra {
  opacity: 0.9;
}

.alert-link {
  height: auto;
  padding: 0;
  margin-top: 4px;
  font-size: 13px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.auth-card,
.stats-card,
.form-card,
.reviews-card {
  padding: 20px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-900);
}

.card-subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-500);
}

.auth-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.auth-row {
  padding: 12px 14px;
  background: var(--brand-50);
  border-radius: 10px;
}

.auth-row dt {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--ink-500);
}

.auth-row dd {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
}

.auth-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 13px;
  color: var(--brand-700);
}

.auth-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-500);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  background: var(--brand-50);
  border-radius: 10px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--brand-700);
}

.stat-label {
  font-size: 12px;
  color: var(--ink-500);
}

.stat-rating {
  margin-top: 2px;
}

.username {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--ink-500);
}

.avatar-editor {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

.avatar-fields {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.field-hint {
  font-size: 12px;
  color: var(--ink-300);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.visibility-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.visibility-radio {
  width: 100%;
  height: auto;
  margin-right: 0;
  padding: 12px 14px;
  align-items: flex-start;
}

.visibility-radio :deep(.el-radio__label) {
  width: 100%;
  padding-left: 8px;
}

.visibility-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.5;
  white-space: normal;
}

.visibility-text strong {
  font-size: 13px;
  color: var(--ink-900);
}

.visibility-text span {
  font-size: 11px;
  color: var(--ink-500);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

.review-list {
  min-height: 80px;
}

.review-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px dashed var(--line);
}

.review-item:last-child {
  border-bottom: none;
}

.review-main {
  flex: 1;
  min-width: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.review-head strong {
  font-size: 14px;
  color: var(--ink-900);
}

.review-time {
  margin-left: auto;
}

.review-content {
  margin: 8px 0 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-700);
  word-break: break-word;
}

.review-order {
  font-size: 12px;
  color: var(--brand-600);
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .head-actions {
    justify-content: flex-end;
  }

  .auth-card,
  .stats-card,
  .form-card,
  .reviews-card {
    padding: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .visibility-group {
    grid-template-columns: 1fr;
  }

  .avatar-editor {
    flex-direction: column;
  }

  .auth-info {
    grid-template-columns: 1fr;
  }

  .review-time {
    margin-left: 0;
  }
}
</style>
