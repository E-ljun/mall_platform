<template>
  <div class="app-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <!-- Sidebar -->
    <aside class="sidebar">
      <!-- Subtle animated accent line -->
      <div class="sidebar-accent"></div>

      <!-- Logo -->
      <div class="logo-block" @click="$router.push(userStore.isAdmin ? '/admin/dashboard' : '/dashboard')">
        <div class="logo-mark">
          <svg viewBox="0 0 40 40" fill="none" width="36" height="36">
            <rect width="40" height="40" rx="10" fill="url(#logoGrad)"/>
            <path d="M11 26V14l9 9 9-9v12" stroke="#ffffff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="40" y2="40">
                <stop offset="0%" stop-color="#d9ba78"/>
                <stop offset="100%" stop-color="#c8a45c"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="logo-text">
          <span class="logo-title">CraftWrite</span>
          <span class="logo-sub">智能文案工作台</span>
        </div>
      </div>

      <!-- Collapse toggle -->
      <button class="collapse-toggle" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <polyline v-if="!sidebarCollapsed" points="15 18 9 12 15 6" />
          <polyline v-else points="9 18 15 12 9 6" />
        </svg>
      </button>

      <!-- Menu -->
      <nav class="nav-section">
        <!-- User menu -->
        <template v-if="!userStore.isAdmin">
          <div class="nav-group-label">工作区</div>
          <router-link to="/dashboard" class="nav-item" :class="{ active: route.path === '/dashboard' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
            </span>
            <span class="nav-label">工作台</span>
            <span class="nav-shortcut" v-if="route.path === '/dashboard'">⌘1</span>
          </router-link>
          <router-link to="/products" class="nav-item" :class="{ active: route.path.startsWith('/products') }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
            </span>
            <span class="nav-label">我的商品</span>
          </router-link>
          <router-link to="/library" class="nav-item" :class="{ active: route.path === '/library' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
            </span>
            <span class="nav-label">文案库</span>
          </router-link>
          <router-link to="/guide" class="nav-item" :class="{ active: route.path === '/guide' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="2" y="2" width="20" height="20" rx="2.5"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
            </span>
            <span class="nav-label">AI 出图指南</span>
          </router-link>
          <router-link to="/recycle-bin" class="nav-item" :class="{ active: route.path === '/recycle-bin' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </span>
            <span class="nav-label">回收站</span>
          </router-link>

          <div class="nav-group-label" style="margin-top:20px;">账户</div>
          <router-link to="/profile" class="nav-item" :class="{ active: route.path === '/profile' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="8" r="4"/><path d="M20 21a8 8 0 1 0-16 0"/></svg>
            </span>
            <span class="nav-label">个人中心</span>
          </router-link>
        </template>

        <!-- Admin menu -->
        <template v-else>
          <div class="nav-group-label">管理</div>
          <router-link to="/admin/dashboard" class="nav-item" :class="{ active: route.path === '/admin/dashboard' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
            </span>
            <span class="nav-label">管理概览</span>
          </router-link>
          <router-link to="/admin/users" class="nav-item" :class="{ active: route.path === '/admin/users' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="8" r="4"/><path d="M20 21a8 8 0 1 0-16 0"/></svg>
            </span>
            <span class="nav-label">用户管理</span>
          </router-link>
          <router-link to="/admin/products" class="nav-item" :class="{ active: route.path === '/admin/products' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
            </span>
            <span class="nav-label">商品管理</span>
          </router-link>
          <router-link to="/admin/stats" class="nav-item" :class="{ active: route.path === '/admin/stats' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            </span>
            <span class="nav-label">数据分析</span>
          </router-link>
          <router-link to="/admin/recycle-bin" class="nav-item" :class="{ active: route.path === '/admin/recycle-bin' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </span>
            <span class="nav-label">回收站</span>
          </router-link>

          <div class="nav-group-label" style="margin-top:20px;">账户</div>
          <router-link to="/profile" class="nav-item" :class="{ active: route.path === '/profile' }">
            <span class="nav-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
            </span>
            <span class="nav-label">账号设置</span>
          </router-link>
        </template>
      </nav>

      <!-- Sidebar footer -->
      <div class="sidebar-footer">
        <div class="footer-quota" v-if="!userStore.isAdmin && userStore.quotaRemaining !== Infinity">
          <div class="quota-mini-ring">
            <svg viewBox="0 0 36 36" width="36" height="36">
              <circle cx="18" cy="18" r="14" fill="none" stroke="rgba(0,0,0,0.06)" stroke-width="3"/>
              <circle
                cx="18" cy="18" r="14" fill="none" stroke="var(--accent)"
                stroke-width="3" stroke-linecap="round"
                :stroke-dasharray="88" :stroke-dashoffset="88 - 88 * quotaPercent"
                transform="rotate(-90 18 18)"
                style="transition: stroke-dashoffset 0.8s var(--ease-out)"
              />
            </svg>
            <span class="quota-mini-num">{{ userStore.quotaRemaining }}</span>
          </div>
          <span class="footer-quota-label">剩余配额</span>
        </div>
      </div>
    </aside>

    <!-- Main area -->
    <div class="main-area">
      <!-- Header -->
      <header class="top-header">
        <div class="header-breadcrumb">
          <template v-for="(item, idx) in breadcrumbs" :key="idx">
            <span class="breadcrumb-sep" v-if="idx > 0">/</span>
            <router-link
              v-if="idx < breadcrumbs.length - 1"
              :to="item.path"
              class="breadcrumb-link"
            >{{ item.title }}</router-link>
            <span v-else class="breadcrumb-current">{{ item.title }}</span>
          </template>
        </div>
        <div class="header-actions">
          <span v-if="!userStore.isAdmin && userStore.quotaRemaining !== Infinity" class="quota-chip">
            剩余 <strong>{{ userStore.quotaRemaining }}</strong> 次
          </span>
          <div class="user-chip" @click="$router.push('/profile')">
            <span class="user-avatar" v-if="userStore.avatar">
              <img :src="userStore.avatar" class="avatar-img" alt="avatar" />
            </span>
            <span class="user-avatar" v-else>
              {{ (userStore.nickname || userStore.username).charAt(0) }}
            </span>
            <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
            <span class="user-role-dot" :class="{ admin: userStore.isAdmin }"></span>
          </div>
          <button class="logout-btn" @click="onLogout" title="退出登录">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </button>
        </div>
      </header>

      <!-- Page content with transition -->
      <main class="content-area dot-grid">
        <div class="bg-particles" aria-hidden="true">
          <span v-for="i in 12" :key="i" class="bg-particle" :style="particleStyle(i)"></span>
        </div>
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ---- Sidebar collapse ----
const sidebarCollapsed = ref(false)

onMounted(() => {
  const saved = localStorage.getItem('sidebarCollapsed')
  if (saved !== null) sidebarCollapsed.value = saved === 'true'
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
}

watch(() => route.path, () => {
  if (!userStore.isAdmin) userStore.refreshQuota()
})

const quotaPercent = computed(() =>
  userStore.quotaTotal > 0 ? Math.min(1, (userStore.quotaTotal - userStore.quotaUsed) / userStore.quotaTotal) : 1
)

const breadcrumbs = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '工作台',
    '/products': '我的商品',
    '/profile': '个人中心',
    '/library': '文案库',
    '/guide': 'AI 出图指南',
    '/recycle-bin': '回收站',
    '/admin/dashboard': '管理概览',
    '/admin/users': '用户管理',
    '/admin/products': '商品管理',
    '/admin/stats': '数据分析',
    '/admin/recycle-bin': '回收站',
  }
  const items: { title: string; path: string }[] = [{ title: '首页', path: '/' }]
  const title = map[route.path]
  if (title) items.push({ title, path: route.path })
  if (route.path.startsWith('/products/') && route.path.endsWith('/edit')) {
    items.push({ title: '商品编辑', path: route.path })
  }
  return items
})

function particleStyle(i: number) {
  const x = Math.random() * 100
  const y = Math.random() * 100
  const size = 1 + Math.random() * 3
  const dur = 8 + Math.random() * 12
  const delay = Math.random() * 12
  return {
    left: x + '%',
    top: y + '%',
    width: size + 'px',
    height: size + 'px',
    animationDuration: dur + 's',
    animationDelay: delay + 's',
    opacity: 0.08 + Math.random() * 0.12,
  }
}

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* ===== Shell ===== */
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--bg-root);
}

/* ===== Sidebar ===== */
.sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
  transition: width 0.35s var(--ease-out);
}

.sidebar-collapsed .sidebar {
  width: var(--sidebar-collapsed-width);
}

/* Animated accent line on left edge */
.sidebar-accent {
  position: absolute;
  left: 0;
  top: 0;
  width: 2px;
  height: 100%;
  background: linear-gradient(180deg,
    transparent 0%,
    var(--accent) 15%,
    var(--accent-teal) 50%,
    var(--accent) 85%,
    transparent 100%
  );
  opacity: 0.6;
  animation: accentPulse 4s ease-in-out infinite;
}
@keyframes accentPulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

/* Logo */
.logo-block {
  padding: 24px 20px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-subtle);
  transition: all 0.35s var(--ease-out);
}
.sidebar-collapsed .logo-block {
  padding: 20px 14px;
  justify-content: center;
}
.logo-block:hover { opacity: 0.85; }
.logo-mark {
  flex-shrink: 0;
  filter: drop-shadow(0 4px 12px rgba(200, 164, 92, 0.35));
  animation: float 5s ease-in-out infinite;
  transition: transform 0.35s var(--ease-out);
}
.sidebar-collapsed .logo-mark {
  transform: scale(0.85);
}
.logo-text {
  display: flex; flex-direction: column; gap: 1px;
  transition: opacity 0.25s var(--ease-out), width 0.35s var(--ease-out);
  overflow: hidden; white-space: nowrap;
}
.sidebar-collapsed .logo-text {
  opacity: 0; width: 0;
}
.logo-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: 0.5px;
}
.logo-sub {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* Collapse toggle */
.collapse-toggle {
  position: absolute;
  right: -12px;
  top: 28px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--border-default);
  background: var(--bg-elevated);
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
  transition: all 0.25s var(--ease-out);
  padding: 0;
}
.collapse-toggle:hover {
  color: var(--accent);
  border-color: var(--accent);
  box-shadow: 0 0 12px var(--accent-glow);
}

/* Navigation */
.nav-section {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 0;
}
.nav-group-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: var(--text-muted);
  padding: 8px 20px 6px;
  transition: opacity 0.25s var(--ease-out), padding 0.35s var(--ease-out);
  overflow: hidden; white-space: nowrap;
}
.sidebar-collapsed .nav-group-label {
  opacity: 0; padding: 4px 0;
  text-align: center; font-size: 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin: 1px 10px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 500;
  transition: all 0.25s var(--ease-out);
  position: relative;
  overflow: hidden; white-space: nowrap;
}
.sidebar-collapsed .nav-item {
  justify-content: center;
  padding: 10px 8px;
  margin: 1px 10px;
}
.nav-item:hover {
  color: var(--text-primary);
  background: rgba(0, 0, 0, 0.04);
}
.nav-item.active {
  color: var(--accent);
  background: rgba(196, 155, 74, 0.08);
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: -2px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--accent);
  border-radius: 0 3px 3px 0;
}
.nav-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.6;
  transition: opacity 0.25s var(--ease-out);
}
.nav-item:hover .nav-icon,
.nav-item.active .nav-icon { opacity: 1; }
.nav-label {
  flex: 1;
  transition: opacity 0.25s var(--ease-out), width 0.35s var(--ease-out);
  overflow: hidden;
}
.sidebar-collapsed .nav-label {
  opacity: 0; width: 0;
}
.nav-shortcut {
  font-size: 10px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  transition: opacity 0.25s;
}
.sidebar-collapsed .nav-shortcut {
  opacity: 0;
}

/* Sidebar footer */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-subtle);
  transition: opacity 0.25s var(--ease-out), padding 0.35s var(--ease-out);
  overflow: hidden;
}
.sidebar-collapsed .sidebar-footer {
  opacity: 0; padding: 12px 8px;
}
.sidebar-collapsed .footer-quota {
  justify-content: center;
}
.sidebar-collapsed .footer-quota-label {
  display: none;
}
.footer-quota {
  display: flex;
  align-items: center;
  gap: 10px;
}
.quota-mini-ring {
  position: relative;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}
.quota-mini-num {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: var(--text-primary);
}
.footer-quota-label {
  font-size: 11px;
  color: var(--text-muted);
}

/* ===== Main Area ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* Header */
.top-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(248, 246, 242, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-subtle);
  position: sticky;
  top: 0;
  z-index: 5;
}
.header-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.breadcrumb-sep {
  color: var(--text-muted);
  font-size: 11px;
}
.breadcrumb-link {
  color: var(--text-muted);
  text-decoration: none;
  transition: color 0.2s;
}
.breadcrumb-link:hover { color: var(--accent); }
.breadcrumb-current {
  color: var(--text-primary);
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quota-chip {
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(196, 155, 74, 0.06);
  border: 1px solid rgba(196, 155, 74, 0.15);
  padding: 4px 12px;
  border-radius: 20px;
}
.quota-chip strong {
  color: var(--accent);
  font-size: 13px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 6px;
  border-radius: 24px;
  transition: background 0.2s;
}
.user-chip:hover {
  background: rgba(0, 0, 0, 0.04);
}
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-dark), var(--accent));
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.user-name {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}
.user-role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-teal);
}
.user-role-dot.admin { background: var(--coral); }

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}
.logout-btn:hover {
  background: rgba(212, 114, 106, 0.1);
  color: var(--coral);
}

/* Content */
.content-area {
  flex: 1;
  padding: 28px;
  position: relative;
  overflow-y: auto;
}

/* Page transitions */
.page-enter-active {
  animation: pageIn 0.4s var(--ease-out);
}
.page-leave-active {
  animation: pageOut 0.2s var(--ease-in-out);
}
@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes pageOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

/* Background floating particles */
.bg-particles {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  overflow: hidden;
}
.bg-particle {
  position: absolute;
  border-radius: 50%;
  background: var(--accent);
  animation: particleRise linear infinite;
}

/* Responsive */
@media (max-width: 768px) {
  .sidebar {
    width: var(--sidebar-collapsed-width) !important;
  }
  .collapse-toggle { display: none; }
  .logo-block { padding: 16px 12px; justify-content: center; }
  .logo-text, .nav-label, .nav-group-label, .nav-shortcut, .footer-quota-label { opacity: 0; width: 0; }
  .nav-item { justify-content: center; padding: 10px 8px; margin: 1px 6px; }
  .nav-icon { margin: 0; }
  .content-area { padding: 16px; }
}
</style>
