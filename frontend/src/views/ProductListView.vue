<template>
  <div class="page">
    <div class="toolbar">
      <h2>我的商品</h2>
      <el-button type="primary" @click="createProduct">新建商品</el-button>
    </div>

    <el-card class="filter-card">
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
          <el-input-number v-model="filters.minPrice" :min="0" placeholder="最低" controls-position="right" />
          <span style="margin:0 6px">-</span>
          <el-input-number v-model="filters.maxPrice" :min="0" placeholder="最高" controls-position="right" />
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
    </el-card>

    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="item in products" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="product-card" @click="goEdit(item.id)">
          <el-image v-if="item.mainImageUrl" :src="item.mainImageUrl" fit="cover" class="cover" lazy />
          <div v-else class="cover placeholder">暂无图片</div>
          <h3>{{ item.name }}</h3>
          <p class="price">¥{{ item.price }}</p>
          <el-tag size="small">{{ statusLabel(item.status) }}</el-tag>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />

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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http, { type ApiResponse } from '../api/http'

interface Product {
  id: number
  name: string
  price: number
  status: string
  mainImageUrl?: string
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
  categoryId: undefined as number | undefined,
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  status: '',
})

function statusLabel(s: string) {
  return { DRAFT: '草稿', ON_SHELF: '上架', OFF_SHELF: '下架' }[s] || s
}

async function loadCategories() {
  const { data } = await http.get<ApiResponse<Category[]>>('/categories')
  categories.value = data.data
}

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/products', {
      params: {
        page: page.value,
        size,
        keyword: filters.keyword || undefined,
        categoryId: filters.categoryId,
        minPrice: filters.minPrice,
        maxPrice: filters.maxPrice,
        status: filters.status || undefined,
      },
    })
    products.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = undefined
  filters.minPrice = undefined
  filters.maxPrice = undefined
  filters.status = ''
  page.value = 1
  load()
}

async function createProduct() {
  const { data } = await http.post<ApiResponse<Product>>('/products', {
    name: '新商品',
    price: 99,
    stock: 100,
  })
  router.push(`/products/${data.data.id}/edit`)
}

function goEdit(id: number) {
  router.push(`/products/${id}/edit`)
}

onMounted(async () => {
  await loadCategories()
  await load()
})
</script>

<style scoped>
.page { padding: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.filter-card { margin-bottom: 16px; }
.product-card { cursor: pointer; margin-bottom: 16px; }
.cover { width: 100%; height: 160px; border-radius: 6px; }
.placeholder { background: #f0f0f0; display:flex; align-items:center; justify-content:center; color:#999; }
.price { color: #f5222d; font-weight: 600; }
.pager { margin-top: 20px; justify-content: center; }
</style>
