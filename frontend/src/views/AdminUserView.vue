<template>
  <div class="admin-users">
    <h3>用户管理 <span class="sub">（已注册 {{ currentCount }} / 上限 50）</span></h3>

    <el-card class="filter-card mb">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="用户名/昵称" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width:140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
      </el-form>
    </el-card>

    <el-table :data="users" v-loading="loading" stripe class="user-table">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="100" />
      <el-table-column prop="role" label="角色" width="80">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
            {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag
            :type="row.status === 'ACTIVE' ? 'success' : row.status === 'PENDING' ? 'warning' : 'danger'"
            size="small"
          >{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配额" width="110">
        <template #default="{ row }">
          <span v-if="row.quotaTotal === -1" style="color:#67c23a">不限</span>
          <span v-else>{{ row.quotaUsed || 0 }} / {{ row.quotaTotal }}</span>
        </template>
      </el-table-column>
      <el-table-column label="权限" min-width="140">
        <template #default="{ row }">
          <span v-if="!row.permissions" style="color:#67c23a">全部</span>
          <span v-else style="font-size:12px;">
            <el-tag size="small" v-for="p in parsePermissions(row.permissions)" :key="p" style="margin:1px;">
              {{ permLabel(p.trim()) }}
            </el-tag>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <div class="actions">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success" link size="small" @click="approve(row.id)"
            >通过</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="danger" link size="small" @click="disableUser(row.id)"
            >禁用</el-button>
            <el-button
              v-if="row.status === 'DISABLED'"
              type="primary" link size="small" @click="enableUser(row.id)"
            >启用</el-button>
            <el-button
              v-if="row.role !== 'ADMIN'"
              link type="warning" size="small" @click="openQuota(row)"
            >配额</el-button>
            <el-button
              v-if="row.role !== 'ADMIN'"
              link type="primary" size="small" @click="openPerm(row)"
            >权限</el-button>
            <el-button
              link type="info" size="small" @click="viewDetail(row.id)"
            >查看</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page" :total="total" :page-size="size"
      layout="total, prev, pager, next" class="pager" @current-change="load"
    />

    <!-- 配额弹窗 -->
    <el-dialog v-model="quotaVisible" title="设置配额" width="420px">
      <p>用户：<strong>{{ quotaTarget?.nickname || quotaTarget?.username }}</strong></p>
      <el-form>
        <el-form-item label="总配额（-1 不限）">
          <el-input-number v-model="quotaValue" :min="-1" style="width:100%" />
        </el-form-item>
      </el-form>
      <el-alert v-if="quotaValue === -1" type="success" :closable="false">不限配额，用户可无限次使用</el-alert>
      <template #footer>
        <el-button @click="quotaVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuota">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限弹窗 -->
    <el-dialog v-model="permVisible" title="设置权限" width="420px">
      <p>用户：<strong>{{ permTarget?.nickname || permTarget?.username }}</strong></p>
      <el-checkbox-group v-model="permSelected">
        <el-checkbox label="description" style="display:block;margin-bottom:8px;">商品描述生成</el-checkbox>
        <el-checkbox label="copy" style="display:block;margin-bottom:8px;">营销文案生成</el-checkbox>
        <el-checkbox label="image" style="display:block;margin-bottom:8px;">详情图生成</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="520px">
      <el-descriptions v-if="detailUser" :column="2" border size="small">
        <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailUser.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="detailUser.role === 'ADMIN' ? 'danger' : 'primary'" size="small">{{ detailUser.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailUser.phone || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailUser.email || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailUser.status === 'ACTIVE' ? 'success' : detailUser.status === 'PENDING' ? 'warning' : 'danger'" size="small">{{ statusLabel(detailUser.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detailUser.createdAt?.substring(0, 10) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="AI 配额" :span="2">
          <template v-if="detailUser.quotaTotal === -1">不限制</template>
          <template v-else>总计 {{ detailUser.quotaTotal }} 次，已用 {{ detailUser.quotaUsed }} 次，剩余 {{ Math.max(0, (detailUser.quotaTotal || 0) - (detailUser.quotaUsed || 0)) }} 次</template>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { type ApiResponse } from '../api/http'

interface User { id: number; username: string; nickname: string; role: string; status: string; quotaTotal: number; quotaUsed: number; permissions: string }

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)
const currentCount = ref(0)
const filters = reactive({ keyword: '', status: '' })

const quotaVisible = ref(false)
const quotaTarget = ref<User | null>(null)
const quotaValue = ref(20)

const permVisible = ref(false)
const permTarget = ref<User | null>(null)
const permSelected = ref<string[]>([])

const detailVisible = ref(false)
const detailUser = ref<any>(null)

async function viewDetail(id: number) {
  try {
    const { data } = await http.get<ApiResponse<any>>(`/admin/users/${id}/detail`)
    detailUser.value = data.data
    detailVisible.value = true
  } catch { ElMessage.error('获取用户详情失败') }
}

function statusLabel(s: string) {
  return { PENDING: '待审核', ACTIVE: '正常', DISABLED: '已禁用' }[s] || s
}
function permLabel(p: string) {
  return { description: '商品描述', copy: '营销文案', image: '详情图' }[p] || p
}
// 解析权限字段（兼容 JSON 数组 ["a","b"] 和旧逗号格式 "a,b"）
function parsePermissions(raw: string | null): string[] {
  if (!raw) return []
  const s = raw.trim()
  if (s.startsWith('[')) {
    try { return JSON.parse(s) } catch { return [] }
  }
  return s.split(',').filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/admin/users', {
      params: { page: page.value, size, keyword: filters.keyword || undefined, status: filters.status || undefined },
    })
    users.value = data.data.records
    total.value = data.data.total
    currentCount.value = data.data.currentCount || data.data.total
  } finally { loading.value = false }
}

async function approve(id: number) {
  await ElMessageBox.confirm('确定通过该用户审核？', '审核')
  await http.post(`/admin/users/${id}/approve`)
  ElMessage.success('已通过审核')
  load()
}

async function disableUser(id: number) {
  await ElMessageBox.confirm('确定禁用该用户？', '禁用')
  await http.post(`/admin/users/${id}/disable`)
  ElMessage.success('已禁用')
  load()
}

async function enableUser(id: number) {
  await http.post(`/admin/users/${id}/enable`)
  ElMessage.success('已启用')
  load()
}

function openQuota(row: User) {
  quotaTarget.value = row
  quotaValue.value = row.quotaTotal ?? 20
  quotaVisible.value = true
}

async function saveQuota() {
  await http.post(`/admin/users/${quotaTarget.value!.id}/quota`, { quotaTotal: quotaValue.value })
  ElMessage.success('配额已更新')
  quotaVisible.value = false
  load()
}

const ALL_PERMS = ['description', 'copy', 'image']
function openPerm(row: User) {
  permTarget.value = row
  // null = 全部可用 → 默认全选；"[]" = 无权限 → 全不选
  if (row.permissions === null || row.permissions === undefined) {
    permSelected.value = [...ALL_PERMS]
  } else {
    permSelected.value = parsePermissions(row.permissions)
  }
  permVisible.value = true
}

async function savePerm() {
  await http.post(`/admin/users/${permTarget.value!.id}/permissions`, { permissions: permSelected.value })
  ElMessage.success('权限已更新')
  permVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.admin-users { max-width: 1400px; }
.admin-users h3 { margin-top: 0; color: var(--text-primary); font-family: var(--font-display); }
.sub { font-size: 13px; color: var(--text-muted); font-weight: 400; }
.mb { margin-bottom: 16px; }
.filter-card { border-radius: var(--radius-md); }
.user-table { border-radius: var(--radius-md); }
.actions { display: flex; flex-wrap: wrap; gap: 2px; }
.pager { margin-top: 16px; justify-content: center; }
</style>
