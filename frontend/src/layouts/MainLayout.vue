<template>
  <div class="min-h-screen bg-slate-50/70 flex flex-col font-sans text-slate-900 antialiased">
    <!-- Top Modern Navigation Header -->
    <header class="bg-white border-b border-slate-200/80 sticky top-0 z-30 shadow-2xs">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-15 flex items-center justify-between">
        <!-- Left: Brand -->
        <div class="flex items-center space-x-6">
          <router-link to="/agent" class="flex items-center space-x-3 group">
            <div class="w-8 h-8 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-sm shadow-blue-500/20 group-hover:scale-105 transition-transform">
              <Database class="w-4 h-4" />
            </div>
            <div class="flex items-center space-x-2">
              <span class="text-sm font-bold text-slate-900 tracking-tight">DataPulse</span>
              <span class="px-1.5 py-0.5 text-[10px] font-semibold rounded bg-slate-100 text-slate-600 border border-slate-200/90 tracking-wider uppercase">
                Enterprise
              </span>
            </div>
          </router-link>

          <!-- Primary Navigation Tabs -->
          <nav class="hidden md:flex items-center space-x-1 pl-4 border-l border-slate-200/80">
            <router-link
              to="/agent"
              class="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all"
              :class="$route.path === '/agent'
                ? 'bg-slate-100 text-blue-700 font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
            >
              <Bot class="w-3.5 h-3.5" />
              <span>智能分析工作台</span>
            </router-link>

            <router-link
              to="/schema"
              class="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all"
              :class="$route.path === '/schema'
                ? 'bg-slate-100 text-blue-700 font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
            >
              <Table2 class="w-3.5 h-3.5" />
              <span>数仓元数据字典</span>
            </router-link>

            <router-link
              to="/datasource"
              class="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all"
              :class="$route.path === '/datasource'
                ? 'bg-slate-100 text-blue-700 font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
            >
              <Server class="w-3.5 h-3.5" />
              <span>数据源管理</span>
            </router-link>

            <router-link
              to="/audit"
              class="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all"
              :class="$route.path === '/audit'
                ? 'bg-slate-100 text-blue-700 font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
            >
              <ShieldCheck class="w-3.5 h-3.5" />
              <span>SQL安全审计</span>
            </router-link>

            <router-link
              to="/dashboard"
              class="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-lg transition-all"
              :class="$route.path === '/dashboard'
                ? 'bg-slate-100 text-blue-700 font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'"
            >
              <LayoutDashboard class="w-3.5 h-3.5" />
              <span>运营监控</span>
            </router-link>
          </nav>
        </div>

        <!-- Right Controls -->
        <div class="flex items-center space-x-3 sm:space-x-4">
          <!-- Active Datasource Selector -->
          <div class="flex items-center space-x-2 bg-slate-50 hover:bg-slate-100/80 border border-slate-200/90 px-2.5 py-1 rounded-lg transition-colors">
            <Server class="w-3.5 h-3.5 text-blue-600 flex-shrink-0" />
            <select
              :value="datasourceStore.currentDatasourceId"
              @change="handleDatasourceSwitch(Number($event.target.value))"
              class="bg-transparent text-xs font-medium text-slate-700 focus:outline-none cursor-pointer max-w-[150px] sm:max-w-[200px] truncate"
              title="当前分析目标数据源"
            >
              <option v-for="ds in datasourceStore.datasources" :key="ds.id" :value="ds.id">
                {{ ds.name }}
              </option>
            </select>
          </div>

          <!-- Operational Health Indicator -->
          <div class="hidden lg:flex items-center space-x-1.5 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200/80 text-[11px] font-medium" title="服务运行正常：MySQL 8.0 · Redis 6379 · AST 安全沙箱已激活">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>服务在线</span>
          </div>

          <!-- User Profile & Dropdown Menu -->
          <div class="relative" ref="dropdownRef">
            <button
              @click="isMenuOpen = !isMenuOpen"
              class="flex items-center space-x-2 p-1 pl-1.5 pr-2.5 rounded-xl hover:bg-slate-100 border border-slate-200/80 text-xs font-medium transition-colors"
            >
              <div
                class="w-6 h-6 rounded-full text-white font-semibold text-[11px] flex items-center justify-center shadow-xs"
                :class="authStore.isAdmin ? 'bg-blue-600' : 'bg-indigo-600'"
              >
                {{ (authStore.user?.realName || authStore.user?.username || 'U').slice(0, 1) }}
              </div>
              <span class="text-slate-800 font-semibold hidden sm:inline">
                {{ authStore.user?.realName || authStore.user?.username || '用户' }}
              </span>
              <span
                class="px-1.5 py-0.2 rounded text-[10px] font-medium hidden sm:inline"
                :class="authStore.isAdmin ? 'bg-blue-50 text-blue-700 border border-blue-200' : 'bg-indigo-50 text-indigo-700 border border-indigo-200'"
              >
                {{ authStore.isAdmin ? '管理员' : '分析师' }}
              </span>
              <ChevronDown class="w-3.5 h-3.5 text-slate-400" />
            </button>

            <!-- Dropdown Menu -->
            <div
              v-if="isMenuOpen"
              class="absolute right-0 mt-2 w-64 bg-white rounded-2xl shadow-xl border border-slate-200/90 py-2 z-50 animate-in fade-in zoom-in-95 duration-100"
            >
              <!-- User Info Header -->
              <div class="px-4 py-2.5 border-b border-slate-100">
                <div class="text-xs font-bold text-slate-900">
                  {{ authStore.user?.realName || authStore.user?.username }}
                </div>
                <div class="text-[11px] text-slate-500 mt-0.5 flex items-center justify-between">
                  <span>{{ authStore.user?.department || '数字化运营中心' }}</span>
                  <span class="font-mono text-slate-400">{{ authStore.user?.role }}</span>
                </div>
              </div>

              <!-- Persona Switcher Section -->
              <div class="p-2 border-b border-slate-100">
                <div class="text-[10px] font-semibold uppercase text-slate-400 px-2 py-1 tracking-wider">
                  切换系统权限角色
                </div>
                <div class="grid grid-cols-2 gap-1 mt-1">
                  <button
                    @click="handleFastSwitchRole('admin')"
                    :disabled="authStore.isAdmin || authStore.switchingRole"
                    class="px-2.5 py-1.5 text-xs rounded-lg text-left transition-colors flex items-center space-x-1.5"
                    :class="authStore.isAdmin
                      ? 'bg-blue-50 text-blue-700 font-semibold'
                      : 'text-slate-600 hover:bg-slate-50 cursor-pointer'"
                  >
                    <Crown class="w-3.5 h-3.5" :class="authStore.isAdmin ? 'text-amber-500 fill-amber-500' : 'text-slate-400'" />
                    <span>赵岚 (Admin)</span>
                  </button>
                  <button
                    @click="handleFastSwitchRole('analyst')"
                    :disabled="authStore.isAnalyst || authStore.switchingRole"
                    class="px-2.5 py-1.5 text-xs rounded-lg text-left transition-colors flex items-center space-x-1.5"
                    :class="authStore.isAnalyst
                      ? 'bg-indigo-50 text-indigo-700 font-semibold'
                      : 'text-slate-600 hover:bg-slate-50 cursor-pointer'"
                  >
                    <UserCheck class="w-3.5 h-3.5" :class="authStore.isAnalyst ? 'text-indigo-600' : 'text-slate-400'" />
                    <span>李晨 (Analyst)</span>
                  </button>
                </div>
              </div>

              <!-- Actions -->
              <div class="p-1">
                <button
                  @click="handleLogout"
                  class="w-full flex items-center space-x-2 px-3 py-2 text-xs text-rose-600 hover:bg-rose-50 rounded-xl transition-colors font-medium cursor-pointer"
                >
                  <LogOut class="w-3.5 h-3.5" />
                  <span>退出当前账号</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Mobile Navigation Tabs -->
      <div class="md:hidden border-t border-slate-100 px-4 py-2 flex space-x-2 overflow-x-auto">
        <router-link
          to="/agent"
          class="px-3 py-1 text-xs rounded-lg whitespace-nowrap"
          :class="$route.path === '/agent' ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-600'"
        >
          智能工作台
        </router-link>
        <router-link
          to="/schema"
          class="px-3 py-1 text-xs rounded-lg whitespace-nowrap"
          :class="$route.path === '/schema' ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-600'"
        >
          数据字典
        </router-link>
        <router-link
          to="/datasource"
          class="px-3 py-1 text-xs rounded-lg whitespace-nowrap"
          :class="$route.path === '/datasource' ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-600'"
        >
          数仓管理
        </router-link>
        <router-link
          to="/audit"
          class="px-3 py-1 text-xs rounded-lg whitespace-nowrap"
          :class="$route.path === '/audit' ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-600'"
        >
          安全审计
        </router-link>
        <router-link
          to="/dashboard"
          class="px-3 py-1 text-xs rounded-lg whitespace-nowrap"
          :class="$route.path === '/dashboard' ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-600'"
        >
          运营监控
        </router-link>
      </div>
    </header>

    <!-- Main Content Area with KeepAlive -->
    <main class="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <router-view v-slot="{ Component }">
        <keep-alive :include="['AgentQueryView', 'SchemaExplorerView']">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>

    <!-- Minimal Enterprise Footer -->
    <footer class="bg-white border-t border-slate-200/80 py-4 text-xs text-slate-500">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2">
        <div class="flex items-center space-x-2 text-slate-600 font-medium">
          <span>DataPulse Platform</span>
          <span class="text-slate-300">·</span>
          <span class="text-slate-400">企业级多维数据智能与安全只读沙箱中台</span>
        </div>
        <div class="text-slate-400">
          <span>© 2026 DataPulse Analytics Inc. All rights reserved.</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDatasourceStore } from '../stores/datasource'
import { useToastStore } from '../stores/toast'
import {
  Database,
  Bot,
  Table2,
  Server,
  ShieldCheck,
  LayoutDashboard,
  LogOut,
  ChevronDown,
  Crown,
  UserCheck
} from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const datasourceStore = useDatasourceStore()
const toast = useToastStore()

const isMenuOpen = ref(false)
const dropdownRef = ref(null)

const handleDatasourceSwitch = (newId) => {
  datasourceStore.setDatasourceId(newId)
  const current = datasourceStore.datasources.find(d => d.id === newId)
  if (current) {
    toast.info(`已切换当前分析数仓为 [${current.name}]`)
  }
}

const handleFastSwitchRole = async (roleKey) => {
  try {
    await authStore.switchDemoRole(roleKey)
    isMenuOpen.value = false
    toast.success(`已切换为 ${authStore.user?.realName} (${authStore.roleDisplayName})`)
  } catch (err) {
    toast.error('切换身份失败: ' + (err.message || '未知错误'))
  }
}

const handleLogout = () => {
  authStore.logout()
  isMenuOpen.value = false
  router.push('/login')
  toast.info('您已成功退出登录')
}

const handleClickOutside = (e) => {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    isMenuOpen.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await datasourceStore.fetchDatasources()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
