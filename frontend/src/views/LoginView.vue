<template>
  <div class="login-page">
    <!-- Background layer -->
    <div class="bg-layer">
      <div class="grid-texture"></div>
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="blob blob-4"></div>
      <div v-for="i in 20" :key="i" class="sparkle" :style="sparkleStyle(i)"></div>
    </div>

    <div class="login-container">
      <!-- Brand panel with carousel -->
      <div class="brand-panel">
        <!-- Carousel slides -->
        <div class="carousel-track">
          <div
            v-for="(slide, idx) in slides"
            :key="slide.id"
            :class="['carousel-slide', { active: currentSlide === idx }]"
            :style="slide.bgStyle"
          >
            <div class="slide-visual">
              <div :class="['slide-illustration', 'illus-' + slide.visualClass]">
                <div class="illus-shape shape-1"></div>
                <div class="illus-shape shape-2"></div>
                <div class="illus-shape shape-3"></div>
                <div class="illus-shape shape-4"></div>
                <span class="illus-emoji">{{ slide.emoji }}</span>
              </div>
            </div>
            <div class="slide-caption">
              <span class="slide-badge">{{ slide.badge }}</span>
              <h3>{{ slide.title }}</h3>
              <p>{{ slide.desc }}</p>
            </div>
          </div>
        </div>

        <!-- Overlay brand info -->
        <div class="brand-overlay">
          <div class="brand-mark">
            <svg viewBox="0 0 64 64" fill="none" width="48" height="48">
              <rect width="64" height="64" rx="16" fill="url(#brandGrad)"/>
              <path d="M18 42V22l14 14 14-14v20" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
              <defs>
                <linearGradient id="brandGrad" x1="0" y1="0" x2="64" y2="64">
                  <stop offset="0%" stop-color="#e2c278"/>
                  <stop offset="100%" stop-color="#d4a853"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="brand-title">CraftWrite</h1>
          <p class="brand-tagline">智能文案工作台</p>
        </div>

        <!-- Slide indicators -->
        <div class="slide-dots">
          <button
            v-for="(_, idx) in slides"
            :key="idx"
            :class="['slide-dot', { active: currentSlide === idx }]"
            @click="goToSlide(idx)"
          ></button>
        </div>
      </div>

      <!-- Form panel -->
      <div class="form-panel">
        <div class="form-card">
          <div class="login-tabs">
            <button :class="['tab-btn', { active: !isAdminMode }]" @click="isAdminMode = false">
              用户登录
            </button>
            <button :class="['tab-btn', { active: isAdminMode }]" @click="isAdminMode = true">
              管理员
            </button>
          </div>

          <p class="form-subtitle">
            {{ isAdminMode ? '管理员专用入口' : '登录账号，开始智能创作' }}
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
              :class="{ 'is-admin': isAdminMode }"
            >
              <span v-if="!loading">{{ isAdminMode ? '管理员登录' : '登 录' }}</span>
            </el-button>
          </el-form>

          <div class="form-footer" v-if="!isAdminMode">
            <el-divider><span class="divider-label">还没有账号？</span></el-divider>
            <el-button link type="primary" @click="onRegister" class="register-link">立即注册</el-button>
          </div>
          <div class="form-footer" v-else>
            <p class="admin-hint">管理员账号由系统预置，不可注册</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
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

// ---- Carousel ----
const currentSlide = ref(0)
let slideTimer: ReturnType<typeof setInterval> | null = null

const slides = [
  {
    id: 1,
    bgStyle: 'background: linear-gradient(160deg, #141e30 0%, #1a2740 50%, #1a3360 100%);',
    visualClass: 'vision',
    emoji: '🔍',
    badge: 'AI 视觉识别',
    title: '智能解析商品图片',
    desc: '上传实拍图，AI 自动识别商品属性、材质与风格',
  },
  {
    id: 2,
    bgStyle: 'background: linear-gradient(160deg, #1b1228 0%, #2d1840 50%, #452050 100%);',
    visualClass: 'platform',
    emoji: '📱',
    badge: '多平台营销',
    title: '一键生成带货文案',
    desc: '小红书 / 淘宝 / 抖音 三平台风格，场景模板随心切换',
  },
  {
    id: 3,
    bgStyle: 'background: linear-gradient(160deg, #0d1f28 0%, #183a42 50%, #1f4658 100%);',
    visualClass: 'image',
    emoji: '🎨',
    badge: 'AI 详情图',
    title: '电商级商品详情图',
    desc: 'Wan 2.7 图像生成，场景展示与功能说明一站式完成',
  },
  {
    id: 4,
    bgStyle: 'background: linear-gradient(160deg, #1b1a14 0%, #2d2218 50%, #3d2818 100%);',
    visualClass: 'compliance',
    emoji: '🛡️',
    badge: '智能合规',
    title: '广告法自动审查',
    desc: '内置合规检查，杜绝极限词与虚假宣传风险',
  },
  {
    id: 5,
    bgStyle: 'background: linear-gradient(160deg, #161e28 0%, #1e2840 50%, #1a3040 100%);',
    visualClass: 'pipeline',
    emoji: '⚡',
    badge: '工作流引擎',
    title: '一键组合流水线',
    desc: '描述 → 文案 → 出图，三步串联，实时进度一目了然',
  },
]

function goToSlide(idx: number) {
  currentSlide.value = idx
  resetTimer()
}

function nextSlide() {
  currentSlide.value = (currentSlide.value + 1) % slides.length
}

function resetTimer() {
  if (slideTimer) clearInterval(slideTimer)
  slideTimer = setInterval(nextSlide, 4500)
}

onMounted(resetTimer)
onUnmounted(() => { if (slideTimer) clearInterval(slideTimer) })

function sparkleStyle(i: number) {
  const x = Math.random() * 100
  const y = Math.random() * 100
  const size = 2 + Math.random() * 4
  const dur = 3 + Math.random() * 6
  const delay = Math.random() * 6
  return {
    left: x + '%',
    top: y + '%',
    width: size + 'px',
    height: size + 'px',
    animationDuration: dur + 's',
    animationDelay: delay + 's',
    opacity: 0.3 + Math.random() * 0.5,
  }
}

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
    if (d.role === 'ADMIN') router.push('/admin/dashboard')
    else router.push('/dashboard')
  } catch { ElMessage.error('登录失败，请检查账号密码') } finally { loading.value = false }
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
    const { data } = await http.post<ApiResponse<any>>('/auth/register', { username: regName, password: regPwd, nickname: regName })
    if (data.success) ElMessage.success(data.data.message || '注册成功！请等待管理员审核')
  } catch { ElMessage.error('注册失败，用户名可能已被占用或已达注册上限') }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-root);
}

/* ===== Background ===== */
.bg-layer { position: fixed; inset: 0; z-index: 0; pointer-events: none; }

.bg-layer::before {
  content: '';
  position: absolute; inset: -20%;
  background:
    radial-gradient(ellipse 80% 60% at 20% 30%, rgba(196,155,74,0.1) 0%, transparent 60%),
    radial-gradient(ellipse 60% 50% at 75% 20%, rgba(91,138,138,0.08) 0%, transparent 55%),
    radial-gradient(ellipse 70% 50% at 50% 75%, rgba(201,122,122,0.06) 0%, transparent 55%),
    linear-gradient(180deg, var(--bg-root) 0%, #f0ede6 40%, #ede9e1 70%, var(--bg-root) 100%);
}

.grid-texture {
  position: absolute; inset: 0; opacity: 0.03;
  background-image:
    linear-gradient(rgba(0,0,0,0.2) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.2) 1px, transparent 1px);
  background-size: 50px 50px;
}

/* Blobs — refined tones */
.blob {
  position: absolute; border-radius: 50%; filter: blur(120px);
  will-change: transform, opacity;
}
.blob-1 {
  width: 550px; height: 550px;
  background: radial-gradient(circle, rgba(196,155,74,0.1) 0%, transparent 70%);
  top: -15%; left: 0%;
  animation: drift1 25s ease-in-out infinite, breathe1 8s ease-in-out infinite;
}
.blob-2 {
  width: 480px; height: 480px;
  background: radial-gradient(circle, rgba(91,138,138,0.08) 0%, transparent 70%);
  top: 20%; right: -8%;
  animation: drift2 28s ease-in-out infinite, breathe2 10s ease-in-out infinite;
}
.blob-3 {
  width: 420px; height: 420px;
  background: radial-gradient(circle, rgba(201,122,122,0.06) 0%, transparent 70%);
  bottom: -10%; left: 30%;
  animation: drift3 22s ease-in-out infinite, breathe3 9s ease-in-out infinite;
}
.blob-4 {
  width: 350px; height: 350px;
  background: radial-gradient(circle, rgba(196,155,74,0.06) 0%, transparent 70%);
  top: 45%; left: 50%;
  animation: drift4 20s ease-in-out infinite, breathe2 7s ease-in-out infinite;
}

@keyframes drift1 {
  0%,100% { transform: translate(0,0); }
  25% { transform: translate(130px,70px); }
  50% { transform: translate(50px,150px); }
  75% { transform: translate(-70px,90px); }
}
@keyframes drift2 {
  0%,100% { transform: translate(0,0); }
  33% { transform: translate(-110px,90px); }
  66% { transform: translate(-40px,-70px); }
}
@keyframes drift3 {
  0%,100% { transform: translate(0,0); }
  20% { transform: translate(90px,-50px); }
  50% { transform: translate(-60px,-110px); }
  80% { transform: translate(-100px,30px); }
}
@keyframes drift4 {
  0%,100% { transform: translate(0,0); }
  50% { transform: translate(-80px,-60px); }
}
@keyframes breathe1 {
  0%,100% { opacity: 0.35; }
  50% { opacity: 0.65; }
}
@keyframes breathe2 {
  0%,100% { opacity: 0.28; }
  50% { opacity: 0.55; }
}
@keyframes breathe3 {
  0%,100% { opacity: 0.22; }
  50% { opacity: 0.5; }
}

/* Sparkles */
.sparkle {
  position: absolute; border-radius: 50%;
  background: rgba(196,155,74,0.5);
  animation: sparkleFloat linear infinite;
  box-shadow: 0 0 6px 2px rgba(196,155,74,0.18);
}
@keyframes sparkleFloat {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: var(--sparkle-opacity, 0.6); }
  90% { opacity: var(--sparkle-opacity, 0.6); }
  100% { transform: translateY(-100vh) scale(0.3); opacity: 0; }
}

/* ===== Container ===== */
.login-container {
  display: flex;
  width: 980px; max-width: 95vw; min-height: 580px;
  background: var(--bg-surface);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 28px;
  border: 1px solid var(--border-subtle);
  box-shadow:
    0 40px 100px rgba(0,0,0,0.08),
    0 0 1px rgba(0,0,0,0.04) inset,
    0 0 60px rgba(196,155,74,0.03);
  overflow: hidden;
  z-index: 1;
  animation: fadeInUp 0.9s cubic-bezier(0.16,1,0.3,1);
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(40px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== Brand Panel with Carousel ===== */
.brand-panel {
  width: 420px;
  position: relative; overflow: hidden;
  border-right: 1px solid var(--border-subtle);
  background: #f0ede6;
}

/* Carousel track */
.carousel-track {
  position: absolute; inset: 0;
}
.carousel-slide {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; justify-content: flex-end;
  padding: 80px 36px 48px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.9s cubic-bezier(0.4, 0, 0.2, 1);
}
.carousel-slide.active {
  opacity: 1;
  pointer-events: auto;
}
.carousel-slide::before {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(0deg, rgba(240,237,230,0.95) 0%, rgba(240,237,230,0.4) 50%, rgba(240,237,230,0.15) 100%);
}
.slide-visual {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.slide-image {
  width: 100%; height: 100%;
  object-fit: cover;
  opacity: 0.4;
}

/* ===== CSS Illustrations ===== */
.slide-illustration {
  position: relative;
  width: 220px; height: 220px;
  display: flex; align-items: center; justify-content: center;
}

.illus-emoji {
  font-size: 72px;
  position: relative; z-index: 3;
  filter: drop-shadow(0 8px 24px rgba(0,0,0,0.25));
  animation: illusFloat 5s ease-in-out infinite;
}
@keyframes illusFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.illus-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.18;
  animation: shapeDrift 8s ease-in-out infinite;
}
.shape-1 { animation-delay: 0s; }
.shape-2 { animation-delay: -2s; }
.shape-3 { animation-delay: -4s; }
.shape-4 { animation-delay: -6s; }

@keyframes shapeDrift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(15px, -20px) scale(1.08); }
  50% { transform: translate(-10px, -5px) scale(0.94); }
  75% { transform: translate(-15px, 15px) scale(1.04); }
}

/* Illus 1 — Vision (rings / lens) */
.illus-vision .shape-1 {
  width: 180px; height: 180px;
  border: 3px solid rgba(226,194,120,0.4);
  background: radial-gradient(circle, rgba(226,194,120,0.15) 0%, transparent 70%);
  top: 20px; left: 20px;
}
.illus-vision .shape-2 {
  width: 120px; height: 120px;
  border: 2px solid rgba(91,200,200,0.35);
  background: radial-gradient(circle, rgba(91,200,200,0.1) 0%, transparent 70%);
  top: 50px; left: 50px;
}
.illus-vision .shape-3 {
  width: 60px; height: 60px;
  background: radial-gradient(circle, rgba(226,194,120,0.3) 0%, transparent 70%);
  top: 80px; left: 80px;
}
.illus-vision .shape-4 {
  width: 16px; height: 16px;
  background: rgba(226,194,120,0.7);
  top: 102px; left: 102px;
  box-shadow: 0 0 20px rgba(226,194,120,0.5);
}

/* Illus 2 — Platform (three overlapping cards) */
.illus-platform .shape-1 {
  width: 110px; height: 140px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(255,140,140,0.25), rgba(255,100,120,0.1));
  border: 1px solid rgba(255,140,140,0.2);
  top: 30px; left: 20px;
  transform: rotate(-8deg);
}
.illus-platform .shape-2 {
  width: 110px; height: 140px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(226,194,120,0.25), rgba(200,160,80,0.1));
  border: 1px solid rgba(226,194,120,0.2);
  top: 40px; left: 55px;
  transform: rotate(3deg);
}
.illus-platform .shape-3 {
  width: 110px; height: 140px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(91,200,200,0.25), rgba(70,160,160,0.1));
  border: 1px solid rgba(91,200,200,0.2);
  top: 35px; left: 90px;
  transform: rotate(10deg);
}
.illus-platform .shape-4 { display: none; }
.illus-platform .illus-emoji { font-size: 56px; }

/* Illus 3 — Image gen (canvas / frame with landscape) */
.illus-image .shape-1 {
  width: 170px; height: 130px; border-radius: 14px;
  background: linear-gradient(180deg, rgba(91,180,220,0.2) 0%, rgba(70,140,170,0.15) 40%, rgba(60,180,140,0.12) 70%, rgba(200,160,100,0.08) 100%);
  border: 2px solid rgba(255,255,255,0.15);
  top: 30px; left: 25px;
}
.illus-image .shape-2 {
  width: 50px; height: 50px; border-radius: 50%;
  background: radial-gradient(circle, rgba(255,200,100,0.35) 0%, transparent 70%);
  top: 28px; left: 140px;
}
.illus-image .shape-3 {
  width: 80px; height: 6px; border-radius: 3px;
  background: rgba(255,255,255,0.1);
  top: 110px; left: 60px;
}
.illus-image .shape-4 { display: none; }

/* Illus 4 — Compliance (shield + check) */
.illus-compliance .shape-1 {
  width: 130px; height: 150px; border-radius: 20px 20px 30px 30px;
  background: linear-gradient(180deg, rgba(100,200,130,0.2) 0%, rgba(80,170,110,0.1) 100%);
  border: 2px solid rgba(100,200,130,0.3);
  top: 25px; left: 45px;
}
.illus-compliance .shape-2 {
  width: 50px; height: 4px; border-radius: 2px;
  background: rgba(100,200,130,0.5);
  top: 95px; left: 85px;
  transform: rotate(-45deg);
}
.illus-compliance .shape-3 {
  width: 70px; height: 4px; border-radius: 2px;
  background: rgba(100,200,130,0.5);
  top: 100px; left: 75px;
  transform: rotate(45deg);
}
.illus-compliance .shape-4 {
  width: 90px; height: 90px; border-radius: 50%;
  background: radial-gradient(circle, rgba(100,200,130,0.08) 0%, transparent 70%);
  top: 55px; left: 65px;
}
.illus-compliance .illus-emoji { font-size: 60px; }

/* Illus 5 — Pipeline (connected nodes) */
.illus-pipeline .shape-1 {
  width: 56px; height: 56px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(226,194,120,0.35), rgba(200,160,80,0.15));
  border: 1px solid rgba(226,194,120,0.3);
  top: 55px; left: 10px;
}
.illus-pipeline .shape-2 {
  width: 56px; height: 56px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(91,200,200,0.35), rgba(70,160,160,0.15));
  border: 1px solid rgba(91,200,200,0.3);
  top: 55px; left: 82px;
}
.illus-pipeline .shape-3 {
  width: 56px; height: 56px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(255,140,140,0.35), rgba(220,100,120,0.15));
  border: 1px solid rgba(255,140,140,0.3);
  top: 55px; left: 154px;
}
.illus-pipeline .shape-4 {
  width: 150px; height: 3px;
  background: linear-gradient(90deg, rgba(226,194,120,0.3), rgba(91,200,200,0.3), rgba(255,140,140,0.3));
  top: 83px; left: 35px;
  border-radius: 2px;
}
.illus-pipeline .illus-emoji { font-size: 48px; }
.slide-caption {
  position: relative; z-index: 2;
}
.slide-badge {
  display: inline-block;
  font-size: 10px; font-weight: 600; letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--accent);
  background: rgba(196,155,74,0.08);
  border: 1px solid rgba(196,155,74,0.18);
  padding: 4px 10px; border-radius: 20px;
  margin-bottom: 14px;
}
.slide-caption h3 {
  font-family: var(--font-display);
  font-size: 22px; font-weight: 700; color: var(--text-primary);
  margin: 0 0 8px; letter-spacing: 1px;
}
.slide-caption p {
  font-size: 13px; color: var(--text-secondary);
  margin: 0; line-height: 1.6;
}

/* Brand overlay */
.brand-overlay {
  position: absolute; top: 0; left: 0; right: 0;
  padding: 32px 36px 0;
  z-index: 10;
  display: flex; align-items: center; gap: 12px;
  pointer-events: none;
}
.brand-mark {
  filter: drop-shadow(0 4px 16px rgba(212,168,83,0.4));
}
.brand-title {
  font-family: var(--font-display);
  font-size: 20px; font-weight: 700;
  color: var(--accent-light);
  margin: 0;
  letter-spacing: 1px;
}
.brand-tagline {
  font-size: 10px;
  color: var(--text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
  margin: 0;
}

/* Slide indicators */
.slide-dots {
  position: absolute; bottom: 28px; right: 32px;
  z-index: 10; display: flex; gap: 8px;
}
.slide-dot {
  width: 6px; height: 6px; border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.2);
  background: transparent;
  cursor: pointer; padding: 0;
  transition: all 0.3s var(--ease-out);
}
.slide-dot.active {
  background: var(--accent);
  border-color: var(--accent);
  box-shadow: 0 0 8px var(--accent-glow);
  width: 20px; border-radius: 3px;
}
.slide-dot:hover:not(.active) {
  border-color: rgba(0,0,0,0.35);
}

/* ===== Form Panel ===== */
.form-panel { flex: 1; display: flex; align-items: center; justify-content: center; padding: 48px 44px; }
.form-card { width: 100%; max-width: 380px; }

.login-tabs {
  display: flex; background: rgba(0,0,0,0.04);
  border-radius: 12px; padding: 4px; gap: 2px;
  border: 1px solid var(--border-subtle);
}
.tab-btn {
  flex: 1; padding: 10px 0; border: none; background: transparent;
  color: var(--text-muted); font-size: 14px; font-weight: 500;
  cursor: pointer; border-radius: 10px;
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
}
.tab-btn.active {
  background: rgba(196,155,74,0.15);
  color: var(--accent);
  box-shadow: 0 4px 20px rgba(196,155,74,0.12);
}
.tab-btn:hover:not(.active) { color: var(--text-secondary); }

.form-subtitle {
  color: var(--text-muted); font-size: 13px;
  margin: 20px 0 28px; text-align: center;
}

.login-form { display: flex; flex-direction: column; gap: 4px; }

.login-form :deep(.el-input__wrapper) {
  background: rgba(0,0,0,0.02) !important;
  border: 1px solid var(--border-default) !important;
  border-radius: 12px !important; box-shadow: none !important;
  transition: all 0.35s !important;
}
.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--border-strong) !important;
  background: rgba(0,0,0,0.04) !important;
}
.login-form :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: rgba(196,155,74,0.4) !important;
  box-shadow: 0 0 20px rgba(196,155,74,0.08), 0 0 0 3px rgba(196,155,74,0.04) !important;
  background: rgba(0,0,0,0.03) !important;
}
.login-form :deep(.el-input__inner) { color: var(--text-primary) !important; }
.login-form :deep(.el-input__inner::placeholder) { color: var(--text-muted) !important; }
.login-form :deep(.el-input__prefix) { color: var(--text-muted) !important; }
.login-form :deep(.el-input.is-focus .el-input__prefix) { color: var(--accent) !important; }

.submit-btn {
  width: 100%; height: 50px; margin-top: 20px; border-radius: 12px;
  font-size: 16px; font-weight: 700; letter-spacing: 4px;
  border: none;
  background: linear-gradient(135deg, #c49b4a, #a37b2e) !important;
  transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
  position: relative; overflow: hidden;
}
.submit-btn::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), transparent);
  opacity: 0; transition: opacity 0.3s;
}
.submit-btn:hover::after { opacity: 1; }
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(196,155,74,0.35);
}
.submit-btn:active { transform: translateY(0); }
.submit-btn.is-admin {
  background: linear-gradient(135deg, #d4726a, #b85a52) !important;
}
.submit-btn.is-admin:hover { box-shadow: 0 12px 35px rgba(212,114,106,0.35); }

.form-footer { margin-top: 28px; }
.divider-label { color: var(--text-muted); font-size: 12px; }
.register-link { display: block; margin: 0 auto; transition: opacity 0.3s; color: var(--accent) !important; }
.register-link:hover { opacity: 0.8; }
.admin-hint { color: var(--text-muted); font-size: 12px; text-align: center; }

@media (max-width: 768px) {
  .brand-panel { display: none; }
  .login-container { width: 100%; min-height: auto; border-radius: 20px; }
  .form-panel { padding: 36px 28px; }
}
</style>
