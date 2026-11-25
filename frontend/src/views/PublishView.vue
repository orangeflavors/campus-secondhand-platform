<template>
  <PageContainer :title="isEdit ? '编辑商品' : '发布闲置'" :subtitle="pageSubtitle">
    <template #actions>
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </template>

    <div v-if="loading" class="surface publish-loading">
      <el-skeleton animated :rows="10" />
    </div>

    <section v-else-if="notFound || forbidden" class="surface publish-missing">
      <EmptyState
        :description="forbidden ? '你无权编辑该商品' : '商品不存在或已被删除'"
        action-text="返回我的商品"
        @action="router.push('/me/items')"
      />
    </section>

    <template v-else>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="publish-form"
        :disabled="readonly"
      >
      <div v-if="isEdit && currentStatus" class="edit-status-bar surface">
        <span class="status-label">当前状态</span>
        <StatusTag kind="item" :value="currentStatus" />
        <span class="muted small">{{ statusDescription }}</span>
        <span class="spacer" />
        <el-button v-if="canSubmitFromPage && !readonly" size="small" type="primary" plain round :loading="submitting" @click="handleSubmitReview">
          直接提交审核
        </el-button>
      </div>

      <el-alert v-if="metaError" type="error" :closable="false" show-icon class="form-alert">
        <template #title>分类与区域数据加载失败</template>
        <div class="alert-line">
          <span>下拉选项暂不可用，你仍可填写其他内容。</span>
          <el-button link type="primary" @click="loadMeta">重新加载</el-button>
        </div>
      </el-alert>

      <el-alert v-if="isEdit && readonly" type="warning" :closable="false" show-icon class="form-alert">
        <template #title>当前状态不可编辑</template>
        <span>{{ readonlyReason }}</span>
      </el-alert>

      <el-alert v-if="isEdit && currentStatus === 'ON_SALE'" type="warning" :closable="false" show-icon class="form-alert">
        <template #title>在售商品修改提示</template>
        <span>修改标题、分类、价格、描述、成色或图片等关键信息后，商品会重新进入待审核状态并暂停展示，审核通过后恢复上架。</span>
      </el-alert>

      <el-alert v-if="rejectReason" type="error" :closable="false" show-icon class="form-alert">
        <template #title>审核驳回原因</template>
        <span>{{ rejectReason }}</span>
      </el-alert>

      <div class="form-grid">
        <div class="form-main">
          <section class="surface form-section">
            <h2 class="section-title"><span class="bar"></span>基本信息</h2>

            <el-form-item label="商品标题" prop="title">
              <el-input
                v-model="form.title"
                maxlength="100"
                show-word-limit
                placeholder="例如：高等数学（第七版）教材，九成新，无笔记"
              />
            </el-form-item>

            <div class="two-cols">
              <el-form-item label="商品分类" prop="categoryId">
                <el-select v-model="form.categoryId" placeholder="请选择分类" class="full-width" :loading="metaLoading">
                  <el-option
                    v-for="category in categories"
                    :key="category.id"
                    :label="category.name"
                    :value="category.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="商品成色" prop="condition">
                <el-select v-model="form.condition" placeholder="请选择成色" class="full-width">
                  <el-option
                    v-for="option in conditionChoices"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
            </div>

            <div class="two-cols">
              <el-form-item label="价格（元）" prop="price">
                <el-input-number
                  v-model="form.price"
                  :min="0"
                  :max="99999999.99"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  placeholder="0 表示免费流转"
                  class="full-width"
                />
              </el-form-item>

              <el-form-item label="交付区域" prop="handoverArea">
                <el-select
                  v-model="form.handoverArea"
                  filterable
                  allow-create
                  default-first-option
                  clearable
                  :value-on-clear="''"
                  placeholder="选择或输入校内交付区域"
                  class="full-width"
                >
                  <el-option v-for="areaItem in areaChoices" :key="areaItem" :label="areaItem" :value="areaItem" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="商品描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="7"
                maxlength="2000"
                show-word-limit
                placeholder="请描述商品的使用情况、成色细节、配件、交易方式等，至少 10 个字。"
              />
            </el-form-item>
          </section>
        </div>

        <aside class="form-side">
          <section class="surface form-section">
            <div class="side-title-row">
              <h2 class="section-title"><span class="bar"></span>商品图片</h2>
              <span class="muted small">{{ form.images.length }} / 6</span>
            </div>
            <p class="field-tip">第一张图片将作为商品封面。支持 png、jpg、jpeg、webp、gif，单张不超过 5MB。</p>

            <div class="image-grid">
              <div v-for="(image, index) in form.images" :key="`${image}-${index}`" class="image-item">
                <el-image
                  :src="image"
                  fit="cover"
                  :preview-src-list="form.images"
                  :initial-index="index"
                  preview-teleported
                  class="image-preview"
                >
                  <template #error>
                    <div class="image-error"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <span v-if="index === 0" class="cover-badge">封面</span>
                <button
                  v-if="!readonly"
                  type="button"
                  class="image-remove"
                  title="删除图片"
                  @click="removeImage(index)"
                >
                  <el-icon><Close /></el-icon>
                </button>
              </div>

              <el-upload
                v-if="form.images.length < 6 && !readonly"
                class="upload-tile"
                :auto-upload="false"
                :show-file-list="false"
                :disabled="uploading"
                accept="image/png,image/jpeg,image/webp,image/gif"
                :on-change="handleFileChange"
              >
                <div class="upload-trigger">
                  <el-icon v-if="uploading" class="is-loading"><Loading /></el-icon>
                  <el-icon v-else><Plus /></el-icon>
                  <span>{{ uploading ? '上传中…' : '上传图片' }}</span>
                </div>
              </el-upload>
            </div>

            <p v-if="imageError" class="field-error">{{ imageError }}</p>

            <div v-if="!readonly" class="url-add">
              <el-input
                v-model="imageUrlInput"
                placeholder="或粘贴图片地址，如 /uploads/… 或 https://…"
                clearable
                @keyup.enter="addImageUrl"
              />
              <el-button :disabled="form.images.length >= 6" @click="addImageUrl">添加</el-button>
            </div>
          </section>

          <section class="surface form-section tips-card">
            <h2 class="section-title"><span class="bar"></span>发布须知</h2>
            <ul class="tips-list">
              <li>请如实描述商品成色与瑕疵，避免交易纠纷。</li>
              <li>平台不提供在线支付与物流，请在校内公共区域当面交易。</li>
              <li>禁止发布违禁物品、虚假信息或与交易无关的广告。</li>
              <li>提交审核后可在「我的商品」查看审核进度与驳回原因。</li>
            </ul>
          </section>
        </aside>
      </div>
    </el-form>

    <div class="form-actions surface">
      <div class="actions-info">
        <StatusTag v-if="currentStatus" kind="item" :value="currentStatus" />
        <span class="muted small">{{ actionHint }}</span>
      </div>
      <div class="actions-buttons">
        <el-button v-if="readonly" size="large" @click="router.push('/me/items')">返回我的商品</el-button>
        <template v-else>
          <el-button size="large" :loading="draftSaving" :disabled="submitting" @click="handleSaveDraft">
            保存草稿
          </el-button>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="draftSaving"
            @click="handleSubmitReview"
          >
            提交审核
          </el-button>
        </template>
        </div>
      </div>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import type {
  CategoryVO,
  ItemCondition,
  ItemDetailVO,
  ItemStatus,
  ItemUpsertPayload,
  OptionVO
} from '@/types'
import { itemApi, metaApi } from '@/api/item'
import { fileApi } from '@/api/file'
import {
  areas as fallbackAreas,
  conditionOptions as fallbackConditionOptions
} from '@/utils/dict'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusTag from '@/components/StatusTag.vue'

interface PublishForm {
  title: string
  categoryId: number | null
  condition: ItemCondition | ''
  price: number | null | undefined
  handoverArea: string
  description: string
  images: string[]
}

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const form = reactive<PublishForm>(createEmptyForm())

const categories = ref<CategoryVO[]>([])
const metaConditions = ref<OptionVO[]>([])
const metaAreas = ref<string[]>([])
const metaLoading = ref(false)
const metaError = ref(false)

const loading = ref(true)
const notFound = ref(false)
const forbidden = ref(false)
const readonly = ref(false)
const currentStatus = ref<ItemStatus | ''>('')
const rejectReason = ref('')
const currentItemId = ref<number | null>(null)

const imageUrlInput = ref('')
const imageError = ref('')
const uploading = ref(false)
const draftSaving = ref(false)
const submitting = ref(false)

const isEdit = computed(() => route.name === 'item-edit')

const EDITABLE_STATUSES = ['DRAFT', 'REJECTED', 'OFF_SHELF', 'ON_SALE']
const SUBMITTABLE_STATUSES = ['DRAFT', 'REJECTED', 'OFF_SHELF']

const pageSubtitle = computed(() =>
  isEdit.value
    ? '修改商品信息；在售商品修改关键字段后会重新进入审核'
    : '填写商品信息，保存草稿或提交平台审核后上架'
)

const conditionChoices = computed(() =>
  metaConditions.value.length ? metaConditions.value : fallbackConditionOptions
)

const areaChoices = computed(() => {
  const base = metaAreas.value.length ? [...metaAreas.value] : [...fallbackAreas]
  if (form.handoverArea && !base.includes(form.handoverArea)) base.push(form.handoverArea)
  return base
})

const canSubmitFromPage = computed(
  () => !!currentStatus.value && SUBMITTABLE_STATUSES.includes(currentStatus.value)
)

const readonlyReason = computed(() => {
  switch (currentStatus.value) {
    case 'PENDING':
      return '商品正在审核中，暂不能编辑。审核完成后可再修改。'
    case 'LOCKED':
      return '商品已被进行中的订单锁定，暂不能编辑。'
    case 'SOLD':
      return '商品已售出，不能再编辑。'
    case 'VIOLATED':
      return '商品因违规被下架，暂不能编辑。如有疑问请联系平台。'
    default:
      return '当前状态下的商品不支持编辑。'
  }
})

const statusDescription = computed(() => {
  switch (currentStatus.value) {
    case 'DRAFT':
      return '完善信息后可提交审核。'
    case 'PENDING':
      return '平台审核中，请耐心等待。'
    case 'REJECTED':
      return '请根据驳回原因修改后重新提交。'
    case 'ON_SALE':
      return '正在出售，修改关键信息会触发重新审核。'
    case 'LOCKED':
      return '有订单进行中，商品暂时锁定。'
    case 'SOLD':
      return '交易已完成。'
    case 'OFF_SHELF':
      return '已下架，可修改后重新提交审核。'
    case 'VIOLATED':
      return '因违规被下架。'
    default:
      return ''
  }
})

const actionHint = computed(() => {
  if (readonly.value) return '当前状态不可编辑'
  if (isEdit.value && currentStatus.value === 'ON_SALE') {
    return '保存后，如关键字段有变更将自动重新审核'
  }
  return '保存草稿不会提交审核，可稍后继续编辑'
})

const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
const MAX_IMAGE_SIZE = 5 * 1024 * 1024

const rules: FormRules<PublishForm> = {
  title: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        const text = typeof value === 'string' ? value.trim() : ''
        if (!text) {
          callback(new Error('请输入商品标题'))
        } else if (text.length < 2 || text.length > 100) {
          callback(new Error('标题长度须为 2-100 个字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  condition: [{ required: true, message: '请选择商品成色', trigger: 'change' }],
  price: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('请输入价格，0 元表示免费流转'))
        } else if (typeof value !== 'number' || Number.isNaN(value) || value < 0) {
          callback(new Error('价格必须大于等于 0'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ],
  handoverArea: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        const text = typeof value === 'string' ? value.trim() : ''
        if (!text) {
          callback(new Error('请选择或输入交付区域'))
        } else if (text.length > 50) {
          callback(new Error('交付区域不能超过 50 个字符'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  description: [
    {
      required: true,
      validator: (_rule, value, callback) => {
        const text = typeof value === 'string' ? value.trim() : ''
        if (!text) {
          callback(new Error('请填写商品描述'))
        } else if (text.length < 10) {
          callback(new Error('商品描述至少需要 10 个字'))
        } else if (text.length > 2000) {
          callback(new Error('商品描述不能超过 2000 个字'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function createEmptyForm(): PublishForm {
  return {
    title: '',
    categoryId: null,
    condition: '',
    price: undefined,
    handoverArea: '',
    description: '',
    images: []
  }
}

function resetForm(): void {
  Object.assign(form, createEmptyForm())
  imageUrlInput.value = ''
  imageError.value = ''
}

function fillForm(detail: ItemDetailVO): void {
  form.title = detail.title
  form.categoryId = detail.categoryId
  form.condition = detail.condition
  form.price = detail.price
  form.handoverArea = detail.handoverArea || ''
  form.description = detail.description || ''
  form.images = detail.images.slice(0, 6)
}

function parseItemId(value: string | string[]): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  if (!raw || !/^\d+$/.test(raw)) return null
  const parsed = Number(raw)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

async function loadMeta(): Promise<void> {
  metaLoading.value = true
  metaError.value = false
  try {
    const meta = await metaApi.get()
    categories.value = meta.categories
    metaConditions.value = meta.conditions
    metaAreas.value = meta.areas
  } catch {
    metaError.value = true
  } finally {
    metaLoading.value = false
  }
}

let initSeq = 0

async function initialize(): Promise<void> {
  const seq = ++initSeq
  loading.value = true
  notFound.value = false
  forbidden.value = false
  readonly.value = false
  currentStatus.value = ''
  rejectReason.value = ''
  currentItemId.value = null
  resetForm()
  formRef.value?.clearValidate()

  if (!categories.value.length && !metaLoading.value) {
    void loadMeta()
  }

  if (!isEdit.value) {
    loading.value = false
    return
  }

  const id = parseItemId(route.params.id)
  if (id === null) {
    loading.value = false
    notFound.value = true
    return
  }

  try {
    const detail = await itemApi.detail(id)
    if (seq !== initSeq) return
    if (!detail.isOwner) {
      forbidden.value = true
      return
    }
    currentItemId.value = detail.id
    currentStatus.value = detail.status
    rejectReason.value = detail.rejectReason || ''
    readonly.value = !EDITABLE_STATUSES.includes(detail.status)
    fillForm(detail)
  } catch {
    if (seq === initSeq) notFound.value = true
  } finally {
    if (seq === initSeq) loading.value = false
  }
}

async function validateForm(): Promise<boolean> {
  if (!formRef.value) return false
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

function buildPayload(): ItemUpsertPayload {
  return {
    title: form.title.trim(),
    categoryId: form.categoryId,
    condition: form.condition,
    description: form.description.trim(),
    price: form.price ?? null,
    handoverArea: (form.handoverArea || '').trim(),
    images: [...form.images]
  }
}

async function persistForm(): Promise<ItemDetailVO | null> {
  const payload = buildPayload()
  const saved =
    currentItemId.value !== null
      ? await itemApi.update(currentItemId.value, payload)
      : await itemApi.create(payload)
  currentItemId.value = saved.id
  currentStatus.value = saved.status
  rejectReason.value = saved.rejectReason || ''
  return saved
}

async function handleSaveDraft(): Promise<void> {
  if (draftSaving.value || submitting.value) return
  const valid = await validateForm()
  if (!valid) return

  draftSaving.value = true
  try {
    const saved = await persistForm()
    if (!saved) return
    if (saved.status === 'ON_SALE') {
      ElMessage.success('商品信息已保存；如有重要内容变更，将重新进入审核')
    } else {
      ElMessage.success('草稿已保存')
    }
    await router.push('/me/items')
  } catch {
    /* 拦截器已统一提示，保留表单内容 */
  } finally {
    draftSaving.value = false
  }
}

async function handleSubmitReview(): Promise<void> {
  if (submitting.value || draftSaving.value) return

  const valid = await validateForm()
  if (!valid) return

  if (!form.images.length) {
    imageError.value = '提交审核前请至少添加 1 张商品图片'
    ElMessage.warning('提交审核前请至少添加 1 张商品图片')
    return
  }
  imageError.value = ''

  submitting.value = true
  try {
    const saved = await persistForm()
    if (!saved) return

    if (saved.status === 'PENDING') {
      ElMessage.success('修改已提交审核，请等待审核结果')
    } else if (saved.status === 'ON_SALE') {
      ElMessage.success('商品信息已保存，当前仍为在售状态')
    } else {
      const submitted = await itemApi.submit(saved.id)
      currentStatus.value = submitted.status
      ElMessage.success('已提交审核，请等待平台审核')
    }
    await router.push('/me/items')
  } catch {
    /* 拦截器已统一提示，页面停留在当前表单以便重试 */
  } finally {
    submitting.value = false
  }
}

function isAllowedImage(raw: File): boolean {
  const typeOk = ALLOWED_IMAGE_TYPES.includes(raw.type)
  const extOk = /\.(png|jpe?g|webp|gif)$/i.test(raw.name)
  return typeOk || extOk
}

async function handleFileChange(uploadFile: UploadFile): Promise<void> {
  const raw = uploadFile.raw
  if (!raw || uploading.value) return
  if (readonly.value) return
  if (form.images.length >= 6) {
    ElMessage.warning('最多添加 6 张商品图片')
    return
  }
  if (!isAllowedImage(raw)) {
    ElMessage.warning('仅支持 png、jpg、jpeg、webp、gif 格式的图片')
    return
  }
  if (raw.size > MAX_IMAGE_SIZE) {
    ElMessage.warning('单张图片不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    const result = await fileApi.upload(raw)
    if (!result.url) {
      ElMessage.warning('图片上传失败，请重试')
      return
    }
    if (form.images.includes(result.url)) {
      ElMessage.info('该图片已添加')
      return
    }
    if (form.images.length >= 6) {
      ElMessage.warning('最多添加 6 张商品图片')
      return
    }
    form.images.push(result.url)
    imageError.value = ''
    ElMessage.success('图片上传成功')
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    uploading.value = false
  }
}

function addImageUrl(): void {
  if (form.images.length >= 6) {
    ElMessage.warning('最多添加 6 张商品图片')
    return
  }
  const url = imageUrlInput.value.trim()
  if (!url) {
    ElMessage.warning('请输入图片地址')
    return
  }
  if (!url.startsWith('/') && !/^https?:\/\//i.test(url)) {
    ElMessage.warning('图片地址需以 / 或 http(s):// 开头')
    return
  }
  if (url.length > 255) {
    ElMessage.warning('图片地址不能超过 255 个字符')
    return
  }
  if (form.images.includes(url)) {
    ElMessage.info('该图片已添加')
    return
  }
  form.images.push(url)
  imageUrlInput.value = ''
  imageError.value = ''
  ElMessage.success('图片已添加')
}

function removeImage(index: number): void {
  form.images.splice(index, 1)
}

function goBack(): void {
  router.back()
}

watch([() => route.name, () => route.params.id], () => {
  void initialize()
}, { immediate: true })
</script>

<style scoped>
.publish-loading,
.publish-missing {
  padding: 24px;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-status-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
}

.status-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-700);
}

.form-alert {
  border-radius: 10px;
}

.alert-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.form-main,
.form-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.form-section {
  padding: 20px 22px;
}

.two-cols {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.full-width {
  width: 100%;
}

.side-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.side-title-row .section-title {
  margin-bottom: 0;
}

.field-tip {
  margin: 8px 0 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-500);
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.image-item {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1 / 1;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--brand-50);
}

.image-preview {
  display: block;
  width: 100%;
  height: 100%;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-300);
  font-size: 22px;
}

.cover-badge {
  position: absolute;
  left: 6px;
  bottom: 6px;
  padding: 2px 7px;
  border-radius: 8px;
  background: rgba(47, 130, 89, 0.88);
  color: #fff;
  font-size: 10px;
}

.image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(28, 43, 36, 0.65);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
}

.image-remove:hover {
  background: var(--el-color-danger);
}

.upload-tile {
  aspect-ratio: 1 / 1;
  border: 1px dashed #b9d8c8;
  border-radius: 10px;
  background: #fbfefc;
  overflow: hidden;
}

.upload-tile :deep(.el-upload) {
  width: 100%;
  height: 100%;
  display: block;
}

.upload-trigger {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--brand-600);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.upload-trigger:hover {
  background: var(--brand-50);
}

.upload-trigger .el-icon {
  font-size: 22px;
}

.field-error {
  margin: 8px 0 0;
  color: var(--el-color-danger);
  font-size: 12px;
}

.url-add {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.url-add .el-input {
  flex: 1;
  min-width: 0;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: var(--ink-700);
  font-size: 12px;
  line-height: 1.9;
}

.form-actions {
  position: sticky;
  bottom: 10px;
  z-index: 5;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 20px;
  box-shadow: 0 8px 24px rgba(31, 107, 74, 0.12);
}

.actions-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.actions-buttons {
  display: flex;
  gap: 9px;
}

.actions-buttons .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 980px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-side {
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: start;
  }
}

@media (max-width: 768px) {
  .two-cols {
    grid-template-columns: 1fr;
  }

  .form-side {
    display: flex;
  }

  .form-section {
    padding: 16px;
  }

  .image-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .form-actions {
    position: static;
    flex-direction: column;
    align-items: stretch;
  }

  .actions-buttons {
    flex-direction: column;
  }

  .actions-buttons .el-button {
    width: 100%;
  }
}
</style>
