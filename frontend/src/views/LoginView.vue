<template>
  <div class="login-page">
    <!-- ======== 背景层：纯CSS实现 ======== -->
    <div class="bg-layer">
      <!-- 细网格纹理 -->
      <div class="grid-texture"></div>
      <!-- 3个大光斑 -->
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-icon">
            <svg viewBox="0 0 64 64" fill="none" width="64" height="64">
              <rect width="64" height="64" rx="16" fill="url(#grad1)"/>
              <path d="M18 42V22l14 14 14-14v20" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              <defs>
                <linearGradient id="grad1" x1="0" y1="0" x2="64" y2="64">
                  <stop offset="0%" stop-color="#667eea"/>
                  <stop offset="100%" stop-color="#764ba2"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="brand-title">智能文案工作台</h1>
          <p class="brand-desc">基于多模态大模型的<br/>智能商品详情与营销文案生成系统</p>
          <div class="features">
            <div class="feat-item">
              <span class="feat-icon">🤖</span>
              <span>AI 商品描述生成</span>
            </div>
            <div class="feat-item">
              <span class="feat-icon">📝</span>
              <span>多平台营销文案</span>
            </div>
            <div class="feat-item">
              <span class="feat-icon">🎨</span>
              <span>Wan 2.7 详情图</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="form-panel">
        <div class="form-card">
          <div class="form-header">
            <div class="login-tabs">
              <button
                :class="['tab-btn', { active: !isAdminMode }]"
                @click="isAdminMode = false"
              >用户登录</button>
              <button
                :class="['tab-btn', { active: isAdminMode }]"
                @click="isAdminMode = true"
              >管理员登录</button>
            </div>
          </div>

          <p class="form-subtitle">
            {{ isAdminMode ? '管理员专用入口，请使用预置账号登录' : '登录您的账号，开始智能文案创作' }}
          </p>

          <el-form @submit.prevent="onSubmit" class="login-form">
            <el-form-item>
              <el-input
                v-model="username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                autocomplete="current-password"
              />
            </el-form-item>
            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              size="large"
              class="submit-btn"
              :class="{ 'admin-btn': isAdminMode }"
            >
              {{ isAdminMode ? '管理员登录' : '登 录' }}
            </el-button>
          </el-form>

          <div class="form-footer" v-if="!isAdminMode">
            <el-divider><span style="color:#999;font-size:12px;">还没有账号？</span></el-divider>
            <el-button link type="primary" @click="onRegister" class="register-link">立即注册</el-button>
          </div>
          <div class="form-footer" v-else>
            <p style="color:#999;font-size:12px;text-align:center;">
              管理员账号由系统预置，不可注册
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const username = ref('')
const password = ref('')
const loading = ref(false)
const isAdminMode = ref(false)

async function onSubmit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const { data } = await http.post<ApiResponse<any>>('/auth/login', {
      username: username.value,
      password: password.value,
      adminLogin: isAdminMode.value,
    })
    const d = data.data
    userStore.setSession({
      token: d.token,
      username: d.username,
      role: d.role,
      nickname: d.nickname,
      avatar: d.avatar || '',
      quotaTotal: d.quotaTotal ?? 0,
      quotaUsed: d.quotaUsed ?? 0,
    } as any)
    ElMessage.success(`欢迎回来，${d.nickname || d.username}！`)
    if (d.role === 'ADMIN') {
      router.push('/admin/dashboard')
    } else {
      router.push('/dashboard')
    }
  } catch { /* handled by interceptor */ } finally { loading.value = false }
}

async function onRegister() {
  const { value: regName } = await ElMessageBox.prompt('请输入用户名', '注册新账号', {
    inputPattern: /^[a-zA-Z0-9_一-龥]{2,20}$/,
    inputErrorMessage: '用户名2-20位，支持中英文、数字、下划线',
  }).catch(() => ({ value: null }))
  if (!regName) return

  const { value: regPwd } = await ElMessageBox.prompt('请输入密码（至少6位）', '设置密码', {
    inputType: 'password',
    inputPattern: /.{6,}/,
    inputErrorMessage: '密码至少6位',
  }).catch(() => ({ value: null }))
  if (!regPwd) return

  try {
    const { data } = await http.post<ApiResponse<any>>('/auth/register', {
      username: regName,
      password: regPwd,
      nickname: regName,
    })
    if (data.success) {
      ElMessage.success(data.data.message || '注册成功！请等待管理员审核')
    }
  } catch { /* handled by interceptor */ }
}
</script>

<style scoped>
/* ========== 页面容器 ========== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #0a0e1a;
}

/* ========== 背景层 ========== */
.bg-layer {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* 基底渐变：低饱和度藏蓝+靛紫+浅青 */
.bg-layer::before {
  content: '';
  position: absolute;
  inset: -20%;
  background:
    radial-gradient(ellipse 80% 60% at 20% 30%, rgba(62, 84, 172, 0.5) 0%, transparent 60%),
    radial-gradient(ellipse 60% 50% at 75% 20%, rgba(99, 78, 156, 0.4) 0%, transparent 55%),
    radial-gradient(ellipse 70% 50% at 50% 75%, rgba(45, 140, 168, 0.3) 0%, transparent 55%),
    linear-gradient(180deg, #0a0e1a 0%, #111836 40%, #0d1230 70%, #0a0e1a 100%);
}

/* 细网格纹理 */
.grid-texture {
  position: absolute;
  inset: 0;
  opacity: 0.04;
  background-image:
    linear-gradient(rgba(255,255,255,0.3) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.3) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* ========== 柔光大光斑（3个） ========== */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  will-change: transform, opacity;
}

/* 光斑1：藏蓝色，左上区域 */
.blob-1 {
  width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(92, 124, 224, 0.35) 0%, transparent 70%);
  top: -10%; left: 5%;
  animation: drift1 25s ease-in-out infinite, breathe1 8s ease-in-out infinite;
}
@keyframes drift1 {
  0%, 100% { transform: translate(0, 0); }
  25%  { transform: translate(120px, 60px); }
  50%  { transform: translate(40px, 140px); }
  75%  { transform: translate(-60px, 80px); }
}
@keyframes breathe1 {
  0%, 100% { opacity: 0.7; }
  50%  { opacity: 1; }
}

/* 光斑2：靛紫色，右侧中部 */
.blob-2 {
  width: 450px; height: 450px;
  background: radial-gradient(circle, rgba(130, 100, 200, 0.3) 0%, transparent 70%);
  top: 25%; right: -5%;
  animation: drift2 28s ease-in-out infinite, breathe2 10s ease-in-out infinite;
}
@keyframes drift2 {
  0%, 100% { transform: translate(0, 0); }
  33%  { transform: translate(-100px, 80px); }
  66%  { transform: translate(-30px, -60px); }
}
@keyframes breathe2 {
  0%, 100% { opacity: 0.6; }
  50%  { opacity: 0.9; }
}

/* 光斑3：浅青色，底部 */
.blob-3 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(70, 160, 180, 0.28) 0%, transparent 70%);
  bottom: -8%; left: 35%;
  animation: drift3 22s ease-in-out infinite, breathe3 9s ease-in-out infinite;
}
@keyframes drift3 {
  0%, 100% { transform: translate(0, 0); }
  20%  { transform: translate(80px, -40px); }
  50%  { transform: translate(-50px, -100px); }
  80%  { transform: translate(-90px, 20px); }
}
@keyframes breathe3 {
  0%, 100% { opacity: 0.5; }
  50%  { opacity: 0.85; }
}

/* ========== 登录容器（毛玻璃卡片） ========== */
.login-container {
  display: flex;
  width: 960px;
  max-width: 95vw;
  min-height: 560px;
  background: rgba(255,255,255,0.05);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.1);
  box-shadow: 0 32px 80px rgba(0,0,0,0.45), inset 0 1px 0 rgba(255,255,255,0.05);
  overflow: hidden;
  z-index: 1;
  animation: fadeInUp 0.8s ease-out;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ========== 左侧品牌面板 ========== */
.brand-panel {
  width: 420px;
  background: linear-gradient(135deg, rgba(102,126,234,0.25) 0%, rgba(118,75,162,0.25) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  border-right: 1px solid rgba(255,255,255,0.06);
}
.brand-content { text-align: center; color: #fff; }
.brand-icon {
  margin-bottom: 20px;
  filter: drop-shadow(0 8px 24px rgba(102,126,234,0.5));
}
.brand-title {
  font-size: 24px; font-weight: 700; margin: 0 0 12px; letter-spacing: 2px;
  background: linear-gradient(90deg, #d8e0ff, #c4b5fd);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.brand-desc {
  font-size: 14px; color: rgba(255,255,255,0.55); line-height: 1.8; margin: 0 0 32px;
}
.features { text-align: left; display: flex; flex-direction: column; gap: 14px; }
.feat-item { display: flex; align-items: center; gap: 12px; color: rgba(255,255,255,0.75); font-size: 14px; }
.feat-icon { font-size: 20px; }

/* ========== 右侧表单面板 ========== */
.form-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 48px 40px; }
.form-card { width: 100%; max-width: 360px; }
.form-header { margin-bottom: 8px; }
.login-tabs { display: flex; background: rgba(255,255,255,0.05); border-radius: 10px; padding: 4px; }
.tab-btn {
  flex: 1; padding: 10px 0; border: none; background: transparent;
  color: rgba(255,255,255,0.45); font-size: 14px; font-weight: 500;
  cursor: pointer; border-radius: 8px; transition: all 0.3s;
}
.tab-btn.active { background: rgba(102,126,234,0.45); color: #fff; box-shadow: 0 4px 12px rgba(102,126,234,0.25); }
.form-subtitle { color: rgba(255,255,255,0.4); font-size: 13px; margin: 16px 0 24px; text-align: center; }
.login-form { display: flex; flex-direction: column; gap: 4px; }
.login-form :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.06) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  border-radius: 10px !important; box-shadow: none !important;
}
.login-form :deep(.el-input__inner) { color: #fff !important; }
.login-form :deep(.el-input__inner::placeholder) { color: rgba(255,255,255,0.25) !important; }
.login-form :deep(.el-input__prefix) { color: rgba(255,255,255,0.35) !important; }

.submit-btn {
  width: 100%; height: 46px; margin-top: 16px; border-radius: 10px;
  font-size: 16px; font-weight: 600; letter-spacing: 4px;
  background: linear-gradient(135deg, #667eea, #764ba2); border: none;
  transition: transform 0.2s, box-shadow 0.2s;
}
.submit-btn:hover { background: linear-gradient(135deg, #7c94f5, #8c62b8); transform: translateY(-1px); box-shadow: 0 8px 24px rgba(102,126,234,0.35); }
.admin-btn { background: linear-gradient(135deg, #e67e22, #e74c3c); }
.admin-btn:hover { background: linear-gradient(135deg, #f0932b, #f05a4b); box-shadow: 0 8px 24px rgba(230,126,34,0.35); }

.form-footer { margin-top: 24px; }
.register-link { display: block; margin: 0 auto; }

/* 移动端适配 */
@media (max-width: 768px) {
  .brand-panel { display: none; }
  .login-container { width: 100%; min-height: auto; }
  .form-panel { padding: 32px 24px; }
}
</style>
