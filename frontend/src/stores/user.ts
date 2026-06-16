import { createPinia, defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')

  const isAdmin = computed(() => role.value === 'ADMIN')
  const isLoggedIn = computed(() => Boolean(token.value))

  function setSession(data: { token: string; username: string; role: string; nickname: string }) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    nickname.value = data.nickname
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('nickname', data.nickname)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    nickname.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('nickname')
  }

  return { token, username, role, nickname, isAdmin, isLoggedIn, setSession, logout }
})

export function setupPinia() {
  return createPinia()
}
