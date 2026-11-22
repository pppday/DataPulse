import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '../api/client'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('datapulse_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('datapulse_user') || 'null'))
  const switchingRole = ref(false)

  const isAdmin = computed(() => {
    const role = user.value?.role || ''
    return role === 'ROLE_ADMIN' || role === 'ADMIN'
  })

  const isAnalyst = computed(() => {
    const role = user.value?.role || ''
    return role === 'ROLE_ANALYST' || role === 'ANALYST'
  })

  const roleDisplayName = computed(() => {
    if (isAdmin.value) return '企业总架构师 (ADMIN)'
    if (isAnalyst.value) return '资深数据分析师 (ANALYST)'
    return '系统成员'
  })

  const login = async (username, password) => {
    const data = await apiClient.post('/auth/login', { username, password })
    token.value = data.token
    user.value = {
      id: data.id,
      username: data.username,
      realName: data.realName,
      role: data.role,
      avatar: data.avatar,
      department: data.department
    }
    localStorage.setItem('datapulse_token', data.token)
    localStorage.setItem('datapulse_user', JSON.stringify(user.value))
    return data
  }

  /**
   * 1-Click 极速身份角色切换（面向企业演示与面试评审）
   * @param {'admin' | 'analyst'} targetUsername 目标身份
   */
  const switchDemoRole = async (targetUsername) => {
    if (switchingRole.value) return
    switchingRole.value = true
    try {
      const password = targetUsername === 'admin' ? 'admin123' : 'data123'
      const data = await login(targetUsername, password)
      return data
    } finally {
      switchingRole.value = false
    }
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('datapulse_token')
    localStorage.removeItem('datapulse_user')
  }

  const isAuthenticated = () => !!token.value

  return {
    token,
    user,
    isAdmin,
    isAnalyst,
    roleDisplayName,
    switchingRole,
    login,
    switchDemoRole,
    logout,
    isAuthenticated
  }
})
