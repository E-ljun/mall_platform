<template>
  <div class="page">
    <h2>管理后台 - 数据分析</h2>

    <!-- AI 调用趋势 -->
    <el-card style="margin-bottom: 20px;">
      <template #header><span style="font-weight:700;">📊 近 7 天 AI 调用趋势</span></template>
      <div class="trend-bars">
        <div v-for="d in aiTrend" :key="d.date" class="trend-item">
          <div class="trend-bar" :style="{ height: barHeight(d.count, maxAiCount) }">
            <span class="trend-val">{{ d.count }}</span>
          </div>
          <span class="trend-label">{{ d.date.slice(5) }}</span>
        </div>
      </div>
      <p style="text-align:center;color:var(--text-muted);margin-top:8px;">本周共 {{ aiTrend.reduce((s:number,d:any) => s + d.count, 0) }} 次 AI 调用</p>
    </el-card>

    <!-- 商品新增趋势 -->
    <el-card style="margin-bottom: 20px;">
      <template #header><span style="font-weight:700;">📦 近 7 天商品新增趋势</span></template>
      <div class="trend-bars">
        <div v-for="d in productTrend" :key="d.date" class="trend-item">
          <div class="trend-bar green" :style="{ height: barHeight(d.count, maxProductCount) }">
            <span class="trend-val">{{ d.count }}</span>
          </div>
          <span class="trend-label">{{ d.date.slice(5) }}</span>
        </div>
      </div>
      <p style="text-align:center;color:var(--text-muted);margin-top:8px;">本周共新增 {{ productTrend.reduce((s:number,d:any) => s + d.count, 0) }} 件商品</p>
    </el-card>

    <el-row :gutter="20">
      <!-- 配额排行 -->
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header><span style="font-weight:700;">🏆 配额消耗排行 (Top 10)</span></template>
          <el-table :data="quotaRank" v-loading="loading" size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="username" label="用户" />
            <el-table-column label="已用 / 总量" width="200">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.quotaTotal === -1 ? 0 : Math.round((row.quotaUsed / (row.quotaTotal || 1)) * 100)"
                  :color="row.quotaTotal === -1 ? '#67c23a' : undefined"
                  :format="() => `${row.quotaUsed} / ${row.quotaTotal === -1 ? '∞' : row.quotaTotal}`"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 平台分布 -->
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header><span style="font-weight:700;">📝 文案平台分布</span></template>
          <div class="platform-list">
            <div v-for="p in platformDist" :key="p.platform" class="platform-row">
              <span class="platform-name">{{ platformLabel(p.platform) }}</span>
              <el-progress
                :percentage="Math.round((p.count / maxPlatformCount) * 100)"
                :stroke-width="18"
                :color="platformColor(p.platform)"
                :format="() => `${p.count} 条`"
                style="flex: 1; margin: 0 12px;"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import http, { type ApiResponse } from '../api/http'

const loading = ref(false)
const aiTrend = ref<{ date: string; count: number }[]>([])
const productTrend = ref<{ date: string; count: number }[]>([])
const quotaRank = ref<any[]>([])
const platformDist = ref<{ platform: string; count: number }[]>([])

const maxAiCount = computed(() => Math.max(1, ...aiTrend.value.map(d => d.count)))
const maxProductCount = computed(() => Math.max(1, ...productTrend.value.map(d => d.count)))
const maxPlatformCount = computed(() => Math.max(1, ...platformDist.value.map(d => d.count)))

function barHeight(count: number, max: number) {
  return Math.max(8, (count / max) * 180) + 'px'
}

function platformLabel(p: string) {
  const map: Record<string, string> = { XIAOHONGSHU: '小红书', TAOBAO: '淘宝', DOUYIN: '抖音', OTHER: '其它' }
  return map[p] || p
}

function platformColor(p: string) {
  const map: Record<string, string> = { XIAOHONGSHU: '#ff2442', TAOBAO: '#ff5000', DOUYIN: '#000000', OTHER: '#909399' }
  return map[p] || '#909399'
}

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/admin/stats/dashboard')
    if (!data.success || !data.data) return
    aiTrend.value = data.data.aiTrend || []
    productTrend.value = data.data.productTrend || []
    quotaRank.value = data.data.quotaRank || []
    platformDist.value = data.data.platformDist || []
  } catch { /* 统计加载失败 */ } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page h2 { color: var(--text-primary); font-family: var(--font-display); }
.trend-bars {
  display: flex; align-items: flex-end; justify-content: space-around;
  height: 220px; padding: 0 10px;
}
.trend-item {
  display: flex; flex-direction: column; align-items: center;
  width: 50px;
}
.trend-bar {
  width: 36px; min-height: 8px;
  background: linear-gradient(to top, var(--accent-dark), var(--accent-light));
  border-radius: 4px 4px 0 0;
  display: flex; align-items: flex-start; justify-content: center;
  transition: height 0.5s var(--ease-out);
}
.trend-bar.green {
  background: linear-gradient(to top, #5e8a6a, var(--success));
}
.trend-val {
  margin-top: -22px; font-size: 12px; font-weight: 600; color: var(--text-primary);
}
.trend-label {
  margin-top: 6px; font-size: 12px; color: var(--text-muted);
}
.platform-list { display: flex; flex-direction: column; gap: 16px; }
.platform-row { display: flex; align-items: center; }
.platform-name { width: 60px; font-weight: 600; font-size: 14px; color: var(--text-primary); }
</style>
