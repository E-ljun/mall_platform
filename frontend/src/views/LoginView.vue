<template>
  <div class="login-page">
    <el-card class="card">
      <h2>智能商品详情与营销文案生成系统</h2>
      <p class="sub">基于多模态大模型的电商文案工作台</p>
      <el-form @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
        <el-button link @click="onRegister">没有账号？注册</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    const { data } = await http.post<ApiResponse<any>>('/auth/login', {
      username: username.value,
      password: password.value,
    })
    userStore.setSession({
      token: data.data.token,
      username: data.data.username,
      role: data.data.role,
      nickname: data.data.nickname,
    })
    router.push('/products')
  } catch {
    /* handled by interceptor */
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  const { value } = await ElMessageBox.prompt('请输入注册用户名', '注册', {
    inputPattern: /.+/,
    inputErrorMessage: '用户名不能为空',
  }).catch(() => ({ value: null }))
  if (!value) return
  const pwd = await ElMessageBox.prompt('请输入密码', '注册', { inputType: 'password' }).catch(() => ({ value: null }))
  if (!pwd.value) return
  await http.post('/auth/register', { username: value, password: pwd.value, nickname: value })
  ElMessage.success('注册成功，请登录')
  username.value = value
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.card { width: 420px; max-width: 92vw; }
.sub { color: #666; font-size: 13px; margin-top: -8px; margin-bottom: 20px; }
</style>
