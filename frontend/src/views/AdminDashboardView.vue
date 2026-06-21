<template>
  <div class="admin-dashboard">
    <h3>管理概览</h3>
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="s in stats" :key="s.label">
        <div class="stat-card" :style="{ '--accent': s.color }">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card class="mt">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>📢 系统公告</span>
          <el-button type="primary" size="small" @click="showAnnDialog = true">编辑公告</el-button>
        </div>
      </template>
      <div v-if="announcement" class="ann-content">{{ announcement.content }}</div>
      <el-empty v-else description="暂无公告" :image-size="60" />
    </el-card>

    <el-dialog v-model="showAnnDialog" title="编辑系统公告" width="500px">
      <el-input v-model="annForm.content" type="textarea" :rows="4" placeholder="输入公告内容..." />
      <template #footer>
        <el-button @click="showAnnDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http, { type ApiResponse } from '../api/http'

const stats = ref([
  { label: '注册用户', value: 0, color: '#667eea' },
  { label: '待审核', value: 0, color: '#e6a23c' },
  { label: '启用用户', value: 0, color: '#67c23a' },
  { label: '禁用用户', value: 0, color: '#f56c6c' },
])

const announcement = ref<any>(null)
const showAnnDialog = ref(false)
const annForm = ref({ content: '' })

async function load() {
  try {
    const { data } = await http.get<ApiResponse<any>>('/admin/users', { params: { page: 1, size: 1 } })
    stats.value[0].value = data.data.currentCount || data.data.total
  } catch { /* */ }
  try {
    const [pendingRes, activeRes, disabledRes] = await Promise.all([
      http.get<ApiResponse<any>>('/admin/users', { params: { page: 1, size: 1, status: 'PENDING' } }),
      http.get<ApiResponse<any>>('/admin/users', { params: { page: 1, size: 1, status: 'ACTIVE' } }),
      http.get<ApiResponse<any>>('/admin/users', { params: { page: 1, size: 1, status: 'DISABLED' } }),
    ])
    stats.value[1].value = pendingRes.data.data.total
    stats.value[2].value = activeRes.data.data.total
    stats.value[3].value = disabledRes.data.data.total
  } catch { /* */ }

  try {
    const { data } = await http.get<ApiResponse<any>>('/admin/announcement')
    announcement.value = data.data
  } catch { /* */ }
}

async function saveAnnouncement() {
  await http.post('/admin/announcement', { content: annForm.value.content })
  ElMessage.success('公告已发布')
  showAnnDialog.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.admin-dashboard { max-width: 1200px; }
.admin-dashboard h3 { margin-top: 0; }

.stat-row { margin-bottom: 20px; }
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border-top: 3px solid var(--accent);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-num { font-size: 32px; font-weight: 700; color: var(--accent); }
.stat-label { color: #999; font-size: 13px; margin-top: 4px; }

.mt { margin-top: 20px; border-radius: 12px; }
.ann-content { padding: 8px 0; font-size: 14px; color: #333; line-height: 1.8; white-space: pre-wrap; }
</style>
