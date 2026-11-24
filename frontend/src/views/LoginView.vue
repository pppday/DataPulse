<template>
  <div class="min-h-screen bg-[#F9F8F5] text-slate-800 grid lg:grid-cols-12">
    <!-- Left Architectural Editorial Panel -->
    <div class="hidden lg:flex lg:col-span-7 flex-col justify-between p-12 xl:p-16 bg-[#F4F2EC] relative overflow-hidden">
      <!-- Subtle Architectural Hairlines -->
      <div class="pointer-events-none absolute inset-y-0 right-12 w-px border-r border-slate-300/35"></div>
      <div class="pointer-events-none absolute inset-x-0 bottom-20 h-px border-b border-slate-300/35"></div>

      <!-- Brand Header -->
      <div class="relative flex items-center space-x-2.5">
        <div class="w-8 h-8 rounded-lg bg-ink-900 text-[#FAF9F6] flex items-center justify-center">
          <Activity class="w-4 h-4" :stroke-width="1.75" />
        </div>
        <div class="flex items-center space-x-2">
          <span class="font-display text-[15px] font-semibold tracking-[-0.02em] text-ink-900">
            DataPulse
          </span>
          <span class="w-px h-3.5 bg-slate-300"></span>
          <span class="text-[10px] font-medium uppercase tracking-[0.16em] text-slate-400">
            Enterprise
          </span>
        </div>
      </div>

      <!-- Editorial Headline & Capabilities -->
      <div class="relative max-w-xl dp-rise">
        <div class="dp-eyebrow">
          <span class="w-5 h-px bg-accent-500"></span>
          <span>ENTERPRISE DATA INTELLIGENCE</span>
        </div>
        <h1 class="mt-4 font-display text-[44px] xl:text-[50px] font-semibold leading-[1.12] tracking-[-0.03em] text-ink-900">
          让企业数据，<br />自然回答业务之问
        </h1>
        <p class="mt-3 font-serif italic text-xl text-accent-600">
          Natural language to verified SQL, rendered with precision.
        </p>
        <p class="mt-4 text-sm leading-7 text-slate-500 max-w-md">
          依据企业数仓字典自动生成合规只读 SQL，经语法树沙箱校验后即时输出多维图表与经营洞察。
        </p>

        <div class="mt-12 pt-8 border-t border-slate-300/60 grid grid-cols-3 gap-6">
          <div>
            <div class="font-mono text-[11px] text-accent-600">01</div>
            <div class="mt-2 text-xs font-semibold text-ink-900">自然语言推导</div>
            <div class="mt-1 text-[11px] leading-relaxed text-slate-500">依据物理字典与业务口径生成只读 SQL</div>
          </div>
          <div>
            <div class="font-mono text-[11px] text-accent-600">02</div>
            <div class="mt-2 text-xs font-semibold text-ink-900">AST 语法沙箱</div>
            <div class="mt-1 text-[11px] leading-relaxed text-slate-500">语法树白名单审查与行数自动收敛</div>
          </div>
          <div>
            <div class="font-mono text-[11px] text-accent-600">03</div>
            <div class="mt-2 text-xs font-semibold text-ink-900">毫秒语义缓存</div>
            <div class="mt-1 text-[11px] leading-relaxed text-slate-500">Redis 查询指纹命中免除重复开销</div>
          </div>
        </div>
      </div>

      <!-- Trust Footer -->
      <div class="relative text-[11px] text-slate-400">
        DataPulse Enterprise Security Standard · RBAC 细粒度审计与 AST 沙箱防护
      </div>
    </div>

    <!-- Right Clean Paper Auth Panel -->
    <div class="lg:col-span-5 bg-white lg:border-l border-slate-200/80 flex items-center justify-center px-6 py-12">
      <div class="w-full max-w-[360px] space-y-7 dp-rise">
        <!-- Mobile Brand Header -->
        <div class="lg:hidden space-y-2 pb-4 border-b border-slate-200/80">
          <div class="flex items-center space-x-2.5">
            <div class="w-8 h-8 rounded-lg bg-ink-900 text-[#FAF9F6] flex items-center justify-center">
              <Activity class="w-4 h-4" :stroke-width="1.75" />
            </div>
            <span class="font-display text-[15px] font-semibold tracking-[-0.02em] text-ink-900">
              DataPulse
            </span>
          </div>
          <p class="text-xs text-slate-500">
            企业级智能 Text-to-SQL 与多维数据安全治理平台
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-semibold text-ink-900 tracking-[-0.025em]">登录工作台</h2>
          <p class="mt-1.5 text-xs text-slate-500">
            输入企业账号或使用演示身份进入分析平台
          </p>
        </div>

        <!-- Error Message -->
        <div
          v-if="errorMessage"
          class="rounded-xl bg-crimson-50 border border-crimson-200 text-crimson-600 p-3 text-xs flex items-center space-x-2 animate-pop"
        >
          <AlertCircle class="w-4 h-4 flex-shrink-0" />
          <span>{{ errorMessage }}</span>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-xs font-medium text-slate-600 mb-1.5">企业账号</label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <User class="w-4 h-4" />
              </div>
              <input
                v-model="form.username"
                type="text"
                required
                placeholder="请输入用户名或工号"
                class="dp-input pl-10"
              />
            </div>
          </div>

          <div>
            <label class="block text-xs font-medium text-slate-600 mb-1.5">登录密码</label>
            <div class="relative">
              <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                <Lock class="w-4 h-4" />
              </div>
              <input
                v-model="form.password"
                type="password"
                required
                placeholder="请输入登录密码"
                class="dp-input pl-10"
              />
            </div>
          </div>

          <div class="pt-2">
            <button
              type="submit"
              :disabled="loading"
              class="dp-btn-primary w-full py-2.5 text-[13px] group"
            >
              <Loader2 v-if="loading" class="w-4 h-4 animate-spin" />
              <span>{{ loading ? '正在进行安全鉴权...' : '登 录' }}</span>
              <ArrowRight v-if="!loading" class="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
            </button>
          </div>
        </form>

        <!-- Divider -->
        <div class="flex items-center gap-3 text-[11px] text-slate-400">
          <div class="dp-divider flex-1"></div>
          <span>快捷体验账号</span>
          <div class="dp-divider flex-1"></div>
        </div>

        <!-- Fast Demo Account Access -->
        <div class="grid grid-cols-2 gap-2.5">
          <button
            @click="quickLogin('admin', 'admin123')"
            type="button"
            class="dp-btn-secondary py-2.5 px-3 justify-start"
          >
            <span class="w-6 h-6 rounded-full bg-ink-900 text-[#FAF9F6] text-[11px] font-semibold flex items-center justify-center flex-shrink-0">
              赵
            </span>
            <span class="text-left leading-tight">
              <span class="block text-xs font-semibold text-ink-900">赵岚</span>
              <span class="block text-[10px] text-slate-400 font-normal">管理员</span>
            </span>
          </button>

          <button
            @click="quickLogin('analyst', 'data123')"
            type="button"
            class="dp-btn-secondary py-2.5 px-3 justify-start"
          >
            <span class="w-6 h-6 rounded-full bg-pine-600 text-white text-[11px] font-semibold flex items-center justify-center flex-shrink-0">
              李
            </span>
            <span class="text-left leading-tight">
              <span class="block text-xs font-semibold text-ink-900">李晨</span>
              <span class="block text-[10px] text-slate-400 font-normal">分析师</span>
            </span>
          </button>
        </div>

        <!-- Mobile Trust Footer -->
        <div class="lg:hidden text-center text-[11px] text-slate-400 pt-2">
          DataPulse Enterprise Security Standard · RBAC 细粒度审计与 AST 沙箱防护
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { Activity, ArrowRight, AlertCircle, Loader2, User, Lock } from 'lucide-vue-next'

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
    router.push('/query')
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
