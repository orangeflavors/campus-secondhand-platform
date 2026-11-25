<template>
  <PageContainer title="校园集市" subtitle="只展示在售商品 · 校内认证同学发布 · 当面验货更放心">
    <template #actions>
      <el-button type="primary" round @click="goPublish">
        <el-icon><Plus /></el-icon>
        发布闲置
      </el-button>
    </template>

    <section class="market-hero">
      <div class="hero-copy">
        <h2>让闲置在校园里继续流转</h2>
        <p>教材、数码、生活用品……搜一搜，也许正好有人需要它。</p>
      </div>

      <div class="search-bar">
        <el-input
          v-model="keyword"
          size="large"
          clearable
          placeholder="搜索教材、数码、生活用品…"
          @keyup.enter="applyFilters()"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" @click="applyFilters()">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>

      <div v-if="metaLoading" class="category-chips">
        <span v-for="n in 6" :key="n" class="chip-skeleton" aria-hidden="true" />
      </div>
      <div v-else class="category-chips">
        <button
          type="button"
          class="chip"
          :class="{ active: selectedCategoryId === null }"
          @click="selectCategory(null)"
        >
          全部
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="chip"
          :class="{ active: selectedCategoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
        </button>
        <button
          v-if="!metaLoading && !categories.length"
          type="button"
          class="chip retry-chip"
          @click="loadMeta"
        >
          <el-icon><Refresh /></el-icon>
          重新加载分类
        </button>
      </div>
    </section>

    <section class="filter-card surface">
      <div class="toolbar filter-toolbar">
        <el-button text class="advanced-toggle" @click="advancedOpen = !advancedOpen">
          <el-icon><Filter /></el-icon>
          <span>更多筛选</span>
          <span v-if="activeAdvancedCount" class="active-count">{{ activeAdvancedCount }}</span>
          <el-icon class="toggle-arrow" :class="{ rotated: advancedOpen }"><ArrowDown /></el-icon>
        </el-button>
        <span v-if="activeAdvancedCount" class="muted small">已选 {{ activeAdvancedCount }} 项高级条件</span>
        <span class="spacer" />
        <span class="sort-label">排序</span>
        <el-select v-model="sort" class="sort-select" @change="applyFilters()">
          <el-option v-for="option in sortOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-button v-if="hasAnyFilter" text type="primary" @click="resetFilters">清除筛选</el-button>
      </div>

      <el-collapse-transition>
        <div v-show="advancedOpen" class="advanced-panel">
          <div class="advanced-grid">
            <div class="filter-field">
              <label>价格区间（元）</label>
              <div class="price-range">
                <el-input-number
                  v-model="minPrice"
                  :min="0"
                  :max="99999999.99"
                  :precision="2"
                  :controls="false"
                  placeholder="最低价"
                />
                <span class="range-sep">—</span>
                <el-input-number
                  v-model="maxPrice"
                  :min="0"
                  :max="99999999.99"
                  :precision="2"
                  :controls="false"
                  placeholder="最高价"
                />
              </div>
            </div>

            <div class="filter-field">
              <label>成色</label>
              <el-select v-model="condition" clearable :value-on-clear="''" placeholder="全部成色">
                <el-option
                  v-for="option in conditionChoices"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>

            <div class="filter-field">
              <label>交付区域</label>
              <el-select v-model="area" clearable filterable :value-on-clear="''" placeholder="全部区域">
                <el-option v-for="areaItem in areaChoices" :key="areaItem" :label="areaItem" :value="areaItem" />
              </el-select>
            </div>

            <div class="filter-field filter-actions">
              <el-button type="primary" round @click="applyFilters()">
                <el-icon><Check /></el-icon>
                应用筛选
              </el-button>
              <el-button round @click="resetFilters">重置</el-button>
            </div>
          </div>
        </div>
      </el-collapse-transition>
    </section>

    <div class="result-head">
      <span>共 <strong>{{ total }}</strong> 件在售商品</span>
      <span v-if="keyword.trim()" class="muted small">关键词「{{ keyword.trim() }}」</span>
      <span v-if="selectedCategoryName" class="muted small">分类「{{ selectedCategoryName }}」</span>
    </div>

    <div v-if="loading" class="grid-cards" aria-busy="true">
      <div v-for="n in PAGE_SIZE" :key="n" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-image" />
            <div class="skeleton-body">
              <el-skeleton-item variant="h3" style="width: 78%" />
              <el-skeleton-item variant="text" style="width: 48%" />
              <div class="skeleton-footer">
                <el-skeleton-item variant="text" style="width: 34%" />
                <el-skeleton-item variant="text" style="width: 28%" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <section v-else-if="loadError && !items.length" class="surface empty-card">
      <EmptyState description="商品加载失败，请稍后重试" action-text="重新加载" @action="fetchItems" />
    </section>

    <section v-else-if="!items.length" class="surface empty-card">
      <EmptyState description="没有找到符合条件的在售商品" action-text="清除筛选" @action="resetFilters" />
    </section>

    <div v-else class="grid-cards">
      <ItemCard v-for="item in items" :key="item.id" :item="item" />
    </div>

    <AppPagination :page="page" :size="PAGE_SIZE" :total="total" @change="handlePageChange" />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw, type LocationQueryValueRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import type {
  CategoryVO,
  ItemCardVO,
  ItemCondition,
  ItemSearchParams,
  OptionVO
} from '@/types'
import { itemApi, metaApi } from '@/api/item'
import {
  areas as fallbackAreas,
  conditionOptions as fallbackConditionOptions,
  sortOptions
} from '@/utils/dict'
import PageContainer from '@/components/PageContainer.vue'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppPagination from '@/components/AppPagination.vue'

type SortValue = NonNullable<ItemSearchParams['sort']>

const PAGE_SIZE = 12
const SORT_VALUES: SortValue[] = ['latest', 'price_asc', 'price_desc']
const CONDITION_VALUES: ItemCondition[] = ['NEW', 'LIKE_NEW', 'GOOD', 'FAIR']

const route = useRoute()
const router = useRouter()

const categories = ref<CategoryVO[]>([])
const metaConditions = ref<OptionVO[]>([])
const metaAreas = ref<string[]>([])
const metaLoading = ref(false)

const keyword = ref('')
const selectedCategoryId = ref<number | null>(null)
const minPrice = ref<number | undefined>(undefined)
const maxPrice = ref<number | undefined>(undefined)
const condition = ref<ItemCondition | ''>('')
const area = ref('')
const sort = ref<SortValue>('latest')
const page = ref(1)
const advancedOpen = ref(false)

const items = ref<ItemCardVO[]>([])
const total = ref(0)
const loading = ref(true)
const loadError = ref(false)

const conditionChoices = computed(() =>
  metaConditions.value.length ? metaConditions.value : fallbackConditionOptions
)
const areaChoices = computed(() => (metaAreas.value.length ? metaAreas.value : fallbackAreas))
const selectedCategoryName = computed(
  () => categories.value.find((category) => category.id === selectedCategoryId.value)?.name ?? ''
)
const activeAdvancedCount = computed(
  () =>
    [
      typeof minPrice.value === 'number',
      typeof maxPrice.value === 'number',
      Boolean(condition.value),
      Boolean((area.value || '').trim())
    ].filter(Boolean).length
)
const hasAnyFilter = computed(
  () =>
    keyword.value.trim() !== '' ||
    selectedCategoryId.value !== null ||
    activeAdvancedCount.value > 0 ||
    sort.value !== 'latest'
)

let queryKeyCache = ''
let requestSeq = 0

function queryText(value: LocationQueryValueRaw | LocationQueryValueRaw[] | undefined): string {
  if (Array.isArray(value)) return value[0] !== null && value[0] !== undefined ? String(value[0]) : ''
  return value !== null && value !== undefined ? String(value) : ''
}

function isSortValue(value: string): value is SortValue {
  return (SORT_VALUES as string[]).includes(value)
}

function isCondition(value: string): value is ItemCondition {
  return (CONDITION_VALUES as string[]).includes(value)
}

function parsePositiveInt(value: string): number | null {
  if (!/^\d+$/.test(value)) return null
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function parsePrice(value: string): number | undefined {
  if (!value.trim()) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined
}

function hydrateFromQuery(query: LocationQueryRaw): void {
  keyword.value = queryText(query.keyword)
  selectedCategoryId.value = parsePositiveInt(queryText(query.categoryId))
  minPrice.value = parsePrice(queryText(query.minPrice))
  maxPrice.value = parsePrice(queryText(query.maxPrice))
  const conditionValue = queryText(query.condition)
  condition.value = isCondition(conditionValue) ? conditionValue : ''
  area.value = queryText(query.area)
  const sortValue = queryText(query.sort)
  sort.value = isSortValue(sortValue) ? sortValue : 'latest'
}

function buildQuery(): LocationQueryRaw {
  const query: LocationQueryRaw = {}
  const trimmedKeyword = keyword.value.trim()
  if (trimmedKeyword) query.keyword = trimmedKeyword
  if (selectedCategoryId.value !== null) query.categoryId = String(selectedCategoryId.value)
  if (typeof minPrice.value === 'number') query.minPrice = String(minPrice.value)
  if (typeof maxPrice.value === 'number') query.maxPrice = String(maxPrice.value)
  if (condition.value) query.condition = condition.value
  if ((area.value || '').trim()) query.area = area.value.trim()
  if (sort.value !== 'latest') query.sort = sort.value
  return query
}

function canonicalQuery(query: LocationQueryRaw): string {
  const normalized: Record<string, string> = {}
  Object.keys(query)
    .sort()
    .forEach((key) => {
      const value = query[key]
      normalized[key] = Array.isArray(value)
        ? value.map((entry) => entry ?? '').join(',')
        : String(value ?? '')
    })
  return JSON.stringify(normalized)
}

async function fetchItems(): Promise<void> {
  const seq = ++requestSeq
  loading.value = true
  loadError.value = false

  const params: ItemSearchParams = { page: page.value, size: PAGE_SIZE }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (selectedCategoryId.value !== null) params.categoryId = selectedCategoryId.value
  if (typeof minPrice.value === 'number') params.minPrice = minPrice.value
  if (typeof maxPrice.value === 'number') params.maxPrice = maxPrice.value
  if (condition.value) params.condition = condition.value
  if ((area.value || '').trim()) params.area = area.value.trim()
  if (sort.value !== 'latest') params.sort = sort.value

  try {
    const result = await itemApi.search(params)
    if (seq !== requestSeq) return
    items.value = result.records
    total.value = result.total
  } catch {
    if (seq === requestSeq) loadError.value = true
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

async function applyFilters(resetPage = true): Promise<void> {
  if (
    typeof minPrice.value === 'number' &&
    typeof maxPrice.value === 'number' &&
    minPrice.value > maxPrice.value
  ) {
    ElMessage.warning('最低价不能高于最高价')
    return
  }
  if (resetPage) page.value = 1

  const query = buildQuery()
  const nextKey = canonicalQuery(query)
  queryKeyCache = nextKey
  if (canonicalQuery(route.query) !== nextKey) {
    await router.replace({ query })
  }
  await fetchItems()
}

function selectCategory(categoryId: number | null): void {
  if (selectedCategoryId.value === categoryId) return
  selectedCategoryId.value = categoryId
  void applyFilters()
}

function resetFilters(): void {
  keyword.value = ''
  selectedCategoryId.value = null
  minPrice.value = undefined
  maxPrice.value = undefined
  condition.value = ''
  area.value = ''
  sort.value = 'latest'
  advancedOpen.value = false
  void applyFilters()
}

function handlePageChange(nextPage: number): void {
  page.value = nextPage
  void fetchItems()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goPublish(): void {
  void router.push('/publish')
}

async function loadMeta(): Promise<void> {
  metaLoading.value = true
  try {
    const meta = await metaApi.get()
    categories.value = meta.categories
    metaConditions.value = meta.conditions
    metaAreas.value = meta.areas
  } catch {
    /* 元数据加载失败时使用 utils/dict 中的内置选项兜底 */
  } finally {
    metaLoading.value = false
  }
}

onMounted(() => {
  hydrateFromQuery(route.query)
  queryKeyCache = canonicalQuery(route.query)
  void loadMeta()
  void fetchItems()
})

watch(
  () => route.query,
  (query) => {
    const nextKey = canonicalQuery(query)
    if (nextKey === queryKeyCache) return
    queryKeyCache = nextKey
    hydrateFromQuery(query)
    page.value = 1
    void fetchItems()
  }
)
</script>

<style scoped>
.market-hero {
  position: relative;
  overflow: hidden;
  border: 1px solid #dcefe4;
  border-radius: 18px;
  padding: 28px 30px 24px;
  margin-bottom: 16px;
  background:
    radial-gradient(circle at 88% 12%, rgba(58, 156, 108, 0.16), transparent 32%),
    linear-gradient(135deg, #eaf7f0 0%, #f6fcf9 55%, #ffffff 100%);
}

.market-hero::after {
  content: '';
  position: absolute;
  right: -46px;
  bottom: -64px;
  width: 190px;
  height: 190px;
  border: 22px solid rgba(58, 156, 108, 0.08);
  border-radius: 50%;
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero-copy h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--brand-700);
}

.hero-copy p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--ink-500);
}

.search-bar {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 10px;
  max-width: 720px;
  margin-top: 20px;
}

.search-bar .el-input {
  flex: 1;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding-left: 14px;
  box-shadow: 0 2px 10px rgba(31, 107, 74, 0.06);
}

.search-bar .el-button {
  border-radius: 12px;
  padding: 0 24px;
}

.category-chips {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.chip {
  border: 1px solid #d8e8df;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--ink-700);
  font-size: 13px;
  line-height: 1;
  padding: 8px 15px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.chip:hover {
  border-color: var(--brand-500);
  color: var(--brand-700);
}

.chip.active {
  border-color: var(--brand-600);
  background: var(--brand-600);
  color: #fff;
  box-shadow: 0 6px 14px rgba(47, 130, 89, 0.2);
}

.retry-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--brand-600);
  border-style: dashed;
}

.chip-skeleton {
  display: inline-block;
  width: 78px;
  height: 31px;
  border-radius: 999px;
  background: linear-gradient(90deg, #e8f2ec 25%, #f7fcfa 37%, #e8f2ec 63%);
  background-size: 400% 100%;
  animation: chip-shimmer 1.4s ease infinite;
}

@keyframes chip-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.filter-card {
  padding: 12px 16px;
  margin-bottom: 18px;
}

.filter-toolbar {
  margin-bottom: 0;
  gap: 10px;
}

.advanced-toggle {
  color: var(--ink-700);
  padding-left: 0;
}

.advanced-toggle:hover {
  color: var(--brand-600);
}

.toggle-arrow {
  margin-left: 2px;
  transition: transform 0.2s ease;
}

.toggle-arrow.rotated {
  transform: rotate(180deg);
}

.active-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  margin-left: 2px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--brand-100);
  color: var(--brand-700);
  font-size: 11px;
  font-weight: 600;
}

.sort-label {
  font-size: 12px;
  color: var(--ink-500);
}

.sort-select {
  width: 148px;
}

.advanced-panel {
  border-top: 1px dashed var(--line);
  margin-top: 10px;
  padding-top: 16px;
}

.advanced-grid {
  display: grid;
  grid-template-columns: minmax(260px, 1.35fr) minmax(160px, 1fr) minmax(160px, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.filter-field label {
  font-size: 12px;
  color: var(--ink-500);
}

.filter-field .el-select {
  width: 100%;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-range :deep(.el-input-number) {
  width: 126px;
}

.range-sep {
  color: var(--ink-300);
}

.filter-actions {
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 1px;
}

.result-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin: 2px 0 14px;
  font-size: 14px;
  color: var(--ink-700);
}

.result-head strong {
  font-size: 19px;
  color: var(--brand-700);
}

.skeleton-card {
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
}

.skeleton-image {
  display: block;
  width: 100%;
  height: 172px;
}

.skeleton-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.skeleton-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
}

.empty-card {
  padding: 20px;
}

@media (max-width: 960px) {
  .advanced-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .market-hero {
    padding: 20px 18px;
  }

  .hero-copy h2 {
    font-size: 21px;
  }

  .search-bar {
    flex-direction: column;
  }

  .search-bar .el-button {
    width: 100%;
  }

  .filter-toolbar {
    align-items: flex-start;
  }

  .sort-select {
    width: 132px;
  }

  .advanced-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .skeleton-image {
    height: 140px;
  }
}
</style>
