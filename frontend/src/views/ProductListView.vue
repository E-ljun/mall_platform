<template>
  <div class="product-list-page page-enter">
    <!-- Page hero -->
    <div class="page-hero">
      <div class="hero-content">
        <h2>我的商品</h2>
        <p>管理商品库，用 AI 智能生成商品内容</p>
      </div>
      <el-button type="primary" size="large" @click="createProduct" class="create-btn">
        ＋ 新建商品
      </el-button>
    </div>

    <!-- Filters -->
    <div class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="商品名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" clearable placeholder="全部" style="width:140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="filters.minPrice" :min="0" placeholder="最低" controls-position="right" style="width:110px" />
          <span class="price-sep">—</span>
          <el-input-number v-model="filters.maxPrice" :min="0" placeholder="最高" controls-position="right" style="width:110px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width:120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="上架" value="ON_SHELF" />
            <el-option label="下架" value="OFF_SHELF" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form>
    </div>

    <!-- Stats bar -->
    <div class="stats-bar" v-if="!loading">
      <div class="stat-item">
        <span class="stat-value">{{ total }}</span>
        <span class="stat-label">全部商品</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ countByStatus('ON_SHELF') }}</span>
        <span class="stat-label">已上架</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ countByStatus('OFF_SHELF') }}</span>
        <span class="stat-label">已下架</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ countByStatus('DRAFT') }}</span>
        <span class="stat-label">草稿</span>
      </div>
    </div>

    <!-- Selection bar -->
    <div v-if="selectionMode" class="selection-bar">
      <span>已选 <strong>{{ selectedIds.length }}</strong> 个商品</span>
      <el-button type="danger" plain size="small" @click="confirmBatchDelete" :disabled="selectedIds.length === 0">
        批量删除
      </el-button>
      <el-button size="small" @click="cancelSelection">取消</el-button>
    </div>
    <div class="selection-toggle" v-if="products.length > 0 && !selectionMode">
      <el-button link size="small" @click="toggleSelectionMode">批量管理</el-button>
    </div>
    <div class="selection-toggle" v-if="products.length > 0 && selectionMode">
      <el-button link size="small" @click="cancelSelection">退出选择</el-button>
    </div>

    <!-- Product cards -->
    <div class="product-grid" v-loading="loading">
      <div v-for="item in products" :key="item.id" class="product-card" @click="selectionMode ? toggleSelect(item.id, !selectedIds.includes(item.id)) : goEdit(item.id)">
        <!-- Selection checkbox -->
        <el-checkbox
          v-if="selectionMode"
          class="card-checkbox"
          :model-value="selectedIds.includes(item.id)"
          @change="(checked: boolean) => toggleSelect(item.id, checked)"
          @click.stop
        />
        <!-- Delete button -->
        <button class="card-delete-btn" v-if="!selectionMode" title="删除商品" @click.stop="confirmDelete(item)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </button>
        <!-- Cover -->
        <div class="card-cover">
          <el-image v-if="item.mainImageUrl" :src="item.mainImageUrl" fit="cover" class="cover-img" lazy />
          <div v-else class="cover-empty">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" opacity="0.3">
              <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/>
            </svg>
          </div>
          <div class="cover-status" :class="'s-' + item.status">{{ statusLabel(item.status) }}</div>
          <div class="cover-price">¥{{ item.price }}</div>
        </div>

        <!-- Info -->
        <div class="card-body">
          <h4 class="card-name">{{ item.name }}</h4>
          <p class="card-sub" v-if="item.shortTitle">{{ item.shortTitle }}</p>
          <div class="card-tags">
            <span v-if="item.categoryName" class="card-tag">{{ item.categoryName }}</span>
            <span class="card-tag">库存 {{ item.stock || 0 }}</span>
          </div>
        </div>

        <!-- Overlay -->
        <div class="card-overlay">
          <div class="overlay-inner">
            <h4>{{ item.name }}</h4>
            <div class="overlay-detail">
              <div class="od-row"><span>价格</span><strong>¥{{ item.price }}</strong></div>
              <div class="od-row"><span>库存</span><strong>{{ item.stock || 0 }}</strong></div>
              <div class="od-row"><span>状态</span><strong>{{ statusLabel(item.status) }}</strong></div>
            </div>
            <span class="overlay-cta">查看详情 →</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && products.length === 0" description="暂无商品，点击上方按钮新建" />

    <!-- Pagination -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { type ApiResponse } from '../api/http'

interface Product {
  id: number; name: string; price: number; stock: number; status: string
  mainImageUrl?: string; shortTitle?: string; detailContent?: string; categoryName?: string
}
interface Category { id: number; name: string }

const router = useRouter()
const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const page = ref(1)
const size = 12
const total = ref(0)
const filters = reactive({
  keyword: '',
  categoryId: null as number | null,
  minPrice: null as number | null,
  maxPrice: null as number | null,
  status: '' as string,
})

const statusMap: Record<string, string> = { DRAFT: '草稿', ON_SHELF: '上架', OFF_SHELF: '下架' }
function statusLabel(s: string) { return statusMap[s] || s }
function countByStatus(s: string) { return products.value.filter(p => p.status === s).length }

function buildParams() {
  const p: Record<string, any> = { page: page.value, size }
  if (filters.keyword) p.keyword = filters.keyword
  if (filters.categoryId != null) p.categoryId = filters.categoryId
  if (filters.minPrice != null) p.minPrice = filters.minPrice
  if (filters.maxPrice != null) p.maxPrice = filters.maxPrice
  if (filters.status) p.status = filters.status
  return p
}

async function loadCategories() {
  const { data } = await http.get<ApiResponse<Category[]>>('/categories')
  categories.value = data.data
}
function getCategoryName(catId: number | null) {
  if (!catId) return ''
  return categories.value.find(c => c.id === catId)?.name || ''
}
async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/products', { params: buildParams() })
    const raw = data.data.records || []
    products.value = raw.map((r: any) => ({ ...r, categoryName: getCategoryName(r.categoryId) }))
    total.value = data.data.total
  } finally { loading.value = false }
}
function resetFilters() {
  filters.keyword = ''; filters.categoryId = null
  filters.minPrice = null; filters.maxPrice = null
  filters.status = ''; page.value = 1
  load()
}
async function createProduct() {
  const { data } = await http.post<ApiResponse<Product>>('/products', { name: '新商品', price: 99, stock: 100 })
  router.push(`/products/${data.data.id}/edit`)
}
function goEdit(id: number) { router.push(`/products/${id}/edit`) }

// ---- Selection & Delete ----
const selectionMode = ref(false)
const selectedIds = ref<number[]>([])

function toggleSelectionMode() {
  selectionMode.value = true
  selectedIds.value = []
}
function cancelSelection() {
  selectionMode.value = false
  selectedIds.value = []
}
function toggleSelect(id: number, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}
async function deleteProduct(id: number) {
  try {
    await http.delete(`/products/${id}`)
    ElMessage.success('商品已删除')
    await load()
  } catch { /* error shown by interceptor */ }
}
async function confirmDelete(item: Product) {
  try {
    await ElMessageBox.confirm(
      `确定删除商品「${item.name}」？删除后可在回收站保留 30 天。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteProduct(item.id)
  } catch { /* user cancelled */ }
}
async function confirmBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 个商品？删除后可在回收站保留 30 天。`,
      '批量删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await http.post('/products/batch-delete', { ids: selectedIds.value })
    ElMessage.success(`已删除 ${selectedIds.value.length} 个商品`)
    selectedIds.value = []
    selectionMode.value = false
    await load()
  } catch { /* user cancelled or error */ }
}

onMounted(async () => { await loadCategories(); await load() })
</script>

<style scoped>
.product-list-page { max-width: 1300px; }

/* Hero */
.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 36px;
  margin-bottom: 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}
.hero-content h2 {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-display);
}
.hero-content p { margin: 0; color: var(--text-muted); font-size: 14px; }
.create-btn { height: 44px; font-size: 14px; font-weight: 600; border-radius: var(--radius-md); }

/* Filter */
.filter-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 16px;
}
.filter-card :deep(.el-form-item) { margin-bottom: 0; }
.price-sep { margin: 0 8px; color: var(--text-muted); }

/* Stats */
.stats-bar {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  min-width: 100px;
}
.stat-value { font-size: 20px; font-weight: 700; color: var(--text-primary); font-family: var(--font-display); }
.stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }

/* Product grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
  min-height: 300px;
}

/* Product card */
.product-card {
  position: relative;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s var(--ease-out);
}
.product-card:hover {
  border-color: var(--accent);
  transform: translateY(-4px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.04), 0 8px 24px rgba(0,0,0,0.08), 0 16px 48px rgba(0,0,0,0.06), 0 0 0 1px rgba(196,155,74,0.12);
}

/* Cover */
.card-cover {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(0,0,0,0.02), rgba(0,0,0,0.04));
}
.cover-img {
  width: 100%; height: 100%;
  transition: transform 0.5s var(--ease-out);
}
.product-card:hover .cover-img { transform: scale(1.05); }
.cover-empty {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted);
}
.cover-status {
  position: absolute; top: 12px; left: 12px;
  padding: 3px 10px; border-radius: 12px;
  font-size: 11px; font-weight: 600; color: #fff;
  backdrop-filter: blur(6px);
}
.s-DRAFT { background: rgba(94, 91, 86, 0.8); }
.s-ON_SHELF { background: rgba(122, 170, 122, 0.8); }
.s-OFF_SHELF { background: rgba(212, 114, 106, 0.8); }
.cover-price {
  position: absolute; bottom: 0; right: 0;
  background: linear-gradient(135deg, rgba(196,155,74,0.9), rgba(163,123,46,0.9));
  color: #ffffff; font-size: 18px; font-weight: 700;
  padding: 6px 16px; border-radius: 12px 0 0 0;
}

/* Card body */
.card-body { padding: 14px 16px; }
.card-name {
  margin: 0 0 4px; font-size: 15px; font-weight: 600; color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-sub {
  margin: 0 0 10px; font-size: 12px; color: var(--text-muted);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.card-tag {
  font-size: 11px; color: var(--text-muted);
  background: rgba(0,0,0,0.03);
  padding: 2px 8px; border-radius: 4px;
  border: 1px solid var(--border-subtle);
}

/* Overlay */
.card-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(248,246,242,0.92) 0%, rgba(240,237,230,0.96) 100%);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.35s;
  pointer-events: none;
}
.product-card:hover .card-overlay { opacity: 1; }
.overlay-inner {
  text-align: center; color: var(--text-primary); padding: 20px;
  transform: translateY(8px);
  transition: transform 0.35s;
}
.product-card:hover .overlay-inner { transform: translateY(0); }
.overlay-inner h4 {
  margin: 0 0 14px; font-size: 16px; font-weight: 700;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 200px;
}
.overlay-detail {
  text-align: left; margin-bottom: 16px;
  display: flex; flex-direction: column; gap: 5px;
}
.od-row {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--text-muted);
  padding: 4px 10px; border-radius: 6px;
  background: rgba(0,0,0,0.03);
}
.od-row strong { color: var(--text-primary); font-size: 13px; }
.overlay-cta {
  display: inline-block;
  font-size: 12px;
  color: var(--accent);
  border: 1px solid rgba(196,155,74,0.3);
  padding: 4px 16px; border-radius: 20px;
}

/* Delete button */
.card-delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 6;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--border-default);
  background: var(--bg-elevated);
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.25s var(--ease-out);
  padding: 0;
}
.card-delete-btn:hover {
  color: var(--danger);
  border-color: var(--danger);
  background: rgba(204, 107, 99, 0.08);
}
.product-card:hover .card-delete-btn {
  opacity: 1;
}

/* Selection bar */
.selection-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--accent);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-secondary);
}
.selection-bar strong {
  color: var(--accent);
  font-size: 15px;
}
.selection-toggle {
  margin-bottom: 12px;
}

/* Card checkbox */
.card-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 6;
}

.pager { margin-top: 28px; justify-content: center; }

@media (max-width: 768px) {
  .page-hero { flex-direction: column; gap: 16px; }
  .stats-bar { flex-wrap: wrap; }
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); }
}
</style>
