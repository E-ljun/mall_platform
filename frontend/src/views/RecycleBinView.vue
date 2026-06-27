<template>
  <div class="page">
    <h2>{{ isAdmin ? '管理后台 - 回收站' : '🗑️ 回收站' }}</h2>
    <p style="color:var(--text-muted);margin-bottom:16px;">已删除的商品保留 30 天，期间可恢复。超出期限将被彻底清理。</p>

    <el-table :data="products" v-loading="loading" @selection-change="onSelect">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="商品名称" min-width="160" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isAdmin" prop="userId" label="所属用户ID" width="110" />
      <el-table-column label="删除时间" min-width="160">
        <template #default="{ row }">{{ row.updatedAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="restore(row.id)">恢复</el-button>
          <el-popconfirm title="确定彻底删除？不可恢复！" @confirm="purge(row.id)">
            <template #reference>
              <el-button size="small" type="danger">彻底删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top:16px;justify-content:flex-end;"
      @current-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const products = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/recycle-bin/products', {
      params: { page: page.value, size: size.value },
    })
    if (data.success) {
      products.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } catch { ElMessage.error('加载回收站失败') } finally { loading.value = false }
}

async function restore(id: number) {
  try {
    await http.post(`/recycle-bin/products/${id}/restore`)
    ElMessage.success('商品已恢复')
    await load()
  } catch { ElMessage.error('恢复失败') }
}

async function purge(id: number) {
  try {
    await http.delete(`/recycle-bin/products/${id}`)
    ElMessage.success('商品已彻底删除')
    await load()
  } catch { ElMessage.error('删除失败') }
}

function statusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', ON_SHELF: '上架', OFF_SHELF: '下架' }
  return map[s] || s
}
function onSelect(_rows: any[]) { /* reserved */ }

onMounted(load)
</script>

<style scoped>
.page h2 { color: var(--text-primary); font-family: var(--font-display); }
</style>
