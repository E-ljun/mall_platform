<template>
  <div class="page">
    <h2>管理后台 - 商品管理（只读查看）</h2>
    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable style="width:120px">
            <el-option label="上架" value="ON_SHELF" />
            <el-option label="下架" value="OFF_SHELF" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
      </el-form>
    </el-card>

    <el-table :data="products" v-loading="loading" @selection-change="onSelect">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="ownerUsername" label="所属用户" width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.ownerUsername || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewProduct(row.id)">查看</el-button>
          <el-popconfirm title="确定删除此商品？" @confirm="deleteOne(row.id)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Batch operations bar -->
    <div v-if="selectedRows.length > 0" style="margin-bottom:12px;display:flex;align-items:center;gap:12px;padding:8px 16px;background:var(--bg-surface);border:1px solid var(--accent);border-radius:var(--radius-md);">
      <span style="color:var(--text-secondary);">已选 <strong style="color:var(--accent);">{{ selectedRows.length }}</strong> 项</span>
      <el-button type="danger" plain size="small" @click="confirmAdminBatchDelete">批量删除</el-button>
    </div>

    <!-- 商品详情弹窗 -->
    <el-dialog v-model="productVisible" title="商品详情" width="600px">
      <el-descriptions v-if="productDetail" :column="2" border size="small">
        <el-descriptions-item label="ID">{{ productDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ productDetail.name }}</el-descriptions-item>
        <el-descriptions-item label="价格">¥{{ productDetail.price }}</el-descriptions-item>
        <el-descriptions-item label="库存">{{ productDetail.stock }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small">{{ statusLabel(productDetail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属用户">{{ productDetail.ownerUsername || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="短标题" :span="2">{{ productDetail.shortTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详情描述" :span="2">{{ productDetail.detailContent || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="productDetail?.mainImageUrl" style="text-align:center;margin-top:12px;">
        <el-image :src="productDetail.mainImageUrl" style="max-width:300px;max-height:300px;border-radius:8px;" fit="contain" />
      </div>
    </el-dialog>

    <el-pagination
      v-model:current-page="page"
      :total="total"
      :page-size="size"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { type ApiResponse } from '../api/http'

interface Product {
  id: number
  name: string
  price: number
  stock: number
  status: string
  userId: number
  ownerUsername: string
}

const products = ref<Product[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)
const filters = reactive({ keyword: '', status: '' })
const productVisible = ref(false)
const productDetail = ref<any>(null)

async function viewProduct(id: number) {
  try {
    const { data } = await http.get<ApiResponse<any>>(`/products/${id}`)
    productDetail.value = { ...data.data.product, ownerUsername: data.data.product.ownerUsername || '未知' }
    productVisible.value = true
  } catch { ElMessage.error('获取商品详情失败') }
}

function statusLabel(s: string) {
  return { DRAFT: '草稿', ON_SHELF: '上架', OFF_SHELF: '下架' }[s] || s
}

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/products', {
      params: { page: page.value, size, keyword: filters.keyword || undefined, status: filters.status || undefined },
    })
    products.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

// ---- Selection & Delete ----
const selectedRows = ref<any[]>([])

function onSelect(rows: any[]) {
  selectedRows.value = rows
}

async function deleteOne(id: number) {
  try {
    await http.delete(`/products/${id}`)
    ElMessage.success('商品已删除')
    await load()
  } catch { /* error shown by interceptor */ }
}

async function confirmAdminBatchDelete() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 个商品？仅能删除自己创建的商品。`,
      '批量删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    const ids = selectedRows.value.map((r: any) => r.id)
    await http.post('/products/batch-delete', { ids })
    ElMessage.success('删除操作已完成')
    selectedRows.value = []
    await load()
  } catch { /* user cancelled or error */ }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 8px; }
.page h2 { color: var(--text-primary); font-family: var(--font-display); margin-top: 0; }
.filter-card { margin-bottom: 12px; }
.pager { margin-top: 16px; }
</style>
