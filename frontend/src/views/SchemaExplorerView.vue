<template>
  <div class="space-y-6">
    <!-- Header & Datasource Switcher -->
    <PageHeader
      :icon="Table2"
      eyebrow="Data Catalog"
      title="数仓全景字典与语义治理"
      badge="Information Schema 自动抽取"
      badge-tone="accent"
      description="抽取底层物理表结构、字段字典与注释，配置大模型只读访问白名单与 Prompt 语义计算口径。"
    >
      <template #actions>
        <div class="flex flex-wrap items-center gap-3">
          <div class="dp-select-pill">
            <Server class="w-3.5 h-3.5 text-slate-500 flex-shrink-0" />
            <span class="text-xs text-slate-500 font-medium">当前数仓:</span>
            <select
              :value="datasourceStore.currentDatasourceId"
              @change="handleDatasourceChange(Number($event.target.value))"
              class="bg-transparent text-xs font-semibold text-ink-900 focus:outline-none cursor-pointer"
            >
              <option v-for="ds in datasourceStore.datasources" :key="ds.id" :value="ds.id">
                {{ ds.name }} ({{ ds.databaseName }})
              </option>
            </select>
          </div>

          <button
            @click="fetchTables"
            :disabled="loading"
            class="dp-btn-secondary"
          >
            <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" />
            <span>刷新字典</span>
          </button>
        </div>
      </template>

      <!-- Quick Metrics Summary -->
      <div class="flex flex-wrap items-center gap-4 text-xs">
        <div class="flex items-baseline gap-1.5">
          <span class="text-slate-400">底层数据表</span>
          <span class="font-display text-lg font-semibold tabular-nums text-ink-900">{{ tables.length }}</span>
          <span class="text-slate-400">张</span>
        </div>
        <span class="w-px h-4 bg-slate-200"></span>
        <div class="flex items-baseline gap-1.5">
          <span class="text-slate-400">已开放 AI 分析</span>
          <span class="font-display text-lg font-semibold tabular-nums text-pine-600">{{ whitelistedCount }}</span>
          <span class="text-slate-400">张</span>
        </div>
        <span class="w-px h-4 bg-slate-200"></span>
        <div class="flex items-baseline gap-1.5">
          <span class="text-slate-400">敏感隔离</span>
          <span class="font-display text-lg font-semibold tabular-nums text-slate-500">{{ tables.length - whitelistedCount }}</span>
          <span class="text-slate-400">张</span>
        </div>
        <span class="w-px h-4 bg-slate-200 hidden sm:inline-block"></span>
        <div class="text-[11px] text-slate-400">
          系统敏感表 (sys_*) 默认隔离，防止系统凭证与运行日志泄露。
        </div>
      </div>
    </PageHeader>

    <!-- Analyst Read-Only Notice Banner -->
    <div v-if="!authStore.isAdmin" class="dp-notice-warning">
      <div class="flex items-center space-x-2.5">
        <Lock class="w-4 h-4 text-accent-700 flex-shrink-0" />
        <span class="font-medium">
          当前身份为「数据分析师」，数仓元数据字典处于只读查阅模式。表白名单与语义规则仅供参考，不可更改。
        </span>
      </div>
      <span class="dp-badge-warning">
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
            class="dp-input pl-9 py-2"
          />
        </div>

        <div class="space-y-2 max-h-[calc(100vh-280px)] overflow-y-auto pr-1">
          <div
            v-for="table in filteredTables"
            :key="table.tableName"
            @click="selectTable(table)"
            class="relative p-3.5 rounded-xl transition-all duration-150 cursor-pointer text-left group"
            :class="selectedTable?.tableName === table.tableName
              ? 'bg-white border border-slate-300 shadow-soft'
              : 'bg-white/75 border border-slate-200/80 hover:bg-white hover:border-slate-300'"
          >
            <span
              v-if="selectedTable?.tableName === table.tableName"
              class="absolute left-0 top-3 bottom-3 w-[2.5px] rounded-r-full bg-ink-900"
            ></span>

            <div class="flex items-start justify-between">
              <div class="flex items-center space-x-2">
                <span
                  class="rounded-md px-1.5 py-0.5 text-[9px] font-bold tracking-wider border"
                  :class="table.tableName.startsWith('v_')
                    ? 'bg-sky-50 text-sky-700 border-sky-200'
                    : (table.tableName.startsWith('fact_')
                      ? 'bg-accent-50 text-accent-700 border-accent-200'
                      : (table.tableName.startsWith('sys_')
                        ? 'bg-slate-100 text-slate-600 border-slate-200'
                        : 'bg-pine-50 text-pine-700 border-pine-200'))"
                >
                  {{ table.tableName.startsWith('v_') ? 'VIEW' : (table.tableName.startsWith('fact_') ? 'FACT' : (table.tableName.startsWith('sys_') ? 'SYS' : 'DIM')) }}
                </span>
                <h3 class="text-xs font-bold text-ink-900 font-mono">{{ table.tableName }}</h3>
              </div>

              <!-- Whitelist Pill -->
              <span :class="table.isWhitelisted ? 'dp-badge-success' : 'dp-badge-neutral'">
                <CheckCircle2 v-if="table.isWhitelisted" class="w-3 h-3 text-pine-600" />
                <Lock v-else class="w-3 h-3 text-slate-400" />
                <span>{{ table.isWhitelisted ? 'AI已开放' : '已隔离' }}</span>
              </span>
            </div>

            <p class="mt-1.5 text-xs text-slate-500 line-clamp-1">
              {{ table.tableComment || '暂无业务注释' }}
            </p>

            <div class="mt-2 flex items-center justify-between text-[11px] text-slate-400">
              <span class="tabular-nums">{{ table.rowCount }} 行记录 · {{ table.columns?.length || 0 }} 字段</span>
              <span v-if="table.customPromptRules" class="text-accent-700 font-medium flex items-center space-x-1 text-[10px]">
                <BookOpen class="w-3 h-3" />
                <span>已配口径</span>
              </span>
            </div>
          </div>

          <div v-if="filteredTables.length === 0" class="dp-card p-8 text-center text-xs text-slate-400 border-dashed">
            无匹配的数据表
          </div>
        </div>
      </div>

      <!-- Right: Columns & Live Preview Table -->
      <div class="lg:col-span-8 space-y-6">
        <div v-if="selectedTable" class="dp-card p-5 space-y-4 dp-rise">
          <!-- Table Title & Action Bar -->
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
            <div>
              <div class="flex items-center space-x-2">
                <h2 class="text-base font-bold text-ink-900 font-mono">{{ selectedTable.tableName }}</h2>
                <span class="text-xs text-slate-500 font-normal">({{ selectedTable.tableComment || '无注释' }})</span>
              </div>
              <div class="text-xs text-slate-400 mt-0.5 font-mono tabular-nums">
                {{ selectedTable.rowCount }} 条记录 · {{ selectedTable.columns?.length }} 个物理字段
              </div>
            </div>

            <!-- Whitelist Toggle & Config Button -->
            <div class="flex items-center space-x-2">
              <button
                @click="toggleWhitelist(selectedTable)"
                :disabled="!authStore.isAdmin"
                type="button"
                class="dp-btn px-3 py-1.5 font-semibold border"
                :class="[
                  selectedTable.isWhitelisted
                    ? 'bg-pine-50 text-pine-700 border-pine-200'
                    : 'bg-slate-100 text-slate-600 border-slate-200',
                  !authStore.isAdmin ? 'cursor-not-allowed opacity-80' : 'hover:opacity-90'
                ]"
                :title="!authStore.isAdmin ? '权限不足：仅企业管理员可修改 AI 白名单' : (selectedTable.isWhitelisted ? '点击关闭 AI 访问' : '点击开放 AI 访问')"
              >
                <CheckCircle2 v-if="selectedTable.isWhitelisted" class="w-3.5 h-3.5 text-pine-600" />
                <Lock v-else class="w-3.5 h-3.5 text-slate-400" />
                <span>{{ selectedTable.isWhitelisted ? 'AI 分析权限: 已开放' : 'AI 分析权限: 已隔离' }}</span>
              </button>

              <button
                @click="openConfigModal"
                type="button"
                class="dp-btn-secondary px-3 py-1.5 font-semibold"
              >
                <Settings2 v-if="authStore.isAdmin" class="w-3.5 h-3.5 text-accent-600" />
                <Lock v-else class="w-3.5 h-3.5 text-slate-400" />
                <span>{{ authStore.isAdmin ? '配置业务口径' : '查看口径规则' }}</span>
              </button>
            </div>
          </div>

          <!-- Semantic Rules Banner (if configured) -->
          <div
            v-if="selectedTable.customPromptRules"
            class="rounded-xl bg-[#FAF7F1] border border-accent-200/80 p-3.5 text-xs text-ink-800 flex items-start space-x-2"
          >
            <BookOpen class="w-4 h-4 text-accent-600 flex-shrink-0 mt-0.5" />
            <div>
              <div class="font-semibold text-ink-900">该表专属 Text-to-SQL 业务计算口径规则：</div>
              <div class="mt-0.5 text-slate-600 font-mono">{{ selectedTable.customPromptRules }}</div>
            </div>
          </div>

          <!-- Sub Tabs: Columns vs Live Preview -->
          <div class="flex items-center justify-between">
            <div class="dp-segment">
              <button
                @click="activeSubTab = 'columns'"
                class="dp-segment-item"
                :class="{ 'dp-segment-item-active': activeSubTab === 'columns' }"
              >
                字段字典 ({{ selectedTable.columns?.length }})
              </button>
              <button
                @click="loadPreview(selectedTable.tableName)"
                class="dp-segment-item"
                :class="{ 'dp-segment-item-active': activeSubTab === 'preview' }"
              >
                采样预览 (前10条)
              </button>
            </div>

            <span class="text-[11px] text-slate-400 font-mono tabular-nums">
              DS #{{ datasourceStore.currentDatasourceId }}
            </span>
          </div>

          <!-- Column Dictionary Table -->
          <div v-if="activeSubTab === 'columns'" class="dp-table-wrap">
            <table class="dp-table">
              <thead>
                <tr>
                  <th>字段名</th>
                  <th>数据类型</th>
                  <th>主键约束</th>
                  <th>业务含义 / 注释</th>
                </tr>
              </thead>
              <tbody class="text-slate-700">
                <tr v-for="col in selectedTable.columns" :key="col.columnName">
                  <td class="font-mono font-medium text-ink-900">
                    {{ col.columnName }}
                  </td>
                  <td class="font-mono text-slate-500">
                    {{ col.columnType }}
                  </td>
                  <td>
                    <span v-if="col.isPrimaryKey" class="dp-badge-accent text-[10px] font-bold rounded-md">
                      PK
                    </span>
                    <span v-else class="text-slate-300">-</span>
                  </td>
                  <td class="text-slate-600">
                    {{ col.columnComment || '—' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Live Sample Preview -->
          <div v-if="activeSubTab === 'preview'" class="space-y-3">
            <div v-if="previewLoading" class="p-8 text-center text-xs text-slate-400">
              <Loader2 class="w-5 h-5 mx-auto animate-spin mb-2 text-ink-900" />
              正在从目标数仓直连采样数据...
            </div>
            <div v-else-if="previewData" class="dp-table-wrap max-h-96">
              <table class="dp-table font-mono">
                <thead class="sticky top-0">
                  <tr>
                    <th v-for="c in previewData.columns" :key="c" class="whitespace-nowrap">
                      {{ c }}
                    </th>
                  </tr>
                </thead>
                <tbody class="text-slate-700">
                  <tr v-for="(r, idx) in previewData.rows" :key="idx">
                    <td v-for="c in previewData.columns" :key="c" class="whitespace-nowrap">
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
    <div v-if="showConfigModal" class="dp-modal-mask">
      <div class="dp-modal max-w-lg space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2.5">
            <span class="dp-icon-mark w-8 h-8">
              <Settings2 class="w-4 h-4" />
            </span>
            <div>
              <h3 class="dp-section-title">
                {{ authStore.isAdmin ? '配置业务口径与白名单' : '数据表口径规则 (受控只读)' }}
              </h3>
              <p class="text-xs text-slate-400 font-mono">{{ configForm.tableName }}</p>
            </div>
          </div>
          <button @click="showConfigModal = false" class="dp-btn-ghost p-1.5 rounded-full">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Read-only tip banner -->
        <div v-if="!authStore.isAdmin" class="p-2.5 rounded-xl bg-accent-50/70 border border-accent-200 text-accent-800 text-xs flex items-center space-x-2">
          <Lock class="w-3.5 h-3.5 text-accent-700 flex-shrink-0" />
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
              class="dp-input py-2"
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
              class="dp-input py-2 font-mono"
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
              class="rounded border-slate-300 text-ink-900 accent-[#181715] focus:ring-ink-900/20 disabled:opacity-60"
            />
            <label for="modalWhitelisted" class="font-medium text-slate-700 cursor-pointer">
              纳入大模型只读查询与 AST 沙箱白名单
            </label>
          </div>

          <div class="flex items-center justify-end space-x-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              @click="showConfigModal = false"
              class="dp-btn-ghost px-4 py-2"
            >
              {{ authStore.isAdmin ? '取消' : '关闭' }}
            </button>
            <button
              v-if="authStore.isAdmin"
              type="submit"
              :disabled="savingConfig"
              class="dp-btn-primary px-5"
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
/**
 * 【数仓全景字典与语义治理页（SchemaExplorerView）】
 * 自动扫描当前选中数据源（MySQL / PostgreSQL）的所有物理表与只读分析视图，支持：
 * - 查看每张表的字段结构、主键与注释，以及前 N 行真实数据抽样预览；
 * - 管理员可逐表开关 Text-to-SQL 白名单，并维护补充给大模型的业务计算口径与关联规则。
 */
defineOptions({
  name: 'SchemaExplorerView'
})

import { ref, computed, watch, onMounted } from 'vue'
import apiClient from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useDatasourceStore } from '../stores/datasource'
import { useToastStore } from '../stores/toast'
import PageHeader from '../components/PageHeader.vue'
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
