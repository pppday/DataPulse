/**
 * 用户身份认证与 RBAC 角色状态仓库（Pinia）。
 * 统一管理登录后的 JWT 令牌、用户档案、管理员/分析师角色判定以及演示场景下的一键身份切换；
 * 被路由守卫（router/index.js）、顶栏（MainLayout.vue）以及各权限受控页面共同依赖。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '../api/client'

/**
 * 本地 JWT 过期预检工具函数（不发网络请求，纯浏览器端快速判断）。
 * 原理：标准 JWT 由 header.payload.signature 三段拼接而成，中间段是 Base64URL 编码的 JSON；
 * 这里将其还原为标准 Base64 并补齐长度后解码，比对 payload.exp（秒级过期时间）与当前毫秒时间戳。
 * 只要格式残缺、解码异常或已超过有效期，一律视作已过期，避免带着失效令牌进入页面。
 */
export function isJwtExpired(token) {
  if (!token || typeof token !== 'string') return true
  const parts = token.split('.')
  if (parts.length !== 3) return true
  try {
    // JWT 使用 Base64URL（用 - 和 _ 替代 + 和 / 且可能省略末尾 =），解码前需先转换并补齐 4 的倍数
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const payload = JSON.parse(atob(padded))
    if (!payload.exp) return false
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

export const useAuthStore = defineStore('auth', () => {
  // Store 初始化时先做一次过期预检：若浏览器里留着上次过期的旧 Token，直接清掉，防止 UI 误判为已登录
  const storedToken = localStorage.getItem('datapulse_token') || ''
  if (storedToken && isJwtExpired(storedToken)) {
    localStorage.removeItem('datapulse_token')
    localStorage.removeItem('datapulse_user')
  }

  // 当前有效 JWT 令牌、登录用户基本信息，以及角色切换防重入锁
  const token = ref(localStorage.getItem('datapulse_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('datapulse_user') || 'null'))
  const switchingRole = ref(false)

  // 兼容后端可能返回的带 ROLE_ 前缀或不带前缀的角色枚举，判断是否为系统管理员（拥有写权限）
  const isAdmin = computed(() => {
    const role = user.value?.role || ''
    return role === 'ROLE_ADMIN' || role === 'ADMIN'
  })

  // 判断是否为数据分析师角色（数据源/字典配置等页面处于受控只读模式）
  const isAnalyst = computed(() => {
    const role = user.value?.role || ''
    return role === 'ROLE_ANALYST' || role === 'ANALYST'
  })

  // 将角色标识翻译为界面展示用的可读头衔
  const roleDisplayName = computed(() => {
    if (isAdmin.value) return '企业总架构师 (ADMIN)'
    if (isAnalyst.value) return '资深数据分析师 (ANALYST)'
    return '系统成员'
  })

  /**
   * 账号密码登录：调用后端鉴权接口换取 JWT 与用户档案，并同步写入内存状态与 localStorage 实现刷新持久化。
   */
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
   * 通过 switchingRole 防止连续点击触发并发登录，直接用预置演示账号重新走一遍真实 JWT 签发流程。
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

  // 主动退出登录：清空响应式状态和本地持久化存储
  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('datapulse_token')
    localStorage.removeItem('datapulse_user')
  }

  // 判断当前是否处于已登录状态
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
