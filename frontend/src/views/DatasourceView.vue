<template>
  <div class="space-y-6">
    <!-- Header -->
    <PageHeader
      :icon="Server"
      eyebrow="Connections"
      title="企业动态数仓数据源管理"
      badge="HikariCP 连接池热挂载"
      badge-tone="success"
      description="支持动态挂载异构业务库，秒级连通测试与连接池生命周期隔离管理。"
    >
      <template #actions>
        <button
          v-if="authStore.isAdmin"
          @click="openModal()"
          class="dp-btn-primary"
        >
          <Plus class="w-3.5 h-3.5" />
          <span>接入新数据源</span>
        </button>
        <div
          v-else
          class="dp-badge-neutral px-3 py-1.5 text-xs cursor-not-allowed select-none"
          title="权限受限：仅企业管理员可添加新数据源"
        >
          <Lock class="w-3.5 h-3.5 text-slate-400" />
          <span>接入数据源 (需 ADMIN 权限)</span>
        </div>
      </template>
    </PageHeader>

    <!-- Analyst Read-Only Notice Banner -->
    <div v-if="!authStore.isAdmin" class="dp-notice-warning">
      <div class="flex items-center space-x-2.5">
        <Lock class="w-4 h-4 text-accent-700 flex-shrink-0" />
        <span class="font-medium">
          当前身份为「数据分析师」，数据源配置处于受控只读模式。您可自由测试网络连通性及切换工作台分析目标。
        </span>
      </div>
      <span class="dp-badge-warning">
        只读受控
      </span>
    </div>

    <!-- Data Sources Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="(ds, idx) in datasources"
        :key="ds.id"
        class="dp-card dp-card-hover p-5 dp-rise flex flex-col justify-between space-y-4"
        :style="{ animationDelay: (idx * 60) + 'ms' }"
      >
        <!-- Card Top Section -->
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-2.5">
            <div class="flex items-center space-x-3 min-w-0">
              <div
                class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                :class="ds.status === 1
                  ? 'bg-ink-900 text-[#FAF9F6]'
                  : 'bg-slate-100 text-slate-400 border border-slate-200'"
              >
                <Database class="w-4 h-4" />
              </div>
              <div class="min-w-0">
                <h3 class="text-sm font-semibold text-ink-900 truncate" :title="ds.name">{{ ds.name }}</h3>
                <div class="text-[11px] text-slate-400 font-mono truncate mt-0.5">
                  {{ ds.dbType }} · {{ ds.host }}:{{ ds.port }}
                </div>
              </div>
            </div>

            <!-- Health Status -->
            <span :class="ds.status === 1 ? 'dp-badge-success' : 'dp-badge-neutral'">
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="ds.status === 1 ? 'bg-pine-600' : 'bg-slate-400'"
              ></span>
              <span>{{ ds.status === 1 ? '在线' : '离线' }}</span>
            </span>
          </div>

          <!-- Tags Bar -->
          <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
            <span v-if="ds.isDefault === 1" class="dp-badge-accent">
              <Star class="w-3 h-3 fill-accent-500 text-accent-600" />
              <span>默认数仓</span>
            </span>
            <span v-if="ds.id === datasourceStore.currentDatasourceId" class="dp-badge-success">
              <Check class="w-3 h-3 text-pine-700" />
              <span>当前工作台激活</span>
            </span>
          </div>

          <!-- Specs Info Box -->
          <div class="space-y-1.5 text-xs text-slate-600 rounded-xl bg-slate-50/80 border border-slate-200/60 p-3 font-mono">
            <div class="flex justify-between">
              <span class="text-slate-400 font-sans">目标数据库:</span>
              <span class="font-semibold text-ink-900">{{ ds.databaseName }}</span>
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
        <div class="pt-3 border-t border-slate-100 space-y-2">
          <!-- Primary Action Row -->
          <div class="flex items-center justify-between gap-2">
            <button
              @click="testSingle(ds)"
              :disabled="testingId === ds.id"
              class="dp-btn-ghost text-ink-900 hover:bg-slate-100 px-2 py-1 whitespace-nowrap flex-shrink-0"
            >
              <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': testingId === ds.id }" />
              <span>{{ testingId === ds.id ? '探测中...' : '测试连通性' }}</span>
            </button>

            <div class="flex items-center gap-1 text-xs flex-shrink-0">
              <!-- 编辑 / 查看参数 -->
              <button
                v-if="authStore.isAdmin"
                @click="openModal(ds)"
                class="dp-btn-ghost px-2 py-1 whitespace-nowrap flex-shrink-0"
                title="修改连接配置"
              >
                <Edit3 class="w-3.5 h-3.5" />
                <span>编辑</span>
              </button>
              <button
                v-else
                @click="openModal(ds, true)"
                class="dp-btn-ghost px-2 py-1 whitespace-nowrap flex-shrink-0"
                title="查看连接配置 (只读)"
              >
                <Lock class="w-3.5 h-3.5 text-slate-400" />
                <span>查看参数</span>
              </button>

              <!-- 删除 -->
              <button
                v-if="ds.isDefault !== 1 && authStore.isAdmin"
                @click="deleteDs(ds)"
                class="dp-btn-danger px-2 py-1 whitespace-nowrap flex-shrink-0"
                title="注销数据源"
              >
                <Trash2 class="w-3.5 h-3.5" />
                <span>删除</span>
              </button>
            </div>
          </div>

          <!-- Workspace & Default Switch Row -->
          <div
            v-if="(ds.isDefault !== 1 && authStore.isAdmin) || ds.id !== datasourceStore.currentDatasourceId"
            class="flex items-center gap-2 text-xs"
          >
            <button
              v-if="ds.id !== datasourceStore.currentDatasourceId"
              @click="handleSwitchWorkspace(ds.id)"
              class="dp-btn flex-1 px-2.5 py-1.5 text-pine-700 bg-pine-50/70 border border-pine-200/80 hover:bg-pine-50 font-semibold whitespace-nowrap"
              title="切换工作台分析目标为此数据源"
            >
              设为分析目标
            </button>

            <button
              v-if="ds.isDefault !== 1 && authStore.isAdmin"
              @click="handleSetDefault(ds)"
              :disabled="settingDefaultId === ds.id"
              class="dp-btn flex-1 px-2.5 py-1.5 text-accent-700 bg-accent-50/70 border border-accent-200/80 hover:bg-accent-50 whitespace-nowrap"
              title="设为系统全局默认数据源"
            >
              <Star class="w-3 h-3 text-accent-600 fill-accent-500" />
              <span>{{ settingDefaultId === ds.id ? '设置中...' : '设为默认' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add / Edit Modal -->
    <div
      v-if="showModal"
      class="dp-modal-mask"
      @click.self="showModal = false"
    >
      <div class="dp-modal max-w-lg space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2.5">
            <span class="dp-icon-mark w-8 h-8">
              <Server class="w-4 h-4" />
            </span>
            <h3 class="dp-section-title">
              {{ isReadOnly ? '数据源连接参数 (受控只读)' : (isEditing ? '修改数据源连接参数' : '接入新数据库数据源') }}
            </h3>
          </div>
          <button @click="showModal = false" class="dp-btn-ghost p-1.5 rounded-full">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Read-only tip banner -->
        <div v-if="isReadOnly" class="p-2.5 rounded-xl bg-accent-50/70 border border-accent-200 text-accent-800 text-xs flex items-center space-x-2">
          <Lock class="w-3.5 h-3.5 text-accent-700 flex-shrink-0" />
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
              class="dp-input py-2"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block font-semibold text-slate-700 mb-1">数据库类型</label>
              <select
                v-model="form.dbType"
                @change="handleDbTypeChange"
                :disabled="isReadOnly"
                class="dp-input py-2 bg-white"
              >
                <option value="MYSQL">MySQL 8.0 / 5.7</option>
                <option value="POSTGRESQL">PostgreSQL 16 / pgvector</option>
              </select>
            </div>
            <div>
              <label class="block font-semibold text-slate-700 mb-1">目标库名 (Database)</label>
              <input
                v-model="form.databaseName"
                :disabled="isReadOnly"
                required
                placeholder="例如：datapulse_db"
                class="dp-input py-2 font-mono"
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
                placeholder="127.0.0.1 / IPv6 / 域名"
                class="dp-input py-2 font-mono"
              />
            </div>
            <div>
              <label class="block font-semibold text-slate-700 mb-1">端口</label>
              <input
                v-model.number="form.port"
                :disabled="isReadOnly"
                required
                type="number"
                class="dp-input py-2 font-mono"
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
                class="dp-input py-2 font-mono"
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
                class="dp-input py-2 font-mono"
              />
            </div>
          </div>

          <!-- Default Toggle -->
          <div class="p-3.5 rounded-xl bg-slate-50/80 border border-slate-200/70 flex items-center justify-between">
            <div class="pr-3">
              <div class="text-xs font-semibold text-ink-900">
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
              class="relative inline-flex h-6 w-11 flex-shrink-0 rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none"
              :class="[
                form.isDefault === 1 ? 'bg-ink-900' : 'bg-slate-200',
                isReadOnly ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'
              ]"
            >
              <span
                class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out"
                :class="form.isDefault === 1 ? 'translate-x-5' : 'translate-x-0'"
              />
            </button>
          </div>

          <div
            v-if="testResult"
            class="p-3 rounded-xl text-xs font-mono border"
            :class="testResult.success
              ? 'bg-pine-50 text-pine-800 border-pine-200'
              : 'bg-crimson-50 text-crimson-700 border-crimson-200'"
          >
            {{ testResult.message }}
          </div>

          <div class="flex items-center justify-between pt-3 border-t border-slate-100">
            <button
              type="button"
              @click="testCurrentForm"
              :disabled="testing"
              class="dp-btn-secondary"
            >
              <Loader2 v-if="testing" class="w-3.5 h-3.5 animate-spin" />
              <span>{{ testing ? '探测中...' : '测试连通性' }}</span>
            </button>

            <div class="flex space-x-2">
              <button
                type="button"
                @click="showModal = false"
                class="dp-btn-ghost px-4 py-2"
              >
                {{ isReadOnly ? '关闭' : '取消' }}
              </button>
              <button
                v-if="!isReadOnly"
                type="submit"
                :disabled="saving"
                class="dp-btn-primary px-5"
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
import PageHeader from '../components/PageHeader.vue'
import { Server, Database, Plus, RefreshCw, Loader2, Edit3, Trash2, Check, Star, Lock, X } from 'lucide-vue-next'

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

const handleDbTypeChange = () => {
  if (form.dbType === 'POSTGRESQL') {
    if (!form.port || form.port === 3306) form.port = 5432
    if (!isEditing.value && form.username === 'root') form.username = 'postgres'
  } else {
    if (!form.port || form.port === 5432) form.port = 3306
    if (!isEditing.value && form.username === 'postgres') form.username = 'root'
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
