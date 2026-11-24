<template>
  <div class="min-h-screen flex flex-col bg-[#F9F8F5] text-slate-800">
    <!-- Top Architectural Navigation Header -->
    <header class="sticky top-0 z-30 border-b border-slate-200/80 bg-[#F9F8F5]/90 backdrop-blur-md">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-3">
        <!-- Left: Brand & Hairline Underline Navigation -->
        <div class="flex items-center h-full flex-shrink-0">
          <router-link to="/query" class="flex items-center space-x-2.5 group flex-shrink-0">
            <div class="w-8 h-8 rounded-lg bg-ink-900 text-[#FAF9F6] flex items-center justify-center group-hover:bg-ink-800 transition-colors">
              <Activity class="w-4 h-4" :stroke-width="1.75" />
            </div>
            <div class="flex items-center space-x-2 whitespace-nowrap">
              <span class="font-display text-[15px] font-semibold tracking-[-0.02em] text-ink-900">
                DataPulse
              </span>
              <span class="hidden 2xl:inline-block w-px h-3.5 bg-slate-300"></span>
              <span class="hidden 2xl:inline-block text-[10px] font-medium uppercase tracking-[0.16em] text-slate-400">
                Enterprise
              </span>
            </div>
          </router-link>

          <!-- Primary Navigation Tabs -->
          <nav class="hidden lg:flex items-center h-full space-x-0.5 ml-3 xl:ml-5 flex-shrink-0">
            <router-link
              v-for="item in navItems"
              :key="item.to"
              :to="item.to"
              class="h-full relative flex items-center gap-1.5 px-2 xl:px-2.5 text-xs font-medium whitespace-nowrap flex-shrink-0 transition-colors"
              :class="isActive(item.to)
                ? 'text-ink-900 font-semibold'
                : 'text-slate-500 hover:text-ink-900'"
            >
              <component
                :is="item.icon"
                class="w-3.5 h-3.5 flex-shrink-0"
                :class="isActive(item.to) ? 'text-accent-600' : 'text-slate-400'"
              />
              <span class="xl:hidden">{{ item.short }}</span>
              <span class="hidden xl:inline">{{ item.label }}</span>
              <span
                v-if="isActive(item.to)"
                class="absolute bottom-0 inset-x-2 h-[2px] bg-ink-900 rounded-full"
              ></span>
            </router-link>
          </nav>
        </div>

        <!-- Right Controls -->
        <div class="flex items-center space-x-2 sm:space-x-2.5 flex-shrink-0">
          <!-- Active Datasource Selector -->
          <div class="dp-select-pill flex-shrink-0">
            <Server class="w-3.5 h-3.5 text-slate-500 flex-shrink-0" />
            <select
              :value="datasourceStore.currentDatasourceId"
              @change="handleDatasourceSwitch(Number($event.target.value))"
              class="bg-transparent text-xs font-medium text-ink-900 focus:outline-none cursor-pointer w-[112px] xl:w-[140px] truncate"
              title="当前分析目标数据源"
            >
              <option v-for="ds in datasourceStore.datasources" :key="ds.id" :value="ds.id">
                {{ ds.name }}
              </option>
            </select>
          </div>

          <!-- Operational Health Indicator -->
          <div
            class="hidden xl:inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-pine-50 border border-pine-200/80 text-[11px] font-medium text-pine-700 whitespace-nowrap flex-shrink-0"
            title="服务运行正常：MySQL 8.0 · Redis 6379 · AST 安全沙箱已激活"
          >
            <span class="w-1.5 h-1.5 rounded-full bg-pine-600 flex-shrink-0"></span>
            <span>服务在线</span>
          </div>

          <!-- User Profile & Dropdown Menu -->
          <div class="relative flex-shrink-0" ref="dropdownRef">
            <button
              @click="isMenuOpen = !isMenuOpen"
              class="flex items-center space-x-1.5 rounded-xl bg-white border border-slate-200/90 hover:border-slate-300 px-2 py-1 text-xs font-medium whitespace-nowrap transition-colors cursor-pointer"
            >
              <div
                class="w-6 h-6 rounded-full font-semibold text-[11px] flex items-center justify-center flex-shrink-0"
                :class="authStore.isAdmin
                  ? 'bg-ink-900 text-[#FAF9F6]'
                  : 'bg-pine-600 text-white'"
              >
                {{ (authStore.user?.realName || authStore.user?.username || 'U').slice(0, 1) }}
              </div>
              <span
                class="text-ink-900 font-semibold hidden sm:inline max-w-[68px] xl:max-w-[96px] truncate"
                :title="authStore.user?.realName || authStore.user?.username || '用户'"
              >
                {{ authStore.user?.realName || authStore.user?.username || '用户' }}
              </span>
              <span
                class="whitespace-nowrap flex-shrink-0"
                :class="authStore.isAdmin
                  ? 'text-[10px] font-medium text-accent-700 bg-accent-50 border border-accent-200/80 rounded-md px-1.5 py-0.5 hidden sm:inline-block'
                  : 'text-[10px] font-medium text-pine-700 bg-pine-50 border border-pine-200/80 rounded-md px-1.5 py-0.5 hidden sm:inline-block'"
              >
                {{ authStore.isAdmin ? '管理员' : '分析师' }}
              </span>
              <ChevronDown class="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
            </button>

            <!-- Dropdown Menu -->
            <transition
              enter-active-class="transition duration-150 ease-out"
              enter-from-class="opacity-0 -translate-y-1 scale-95"
              leave-active-class="transition duration-100 ease-in"
              leave-to-class="opacity-0 -translate-y-1 scale-95"
            >
              <div
                v-if="isMenuOpen"
                class="origin-top-right absolute right-0 mt-2 w-64 rounded-xl bg-white border border-slate-200 shadow-lift py-2 z-50"
              >
                <!-- User Info Header -->
                <div class="px-4 py-2.5 border-b border-slate-100">
                  <div class="text-xs font-semibold text-ink-900">
                    {{ authStore.user?.realName || authStore.user?.username }}
                  </div>
                  <div class="text-[11px] text-slate-500 mt-0.5 flex items-center justify-between">
                    <span>{{ authStore.user?.department || '数字化运营中心' }}</span>
                    <span class="font-mono text-slate-400">{{ authStore.user?.role }}</span>
                  </div>
                </div>

                <!-- Persona Switcher Section -->
                <div class="p-2 border-b border-slate-100">
                  <div class="text-[10px] font-medium uppercase text-slate-400 px-2 py-1 tracking-wider">
                    切换系统权限角色
                  </div>
                  <div class="grid grid-cols-2 gap-1.5 mt-1">
                    <button
                      @click="handleFastSwitchRole('admin')"
                      :disabled="authStore.isAdmin || authStore.switchingRole"
                      class="px-2.5 py-1.5 text-xs rounded-lg text-left transition-colors flex items-center space-x-1.5"
                      :class="authStore.isAdmin
                        ? 'bg-ink-900 text-[#FAF9F6] font-semibold'
                        : 'text-slate-600 hover:bg-slate-50 cursor-pointer'"
                    >
                      <Crown class="w-3.5 h-3.5" :class="authStore.isAdmin ? 'text-accent-300 fill-accent-300' : 'text-slate-400'" />
                      <span>赵岚 (Admin)</span>
                    </button>
                    <button
                      @click="handleFastSwitchRole('analyst')"
                      :disabled="authStore.isAnalyst || authStore.switchingRole"
                      class="px-2.5 py-1.5 text-xs rounded-lg text-left transition-colors flex items-center space-x-1.5"
                      :class="authStore.isAnalyst
                        ? 'bg-pine-600 text-white font-semibold'
                        : 'text-slate-600 hover:bg-slate-50 cursor-pointer'"
                    >
                      <UserCheck class="w-3.5 h-3.5" :class="authStore.isAnalyst ? 'text-white' : 'text-slate-400'" />
                      <span>李晨 (Analyst)</span>
                    </button>
                  </div>
                </div>

                <!-- Actions -->
                <div class="p-1">
                  <button
                    @click="handleLogout"
                    class="w-full flex items-center space-x-2 px-3 py-2 text-xs text-crimson-600 hover:bg-crimson-50 rounded-lg transition-colors font-medium cursor-pointer"
                  >
                    <LogOut class="w-3.5 h-3.5" />
                    <span>退出当前账号</span>
                  </button>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>

      <!-- Mobile Navigation Tabs -->
      <div class="lg:hidden border-t border-slate-200/70 px-4 py-2 flex space-x-1.5 overflow-x-auto">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="rounded-lg px-3 py-1 text-xs font-medium whitespace-nowrap transition-colors"
          :class="isActive(item.to)
            ? 'bg-ink-900 text-[#FAF9F6]'
            : 'text-slate-600 hover:bg-slate-200/60'"
        >
          {{ item.short }}
        </router-link>
      </div>
    </header>

    <!-- 主内容区：带路由切换淡入淡出过渡，并对高频状态页（工作台、数据字典）开启 keep-alive 保活缓存 -->
    <main class="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <keep-alive :include="['AgentQueryView', 'SchemaExplorerView']">
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </main>

    <!-- Minimal Enterprise Footer -->
    <footer class="border-t border-slate-200/75 py-6 text-xs text-slate-400">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2">
        <div class="flex items-center space-x-2 text-slate-600 font-medium">
          <span>DataPulse Platform</span>
          <span class="text-slate-300">·</span>
          <span class="text-slate-400 font-normal">企业级多维数据智能与安全只读沙箱中台</span>
        </div>
        <div class="text-slate-400">
          <span>© 2026 DataPulse Analytics Inc. All rights reserved.</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
/**
 * DataPulse 登录后主应用外壳布局（MainLayout）。
 * 负责包裹工作台、数据字典、数据源管理、SQL 安全审计、运营监控五大页面，统一提供：
 * 1. 吸顶导航栏与移动端横向滚动导航；
 * 2. 顶部全局目标数据源快捷切换器（与 Pinia datasourceStore 联动）；
 * 3. 右上角用户身份菜单（支持一键切换管理员/分析师演示 RBAC 权限差异，以及安全退出）；
 * 4. 子路由视图的过渡动画与关键页面 keep-alive 状态保活。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDatasourceStore } from '../stores/datasource'
import { useToastStore } from '../stores/toast'
import {
  Activity,
  Terminal,
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
const route = useRoute()
const authStore = useAuthStore()
const datasourceStore = useDatasourceStore()
const toast = useToastStore()

// 右上角用户下拉菜单展开状态及外层 DOM 引用（用于检测“点击菜单外部自动收起”）
const isMenuOpen = ref(false)
const dropdownRef = ref(null)

// 顶部主导航菜单配置：窄屏显示 short 简称，宽屏显示 label 全称
const navItems = [
  { to: '/query', label: '智能分析工作台', short: '智能工作台', icon: Terminal },
  { to: '/schema', label: '数仓元数据字典', short: '数据字典', icon: Table2 },
  { to: '/datasource', label: '数据源管理', short: '数仓管理', icon: Server },
  { to: '/audit', label: 'SQL安全审计', short: '安全审计', icon: ShieldCheck },
  { to: '/dashboard', label: '运营监控', short: '运营监控', icon: LayoutDashboard }
]

// 判断某导航项是否为当前激活路由
const isActive = (p) => route.path === p

// 顶栏下拉切换全局分析数仓：更新 Pinia 状态后，工作台与字典页会通过 watch 自动联动切换
const handleDatasourceSwitch = (newId) => {
  datasourceStore.setDatasourceId(newId)
  const current = datasourceStore.datasources.find(d => d.id === newId)
  if (current) {
    toast.info(`已切换当前分析数仓为 [${current.name}]`)
  }
}

// 演示用一键切换身份角色（管理员 / 分析师），方便实时对比 RBAC 读写权限控制效果
const handleFastSwitchRole = async (roleKey) => {
  try {
    await authStore.switchDemoRole(roleKey)
    isMenuOpen.value = false
    toast.success(`已切换为 ${authStore.user?.realName} (${authStore.roleDisplayName})`)
  } catch (err) {
    toast.error('切换身份失败: ' + (err.message || '未知错误'))
  }
}

// 退出登录：清掉本地登录凭证并跳转回登录页
const handleLogout = () => {
  authStore.logout()
  isMenuOpen.value = false
  router.push('/login')
  toast.info('您已成功退出登录')
}

// 点击下拉菜单区域以外的空白处时，自动收起用户菜单
const handleClickOutside = (e) => {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    isMenuOpen.value = false
  }
}

// 布局挂载时注册全局点击监听，并预加载可用数据源列表供顶栏下拉框使用
onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await datasourceStore.fetchDatasources()
})

// 布局销毁前解绑全局事件，避免内存泄漏
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>
