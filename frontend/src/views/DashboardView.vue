<template>
  <div class="dashboard page-enter">
    <!-- Welcome -->
    <div class="welcome">
      <div class="welcome-greeting">
        <span class="greeting-wave">👋</span>
        <div>
          <h2>欢迎回来，{{ userStore.nickname || userStore.username }}</h2>
          <p>开启智能文案创作之旅</p>
        </div>
      </div>
      <div class="welcome-quota" v-if="userStore.quotaRemaining !== Infinity">
        <div class="quota-ring-wrap">
          <svg viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="6"/>
            <circle
              cx="50" cy="50" r="42" fill="none" stroke="url(#quotaGrad)"
              stroke-width="6" stroke-linecap="round"
              :stroke-dasharray="264" :stroke-dashoffset="264 - 264 * quotaPercent"
              transform="rotate(-90 50 50)"
              style="transition: stroke-dashoffset 1s cubic-bezier(0.16,1,0.3,1)"
            />
            <defs>
              <linearGradient id="quotaGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#d9ba78"/><stop offset="100%" stop-color="#c8a45c"/>
              </linearGradient>
            </defs>
          </svg>
          <div class="quota-num">{{ userStore.quotaRemaining }}<span>次</span></div>
          <div class="quota-label">剩余可用</div>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="quick-grid stagger-enter">
      <div class="quick-card" @click="$router.push('/products')">
        <div class="qc-icon-wrap">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
          </svg>
        </div>
        <div class="qc-info">
          <div class="qc-title">商品管理</div>
          <div class="qc-desc">管理您的商品库</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
      <div class="quick-card" @click="$router.push('/library')">
        <div class="qc-icon-wrap">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          </svg>
        </div>
        <div class="qc-info">
          <div class="qc-title">文案库</div>
          <div class="qc-desc">收藏与管理文案</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
      <div class="quick-card" @click="$router.push('/guide')">
        <div class="qc-icon-wrap">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
            <rect x="2" y="2" width="20" height="20" rx="2.5"/>
            <circle cx="8.5" cy="8.5" r="1.5"/>
            <path d="m21 15-5-5L5 21"/>
          </svg>
        </div>
        <div class="qc-info">
          <div class="qc-title">AI 出图指南</div>
          <div class="qc-desc">风格模板与技巧</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
      <div class="quick-card" @click="$router.push('/profile')">
        <div class="qc-icon-wrap">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
            <circle cx="12" cy="8" r="4"/>
            <path d="M20 21a8 8 0 1 0-16 0"/>
          </svg>
        </div>
        <div class="qc-info">
          <div class="qc-title">个人中心</div>
          <div class="qc-desc">配额与账号信息</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
    </div>

    <!-- Recent products -->
    <div class="section-card">
      <div class="section-head">
        <h3>最近商品</h3>
        <router-link to="/products" class="section-link">查看全部 →</router-link>
      </div>
      <el-table :data="recentProducts" v-loading="loading" class="recent-table">
        <el-table-column prop="name" label="商品名称" min-width="160">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-thumb" v-if="row.mainImageUrl">
                <img :src="row.mainImageUrl" alt="" />
              </div>
              <div class="product-thumb placeholder" v-else>📷</div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-dot" :class="statusClass(row.status)"></span>
            {{ statusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="" width="70">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && recentProducts.length === 0" description="还没有商品，开始创建吧" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

interface Product { id: number; name: string; price: number; status: string; mainImageUrl?: string }

const userStore = useUserStore()
const recentProducts = ref<Product[]>([])
const loading = ref(false)

const quotaPercent = computed(() =>
  userStore.quotaTotal > 0 ? Math.min(1, (userStore.quotaTotal - userStore.quotaUsed) / userStore.quotaTotal) : 1
)

function statusLabel(s: string) {
  return { DRAFT: '草稿', ON_SHELF: '已上架', OFF_SHELF: '已下架' }[s] || s
}
function statusClass(s: string) {
  return { DRAFT: 'draft', ON_SHELF: 'active', OFF_SHELF: 'off' }[s] || ''
}

async function loadRecent() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/products', { params: { page: 1, size: 5 } })
    recentProducts.value = data.data.records.slice(0, 5)
  } finally { loading.value = false }
}

onMounted(loadRecent)
</script>

<style scoped>
.dashboard { max-width: 1100px; }

/* Welcome */
.welcome {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 36px 40px;
  margin-bottom: 28px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}
.welcome-greeting { display: flex; align-items: center; gap: 16px; }
.greeting-wave {
  font-size: 36px;
  animation: float 4s ease-in-out infinite;
}
.welcome-greeting h2 {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-display);
}
.welcome-greeting p {
  margin: 0;
  color: var(--text-muted);
  font-size: 14px;
}

.welcome-quota { flex-shrink: 0; }
.quota-ring-wrap { position: relative; width: 100px; height: 100px; text-align: center; }
.quota-ring-wrap svg { width: 100%; height: 100%; }
.quota-num {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 700; color: var(--text-primary);
}
.quota-num span { font-size: 12px; margin-left: 2px; margin-top: 6px; color: var(--text-muted); }
.quota-label { color: var(--text-muted); font-size: 12px; margin-top: -6px; position: relative; }

/* Quick grid */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  margin-bottom: 28px;
}
.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s var(--ease-out);
}
.quick-card:hover {
  border-color: var(--accent);
  background: var(--bg-elevated);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}
.qc-icon-wrap {
  width: 44px; height: 44px;
  border-radius: var(--radius-md);
  background: rgba(200, 164, 92, 0.08);
  display: flex; align-items: center; justify-content: center;
  color: var(--accent);
  flex-shrink: 0;
  transition: background 0.3s;
}
.quick-card:hover .qc-icon-wrap {
  background: rgba(200, 164, 92, 0.15);
}
.qc-info { flex: 1; min-width: 0; }
.qc-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 2px; }
.qc-desc { font-size: 12px; color: var(--text-muted); }
.qc-arrow {
  color: var(--text-muted);
  font-size: 16px;
  transition: transform 0.3s, color 0.3s;
}
.quick-card:hover .qc-arrow {
  transform: translateX(4px);
  color: var(--accent);
}

/* Section card */
.section-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-display);
}
.section-link {
  font-size: 13px;
  color: var(--accent);
  text-decoration: none;
  transition: opacity 0.2s;
}
.section-link:hover { opacity: 0.8; }

/* Product cell */
.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.product-thumb {
  width: 36px; height: 36px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(0,0,0,0.03);
}
.product-thumb img { width: 100%; height: 100%; object-fit: cover; }
.product-thumb.placeholder {
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
}
.price { font-weight: 600; color: var(--accent-light); }

.status-dot {
  display: inline-block;
  width: 6px; height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.active { background: var(--success); }
.status-dot.draft { background: var(--text-muted); }
.status-dot.off { background: var(--coral); }

@media (max-width: 768px) {
  .welcome { flex-direction: column; gap: 20px; text-align: center; padding: 24px; }
  .welcome-greeting { flex-direction: column; }
  .quick-grid { grid-template-columns: 1fr; }
}
</style>
