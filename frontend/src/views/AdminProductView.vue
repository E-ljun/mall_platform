<template>
  <div class="page">
    <h2>管理后台 - 商品管理</h2>
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

    <div class="batch-bar">
      <el-button type="success" :disabled="!selected.length" @click="batchStatus('ON_SHELF')">批量上架</el-button>
      <el-button :disabled="!selected.length" @click="batchStatus('OFF_SHELF')">批量下架</el-button>
      <el-button type="danger" :disabled="!selected.length" @click="batchDelete">批量删除</el-button>
    </div>

    <el-table :data="products" v-loading="loading" @selection-change="onSelect">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          <el-button link type="danger" @click="removeOne(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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
}

const products = ref<Product[]>([])
const selected = ref<Product[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)
const filters = reactive({ keyword: '', status: '' })

function statusLabel(s: string) {
  return { DRAFT: '草稿', ON_SHELF: '上架', OFF_SHELF: '下架' }[s] || s
}

function onSelect(rows: Product[]) {
  selected.value = rows
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

async function batchStatus(status: string) {
  await http.patch('/products/batch-status', { ids: selected.value.map((p) => p.id), status })
  ElMessage.success('操作成功')
  load()
}

async function batchDelete() {
  await ElMessageBox.confirm('确定批量删除？', '提示', { type: 'warning' })
  await http.post('/products/batch-delete', { ids: selected.value.map((p) => p.id) })
  ElMessage.success('已删除')
  load()
}

async function removeOne(id: number) {
  await ElMessageBox.confirm('确定删除该商品？', '提示', { type: 'warning' })
  await http.delete(`/products/${id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.page { padding: 8px; }
.filter-card { margin-bottom: 12px; }
.batch-bar { margin-bottom: 12px; display: flex; }
.batch-bar > * + * { margin-left: 8px; }
.pager { margin-top: 16px; }
</style>
