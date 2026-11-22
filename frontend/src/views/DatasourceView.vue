<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div class="flex items-center space-x-2">
            <span class="p-1.5 rounded-lg bg-blue-50 text-blue-600 border border-blue-100">
              <Server class="w-4 h-4" />
            </span>
            <h1 class="text-base font-bold text-slate-900 tracking-tight">企业动态数仓数据源管理</h1>
            <span class="text-[11px] px-2 py-0.5 rounded-full font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
              HikariCP 连接池热挂载
            </span>
          </div>
          <p class="mt-1 text-xs text-slate-500">
            支持动态挂载异构业务库，秒级连通测试与连接池生命周期隔离管理。
          </p>
        </div>

        <button
          v-if="authStore.isAdmin"
          @click="openModal()"
          class="flex items-center space-x-1.5 px-3.5 py-2 text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white shadow-sm shadow-blue-500/20 transition-all cursor-pointer"
        >
          <Plus class="w-3.5 h-3.5" />
          <span>接入新数据源</span>
        </button>
        <div
          v-else
          class="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-medium rounded-xl bg-slate-100 text-slate-400 border border-slate-200/80 cursor-not-allowed select-none"
          title="权限受限：仅企业管理员可添加新数据源"
        >
          <Lock class="w-3.5 h-3.5 text-slate-400" />
          <span>接入数据源 (需 ADMIN 权限)</span>
        </div>
      </div>
    </div>

    <!-- Analyst Read-Only Notice Banner -->
    <div v-if="!authStore.isAdmin" class="bg-amber-50/80 border border-amber-200/80 rounded-xl p-3.5 flex items-center justify-between text-xs text-amber-900 shadow-2xs">
      <div class="flex items-center space-x-2.5">
        <Lock class="w-4 h-4 text-amber-700 flex-shrink-0" />
        <span class="font-medium">
          当前身份为「数据分析师」，数据源配置处于受控只读模式。您可自由测试网络连通性及切换工作台分析目标。
        </span>
      </div>
      <span class="px-2 py-0.5 text-[10px] font-semibold rounded bg-amber-100 text-amber-800 border border-amber-300">
        只读受控
      </span>
    </div>

    <!-- Data Sources Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="ds in datasources"
        :key="ds.id"
        class="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-2xs hover:shadow-xs hover:border-slate-300 transition-all space-y-4 flex flex-col justify-between"
      >
        <!-- Card Top Section -->
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-2.5">
            <div class="flex items-center space-x-3 min-w-0">
              <div class="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0 border border-blue-100 shadow-2xs">
                <Database class="w-5 h-5" />
              </div>
              <div class="min-w-0">
                <h3 class="text-sm font-bold text-slate-900 truncate" :title="ds.name">{{ ds.name }}</h3>
                <div class="text-[11px] text-slate-400 font-mono truncate mt-0.5">
                  {{ ds.dbType }} · {{ ds.host }}:{{ ds.port }}
                </div>
              </div>
            </div>

            <!-- Health Status -->
            <span
              class="inline-flex items-center space-x-1.5 px-2 py-0.5 text-[11px] font-semibold rounded-full flex-shrink-0 whitespace-nowrap"
              :class="ds.status === 1
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                : 'bg-slate-100 text-slate-500 border border-slate-200'"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="ds.status === 1 ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"
              ></span>
              <span>{{ ds.status === 1 ? '在线' : '离线' }}</span>
            </span>
          </div>

          <!-- Tags Bar -->
          <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
            <span
              v-if="ds.isDefault === 1"
              class="inline-flex items-center space-x-1 px-2 py-0.5 text-[11px] font-semibold rounded-md bg-amber-50 text-amber-800 border border-amber-300"
            >
              <span>⭐ 默认数仓</span>
            </span>
            <span
              v-if="ds.id === datasourceStore.currentDatasourceId"
              class="inline-flex items-center space-x-1 px-2 py-0.5 text-[11px] font-semibold rounded-md bg-blue-50 text-blue-700 border border-blue-200"
            >
              <Check class="w-3 h-3 text-blue-600" />
              <span>当前工作台激活</span>
            </span>
          </div>

          <!-- Specs Info Box -->
          <div class="space-y-1.5 text-xs text-slate-600 bg-slate-50/70 p-3 rounded-xl border border-slate-100 font-mono">
            <div class="flex justify-between">
              <span class="text-slate-400 font-sans">目标数据库:</span>
              <span class="font-semibold text-slate-800">{{ ds.databaseName }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400 font-sans">认证账号:</span>
              <span>{{ ds.username }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400 font-sans">挂载时间:</span>
              <span class="text-[11px] text-slate-500">{{ ds.createdAt?.slice(0, 10) }}</span>
            </div>
          </div>
        </div>

        <!-- Card Actions Footer -->
        <div class="flex items-center justify-between pt-3 border-t border-slate-100">
          <button
            @click="testSingle(ds)"
            :disabled="testingId === ds.id"
            class="text-xs text-blue-600 hover:text-blue-800 font-medium flex items-center space-x-1 cursor-pointer"
          >
            <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': testingId === ds.id }" />
            <span>{{ testingId === ds.id ? '探测中...' : '测试连通性' }}</span>
          </button>

          <div class="flex items-center space-x-1.5 text-xs">
            <!-- 设为默认 -->
            <button
              v-if="ds.isDefault !== 1 && authStore.isAdmin"
              @click="handleSetDefault(ds)"
              :disabled="settingDefaultId === ds.id"
              class="text-amber-700 hover:text-amber-800 hover:bg-amber-50 font-medium px-2 py-1 rounded-lg transition-colors border border-amber-200 flex items-center space-x-1 cursor-pointer"
              title="设为系统全局默认数据源"
            >
              <Star class="w-3 h-3 text-amber-500 fill-amber-400" />
              <span>{{ settingDefaultId === ds.id ? '设置中...' : '设为默认' }}</span>
            </button>

            <!-- 切换工作台 -->
            <button
              v-if="ds.id !== datasourceStore.currentDatasourceId"
              @click="handleSwitchWorkspace(ds.id)"
              class="text-blue-600 hover:text-blue-800 font-semibold px-2 py-1 rounded-lg hover:bg-blue-50 transition-colors cursor-pointer"
              title="切换工作台分析目标为此数据源"
            >
              设为分析目标
            </button>

            <!-- 编辑 / 查看参数 -->
            <button
              v-if="authStore.isAdmin"
              @click="openModal(ds)"
              class="text-slate-600 hover:text-blue-600 font-medium flex items-center space-x-1 px-2 py-1 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
              title="修改连接配置"
            >
              <Edit3 class="w-3.5 h-3.5" />
              <span>编辑</span>
            </button>
            <button
              v-else
              @click="openModal(ds, true)"
              class="text-slate-500 hover:text-slate-800 font-medium flex items-center space-x-1 px-2 py-1 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
              title="查看连接配置 (只读)"
            >
              <Lock class="w-3.5 h-3.5 text-slate-400" />
              <span>查看参数</span>
            </button>

            <!-- 删除 -->
            <button
              v-if="ds.isDefault !== 1 && authStore.isAdmin"
              @click="deleteDs(ds)"
              class="text-rose-500 hover:text-rose-700 font-medium flex items-center space-x-1 px-2 py-1 rounded-lg hover:bg-rose-50 transition-colors cursor-pointer"
              title="注销数据源"
            >
              <Trash2 class="w-3.5 h-3.5" />
              <span>删除</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add / Edit Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4"
      @click.self="showModal = false"
    >
      <div class="bg-white rounded-2xl max-w-lg w-full border border-slate-200 shadow-xl overflow-hidden p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2">
            <div class="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Server class="w-4 h-4" />
            </div>
            <h3 class="text-sm font-bold text-slate-900">
              {{ isReadOnly ? '数据源连接参数 (受控只读)' : (isEditing ? '修改数据源连接参数' : '接入新数据库数据源') }}
            </h3>
          </div>
          <button @click="showModal = false" class="text-slate-400 hover:text-slate-700 text-lg leading-none cursor-pointer">&times;</button>
        </div>

        <!-- Read-only tip banner -->
        <div v-if="isReadOnly" class="p-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center space-x-2">
          <Lock class="w-3.5 h-3.5 text-amber-700 flex-shrink-0" />
          <span>受限只读模式：仅供查阅连接参数，不可直接修改生产配置。</span>
        </div>

        <form @submit.prevent="saveDatasource" class="space-y-3.5 text-xs">
          <div>
            <label class="block font-semibold text-slate-700 mb-1">数仓名称</label>
            <input
              v-model="form.name"
              :disabled="isReadOnly"
              required
              placeholder="例如：CRM客户库、财务商城业务库"
              class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none disabled:bg-slate-50 disabled:text-slate-500"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block font-semibold text-slate-700 mb-1">数据库类型</label>
              <select
                v-model="form.dbType"
                :disabled="isReadOnly"
                class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none bg-white disabled:bg-slate-50 disabled:text-slate-500"
              >
                <option value="MYSQL">MySQL 8.0 / 5.7</option>
                <option value="POSTGRESQL">PostgreSQL</option>
              </select>
            </div>
            <div>
              <label class="block font-semibold text-slate-700 mb-1">目标库名 (Database)</label>
              <input
                v-model="form.databaseName"
                :disabled="isReadOnly"
                required
                placeholder="例如：datapulse_db"
                class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none font-mono disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
          </div>

          <div class="grid grid-cols-3 gap-3">
            <div class="col-span-2">
              <label class="block font-semibold text-slate-700 mb-1">主机 IP / 域名</label>
              <input
                v-model="form.host"
                :disabled="isReadOnly"
                required
                placeholder="127.0.0.1"
                class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none font-mono disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
            <div>
              <label class="block font-semibold text-slate-700 mb-1">端口</label>
              <input
                v-model.number="form.port"
                :disabled="isReadOnly"
                required
                type="number"
                class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none font-mono disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block font-semibold text-slate-700 mb-1">认证用户</label>
              <input
                v-model="form.username"
                :disabled="isReadOnly"
                required
                placeholder="root"
                class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none font-mono disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
            <div>
              <label class="block font-semibold text-slate-700 mb-1">认证密码</label>
              <input
                v-model="form.password"
                :disabled="isReadOnly"
                required
                type="password"
                placeholder="••••••••"
                class="w-full px-3 py-2 rounded-xl border border-slate-300 focus:border-blue-600 focus:outline-none font-mono disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
          </div>

          <!-- Default Toggle -->
          <div class="p-3.5 rounded-xl border border-slate-200 bg-slate-50/80 flex items-center justify-between">
            <div class="pr-3">
              <div class="text-xs font-semibold text-slate-800">
                设为系统默认数据源 (Default)
              </div>
              <p class="text-[11px] text-slate-500 mt-0.5">
                新会话未指定数仓时，大模型 Text-to-SQL 与图表将优先采用此数据源。
              </p>
            </div>

            <button
              type="button"
              @click="!isReadOnly && toggleDefaultInForm()"
              :disabled="isReadOnly"
              class="relative inline-flex h-6 w-11 flex-shrink-0 rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none cursor-pointer"
              :class="[
                form.isDefault === 1 ? 'bg-blue-600' : 'bg-slate-300',
                isReadOnly ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'
              ]"
            >
              <span
                class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out"
                :class="form.isDefault === 1 ? 'translate-x-5' : 'translate-x-0'"
              />
            </button>
          </div>

          <div v-if="testResult" class="p-3 rounded-xl text-xs font-mono" :class="testResult.success ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800 border border-rose-200'">
            {{ testResult.message }}
          </div>

          <div class="flex items-center justify-between pt-3 border-t border-slate-100">
            <button
              type="button"
              @click="testCurrentForm"
              :disabled="testing"
              class="px-3.5 py-2 text-xs font-medium rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700 flex items-center space-x-1 cursor-pointer"
            >
              <Loader2 v-if="testing" class="w-3.5 h-3.5 animate-spin" />
              <span>{{ testing ? '探测中...' : '测试连通性' }}</span>
            </button>

            <div class="flex space-x-2">
              <button
                type="button"
                @click="showModal = false"
                class="px-4 py-2 text-xs font-medium rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer"
              >
                {{ isReadOnly ? '关闭' : '取消' }}
              </button>
              <button
                v-if="!isReadOnly"
                type="submit"
                :disabled="saving"
                class="px-5 py-2 text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white shadow-sm cursor-pointer"
              >
                {{ saving ? '正在保存...' : (isEditing ? '保存修改' : '确认接入') }}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import apiClient from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useDatasourceStore } from '../stores/datasource'
import { useToastStore } from '../stores/toast'
import { Server, Database, Plus, RefreshCw, Loader2, Edit3, Trash2, Check, Star, Lock } from 'lucide-vue-next'

const authStore = useAuthStore()
const datasourceStore = useDatasourceStore()
const toast = useToastStore()

const datasources = ref([])
const showModal = ref(false)
const isReadOnly = ref(false)
const testingId = ref(null)
const settingDefaultId = ref(null)
const testing = ref(false)
const saving = ref(false)
const testResult = ref(null)
const isEditing = ref(false)

const form = reactive({
  id: null,
  name: '',
  dbType: 'MYSQL',
  host: '127.0.0.1',
  port: 3306,
  databaseName: '',
  username: 'root',
  password: '',
  isDefault: 0
})

const fetchDatasources = async () => {
  try {
    const data = await apiClient.get('/datasource/list')
    datasources.value = data
  } catch (err) {
    toast.error(err.message || '获取数据源列表失败')
  }
}

const openModal = (targetDs = null, readOnlyMode = false) => {
  testResult.value = null
  isReadOnly.value = readOnlyMode || !authStore.isAdmin
  if (targetDs) {
    isEditing.value = true
    form.id = targetDs.id
    form.name = targetDs.name
    form.dbType = targetDs.dbType || 'MYSQL'
    form.host = targetDs.host
    form.port = targetDs.port
    form.databaseName = targetDs.databaseName
    form.username = targetDs.username
    form.password = targetDs.password
    form.isDefault = targetDs.isDefault || 0
  } else {
    isEditing.value = false
    form.id = null
    form.name = ''
    form.dbType = 'MYSQL'
    form.host = '127.0.0.1'
    form.port = 3306
    form.databaseName = ''
    form.username = 'root'
    form.password = ''
    form.isDefault = 0
  }
  showModal.value = true
}

const testCurrentForm = async () => {
  testing.value = true
  testResult.value = null
  try {
    await apiClient.post('/datasource/test', form)
    testResult.value = { success: true, message: '✓ 数据源连接测试成功，握手正常' }
    toast.success('数据源网络与认证握手成功')
  } catch (err) {
    testResult.value = { success: false, message: '✕ 连接失败: ' + err.message }
    toast.error('数据源握手失败: ' + err.message)
  } finally {
    testing.value = false
  }
}

const testSingle = async (ds) => {
  testingId.value = ds.id
  try {
    await apiClient.post('/datasource/test', ds)
    toast.success(`数据源 [${ds.name}] 握手成功，连接健康`)
  } catch (err) {
    toast.error(`数据源 [${ds.name}] 握手失败: ` + err.message)
  } finally {
    testingId.value = null
  }
}

const saveDatasource = async () => {
  if (!authStore.isAdmin) {
    toast.warning('权限不足：仅限管理员修改数据源')
    return
  }
  saving.value = true
  try {
    await apiClient.post('/datasource/save', form)
    showModal.value = false
    toast.success('数据源配置已更新')
    await fetchDatasources()
    await datasourceStore.fetchDatasources(true)
  } catch (err) {
    toast.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const deleteDs = async (ds) => {
  if (!authStore.isAdmin) {
    toast.warning('权限不足：仅限管理员删除数据源')
    return
  }
  if (!confirm(`确定注销数据源 [${ds.name}] 吗？`)) return
  try {
    await apiClient.delete(`/datasource/${ds.id}`)
    toast.success(`数据源 [${ds.name}] 已注销`)
    await fetchDatasources()
    await datasourceStore.fetchDatasources(true)
  } catch (err) {
    toast.error(err.message || '删除失败')
  }
}

const toggleDefaultInForm = () => {
  if (isReadOnly.value || !authStore.isAdmin) return
  form.isDefault = form.isDefault === 1 ? 0 : 1
}

const handleSetDefault = async (ds) => {
  if (!authStore.isAdmin) {
    toast.warning('权限不足：仅限管理员设置默认数仓')
    return
  }
  if (ds.isDefault === 1) return
  settingDefaultId.value = ds.id
  try {
    await apiClient.post(`/datasource/${ds.id}/default`)
    toast.success(`已将 [${ds.name}] 设为系统默认数仓`)
    await fetchDatasources()
    await datasourceStore.fetchDatasources(true)
  } catch (err) {
    toast.error(err.message || '设置默认数据源失败')
  } finally {
    settingDefaultId.value = null
  }
}

const handleSwitchWorkspace = (dsId) => {
  datasourceStore.setDatasourceId(dsId)
  const current = datasources.value.find(d => d.id === dsId)
  toast.info(`当前工作台已挂载 [${current?.name || dsId}]`)
}

onMounted(() => {
  fetchDatasources()
})
</script>
