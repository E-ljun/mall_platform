<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>👋 欢迎回来，{{ userStore.nickname || userStore.username }}</h2>
        <p>开启智能文案创作之旅</p>
      </div>
      <div class="quota-card" v-if="userStore.quotaRemaining !== Infinity">
        <div class="quota-ring">
          <svg viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="6"/>
            <circle
              cx="50" cy="50" r="42" fill="none" stroke="url(#quotaGrad)"
              stroke-width="6" stroke-linecap="round"
              :stroke-dasharray="264" :stroke-dashoffset="264 - 264 * quotaPercent"
              transform="rotate(-90 50 50)" style="transition: stroke-dashoffset 1s"
            />
            <defs>
              <linearGradient id="quotaGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#667eea"/><stop offset="100%" stop-color="#764ba2"/>
              </linearGradient>
            </defs>
          </svg>
          <div class="quota-num">{{ userStore.quotaRemaining }}<span>次</span></div>
          <div class="quota-label">剩余可用</div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <el-row :gutter="16" class="shortcuts">
      <el-col :xs="24" :sm="8" v-for="card in quickCards" :key="card.path">
        <div class="quick-card" @click="$router.push(card.path)">
          <div class="qc-icon">{{ card.icon }}</div>
          <div class="qc-title">{{ card.title }}</div>
          <div class="qc-desc">{{ card.desc }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近商品 -->
    <el-card class="recent-card">
      <template #header><span>📦 最近商品</span></template>
      <el-table :data="recentProducts" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SHELF' ? 'success' : row.status === 'OFF_SHELF' ? 'info' : ''" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && recentProducts.length === 0" description="暂无商品" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

interface Product { id: number; name: string; price: number; status: string }

const userStore = useUserStore()
const recentProducts = ref<Product[]>([])
const loading = ref(false)

const quotaPercent = computed(() =>
  userStore.quotaTotal > 0 ? Math.min(1, (userStore.quotaTotal - userStore.quotaUsed) / userStore.quotaTotal) : 1
)

const quickCards = [
  { path: '/products', icon: '📦', title: '商品管理', desc: '创建和管理您的商品' },
  { path: '/profile', icon: '👤', title: '个人中心', desc: '查看配额与账号信息' },
]

function statusLabel(s: string) {
  return { DRAFT: '草稿', ON_SHELF: '上架', OFF_SHELF: '下架' }[s] || s
}

async function loadRecent() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/products', { params: { page: 1, size: 5 } })
    recentProducts.value = data.data.records.slice(0, 5)
  } finally {
    loading.value = false
  }
}

onMounted(loadRecent)
</script>

<style scoped>
.dashboard { max-width: 1200px; }

.welcome-banner {
  background: linear-gradient(135deg, #141a33 0%, #1a2050 100%);
  border-radius: 16px;
  padding: 36px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border: 1px solid rgba(255,255,255,0.06);
}
.welcome-text h2 { color: #fff; margin: 0 0 8px; font-size: 22px; }
.welcome-text p { color: rgba(255,255,255,0.55); margin: 0; }

.quota-card { text-align: center; }
.quota-ring { position: relative; width: 100px; height: 100px; }
.quota-ring svg { width: 100%; height: 100%; }
.quota-num { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 700; color: #fff; }
.quota-num span { font-size: 12px; margin-left: 2px; margin-top: 6px; }
.quota-label { color: rgba(255,255,255,0.5); font-size: 12px; margin-top: -6px; }

.shortcuts { margin-bottom: 24px; }
.quick-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
  text-align: center;
}
.quick-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,0.1); }
.qc-icon { font-size: 36px; margin-bottom: 12px; }
.qc-title { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 6px; }
.qc-desc { font-size: 13px; color: #999; }

.recent-card { border-radius: 12px; }
</style>
