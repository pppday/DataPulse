<template>
  <div class="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans antialiased text-slate-900">
    <!-- Brand Header -->
    <div class="sm:mx-auto sm:w-full sm:max-w-md text-center">
      <div class="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-lg shadow-blue-500/20 mb-4">
        <Database class="w-6 h-6" />
      </div>
      <h2 class="text-2xl font-bold tracking-tight text-slate-900">
        DataPulse 分析中台
      </h2>
      <p class="mt-1 text-xs text-slate-500">
        企业级智能 Text-to-SQL 与多维数据安全治理平台
      </p>
    </div>

    <!-- Login Card -->
    <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4">
      <div class="bg-white py-8 px-6 sm:px-10 shadow-sm border border-slate-200/90 rounded-2xl space-y-6">
        <!-- Error Message -->
        <div
          v-if="errorMessage"
          class="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center space-x-2 animate-in fade-in duration-200"
        >
          <AlertCircle class="w-4 h-4 flex-shrink-0" />
          <span>{{ errorMessage }}</span>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1.5">企业账号</label>
            <div class="relative rounded-xl shadow-2xs">
              <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <User class="w-4 h-4" />
              </div>
              <input
                v-model="form.username"
                type="text"
                required
                placeholder="请输入用户名或工号"
                class="block w-full rounded-xl border border-slate-300 pl-10 pr-3.5 py-2.5 text-xs text-slate-900 placeholder:text-slate-400 focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600 transition-colors"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1.5">登录密码</label>
            <div class="relative rounded-xl shadow-2xs">
              <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Lock class="w-4 h-4" />
              </div>
              <input
                v-model="form.password"
                type="password"
                required
                placeholder="请输入登录密码"
                class="block w-full rounded-xl border border-slate-300 pl-10 pr-3.5 py-2.5 text-xs text-slate-900 placeholder:text-slate-400 focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600 transition-colors"
              />
            </div>
          </div>

          <div class="pt-2">
            <button
              type="submit"
              :disabled="loading"
              class="w-full flex justify-center items-center py-2.5 px-4 rounded-xl text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 active:bg-blue-800 shadow-sm shadow-blue-500/20 focus:outline-none disabled:opacity-60 transition-colors cursor-pointer"
            >
              <Loader2 v-if="loading" class="w-4 h-4 mr-2 animate-spin" />
              <span>{{ loading ? '正在进行安全鉴权...' : '登 录 平 台' }}</span>
            </button>
          </div>
        </form>

        <!-- Subtle Fast Demo Account Access -->
        <div class="pt-4 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
          <span>快捷体验预置账号:</span>
          <div class="flex items-center space-x-2">
            <button
              @click="quickLogin('admin', 'admin123')"
              type="button"
              class="text-blue-600 hover:text-blue-800 font-medium hover:underline cursor-pointer"
            >
              赵岚 (管理员)
            </button>
            <span class="text-slate-300">·</span>
            <button
              @click="quickLogin('analyst', 'data123')"
              type="button"
              class="text-indigo-600 hover:text-indigo-800 font-medium hover:underline cursor-pointer"
            >
              李晨 (分析师)
            </button>
          </div>
        </div>
      </div>

      <!-- Trust Footer -->
      <div class="mt-6 text-center text-[11px] text-slate-400">
        DataPulse Enterprise Security Standard · RBAC 细粒度审计与 AST 沙箱防护
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { Database, AlertCircle, Loader2, User, Lock } from 'lucide-vue-next'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const loading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await authStore.login(form.username, form.password)
    toast.success(`欢迎回来，${data.realName || data.username}`)
    router.push('/agent')
  } catch (err) {
    errorMessage.value = err.message || '登录失败，请核实用户名与密码'
  } finally {
    loading.value = false
  }
}

const quickLogin = (u, p) => {
  form.username = u
  form.password = p
  handleLogin()
}
</script>
