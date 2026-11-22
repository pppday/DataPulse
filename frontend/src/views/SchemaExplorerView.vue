<template>
  <div class="space-y-6">
    <!-- Header & Datasource Switcher -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div class="flex items-center space-x-2">
            <span class="p-1.5 rounded-lg bg-blue-50 text-blue-600 border border-blue-100">
              <Table2 class="w-4 h-4" />
            </span>
            <h1 class="text-base font-bold text-slate-900 tracking-tight">数仓全景字典与语义治理</h1>
            <span class="text-[11px] px-2 py-0.5 rounded-full font-medium bg-blue-50 text-blue-700 border border-blue-200">
              Information Schema 自动抽取
            </span>
          </div>
          <p class="mt-1 text-xs text-slate-500">
            抽取底层物理表结构、字段字典与注释，配置大模型只读访问白名单与 Prompt 语义计算口径。
          </p>
        </div>

        <!-- Controls: Datasource Selector & Refresh -->
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex items-center space-x-2 bg-slate-50 hover:bg-slate-100/80 border border-slate-200/90 px-3 py-1.5 rounded-xl transition-colors">
            <Server class="w-3.5 h-3.5 text-blue-600" />
            <span class="text-xs text-slate-500 font-medium">当前数仓:</span>
            <select
              :value="datasourceStore.currentDatasourceId"
              @change="handleDatasourceChange(Number($event.target.value))"
              class="bg-transparent text-xs font-semibold text-slate-800 focus:outline-none cursor-pointer"
            >
              <option v-for="ds in datasourceStore.datasources" :key="ds.id" :value="ds.id">
                {{ ds.name }} ({{ ds.databaseName }})
              </option>
            </select>
          </div>

          <button
            @click="fetchTables"
            :disabled="loading"
            class="flex items-center space-x-1.5 px-3.5 py-1.5 text-xs font-medium rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700 transition-colors cursor-pointer"
          >
            <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" />
            <span>刷新字典</span>
          </button>
        </div>
      </div>

      <!-- Quick Metrics Summary -->
      <div class="mt-4 pt-3 border-t border-slate-100 flex flex-wrap items-center gap-4 text-xs">
        <div class="flex items-center space-x-1.5 text-slate-600">
          <span class="text-slate-400">底层数据表:</span>
          <span class="font-bold text-slate-900">{{ tables.length }} 张</span>
        </div>
        <span class="text-slate-200">|</span>
        <div class="flex items-center space-x-1.5 text-slate-600">
          <span class="text-slate-400">已开放 AI 分析:</span>
          <span class="font-bold text-emerald-600">{{ whitelistedCount }} 张</span>
        </div>
        <span class="text-slate-200">|</span>
        <div class="flex items-center space-x-1.5 text-slate-600">
          <span class="text-slate-400">敏感隔离:</span>
          <span class="font-bold text-slate-500">{{ tables.length - whitelistedCount }} 张</span>
        </div>
        <span class="text-slate-200">|</span>
        <div class="text-[11px] text-slate-400">
          系统敏感表 (sys_*) 默认隔离，防止系统凭证与运行日志泄露。
        </div>
      </div>
    </div>

    <!-- Analyst Read-Only Notice Banner -->
    <div v-if="!authStore.isAdmin" class="bg-amber-50/80 border border-amber-200/80 rounded-xl p-3.5 flex items-center justify-between text-xs text-amber-900 shadow-2xs">
      <div class="flex items-center space-x-2.5">
        <Lock class="w-4 h-4 text-amber-700 flex-shrink-0" />
        <span class="font-medium">
          当前身份为「数据分析师」，数仓元数据字典处于只读查阅模式。表白名单与语义规则仅供参考，不可更改。
        </span>
      </div>
      <span class="px-2 py-0.5 text-[10px] font-semibold rounded bg-amber-100 text-amber-800 border border-amber-300">
        只读查阅
      </span>
    </div>

    <!-- Tables Grid & Detail View -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- Left: Table Selector Cards with Search -->
      <div class="lg:col-span-4 space-y-3">
        <!-- Search Box -->
        <div class="relative">
          <Search class="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索物理表名或业务注释..."
            class="w-full pl-9 pr-3 py-2 bg-white rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-blue-600 shadow-2xs"
          />
        </div>

        <div class="space-y-2 max-h-[calc(100vh-280px)] overflow-y-auto pr-1">
          <div
            v-for="table in filteredTables"
            :key="table.tableName"
            @click="selectTable(table)"
            class="p-3.5 rounded-xl border transition-all cursor-pointer text-left relative group shadow-2xs"
            :class="selectedTable?.tableName === table.tableName
              ? 'bg-blue-50/80 border-blue-300 shadow-xs ring-1 ring-blue-300'
              : 'bg-white border-slate-200/90 hover:border-slate-300 hover:bg-slate-50/60'"
          >
            <div class="flex items-start justify-between">
              <div class="flex items-center space-x-2">
                <span
                  class="px-1.5 py-0.5 text-[9px] font-bold rounded"
                  :class="table.tableName.startsWith('fact_') ? 'bg-amber-100 text-amber-800' : (table.tableName.startsWith('sys_') ? 'bg-slate-100 text-slate-700' : 'bg-blue-100 text-blue-800')"
                >
                  {{ table.tableName.startsWith('fact_') ? 'FACT' : (table.tableName.startsWith('sys_') ? 'SYS' : 'DIM') }}
                </span>
                <h3 class="text-xs font-bold text-slate-900 font-mono">{{ table.tableName }}</h3>
              </div>
              
              <!-- Whitelist Pill -->
              <span
                class="px-2 py-0.5 text-[10px] font-semibold rounded-full border transition-colors flex items-center space-x-1"
                :class="table.isWhitelisted
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                  : 'bg-slate-100 text-slate-500 border-slate-200'"
              >
                <CheckCircle2 v-if="table.isWhitelisted" class="w-3 h-3 text-emerald-600" />
                <Lock v-else class="w-3 h-3 text-slate-400" />
                <span>{{ table.isWhitelisted ? 'AI已开放' : '已隔离' }}</span>
              </span>
            </div>

            <p class="mt-1.5 text-xs text-slate-500 line-clamp-1">
              {{ table.tableComment || '暂无业务注释' }}
            </p>

            <div class="mt-2 flex items-center justify-between text-[11px] text-slate-400">
              <span>{{ table.rowCount }} 行记录 · {{ table.columns?.length || 0 }} 字段</span>
              <span v-if="table.customPromptRules" class="text-blue-600 font-medium flex items-center space-x-1 text-[10px]">
                <BookOpen class="w-3 h-3" />
                <span>已配口径</span>
              </span>
            </div>
          </div>

          <div v-if="filteredTables.length === 0" class="p-8 text-center text-xs text-slate-400 bg-white rounded-xl border border-dashed border-slate-200">
            无匹配的数据表
          </div>
        </div>
      </div>

      <!-- Right: Columns & Live Preview Table -->
      <div class="lg:col-span-8 space-y-6">
        <div v-if="selectedTable" class="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-2xs space-y-4">
          <!-- Table Title & Action Bar -->
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <div class="flex items-center space-x-2">
                <h2 class="text-base font-bold text-slate-900 font-mono">{{ selectedTable.tableName }}</h2>
                <span class="text-xs text-slate-500 font-normal">({{ selectedTable.tableComment || '无注释' }})</span>
              </div>
              <div class="text-xs text-slate-400 mt-0.5 font-mono">
                {{ selectedTable.rowCount }} 条记录 · {{ selectedTable.columns?.length }} 个物理字段
              </div>
            </div>

            <!-- Whitelist Toggle & Config Button -->
            <div class="flex items-center space-x-2">
              <button
                @click="toggleWhitelist(selectedTable)"
                :disabled="!authStore.isAdmin"
                type="button"
                class="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all cursor-pointer"
                :class="[
                  selectedTable.isWhitelisted
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-300'
                    : 'bg-slate-100 text-slate-600 border-slate-200',
                  !authStore.isAdmin ? 'cursor-not-allowed opacity-80' : 'hover:opacity-90'
                ]"
                :title="!authStore.isAdmin ? '权限不足：仅企业管理员可修改 AI 白名单' : (selectedTable.isWhitelisted ? '点击关闭 AI 访问' : '点击开放 AI 访问')"
              >
                <CheckCircle2 v-if="selectedTable.isWhitelisted" class="w-3.5 h-3.5 text-emerald-600" />
                <Lock v-else class="w-3.5 h-3.5 text-slate-400" />
                <span>{{ selectedTable.isWhitelisted ? 'AI 分析权限: 已开放' : 'AI 分析权限: 已隔离' }}</span>
              </button>

              <button
                @click="openConfigModal"
                type="button"
                class="flex items-center space-x-1 px-3 py-1.5 rounded-xl text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors cursor-pointer"
              >
                <Settings2 v-if="authStore.isAdmin" class="w-3.5 h-3.5" />
                <Lock v-else class="w-3.5 h-3.5 text-slate-400" />
                <span>{{ authStore.isAdmin ? '配置业务口径' : '查看口径规则' }}</span>
              </button>
            </div>
          </div>

          <!-- Semantic Rules Banner (if configured) -->
          <div v-if="selectedTable.customPromptRules" class="bg-amber-50/70 border border-amber-200 rounded-xl p-3 text-xs text-amber-900 flex items-start space-x-2">
            <BookOpen class="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
            <div>
              <div class="font-semibold text-amber-950">该表专属 Text-to-SQL 业务计算口径规则：</div>
              <div class="mt-0.5 text-amber-800 font-mono">{{ selectedTable.customPromptRules }}</div>
            </div>
          </div>

          <!-- Sub Tabs: Columns vs Live Preview -->
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl text-xs">
              <button
                @click="activeSubTab = 'columns'"
                class="px-3 py-1 rounded-lg font-medium transition-all cursor-pointer"
                :class="activeSubTab === 'columns' ? 'bg-white text-slate-900 shadow-2xs font-semibold' : 'text-slate-600 hover:text-slate-900'"
              >
                字段字典 ({{ selectedTable.columns?.length }})
              </button>
              <button
                @click="loadPreview(selectedTable.tableName)"
                class="px-3 py-1 rounded-lg font-medium transition-all cursor-pointer"
                :class="activeSubTab === 'preview' ? 'bg-white text-slate-900 shadow-2xs font-semibold' : 'text-slate-600 hover:text-slate-900'"
              >
                采样预览 (前10条)
              </button>
            </div>

            <span class="text-[11px] text-slate-400 font-mono">
              DS #{{ datasourceStore.currentDatasourceId }}
            </span>
          </div>

          <!-- Column Dictionary Table -->
          <div v-if="activeSubTab === 'columns'" class="overflow-x-auto rounded-xl border border-slate-200/90">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-semibold uppercase text-[11px]">
                <tr>
                  <th class="px-4 py-2.5">字段名</th>
                  <th class="px-4 py-2.5">数据类型</th>
                  <th class="px-4 py-2.5">主键约束</th>
                  <th class="px-4 py-2.5">业务含义 / 注释</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 text-slate-700">
                <tr v-for="col in selectedTable.columns" :key="col.columnName" class="hover:bg-slate-50/80 transition-colors">
                  <td class="px-4 py-2.5 font-mono font-medium text-slate-900">
                    {{ col.columnName }}
                  </td>
                  <td class="px-4 py-2.5 font-mono text-slate-500">
                    {{ col.columnType }}
                  </td>
                  <td class="px-4 py-2.5">
                    <span v-if="col.isPrimaryKey" class="px-2 py-0.5 text-[10px] font-bold rounded bg-amber-50 text-amber-700 border border-amber-200">
                      PK
                    </span>
                    <span v-else class="text-slate-300">-</span>
                  </td>
                  <td class="px-4 py-2.5 text-slate-600">
                    {{ col.columnComment || '—' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Live Sample Preview -->
          <div v-if="activeSubTab === 'preview'" class="space-y-3">
            <div v-if="previewLoading" class="p-8 text-center text-xs text-slate-400">
              <Loader2 class="w-5 h-5 mx-auto animate-spin mb-2 text-blue-600" />
              正在从目标数仓直连采样数据...
            </div>
            <div v-else-if="previewData" class="overflow-x-auto rounded-xl border border-slate-200/90 max-h-96">
              <table class="w-full text-left text-xs font-mono">
                <thead class="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase text-[11px] sticky top-0">
                  <tr>
                    <th v-for="c in previewData.columns" :key="c" class="px-3.5 py-2.5 whitespace-nowrap">
                      {{ c }}
                    </th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100 text-slate-700">
                  <tr v-for="(r, idx) in previewData.rows" :key="idx" class="hover:bg-slate-50/80 transition-colors">
                    <td v-for="c in previewData.columns" :key="c" class="px-3.5 py-2 whitespace-nowrap">
                      {{ r[c] !== null ? r[c] : '-' }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="p-6 text-center text-xs text-slate-400">
              点击上方按钮加载数据采样
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Semantic Rules Modal -->
    <div
      v-if="showConfigModal"
      class="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4"
    >
      <div class="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4 animate-in fade-in zoom-in-95 duration-150">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2">
            <div class="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Settings2 class="w-4 h-4" />
            </div>
            <div>
              <h3 class="text-sm font-bold text-slate-900">
                {{ authStore.isAdmin ? '配置业务口径与白名单' : '数据表口径规则 (受控只读)' }}
              </h3>
              <p class="text-xs text-slate-400 font-mono">{{ configForm.tableName }}</p>
            </div>
          </div>
          <button @click="showConfigModal = false" class="text-slate-400 hover:text-slate-600 cursor-pointer">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Read-only tip banner -->
        <div v-if="!authStore.isAdmin" class="p-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex items-center space-x-2">
          <Lock class="w-3.5 h-3.5 text-amber-700 flex-shrink-0" />
          <span>受控只读模式：仅供查阅业务口径，不可直接修改配置。</span>
        </div>

        <form @submit.prevent="saveConfig" class="space-y-4 text-xs">
          <div>
            <label class="block font-medium text-slate-700 mb-1">业务中文注释</label>
            <input
              v-model="configForm.tableComment"
              :disabled="!authStore.isAdmin"
              type="text"
              placeholder="例如：全渠道销售订单明细事实表"
              class="w-full rounded-xl border border-slate-300 px-3 py-2 text-slate-900 focus:border-blue-600 focus:outline-none disabled:bg-slate-50 disabled:text-slate-500"
            />
          </div>

          <div>
            <label class="block font-medium text-slate-700 mb-1">
              Text-to-SQL 业务计算口径规则 (Prompt Injection)
            </label>
            <textarea
              v-model="configForm.customRules"
              :disabled="!authStore.isAdmin"
              rows="3"
              placeholder="例如：'GMV计算需排除status=CANCELLED的订单，毛利计算公式为pay_amount - cost_amount'..."
              class="w-full rounded-xl border border-slate-300 p-3 text-slate-900 focus:border-blue-600 focus:outline-none font-mono disabled:bg-slate-50 disabled:text-slate-500"
            ></textarea>
            <p class="text-[11px] text-slate-400 mt-1">
              规则将注入大模型 Few-Shot 上下文，确保生成出的 SQL 严格符合业务口径。
            </p>
          </div>

          <div class="flex items-center space-x-2 pt-2">
            <input
              v-model="configForm.isWhitelisted"
              :disabled="!authStore.isAdmin"
              type="checkbox"
              id="modalWhitelisted"
              class="rounded border-slate-300 text-blue-600 focus:ring-blue-500 disabled:opacity-60"
            />
            <label for="modalWhitelisted" class="font-medium text-slate-700 cursor-pointer">
              纳入大模型只读查询与 AST 沙箱白名单
            </label>
          </div>

          <div class="flex items-center justify-end space-x-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              @click="showConfigModal = false"
              class="px-4 py-2 rounded-xl text-slate-600 hover:bg-slate-100 cursor-pointer"
            >
              {{ authStore.isAdmin ? '取消' : '关闭' }}
            </button>
            <button
              v-if="authStore.isAdmin"
              type="submit"
              :disabled="savingConfig"
              class="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm flex items-center space-x-1 cursor-pointer"
            >
              <Loader2 v-if="savingConfig" class="w-3.5 h-3.5 animate-spin" />
              <span>保存配置</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({
  name: 'SchemaExplorerView'
})

import { ref, computed, watch, onMounted } from 'vue'
import apiClient from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useDatasourceStore } from '../stores/datasource'
import { useToastStore } from '../stores/toast'
import {
  Table2,
  RefreshCw,
  Loader2,
  Server,
  CheckCircle2,
  Settings2,
  BookOpen,
  Lock,
  X,
  Search
} from 'lucide-vue-next'

const authStore = useAuthStore()
const datasourceStore = useDatasourceStore()
const toast = useToastStore()

const tables = ref([])
const selectedTable = ref(null)
const loading = ref(false)
const activeSubTab = ref('columns')
const searchQuery = ref('')

const previewLoading = ref(false)
const previewData = ref(null)

const showConfigModal = ref(false)
const savingConfig = ref(false)
const configForm = ref({
  tableName: '',
  tableComment: '',
  customRules: '',
  isWhitelisted: true
})

const whitelistedCount = computed(() => {
  return tables.value.filter(t => t.isWhitelisted).length
})

const filteredTables = computed(() => {
  if (!searchQuery.value.trim()) return tables.value
  const q = searchQuery.value.trim().toLowerCase()
  return tables.value.filter(t => 
    t.tableName.toLowerCase().includes(q) || 
    (t.tableComment && t.tableComment.toLowerCase().includes(q))
  )
})

const fetchTables = async () => {
  if (!datasourceStore.currentDatasourceId) return
  loading.value = true
  try {
    const data = await apiClient.get(`/schema/tables?datasourceId=${datasourceStore.currentDatasourceId}`)
    tables.value = data || []
    if (tables.value.length > 0) {
      const found = tables.value.find(t => t.tableName === selectedTable.value?.tableName)
      selectTable(found || tables.value[0])
    } else {
      selectedTable.value = null
    }
  } catch (err) {
    toast.error(err.message || '获取数仓元数据失败')
  } finally {
    loading.value = false
  }
}

const handleDatasourceChange = (newId) => {
  datasourceStore.setDatasourceId(newId)
  selectedTable.value = null
  previewData.value = null
  fetchTables()
}

const selectTable = (table) => {
  selectedTable.value = table
  activeSubTab.value = 'columns'
  previewData.value = null
}

const loadPreview = async (tableName) => {
  activeSubTab.value = 'preview'
  if (previewData.value && previewData.value.tableName === tableName) return

  previewLoading.value = true
  try {
    const data = await apiClient.get(`/schema/preview/${tableName}?datasourceId=${datasourceStore.currentDatasourceId}&limit=10`)
    previewData.value = data
  } catch (err) {
    toast.error(err.message || '数据预览采样失败')
  } finally {
    previewLoading.value = false
  }
}

const toggleWhitelist = async (table) => {
  if (!authStore.isAdmin) {
    toast.warning('权限不足：仅限管理员调整 AI 白名单')
    return
  }
  const newStatus = !table.isWhitelisted
  try {
    await apiClient.post('/schema/table/config', {
      datasourceId: datasourceStore.currentDatasourceId,
      tableName: table.tableName,
      isWhitelisted: newStatus,
      tableComment: table.tableComment,
      customRules: table.customPromptRules
    })
    table.isWhitelisted = newStatus
    toast.success(`表 [${table.tableName}] ${newStatus ? '已开放 AI 分析' : '已移入隔离保护'}`)
  } catch (err) {
    toast.error('修改白名单状态失败: ' + (err.message || ''))
  }
}

const openConfigModal = () => {
  if (!selectedTable.value) return
  configForm.value = {
    tableName: selectedTable.value.tableName,
    tableComment: selectedTable.value.tableComment || '',
    customRules: selectedTable.value.customPromptRules || '',
    isWhitelisted: !!selectedTable.value.isWhitelisted
  }
  showConfigModal.value = true
}

const saveConfig = async () => {
  if (!authStore.isAdmin) {
    toast.warning('权限不足：仅限管理员修改业务规则')
    return
  }
  savingConfig.value = true
  try {
    await apiClient.post('/schema/table/config', {
      datasourceId: datasourceStore.currentDatasourceId,
      tableName: configForm.value.tableName,
      isWhitelisted: configForm.value.isWhitelisted,
      tableComment: configForm.value.tableComment,
      customRules: configForm.value.customRules
    })
    if (selectedTable.value) {
      selectedTable.value.tableComment = configForm.value.tableComment
      selectedTable.value.customPromptRules = configForm.value.customRules
      selectedTable.value.isWhitelisted = configForm.value.isWhitelisted
    }
    showConfigModal.value = false
    toast.success(`表 [${configForm.value.tableName}] 口径配置已保存`)
  } catch (err) {
    toast.error('保存配置失败: ' + (err.message || ''))
  } finally {
    savingConfig.value = false
  }
}

watch(() => datasourceStore.currentDatasourceId, (newId, oldId) => {
  if (oldId && newId !== oldId) {
    selectedTable.value = null
    previewData.value = null
    fetchTables()
  }
})

onMounted(async () => {
  await datasourceStore.fetchDatasources()
  await fetchTables()
})
</script>
