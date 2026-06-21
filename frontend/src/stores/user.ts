import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http, { type ApiResponse } from '../api/http'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const quotaTotal = ref(Number(localStorage.getItem('quotaTotal') || '0'))
  const quotaUsed = ref(Number(localStorage.getItem('quotaUsed') || '0'))

  const isAdmin = computed(() => role.value === 'ADMIN')
  const isLoggedIn = computed(() => Boolean(token.value))
  const quotaRemaining = computed(() => quotaTotal.value === -1 ? Infinity : Math.max(0, quotaTotal.value - quotaUsed.value))

  function setSession(data: { token: string; username: string; role: string; nickname: string; avatar?: string; quotaTotal?: number; quotaUsed?: number }) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    nickname.value = data.nickname
    avatar.value = data.avatar || ''
    quotaTotal.value = data.quotaTotal ?? 0
    quotaUsed.value = data.quotaUsed ?? 0
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('avatar', data.avatar || '')
    localStorage.setItem('quotaTotal', String(data.quotaTotal ?? 0))
    localStorage.setItem('quotaUsed', String(data.quotaUsed ?? 0))
  }

  /** 刷新配额信息（AI操作后调用） */
  async function refreshQuota() {
    if (!token.value || isAdmin.value) return
    try {
      const { data } = await http.get<ApiResponse<any>>('/auth/profile')
      if (data.data) {
        quotaTotal.value = data.data.quotaTotal ?? quotaTotal.value
        quotaUsed.value = data.data.quotaUsed ?? quotaUsed.value
        localStorage.setItem('quotaTotal', String(quotaTotal.value))
        localStorage.setItem('quotaUsed', String(quotaUsed.value))
      }
    } catch { /* ignore */ }
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    nickname.value = ''
    avatar.value = ''
    quotaTotal.value = 0
    quotaUsed.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('nickname')
    localStorage.removeItem('avatar')
    localStorage.removeItem('quotaTotal')
    localStorage.removeItem('quotaUsed')
  }

  return { token, username, role, nickname, avatar, quotaTotal, quotaUsed, quotaRemaining, isAdmin, isLoggedIn, setSession, refreshQuota, logout }
})
