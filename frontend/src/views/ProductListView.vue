<template>
  <div class="product-list-page">
    <!-- 顶部标题栏 -->
    <div class="page-hero">
      <div class="hero-content">
        <h2>📦 我的商品</h2>
        <p class="hero-sub">管理您的商品库，利用 AI 智能生成商品内容</p>
      </div>
      <el-button type="primary" size="large" @click="createProduct" class="create-btn">
        <span>＋ 新建商品</span>
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="🔍 关键词">
          <el-input v-model="filters.keyword" placeholder="商品名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="📂 分类">
          <el-select v-model="filters.categoryId" clearable placeholder="全部" style="width:140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="💰 价格">
          <el-input-number v-model="filters.minPrice" :min="0" placeholder="最低" controls-position="right" style="width:110px" />
          <span style="margin:0 6px;color:#999;">—</span>
          <el-input-number v-model="filters.maxPrice" :min="0" placeholder="最高" controls-position="right" style="width:110px" />
        </el-form-item>
        <el-form-item label="📌 状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width:120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="上架" value="ON_SHELF" />
            <el-option label="下架" value="OFF_SHELF" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form>
    </el-card>

    <!-- 统计信息 -->
    <div class="stats-row" v-if="!loading">
      <span class="stat">共 <strong>{{ total }}</strong> 件商品</span>
      <span class="stat">草稿 <strong>{{ countByStatus('DRAFT') }}</strong></span>
      <span class="stat">上架 <strong>{{ countByStatus('ON_SHELF') }}</strong></span>
      <span class="stat">下架 <strong>{{ countByStatus('OFF_SHELF') }}</strong></span>
      <span class="stat" v-if="total > 0">当前第 <strong>{{ page }}</strong> 页</span>
    </div>

    <!-- 商品卡片网格 -->
    <el-row :gutter="20" v-loading="loading" class="product-grid">
      <el-col v-for="item in products" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
        <div class="product-card" @click="goEdit(item.id)">
          <!-- 图片区 -->
          <div class="card-cover">
            <el-image v-if="item.mainImageUrl" :src="item.mainImageUrl" fit="cover" class="cover-img" lazy />
            <div v-else class="cover-placeholder">
              <span>📷</span>
              <span>暂无图片</span>
            </div>
            <!-- 状态角标 -->
            <div class="status-badge" :class="'status-' + item.status">
              {{ statusLabel(item.status) }}
            </div>
            <!-- 价格浮层 -->
            <div class="price-tag">¥{{ item.price }}</div>
          </div>

          <!-- 信息区 -->
          <div class="card-body">
            <h4 class="card-name">{{ item.name }}</h4>
            <p class="card-subtitle" v-if="item.shortTitle">{{ item.shortTitle }}</p>
            <div class="card-meta">
              <span v-if="item.categoryName" class="meta-tag">
                <span class="meta-icon">📂</span>{{ item.categoryName }}
              </span>
              <span class="meta-tag">
                <span class="meta-icon">📦</span>库存 {{ item.stock || 0 }}
              </span>
            </div>
          </div>

          <!-- 悬停遮罩层 -->
          <div class="card-overlay">
            <div class="overlay-content">
              <h4>{{ item.name }}</h4>
              <div class="overlay-info">
                <div class="oi-row"><span>价格</span><strong>¥{{ item.price }}</strong></div>
                <div class="oi-row"><span>库存</span><strong>{{ item.stock || 0 }}</strong></div>
                <div class="oi-row"><span>状态</span><strong>{{ statusLabel(item.status) }}</strong></div>
                <div class="oi-row" v-if="item.shortTitle"><span>短标题</span><strong>{{ item.shortTitle }}</strong></div>
                <div class="oi-row" v-if="item.detailContent"><span>描述</span><strong>{{ item.detailContent.slice(0, 40) }}{{ item.detailContent.length > 40 ? '...' : '' }}</strong></div>
              </div>
              <span class="overlay-cta">点击查看详情 →</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && products.length === 0" description="暂无商品，点击上方按钮新建" />

    <!-- 分页 -->
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
  const c = categories.value.find(c => c.id === catId)
  return c?.name || ''
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

onMounted(async () => { await loadCategories(); await load() })
</script>

<style scoped>
/* ===== 页面背景 ===== */
.product-list-page {
  padding: 8px;
  background: linear-gradient(180deg, #f8fafd 0%, #f0f2f8 100%);
  min-height: calc(100vh - 56px - 48px);
}

/* ===== 顶部标题栏 ===== */
.page-hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 28px 36px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8px 32px rgba(102,126,234,0.2);
}
.hero-content h2 {
  margin: 0 0 6px;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}
.hero-sub { margin: 0; color: rgba(255,255,255,0.75); font-size: 14px; }
.create-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  background: #fff;
  color: #667eea;
  border: none;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  transition: transform 0.2s, box-shadow 0.2s;
}
.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0,0,0,0.25);
  background: #fff;
  color: #667eea;
}

/* ===== 筛选卡片 ===== */
.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.filter-card :deep(.el-form-item) { margin-bottom: 0; }

/* ===== 统计行 ===== */
.stats-row { display: flex; gap: 24px; margin-bottom: 16px; flex-wrap: wrap; }
.stat { font-size: 13px; color: #666; }
.stat strong { color: #333; }

/* ===== 卡片网格 ===== */
.product-grid { min-height: 300px; }

/* ===== 商品卡片 ===== */
.product-card {
  position: relative;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s;
}
.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(102,126,234,0.18);
}

/* 图片区 */
.card-cover {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #e8ecf1 0%, #dce1e8 100%);
}
.cover-img {
  width: 100%; height: 100%;
  transition: transform 0.4s;
}
.product-card:hover .cover-img { transform: scale(1.06); }
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #b0b8c4; font-size: 14px; gap: 6px;
}

/* 状态角标 */
.status-badge {
  position: absolute; top: 12px; left: 12px;
  padding: 3px 10px; border-radius: 12px;
  font-size: 11px; font-weight: 600; color: #fff;
  backdrop-filter: blur(4px);
}
.status-DRAFT { background: rgba(144,147,153,0.85); }
.status-ON_SHELF { background: rgba(103,194,58,0.85); }
.status-OFF_SHELF { background: rgba(245,108,108,0.85); }

/* 价格浮层 */
.price-tag {
  position: absolute; bottom: 0; right: 0;
  background: linear-gradient(135deg, rgba(102,126,234,0.9), rgba(118,75,162,0.9));
  color: #fff; font-size: 18px; font-weight: 700;
  padding: 6px 16px; border-radius: 14px 0 0 0;
}

/* 信息区 */
.card-body { padding: 14px 16px; }
.card-name {
  margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #1a1a2e;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-subtitle {
  margin: 0 0 10px; font-size: 12px; color: #999;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.meta-tag { font-size: 12px; color: #888; display: flex; align-items: center; gap: 3px; }
.meta-icon { font-size: 13px; }

/* ===== 悬停遮罩 ===== */
.card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(26,26,46,0.88) 0%, rgba(15,21,40,0.94) 100%);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.35s;
  pointer-events: none;
}
.product-card:hover .card-overlay { opacity: 1; }
.overlay-content {
  text-align: center; color: #fff; padding: 20px;
  transform: translateY(8px);
  transition: transform 0.35s;
}
.product-card:hover .overlay-content { transform: translateY(0); }
.overlay-content h4 {
  margin: 0 0 16px; font-size: 16px; font-weight: 700;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 200px;
}
.overlay-info {
  text-align: left; margin-bottom: 16px;
  display: flex; flex-direction: column; gap: 6px;
}
.oi-row {
  display: flex; justify-content: space-between;
  font-size: 12px; color: rgba(255,255,255,0.65);
  padding: 4px 8px; border-radius: 6px;
  background: rgba(255,255,255,0.06);
}
.oi-row strong { color: #fff; font-size: 13px; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.overlay-cta {
  display: inline-block;
  font-size: 12px;
  color: rgba(255,255,255,0.8);
  border: 1px solid rgba(255,255,255,0.25);
  padding: 4px 16px; border-radius: 20px;
}

/* ===== 分页 ===== */
.pager { margin-top: 24px; justify-content: center; }
</style>
