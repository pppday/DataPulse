<template>
  <div class="space-y-6">
    <!-- Top Architectural Header & Presets -->
    <PageHeader
      :icon="Terminal"
      eyebrow="Query Studio"
      title="智能数据分析工作台"
      badge="AST 只读沙箱防护"
      badge-tone="success"
      description="输入业务分析诉求，AI 依据数仓字典自动生成合规 SQL，并直出可视化图表与关键商业洞察。"
    >
      <template #actions>
        <div class="flex items-center space-x-3 text-xs">
          <div class="dp-select-pill py-2">
            <Server class="w-4 h-4 text-slate-500 flex-shrink-0" />
            <div>
              <div class="text-[10px] text-slate-400 leading-none">分析数仓</div>
              <select
                :value="datasourceStore.currentDatasourceId"
                @change="handleDatasourceChange(Number($event.target.value))"
                class="bg-transparent text-xs font-semibold text-ink-900 focus:outline-none cursor-pointer mt-0.5"
              >
                <option v-for="ds in datasourceStore.datasources" :key="ds.id" :value="ds.id">
                  {{ ds.name }} ({{ ds.host }}:{{ ds.port }})
                </option>
              </select>
            </div>
          </div>

          <div class="rounded-xl border border-slate-200/90 bg-white px-3 py-2 shadow-2xs text-slate-700 hidden sm:block">
            <div class="text-[10px] text-slate-400 leading-none">加速缓存</div>
            <div class="text-xs font-semibold text-pine-700 flex items-center space-x-1 mt-1">
              <Zap class="w-3.5 h-3.5 text-accent-500" />
              <span>Redis 毫秒直出</span>
            </div>
          </div>
        </div>
      </template>

      <!-- Quick Preset Prompt Chips (No Emojis) -->
      <div>
        <div class="text-xs font-medium text-slate-500 mb-2.5 flex items-center space-x-1.5">
          <Compass class="w-3.5 h-3.5 text-slate-400" />
          <span>推荐业务场景分析（点击快速生成）：</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="(preset, index) in promptPresets"
            :key="index"
            @click="selectPreset(preset.prompt)"
            type="button"
            class="text-left"
            :class="preset.isThreat
              ? 'dp-chip-danger'
              : (preset.isCacheTest ? 'dp-chip-warning' : 'dp-chip')"
          >
            <component :is="preset.icon" class="w-3.5 h-3.5 flex-shrink-0 opacity-80" />
            <span>{{ preset.label }}</span>
          </button>
        </div>
      </div>
    </PageHeader>

    <!-- Query Input Card -->
    <div class="dp-card p-6 space-y-4 dp-rise" style="animation-delay: 60ms">
      <!-- Input Mode Switcher -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div class="dp-segment w-fit">
          <button
            type="button"
            @click="inputMode = 'nl'"
            class="dp-segment-item"
            :class="{ 'dp-segment-item-active': inputMode === 'nl' }"
          >
            <MessageSquare class="w-3.5 h-3.5" :class="inputMode === 'nl' ? 'text-accent-600' : 'text-slate-400'" />
            <span>自然语言查询 (Text-to-SQL)</span>
          </button>
          <button
            type="button"
            @click="inputMode = 'sql'"
            class="dp-segment-item"
            :class="{ 'dp-segment-item-active': inputMode === 'sql' }"
          >
            <Code2 class="w-3.5 h-3.5" :class="inputMode === 'sql' ? 'text-pine-600' : 'text-slate-400'" />
            <span>自主编写 SQL (AST只读审查)</span>
          </button>
        </div>

        <span class="text-[11px] text-slate-400">
          {{ inputMode === 'nl' ? '支持任意业务提问（如“查所有商品”），由大模型推导只读 SQL' : '支持手工编写 SELECT 查询，由 JSqlParser 严格拦截变更并路由执行' }}
        </span>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-3">
        <!-- Natural Language Textarea -->
        <div
          v-if="inputMode === 'nl'"
          class="rounded-xl border border-slate-200 bg-slate-50/40 transition focus-within:bg-white focus-within:border-ink-900 focus-within:ring-4 focus-within:ring-ink-900/5"
        >
          <textarea
            v-model="queryInput"
            rows="3"
            placeholder="请输入自然语言数据分析需求，例如：'查所有商品'、'查询单价大于1000的商品'，或输入危险指令测试AST拦截..."
            class="block w-full resize-none bg-transparent px-4 py-3.5 text-sm leading-relaxed text-ink-900 placeholder:text-slate-400 focus:outline-none"
            :disabled="loading"
            @keydown.enter.ctrl="handleSubmit"
          ></textarea>
        </div>

        <!-- Direct SQL Textarea -->
        <div v-else class="relative">
          <textarea
            v-model="directSqlInput"
            rows="4"
            placeholder="请输入自定义 SELECT SQL 语句（例如：SELECT id, name, price, sold FROM item WHERE sold > 0 ORDER BY sold DESC LIMIT 20;）..."
            class="dp-code block w-full p-4 font-mono text-[13px] leading-relaxed placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-accent-400/50 resize-none"
            :disabled="loading"
            @keydown.enter.ctrl="handleSubmit"
          ></textarea>
        </div>

        <div class="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div class="text-xs text-slate-400 flex items-center space-x-2">
            <span>支持快捷键</span>
            <kbd class="dp-kbd">Ctrl + Enter</kbd>
            <span>直接发起执行 · 依然受 AST 白名单只读沙箱保护</span>
          </div>

          <div class="flex items-center space-x-2.5 w-full sm:w-auto justify-end">
            <button
              v-if="(inputMode === 'nl' && queryInput) || (inputMode === 'sql' && directSqlInput)"
              @click="clearInput"
              type="button"
              class="dp-btn-ghost"
            >
              清空
            </button>
            <button
              type="submit"
              :disabled="loading || (inputMode === 'nl' ? !queryInput.trim() : !directSqlInput.trim())"
              class="dp-btn-primary flex-1 sm:flex-none px-5 py-2.5 text-xs"
            >
              <Loader2 v-if="loading" class="w-4 h-4 animate-spin" />
              <Play v-else class="w-3.5 h-3.5 fill-current" />
              <span>{{ loading ? '执行与AST审查中...' : (inputMode === 'nl' ? '生成 SQL 并执行' : '执行此 SQL (只读审查)') }}</span>
            </button>
          </div>
        </div>
      </form>
    </div>

    <!-- Restrained Architectural Loading Panel -->
    <div v-if="loading" class="dp-card p-7 space-y-4 dp-rise">
      <div class="flex items-center justify-center space-x-2.5">
        <Loader2 class="w-4 h-4 animate-spin text-accent-600" />
        <h3 class="dp-section-title">智能分析流水线执行中...</h3>
      </div>

      <div class="flex flex-wrap justify-center items-center gap-1.5">
        <template v-for="(step, idx) in pipelineSteps" :key="step">
          <span class="dp-badge-neutral">{{ step }}</span>
          <ChevronRight v-if="idx < pipelineSteps.length - 1" class="w-3 h-3 text-slate-300" />
        </template>
      </div>

      <div class="mx-auto h-[2px] w-full max-w-md overflow-hidden rounded-full bg-slate-200">
        <div class="h-full w-1/3 rounded-full bg-ink-900 animate-indeterminate"></div>
      </div>
    </div>

    <!-- Query Results Presentation -->
    <div v-if="result && !loading" class="space-y-6">
      <!-- Status & Telemetry Header Bar -->
      <div
        class="dp-card p-4 border-l-[3px] flex flex-col md:flex-row md:items-center justify-between gap-4 dp-rise"
        :class="result.isSafe === 1 ? 'border-l-pine-600' : 'border-l-crimson-500'"
      >
        <div class="flex items-center space-x-3">
          <div
            class="w-9 h-9 rounded-xl border flex items-center justify-center flex-shrink-0"
            :class="result.isSafe === 1
              ? 'bg-pine-50 text-pine-700 border-pine-200/80'
              : 'bg-crimson-50 text-crimson-600 border-crimson-200/80'"
          >
            <ShieldCheck v-if="result.isSafe === 1" class="w-4 h-4" />
            <ShieldAlert v-else class="w-4 h-4" />
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-sm font-semibold" :class="result.isSafe === 1 ? 'text-ink-900' : 'text-crimson-700'">
                {{ result.isSafe === 1 ? 'AST 语法安全沙箱：审核通过' : 'AST 语法安全沙箱：阻断拦截' }}
              </span>
              <span
                class="uppercase"
                :class="result.isSafe === 1 ? 'dp-badge-success' : 'dp-badge-danger'"
              >
                {{ result.safetyReason }}
              </span>

              <!-- Redis Cache Hit Badge -->
              <span v-if="result.cacheHit" class="dp-badge-warning">
                <Zap class="w-3 h-3 text-accent-600" />
                <span>Redis 语义指纹命中 (1ms 极速响应)</span>
              </span>
            </div>
            <p class="text-xs mt-0.5 text-slate-500">
              {{ result.isSafe === 1
                ? (result.cacheHit
                    ? '已直接从 Redis 6379 提取全量分析结果，免除大模型 Token 与底层数仓计算开销。'
                    : '已完成只读指令核验、表白名单校验与 LIMIT 1000 自动安全上限收敛。')
                : '该操作已被沙箱拦截，未向底层数仓发送任何危险请求，保障数据资产绝对只读安全。' }}
            </p>
          </div>
        </div>

        <!-- Telemetry Strip -->
        <div class="grid grid-cols-2 sm:inline-flex items-center divide-y sm:divide-y-0 sm:divide-x divide-slate-200/80 bg-slate-50/80 border border-slate-200/70 rounded-xl text-xs flex-shrink-0">
          <div class="px-3 py-1.5">
            <span class="text-slate-400">数据源:</span>
            <span class="ml-1 font-semibold text-ink-900">{{ result.datasourceName || '核心数仓' }}</span>
          </div>
          <div class="px-3 py-1.5">
            <span class="text-slate-400">耗时:</span>
            <span class="ml-1 font-semibold text-ink-900 tabular-nums" :class="{ 'text-accent-700': result.cacheHit }">
              {{ result.executionTimeMs }} ms
            </span>
          </div>
          <div class="px-3 py-1.5">
            <span class="text-slate-400">行数:</span>
            <span class="ml-1 font-semibold text-ink-900 tabular-nums">{{ result.rowCount }} 行</span>
          </div>
          <div class="px-3 py-1.5">
            <span class="text-slate-400">图表:</span>
            <span class="ml-1 font-semibold text-pine-700 uppercase">{{ result.chartType }}</span>
          </div>
        </div>
      </div>

      <!-- Generated SQL & AST Inspection Block (Editable) -->
      <div class="dp-card p-5 space-y-3 dp-rise">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center space-x-2">
            <Code2 class="w-4 h-4 text-accent-600" />
            <h3 class="dp-section-title">执行 SQL (支持直接修改与二次执行)</h3>
            <span class="dp-badge-success">
              AST 沙箱只读守护
            </span>
          </div>

          <div class="flex items-center space-x-2">
            <button
              v-if="editableSql !== result.generatedSql"
              @click="editableSql = result.generatedSql"
              type="button"
              class="dp-btn-ghost px-2.5 py-1"
              title="还原为最初生成的 SQL"
            >
              还原原 SQL
            </button>
            <button
              @click="copySql"
              type="button"
              class="dp-btn-secondary px-2.5 py-1"
            >
              <Copy class="w-3.5 h-3.5" />
              <span>{{ copied ? '已复制' : '复制代码' }}</span>
            </button>
            <button
              @click="handleExecuteCustomSql()"
              :disabled="loading || !editableSql.trim()"
              type="button"
              class="dp-btn-primary px-3.5 py-1.5"
            >
              <Play class="w-3 h-3 fill-current" />
              <span>运行修改后的 SQL</span>
            </button>
          </div>
        </div>

        <div class="dp-code p-4">
          <div class="flex items-center justify-between pb-2.5 mb-3 border-b border-white/10">
            <span class="font-mono text-[10px] font-medium uppercase tracking-[0.18em] text-[#9B958C]">
              SQL · READ-ONLY SANDBOX
            </span>
            <span class="font-mono text-[10px] text-[#78736A]">Ctrl + Enter 执行</span>
          </div>
          <textarea
            v-model="editableSql"
            rows="3"
            class="w-full bg-transparent text-[#C9E4D6] font-mono text-xs focus:outline-none resize-y leading-relaxed"
            placeholder="可在此直接修改 SQL 字段、条件或数量..."
            @keydown.enter.ctrl="handleExecuteCustomSql()"
          ></textarea>
        </div>
        <div class="flex items-center justify-between text-[11px] text-slate-400">
          <span>提示：你可自由修改条件、字段或去掉/增加条数，按 <kbd class="dp-kbd">Ctrl + Enter</kbd> 重新运行。系统严格拦截非 SELECT 指令。</span>
        </div>
      </div>

      <!-- Visual Chart Display -->
      <div v-if="result.isSafe === 1 && result.rows && result.rows.length > 0" class="dp-card p-5 space-y-4 dp-rise">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2">
            <BarChart3 class="w-4 h-4 text-accent-600" />
            <h3 class="dp-section-title">多维可视化分析交互看板</h3>
          </div>

          <!-- Chart Mode Switcher & Export -->
          <div class="flex items-center space-x-2.5">
            <div class="dp-segment">
              <button
                @click="activeViewMode = 'chart'"
                class="dp-segment-item"
                :class="{ 'dp-segment-item-active': activeViewMode === 'chart' }"
              >
                智能图表
              </button>
              <button
                @click="activeViewMode = 'table'"
                class="dp-segment-item"
                :class="{ 'dp-segment-item-active': activeViewMode === 'table' }"
              >
                明细数据表
              </button>
            </div>

            <!-- Export to Excel Button -->
            <button
              @click="handleExportExcel"
              :disabled="exporting"
              class="dp-btn px-3 py-1.5 font-medium active:scale-95"
              :class="exportSuccess
                ? 'bg-pine-600 text-white border border-pine-600'
                : 'bg-pine-50 text-pine-700 border border-pine-200 hover:bg-pine-100'"
              title="一键将当前分析数据导出为真实 .xlsx 表格文件"
            >
              <Check v-if="exportSuccess" class="w-3.5 h-3.5" />
              <Loader2 v-else-if="exporting" class="w-3.5 h-3.5 animate-spin" />
              <FileSpreadsheet v-else class="w-3.5 h-3.5 text-pine-700" />
              <span>{{ exporting ? '正在导出...' : (exportSuccess ? '已导出 Excel' : '导出 Excel') }}</span>
            </button>
          </div>
        </div>

        <!-- ECharts Container -->
        <div v-show="activeViewMode === 'chart'" class="w-full">
          <div ref="chartRef" class="w-full h-80 sm:h-96"></div>
        </div>

        <!-- Data Table Container -->
        <div v-show="activeViewMode === 'table'" class="space-y-2">
          <div class="flex items-center justify-between text-[11px] text-slate-500 px-1">
            <span>共展示 <strong class="text-ink-900 font-semibold tabular-nums">{{ result.rows.length }}</strong> 行数据明细</span>
            <button
              @click="handleExportExcel"
              :disabled="exporting"
              class="text-pine-700 hover:text-pine-800 font-medium flex items-center space-x-1 hover:underline transition-all cursor-pointer"
            >
              <Download class="w-3 h-3" />
              <span>下载此表格 (.xlsx)</span>
            </button>
          </div>
          <div class="dp-table-wrap">
            <table class="dp-table">
              <thead>
                <tr>
                  <th v-for="(col, idx) in result.columns" :key="idx" class="whitespace-nowrap">
                    {{ col }}
                  </th>
                </tr>
              </thead>
              <tbody class="text-slate-700">
                <tr v-for="(row, rIdx) in result.rows" :key="rIdx">
                  <td v-for="(col, cIdx) in result.columns" :key="cIdx" class="whitespace-nowrap">
                    {{ formatCell(row[col]) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Executive Business Insight Card (Editorial Briefing) -->
      <div
        v-if="result.insightSummary"
        class="rounded-2xl bg-[#FAF7F1] border border-accent-200/80 p-6 dp-rise"
      >
        <div class="flex items-center space-x-2">
          <BookOpen class="w-4 h-4 text-accent-600 flex-shrink-0" />
          <span class="text-[11px] font-mono uppercase tracking-[0.16em] text-accent-700">Executive Briefing</span>
          <span class="text-slate-300">·</span>
          <h3 class="dp-section-title">AI 商业决策洞察与经营建议</h3>
        </div>
        <p class="mt-3 pl-4 border-l-2 border-accent-400 text-[13.5px] leading-7 text-slate-700">
          {{ result.insightSummary }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 【智能数据分析工作台（自然语言 Text-to-SQL 与可视化看板）】
 *
 * 通俗理解：这是 DataPulse 最核心的业务取数页面（文件名沿用历史 AgentQueryView，功能为企业级自然语言查库与手写只读 SQL 分析）：
 * 1. 支持自然语言提问自动生成 SQL，也支持直接手写/修改 SQL 二次执行（两种模式均受后端 AST 只读沙箱保护）；
 * 2. 顶部可随时切换目标 MySQL / PostgreSQL 数据源，命中 Redis 查询缓存时会展示毫秒直出徽标；
 * 3. ECharts 渲染引擎会自动识别 X 轴文本维度列，并在多指标量级悬殊（如购票张数 vs 消费总额）时自动启用左右双 Y 轴，避免小指标柱子被压扁；
 * 4. 支持一键切换图表/明细表视图，以及流式导出带审计摘要的 Excel (.xlsx) 报表。
 */
defineOptions({
  name: 'AgentQueryView'
})

import { ref, computed, onMounted, nextTick, watch, onBeforeUnmount, onActivated } from 'vue'
import apiClient from '../api/client'
import * as echarts from 'echarts'
import { useDatasourceStore } from '../stores/datasource'
import PageHeader from '../components/PageHeader.vue'
import {
  Terminal,
  Server,
  Zap,
  Compass,
  Play,
  Loader2,
  ShieldCheck,
  ShieldAlert,
  Code2,
  Copy,
  BarChart3,
  FileSpreadsheet,
  Download,
  Check,
  ChevronRight,
  MessageSquare,
  AlertTriangle,
  Layers,
  Award,
  GraduationCap,
  Package,
  BookOpen
} from 'lucide-vue-next'
import { exportFromBackend } from '../utils/excelExporter'
import { useToastStore } from '../stores/toast'

const datasourceStore = useDatasourceStore()
const toast = useToastStore()

const queryInput = ref('')
const inputMode = ref('nl') // 'nl': AI prompt, 'sql': direct SQL
const directSqlInput = ref('')
const editableSql = ref('')
const loading = ref(false)
const exporting = ref(false)
const exportSuccess = ref(false)
const result = ref(null)
const copied = ref(false)
const activeViewMode = ref('chart')

const chartRef = ref(null)
let chartInstance = null

const pipelineSteps = [
  'Redis 语义缓存',
  'SchemaCatalog 元数据',
  'DeepSeek Text-to-SQL',
  'JSqlParser AST 审查',
  'HikariCP 动态执行',
  '图表与商业洞察'
]

const hexToRgba = (hex, a) => {
  const n = parseInt(hex.slice(1), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`
}

const defaultPresets = [
  {
    icon: BarChart3,
    label: '各大区销售额与毛利率排行榜',
    prompt: '统计2026年各大区的销售额与毛利润排行榜，计算各区毛利率并按销售额降序排列'
  },
  {
    icon: Zap,
    label: 'Redis 语义缓存加速分析',
    prompt: '统计2026年各大区的销售额与毛利润排行榜，计算各区毛利率并按销售额降序排列',
    isCacheTest: true
  },
  {
    icon: AlertTriangle,
    label: '退款最高前3商品与退款原因',
    prompt: '统计2026年退款金额最高的前3个商品及其退款原因，包含商品名称、品类和退款总金额'
  },
  {
    icon: Layers,
    label: '各商品品类销量与毛利贡献度',
    prompt: '按产品品类统计总销量、总销售额与总毛利润'
  },
  {
    icon: Award,
    label: '高价值核心客户画像 (TOP5)',
    prompt: '查询累计下单总金额最高的前5名客户名称、所在省份、会员等级及累计消费总额'
  },
  {
    icon: ShieldAlert,
    label: '沙箱防护：敏感删除拦截',
    prompt: '请帮我删除所有的历史查询记录 DELETE FROM sys_query_history',
    isThreat: true
  },
  {
    icon: MessageSquare,
    label: '非数仓日常闲聊智能引导',
    prompt: '今天北京天气怎么样？',
    isThreat: true
  }
]

const mallPresets = [
  {
    icon: GraduationCap,
    label: '各培训课程报名订单量与实付金额',
    prompt: '统计各个课程的报名订单数量与累计实付总金额，并按销售额降序排列'
  },
  {
    icon: Package,
    label: '最新报名订单与学员明细 (前10笔)',
    prompt: '查询最新的前10笔报名订单，展示订单编号、用户ID、课程ID与下单时间'
  },
  {
    icon: Zap,
    label: '秒杀课程库存与热度分析',
    prompt: '查询所有秒杀课程的名称、秒杀价格与剩余库存'
  },
  {
    icon: ShieldAlert,
    label: '沙箱防护：高危删表拦截',
    prompt: '请帮我清空所有报名记录 DROP TABLE enroll_order',
    isThreat: true
  }
]

const promptPresets = computed(() => {
  const current = datasourceStore.currentDatasource
  if (current && (current.databaseName?.toLowerCase().includes('mall') || current.name?.toLowerCase().includes('mall') || current.id === 2)) {
    return mallPresets
  }
  return defaultPresets
})

const handleDatasourceChange = (newId) => {
  datasourceStore.setDatasourceId(newId)
}

const selectPreset = (p) => {
  queryInput.value = p
  handleExecuteQuery()
}

const handleExecuteQuery = async () => {
  if (!queryInput.value.trim() || loading.value) return

  loading.value = true
  result.value = null

  try {
    const data = await apiClient.post('/agent/query', {
      prompt: queryInput.value.trim(),
      datasourceId: datasourceStore.currentDatasourceId
    })
    result.value = data
    editableSql.value = data.generatedSql || ''
  } catch (err) {
    toast.error(err.message || '查询失败，请检查网络或数仓状态')
  } finally {
    loading.value = false
  }

  // Ensure DOM is mounted after loading is false
  await nextTick()
  if (result.value?.isSafe === 1 && result.value?.rows?.length > 0) {
    setTimeout(() => {
      renderChart()
    }, 60)
  }
}

const handleExecuteCustomSql = async (sqlToRun = null) => {
  const targetSql = (typeof sqlToRun === 'string' && sqlToRun.trim()) ? sqlToRun.trim() : editableSql.value.trim()
  if (!targetSql || loading.value) return

  loading.value = true
  result.value = null

  try {
    const data = await apiClient.post('/agent/query', {
      customSql: targetSql,
      prompt: queryInput.value.trim() || '自定义 SQL 分析',
      datasourceId: datasourceStore.currentDatasourceId
    })
    result.value = data
    editableSql.value = data.generatedSql || targetSql
  } catch (err) {
    toast.error(err.message || 'SQL 执行失败，请检查语法或权限')
  } finally {
    loading.value = false
  }

  // Ensure DOM is mounted after loading is false
  await nextTick()
  if (result.value?.isSafe === 1 && result.value?.rows?.length > 0) {
    setTimeout(() => {
      renderChart()
    }, 60)
  }
}

const handleSubmit = () => {
  if (inputMode.value === 'nl') {
    handleExecuteQuery()
  } else {
    editableSql.value = directSqlInput.value
    handleExecuteCustomSql(directSqlInput.value)
  }
}

const clearInput = () => {
  if (inputMode.value === 'nl') {
    queryInput.value = ''
  } else {
    directSqlInput.value = ''
  }
}

const renderChart = () => {
  if (!chartRef.value || !result.value || !result.value.chartOptions) return

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  chartInstance = echarts.init(chartRef.value)
  const rawOptions = result.value.chartOptions

  // Deep clone to safely manipulate configuration
  const options = JSON.parse(JSON.stringify(rawOptions))

  // 1. Editorial Pine / Bronze / Mineral palette & typography
  options.color = ['#1F6E56', '#C08A53', '#3D7A80', '#B85D4B', '#7A6F64', '#6B8E6E']
  options.textStyle = {
    fontFamily: 'Inter, "PingFang SC", "Microsoft YaHei", sans-serif',
    color: '#545049'
  }

  // 2. Expand grid margins to give rotated/wrapped labels ample breathing room
  options.grid = {
    top: '12%',
    left: '3%',
    right: '4%',
    bottom: '20%',
    containLabel: true,
    ...(options.grid || {})
  }

  // 3. Fix X-axis text collision and style axis lines
  if (options.xAxis) {
    const xList = Array.isArray(options.xAxis) ? options.xAxis : [options.xAxis]
    xList.forEach((xa) => {
      xa.axisLine = {
        lineStyle: { color: '#E7E4DC' },
        ...(xa.axisLine || {})
      }
      xa.axisTick = {
        show: false,
        ...(xa.axisTick || {})
      }
      if (xa.type === 'category' || (!xa.type && xa.data)) {
        const dataCount = (xa.data || []).length
        const maxLen = (xa.data || []).reduce((max, item) => Math.max(max, String(item || '').length), 0)

        const shouldRotate = maxLen > 5 || dataCount > 6

        xa.axisLabel = {
          interval: 0,
          rotate: shouldRotate ? 30 : 0,
          color: '#706B63',
          fontSize: 11,
          formatter: (value) => {
            if (!value) return ''
            const str = String(value)
            return str.length > 10 ? str.slice(0, 10) + '…' : str
          },
          ...(xa.axisLabel || {})
        }
      }
    })
  }

  // 3b. Soften Y-axis gridlines & labels
  const yList = options.yAxis ? (Array.isArray(options.yAxis) ? options.yAxis : [options.yAxis]) : []
  yList.forEach((ya) => {
    ya.splitLine = {
      lineStyle: { color: '#EFECE6', type: 'dashed' },
      ...(ya.splitLine || {})
    }
    ya.axisLabel = {
      color: '#9C968C',
      fontSize: 11,
      ...(ya.axisLabel || {})
    }
    ya.axisLine = {
      show: false,
      ...(ya.axisLine || {})
    }
  })

  // 3c. Refine series appearance (crisp bars, smooth lines, clean pie slices)
  const isHorizontal = yList[0]?.type === 'category'
  const seriesList = Array.isArray(options.series)
    ? options.series
    : (options.series ? [options.series] : [])

  seriesList.forEach((s, i) => {
    const c = options.color[i % options.color.length]
    if (s.type === 'bar') {
      s.barMaxWidth = s.barMaxWidth ?? 34
      s.itemStyle = {
        borderRadius: isHorizontal ? [0, 4, 4, 0] : [4, 4, 0, 0],
        color: isHorizontal
          ? new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: hexToRgba(c, 0.82) },
              { offset: 1, color: c }
            ])
          : new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: c },
              { offset: 1, color: hexToRgba(c, 0.82) }
            ]),
        ...(s.itemStyle || {})
      }
    } else if (s.type === 'line') {
      s.smooth = s.smooth ?? true
      s.symbol = s.symbol ?? 'circle'
      s.symbolSize = s.symbolSize ?? 6
      s.lineStyle = {
        width: 2.2,
        ...(s.lineStyle || {})
      }
      s.areaStyle = s.areaStyle ?? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: hexToRgba(c, 0.14) },
          { offset: 1, color: hexToRgba(c, 0) }
        ])
      }
    } else if (s.type === 'pie') {
      s.itemStyle = {
        borderRadius: 6,
        borderColor: '#FFFFFF',
        borderWidth: 2,
        ...(s.itemStyle || {})
      }
    }
  })

  if (options.legend) {
    options.legend = {
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 16,
      textStyle: { color: '#545049', fontSize: 12 },
      ...options.legend
    }
  }

  // 4. Clean Architectural Tooltip
  const triggerType = seriesList.some(s => s.type === 'pie') ? 'item' : 'axis'
  options.tooltip = {
    trigger: triggerType,
    ...(triggerType === 'axis'
      ? { axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(24, 23, 21, 0.04)' } } }
      : {}),
    backgroundColor: '#FFFFFF',
    borderColor: '#E7E4DC',
    borderWidth: 1,
    padding: [10, 14],
    textStyle: {
      color: '#181715',
      fontSize: 12
    },
    extraCssText: 'box-shadow: 0 12px 28px -12px rgba(24, 23, 21, 0.16); border-radius: 10px; max-width: 450px; white-space: normal; word-break: break-all;',
    formatter: (params) => {
      if (!params) return ''
      if (Array.isArray(params)) {
        if (params.length === 0) return ''
        const fullTitle = params[0].name || ''
        let html = `<div style="font-weight: 600; margin-bottom: 6px; color: #181715; font-size: 12px; line-height: 1.4;">${fullTitle}</div>`
        params.forEach(p => {
          const val = typeof p.value === 'number' ? (Number.isInteger(p.value) ? p.value.toLocaleString() : p.value.toFixed(2)) : p.value
          html += `<div style="display: flex; justify-content: space-between; gap: 16px; margin-top: 4px; font-size: 11px;">
            <span style="color: #706B63;">${p.marker} ${p.seriesName}</span>
            <span style="font-weight: 600; color: #181715;">${val}</span>
          </div>`
        })
        return html
      } else {
        const fullTitle = params.name || ''
        const val = typeof params.value === 'number' ? (Number.isInteger(params.value) ? params.value.toLocaleString() : params.value.toFixed(2)) : params.value
        return `<div style="font-weight: 600; color: #181715; font-size: 12px; margin-bottom: 4px; line-height: 1.4;">${fullTitle}</div>
          <div style="font-size: 11px; color: #706B63;">${params.marker} ${params.seriesName || ''}: <b style="color: #181715;">${val}</b> (${params.percent || 0}%)</div>`
      }
    }
  }

  // 5. Minimal pill dataZoom slider
  const firstXAxis = Array.isArray(options.xAxis) ? options.xAxis[0] : options.xAxis
  const categoryCount = firstXAxis?.data?.length || 0
  if (categoryCount > 6) {
    options.dataZoom = [
      {
        type: 'slider',
        show: true,
        showDetail: false,
        showDataShadow: false,
        start: 0,
        end: Math.min(100, Math.round((8 / categoryCount) * 100)),
        bottom: 4,
        height: 14,
        borderColor: 'transparent',
        backgroundColor: '#F3F1EC',
        fillerColor: 'rgba(166, 123, 78, 0.16)',
        borderRadius: 7,
        handleSize: '120%',
        handleStyle: {
          color: '#181715',
          borderColor: '#181715',
          borderWidth: 1,
          shadowBlur: 2,
          shadowColor: 'rgba(24, 23, 21, 0.15)'
        },
        moveHandleStyle: {
          color: 'rgba(24, 23, 21, 0.25)'
        }
      },
      {
        type: 'inside'
      }
    ]
  } else {
    delete options.dataZoom
  }

  chartInstance.setOption(options)
  chartInstance.resize()
}

const copySql = async () => {
  const sqlToCopy = editableSql.value || result.value?.generatedSql
  if (!sqlToCopy) return
  await navigator.clipboard.writeText(sqlToCopy)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}

const formatCell = (val) => {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'number') {
    return Number.isInteger(val) ? val.toLocaleString() : val.toFixed(2)
  }
  return val
}

const handleExportExcel = async () => {
  if (!result.value?.columns || !result.value?.rows?.length) {
    toast.warning('当前无查询结果可供导出')
    return
  }

  exporting.value = true
  try {
    let subject = (queryInput.value || '').trim()
    if (!subject && editableSql.value) {
      subject = '自定义SQL查询'
    }
    if (subject.length > 20) {
      subject = subject.slice(0, 20)
    }
    const dsName = datasourceStore.currentDatasource?.name || 'DataPulse数仓'
    const fileName = `DataPulse_${dsName}_${subject}`

    const payload = {
      fileName: fileName,
      sheetName: subject ? subject.slice(0, 20) : '数据明细',
      columns: result.value.columns,
      rows: result.value.rows,
      insightSummary: result.value.insightSummary,
      datasourceName: dsName,
      generatedSql: editableSql.value || result.value.generatedSql,
      executionTimeMs: result.value.executionTimeMs,
      cacheHit: result.value.cacheHit,
      prompt: queryInput.value || subject,
      datasourceId: datasourceStore.currentDatasourceId
    }

    await exportFromBackend(payload)

    exportSuccess.value = true
    toast.success('Excel 报表已生成并自动开始下载')
    setTimeout(() => {
      exportSuccess.value = false
    }, 2500)
  } catch (err) {
    toast.error(err.message || '导出 Excel 失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await datasourceStore.fetchDatasources()
})

onActivated(() => {
  if (chartInstance) {
    chartInstance.resize()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
  }
})

watch(() => datasourceStore.currentDatasourceId, (newId, oldId) => {
  if (oldId && newId !== oldId) {
    // Datasource switched: clear previous result to avoid showing data from another database
    result.value = null
    queryInput.value = ''
    directSqlInput.value = ''
    editableSql.value = ''
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  }
})

watch(() => activeViewMode.value, async (val) => {
  if (val === 'chart') {
    await nextTick()
    setTimeout(() => {
      renderChart()
    }, 60)
  }
})
</script>
