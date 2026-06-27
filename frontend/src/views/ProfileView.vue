<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :xs="24" :md="10">
        <el-card class="info-card">
          <div class="profile-header">
            <!-- 可点击更换头像 -->
            <div class="avatar-wrap" @click="triggerUpload" title="点击更换头像">
              <el-avatar :size="80" class="profile-avatar" :src="profile?.avatar || undefined">
                {{ (profile?.nickname || profile?.username || '?').charAt(0) }}
              </el-avatar>
              <div class="avatar-overlay">
                <span>📷</span>
              </div>
            </div>
            <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onAvatarChange" />
            <h3>{{ profile?.nickname || profile?.username }}</h3>
            <div class="status-row">
              <el-tag :type="roleTagType" size="small">{{ profile?.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
              <el-tag :type="statusTagType" size="small" style="margin-left:6px;">{{ statusLabel }}</el-tag>
            </div>
          </div>
          <el-descriptions :column="1" border class="info-table" size="small">
            <el-descriptions-item label="用户名">{{ profile?.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ profile?.nickname }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ profile?.phone || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ profile?.email || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="账户状态">
              <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ profile?.createdAt?.substring(0, 10) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="AI 配额">
              <template v-if="profile?.quotaTotal === -1">
                <el-tag type="success" size="small">不限制</el-tag>
              </template>
              <template v-else>
                <div class="quota-info">
                  <el-progress
                    :percentage="quotaPercent"
                    :color="quotaColor"
                    :stroke-width="8"
                    :show-text="false"
                    style="flex:1;"
                  />
                  <span class="quota-text">
                    {{ profile?.quotaUsed || 0 }} / {{ profile?.quotaTotal }}
                    <span v-if="profile?.quotaTotal !== -1" style="color:#999;font-size:11px;">
                      （剩余 {{ quotaRemaining }} 次）
                    </span>
                  </span>
                </div>
              </template>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 操作区 -->
      <el-col :xs="24" :md="14">
        <!-- 编辑个人信息 -->
        <el-card class="action-card">
          <template #header>✏️ 编辑个人信息</template>
          <el-form label-width="80px" :model="editForm" ref="editRef">
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" placeholder="您的昵称" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" placeholder="手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" placeholder="电子邮箱" />
            </el-form-item>
            <el-button type="primary" :loading="editLoading" @click="saveProfile">保存信息</el-button>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card class="action-card">
          <template #header>🔒 修改密码</template>
          <el-form label-width="80px" :model="pwdForm" :rules="pwdRules" ref="pwdRef">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPwd">
              <el-input v-model="pwdForm.confirmPwd" type="password" show-password />
            </el-form-item>
            <el-button type="primary" :loading="pwdLoading" @click="changePassword">修改密码</el-button>
          </el-form>
        </el-card>

        <!-- 系统公告 -->
        <el-card class="action-card" v-if="announcement">
          <template #header>📢 系统公告</template>
          <div class="announcement-content">{{ announcement.content }}</div>
          <div class="announcement-time">发布于 {{ announcement.createdAt }}</div>
        </el-card>

        <!-- 注销账号 -->
        <el-card class="action-card" v-if="!userStore.isAdmin">
          <template #header>⚠️ 注销账号</template>
          <p style="color:#999;font-size:13px;margin-bottom:12px;">注销后，您的所有商品和图片将被永久删除，不可恢复。</p>
          <el-button type="danger" @click="deleteAccount">注销账号</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

interface Profile {
  id: number; username: string; nickname: string; phone: string; email: string; avatar: string; role: string; status: string
  quotaTotal: number; quotaUsed: number; createdAt: string
}

const router = useRouter()
const userStore = useUserStore()
const profile = ref<Profile | null>(null)
const fileInput = ref<HTMLInputElement>()

const statusLabel = computed(() => {
  if (!profile.value) return ''
  return { ACTIVE: '正常', PENDING: '待审核', DISABLED: '已禁用' }[profile.value.status] || profile.value.status
})
const statusTagType = computed(() => {
  if (!profile.value) return 'info'
  return { ACTIVE: 'success', PENDING: 'warning', DISABLED: 'danger' }[profile.value.status] || 'info'
})
const roleTagType = computed(() => profile.value?.role === 'ADMIN' ? 'danger' : 'primary')
const quotaRemaining = computed(() => {
  if (!profile.value || profile.value.quotaTotal === -1) return Infinity
  return Math.max(0, profile.value.quotaTotal - (profile.value.quotaUsed || 0))
})
const quotaPercent = computed(() => {
  if (!profile.value || profile.value.quotaTotal <= 0) return 0
  return Math.round(((profile.value.quotaUsed || 0) / profile.value.quotaTotal) * 100)
})
const quotaColor = computed(() => {
  const p = quotaPercent.value
  if (p >= 90) return '#f56c6c'
  if (p >= 60) return '#e6a23c'
  return '#67c23a'
})

const announcement = ref<any>(null)
const editForm = ref({ nickname: '', phone: '', email: '' })
const editLoading = ref(false)
const editRef = ref()

const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPwd: '' })
const pwdLoading = ref(false)
const pwdRef = ref()
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少6位', trigger: 'blur' }],
  confirmPwd: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_: any, v: string, cb: any) => v === pwdForm.value.newPassword ? cb() : cb('两次密码不一致'), trigger: 'blur' },
  ],
}

function triggerUpload() { fileInput.value?.click() }

async function onAvatarChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.warning('头像不能超过 2MB'); return }
  const form = new FormData()
  form.append('file', file)
  try {
    const { data } = await http.post<ApiResponse<string>>('/auth/avatar', form)
    if (data.data) {
      profile.value!.avatar = data.data
      userStore.avatar = data.data
      localStorage.setItem('avatar', data.data)
      ElMessage.success('头像已更新')
    }
  } catch { ElMessage.error('头像上传失败，请重试') }
}

async function loadProfile() {
  try {
    const { data } = await http.get<ApiResponse<Profile>>('/auth/profile')
    profile.value = data.data
    editForm.value = {
      nickname: data.data.nickname || '',
      phone: data.data.phone || '',
      email: data.data.email || '',
    }
  } catch { ElMessage.error('加载个人信息失败') }
}

async function saveProfile() {
  editLoading.value = true
  try {
    await http.put('/auth/profile', editForm.value)
    if (profile.value) {
      profile.value.nickname = editForm.value.nickname
      profile.value.phone = editForm.value.phone
      profile.value.email = editForm.value.email
    }
    userStore.nickname = editForm.value.nickname
    localStorage.setItem('nickname', editForm.value.nickname)
    ElMessage.success('个人信息已更新')
  } catch { ElMessage.error('保存个人信息失败，请重试') } finally { editLoading.value = false }
}

async function changePassword() {
  const valid = await pwdRef.value.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await http.put('/auth/password', { oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPwd: '' }
  } catch { ElMessage.error('密码修改失败，请检查当前密码是否正确') } finally { pwdLoading.value = false }
}

async function deleteAccount() {
  try {
    await ElMessageBox.confirm(
      '注销后，您的所有商品和图片将被永久删除且不可恢复。确定继续？',
      '确认注销',
      { type: 'warning', confirmButtonText: '确认注销', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await http.delete('/auth/account')
    ElMessage.success('账号已注销')
    userStore.logout()
    router.push('/login')
  } catch { ElMessage.error('账号注销失败，请稍后重试') }
}

onMounted(async () => {
  await loadProfile()
  try {
    const { data } = await http.get<ApiResponse<any>>('/admin/announcement')
    if (data.data) announcement.value = data.data
  } catch { announcement.value = null }
})
</script>

<style scoped>
.profile-page { max-width: 1000px; }
.info-card { border-radius: var(--radius-lg); margin-bottom: 20px; }
.profile-header {
  text-align: center; padding: 20px 0;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 16px;
}
.avatar-wrap {
  position: relative; display: inline-block; cursor: pointer; margin-bottom: 12px;
}
.avatar-wrap:hover .avatar-overlay { opacity: 1; }
.profile-avatar { background: linear-gradient(135deg, var(--accent-dark), var(--accent)); }
.avatar-overlay {
  position: absolute; inset: 0; border-radius: 50%;
  background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s; font-size: 24px; pointer-events: none;
}
.profile-header h3 { margin: 8px 0 6px; color: var(--text-primary); }
.status-row { display: flex; justify-content: center; margin-bottom: 4px; }
.info-table { margin-top: 12px; }
.quota-info { display: flex; align-items: center; gap: 8px; }
.quota-text { font-size: 12px; white-space: nowrap; min-width: 80px; color: var(--text-secondary); }
.action-card { border-radius: var(--radius-lg); margin-bottom: 20px; }
.announcement-content { padding: 8px 0; font-size: 14px; color: var(--text-primary); line-height: 1.8; white-space: pre-wrap; }
.announcement-time { font-size: 12px; color: var(--text-muted); margin-top: 8px; }
</style>
