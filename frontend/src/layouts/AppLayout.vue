<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo" @click="$router.push(userStore.isAdmin ? '/admin/dashboard' : '/dashboard')">
        <span class="logo-icon">✨</span>
        智能文案工作台
      </div>

      <!-- 普通用户菜单 -->
      <template v-if="!userStore.isAdmin">
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/products">
            <el-icon><Goods /></el-icon>
            <span>我的商品</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><UserFilled /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </template>

      <!-- 管理员菜单 -->
      <template v-else>
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>管理概览</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><Setting /></el-icon>
            <span>账号设置</span>
          </el-menu-item>
        </el-menu>
      </template>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <template v-for="(item, idx) in breadcrumbs" :key="idx">
              <el-breadcrumb-item :to="idx < breadcrumbs.length - 1 ? item.path : undefined">
                {{ item.title }}
              </el-breadcrumb-item>
            </template>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span v-if="!userStore.isAdmin && userStore.quotaRemaining !== Infinity" class="quota-badge">
            剩余次数：<strong>{{ userStore.quotaRemaining }}</strong>
          </span>
          <el-avatar :size="32" class="avatar">
            {{ (userStore.nickname || userStore.username).charAt(0) }}
          </el-avatar>
          <span class="nick">{{ userStore.nickname || userStore.username }}</span>
          <el-tag :type="userStore.isAdmin ? 'danger' : 'primary'" size="small">
            {{ userStore.isAdmin ? '管理员' : '用户' }}
          </el-tag>
          <el-button link type="danger" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Goods, Setting, UserFilled, DataAnalysis } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 每次路由切换时刷新配额
watch(() => route.path, () => {
  if (!userStore.isAdmin) userStore.refreshQuota()
})

const breadcrumbs = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '工作台',
    '/products': '我的商品',
    '/profile': '个人中心',
    '/admin/dashboard': '管理概览',
    '/admin/users': '用户管理',
    '/admin/products': '商品管理',
  }
  const items: { title: string; path: string }[] = [{ title: '首页', path: '/' }]
  const title = map[route.path]
  if (title) items.push({ title, path: route.path })
  // Handle /products/:id/edit
  if (route.path.startsWith('/products/') && route.path.endsWith('/edit')) {
    items.push({ title: '商品编辑', path: route.path })
  }
  return items
})

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.aside {
  background: linear-gradient(180deg, #0c1021 0%, #141a33 100%);
  border-right: 1px solid rgba(255,255,255,0.06);
}
.logo {
  color: #fff;
  font-weight: 700;
  padding: 22px 20px;
  font-size: 15px;
  cursor: pointer;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo-icon { font-size: 20px; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  height: 56px;
  padding: 0 24px;
}
.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; gap: 10px; }
.avatar { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.nick { font-size: 14px; color: #333; }
.quota-badge {
  background: linear-gradient(135deg, #fef3e2, #fde8cd);
  color: #e67e22;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}
.quota-badge strong { font-size: 14px; }
.main { background: #f0f2f5; min-height: calc(100vh - 56px); padding: 24px; }
:deep(.el-menu) {
  border-right: none;
  background: transparent;
}
:deep(.el-menu-item) {
  color: rgba(255,255,255,.65);
  margin: 4px 12px;
  border-radius: 10px;
  transition: all 0.3s;
}
:deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255,255,255,0.08);
}
:deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: linear-gradient(135deg, rgba(102,126,234,0.6), rgba(118,75,162,0.6)) !important;
  box-shadow: 0 4px 12px rgba(102,126,234,0.25);
}
</style>
