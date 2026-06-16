<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">智能文案工作台</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/admin/products">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ userStore.nickname || userStore.username }}</span>
        <el-tag size="small" class="role-tag">{{ userStore.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
        <el-button link type="danger" @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { Goods, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #001529; }
.logo {
  color: #fff;
  font-weight: 600;
  padding: 20px 16px;
  font-size: 15px;
  line-height: 1.4;
}
.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.header > * + * { margin-left: 12px; }
.main { background: #f5f7fa; min-height: calc(100vh - 60px); }
.role-tag { margin-right: auto; margin-left: 8px; }
:deep(.el-menu) { border-right: none; background: #001529; }
:deep(.el-menu-item) { color: rgba(255,255,255,.75); }
:deep(.el-menu-item.is-active) { color: #fff !important; background: #1677ff !important; }
</style>
