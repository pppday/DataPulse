<template>
  <div class="space-y-6">
    <!-- Top Welcome & System Status Bar -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div class="flex items-center space-x-2">
            <span class="p-1.5 rounded-lg bg-blue-50 text-blue-600 border border-blue-100">
              <Sparkles class="w-4 h-4" />
            </span>
            <h1 class="text-base font-bold text-slate-900 tracking-tight">智能数据分析工作台</h1>
            <span class="text-[11px] px-2 py-0.5 rounded-full font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
              AST 只读沙箱防护
            </span>
          </div>
          <p class="mt-1 text-xs text-slate-500">
            输入业务分析诉求，AI 依据数仓字典自动生成合规 SQL，并直出可视化图表与关键商业洞察。
          </p>
        </div>

        <!-- Target Datasource Selector in Header -->
        <div class="flex items-center space-x-3 text-xs flex-shrink-0">
          <div class="bg-slate-50/80 border border-slate-200/90 px-3 py-1.5 rounded-xl flex items-center space-x-2">
            <Server class="w-4 h-4 text-blue-600" />
            <div>
              <div class="text-[10px] text-slate-400">分析数仓</div>
              <select
                :value="datasourceStore.currentDatasourceId"
                @change="handleDatasourceChange(Number($event.target.value))"
                class="bg-transparent text-xs font-semibold text-slate-800 focus:outline-none cursor-pointer mt-0.5"
              >
                <option v-for="ds in datasourceStore.datasources" :key="ds.id" :value="ds.id">
                  {{ ds.name }} ({{ ds.host }}:{{ ds.port }})
                </option>
              </select>
            </div>
          </div>
          <div class="px-3 py-1.5 rounded-xl bg-slate-50/80 border border-slate-200/90 text-slate-700 hidden sm:block">
            <div class="text-[10px] text-slate-400">加速缓存</div>
            <div class="text-xs font-semibold text-emerald-600 flex items-center space-x-1 mt-0.5">
              <Zap class="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
              <span>Redis 毫秒直出</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Preset Prompt Chips -->
      <div class="mt-4 pt-3 border-t border-slate-100">
        <div class="text-xs font-semibold text-slate-600 mb-2 flex items-center space-x-1.5">
          <Compass class="w-3.5 h-3.5 text-slate-400" />
          <span>推荐业务场景分析（点击快速生成）：</span>
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="(preset, index) in promptPresets"
            :key="index"
            @click="selectPreset(preset.prompt)"
            type="button"
            class="text-xs px-3 py-1.5 rounded-lg border transition-all text-left flex items-center space-x-1.5"
            :class="preset.isThreat
              ? 'border-rose-200 bg-rose-50/50 text-rose-700 hover:bg-rose-100'
              : (preset.isCacheTest
                ? 'border-amber-200 bg-amber-50/60 text-amber-800 hover:bg-amber-100'
                : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-blue-50 hover:text-blue-700 hover:border-blue-200')"
          >
            <span>{{ preset.icon }}</span>
            <span>{{ preset.label }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Query Input Card -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-sm space-y-3">
      <!-- Input Mode Switcher -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div class="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl text-xs w-fit">
          <button
            type="button"
            @click="inputMode = 'nl'"
            class="px-3 py-1 rounded-lg font-medium transition-all flex items-center space-x-1.5"
            :class="inputMode === 'nl' ? 'bg-white text-slate-900 shadow-sm font-semibold' : 'text-slate-600 hover:text-slate-900'"
          >
            <Sparkles class="w-3.5 h-3.5 text-blue-600" />
            <span>自然语言智能问答 (AI Agent)</span>
          </button>
          <button
            type="button"
            @click="inputMode = 'sql'"
            class="px-3 py-1 rounded-lg font-medium transition-all flex items-center space-x-1.5"
            :class="inputMode === 'sql' ? 'bg-white text-slate-900 shadow-sm font-semibold' : 'text-slate-600 hover:text-slate-900'"
          >
            <Code2 class="w-3.5 h-3.5 text-indigo-600" />
            <span>自主编写 SQL (AST只读审查)</span>
          </button>
        </div>

        <span class="text-[11px] text-slate-400">
          {{ inputMode === 'nl' ? '支持任意业务提问（如“查所有商品”），由大模型推导只读 SQL' : '支持手工编写 SELECT 查询，由 JSqlParser 严格拦截变更并路由执行' }}
        </span>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-3">
        <!-- Natural Language Textarea -->
        <div v-if="inputMode === 'nl'" class="relative">
          <textarea
            v-model="queryInput"
            rows="3"
            placeholder="请输入自然语言数据分析需求，例如：'查所有商品'、'查询单价大于1000的商品'，或输入危险指令测试AST拦截..."
            class="w-full rounded-xl border border-slate-300 p-3.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 transition-colors resize-none font-mono"
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
            class="w-full rounded-xl border border-slate-800 bg-slate-900 p-3.5 text-sm text-emerald-400 placeholder:text-slate-500 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 transition-colors resize-none font-mono leading-relaxed"
            :disabled="loading"
            @keydown.enter.ctrl="handleSubmit"
          ></textarea>
        </div>

        <div class="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div class="text-xs text-slate-400 flex items-center space-x-2">
            <span>支持快捷键</span>
            <kbd class="px-2 py-0.5 rounded border border-slate-200 bg-slate-100 text-slate-600 font-mono text-[10px]">Ctrl + Enter</kbd>
            <span>直接发起执行 · 依然受 AST 白名单只读沙箱保护</span>
          </div>

          <div class="flex items-center space-x-2.5 w-full sm:w-auto justify-end">
            <button
              v-if="(inputMode === 'nl' && queryInput) || (inputMode === 'sql' && directSqlInput)"
              @click="clearInput"
              type="button"
              class="px-3 py-2 text-xs font-medium text-slate-500 hover:text-slate-800 transition-colors"
            >
              清空
            </button>
            <button
              type="submit"
              :disabled="loading || (inputMode === 'nl' ? !queryInput.trim() : !directSqlInput.trim())"
              class="flex-1 sm:flex-none flex items-center justify-center space-x-2 px-6 py-2.5 rounded-xl font-medium text-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-all shadow-sm shadow-blue-500/20"
            >
              <Loader2 v-if="loading" class="w-4 h-4 animate-spin" />
              <Play v-else class="w-4 h-4 fill-current" />
              <span>{{ loading ? '执行与AST审查中...' : (inputMode === 'nl' ? '生成 SQL 并执行' : '执行此 SQL (只读审查)') }}</span>
            </button>
          </div>
        </div>
      </form>
    </div>

    <!-- Active Loading Indicator -->
    <div v-if="loading" class="bg-white rounded-2xl border border-blue-100 p-8 shadow-sm text-center">
      <div class="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 mb-3 animate-pulse">
        <Cpu class="w-6 h-6 animate-spin" />
      </div>
      <h3 class="text-sm font-semibold text-slate-900">智能分析流水线执行中...</h3>
      <p class="text-xs text-slate-500 mt-1 max-w-md mx-auto">
        正在检查 Redis 语义缓存 -> 抽取元数据 SchemaCatalog -> DeepSeek Text-to-SQL -> JSqlParser AST 语法树安全审查 -> HikariCP 动态连接池执行 -> 动态生成图表与商业洞察
      </p>
    </div>

    <!-- Query Results Presentation -->
    <div v-if="result && !loading" class="space-y-6">
      <!-- Status & Telemetry Header Bar -->
      <div
        class="rounded-2xl border p-4 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3 transition-colors"
        :class="result.isSafe === 1
          ? 'bg-emerald-50/60 border-emerald-200'
          : 'bg-rose-50/60 border-rose-200'"
      >
        <div class="flex items-center space-x-3">
          <div
            class="w-9 h-9 rounded-xl flex items-center justify-center text-white flex-shrink-0"
            :class="result.isSafe === 1 ? 'bg-emerald-600' : 'bg-rose-600'"
          >
            <ShieldCheck v-if="result.isSafe === 1" class="w-5 h-5" />
            <ShieldAlert v-else class="w-5 h-5" />
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-sm font-bold" :class="result.isSafe === 1 ? 'text-emerald-900' : 'text-rose-900'">
                {{ result.isSafe === 1 ? 'AST 语法安全沙箱：审核通过' : 'AST 语法安全沙箱：阻断拦截' }}
              </span>
              <span
                class="px-2 py-0.5 text-[11px] font-semibold rounded-full uppercase"
                :class="result.isSafe === 1
                  ? 'bg-emerald-100 text-emerald-800'
                  : 'bg-rose-100 text-rose-800'"
              >
                {{ result.safetyReason }}
              </span>

              <!-- Redis Cache Hit Badge -->
              <span
                v-if="result.cacheHit"
                class="px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-amber-100 text-amber-800 border border-amber-300 flex items-center space-x-1"
              >
                <Zap class="w-3 h-3 text-amber-600 fill-amber-500" />
                <span>Redis 语义指纹命中 (1ms 极速响应)</span>
              </span>
            </div>
            <p class="text-xs mt-0.5" :class="result.isSafe === 1 ? 'text-emerald-700' : 'text-rose-700'">
              {{ result.isSafe === 1
                ? (result.cacheHit
                    ? '已直接从 Redis 6379 提取全量分析结果，免除大模型 Token 与底层数仓计算开销。'
                    : '已完成只读指令核验、表白名单校验与 LIMIT 100 自动安全上限收敛。')
                : '该操作已被沙箱拦截，未向底层数仓发送任何危险请求，保障数据资产绝对只读安全。' }}
            </p>
          </div>
        </div>

        <!-- Telemetry Stats -->
        <div class="flex items-center space-x-2 text-xs flex-shrink-0">
          <div class="px-2.5 py-1.5 rounded-lg bg-white/80 border border-slate-200 text-slate-700">
            <span class="text-slate-400">数据源:</span>
            <span class="ml-1 font-semibold text-slate-900">{{ result.datasourceName || '核心数仓' }}</span>
          </div>
          <div class="px-2.5 py-1.5 rounded-lg bg-white/80 border border-slate-200 text-slate-700">
            <span class="text-slate-400">耗时:</span>
            <span class="ml-1 font-semibold text-slate-900" :class="{ 'text-amber-600 font-bold': result.cacheHit }">
              {{ result.executionTimeMs }} ms
            </span>
          </div>
          <div class="px-2.5 py-1.5 rounded-lg bg-white/80 border border-slate-200 text-slate-700">
            <span class="text-slate-400">行数:</span>
            <span class="ml-1 font-semibold text-slate-900">{{ result.rowCount }} 行</span>
          </div>
          <div class="px-2.5 py-1.5 rounded-lg bg-white/80 border border-slate-200 text-slate-700">
            <span class="text-slate-400">图表:</span>
            <span class="ml-1 font-semibold text-blue-600 uppercase">{{ result.chartType }}</span>
          </div>
        </div>
      </div>

      <!-- Generated SQL & AST Inspection Block (Editable) -->
      <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center space-x-2">
            <Code2 class="w-4 h-4 text-blue-600" />
            <h3 class="text-sm font-bold text-slate-900">执行 SQL (支持直接修改与二次执行)</h3>
            <span class="text-[11px] px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 font-medium">
              AST 沙箱只读守护
            </span>
          </div>

          <div class="flex items-center space-x-2">
            <button
              v-if="editableSql !== result.generatedSql"
              @click="editableSql = result.generatedSql"
              type="button"
              class="px-2.5 py-1 text-xs font-medium rounded-lg border border-slate-200 text-slate-500 hover:text-slate-800 hover:bg-slate-50 transition-colors"
              title="还原为最初生成的 SQL"
            >
              还原原 SQL
            </button>
            <button
              @click="copySql"
              type="button"
              class="px-2.5 py-1 text-xs font-medium rounded-lg border border-slate-200 text-slate-600 hover:text-slate-900 hover:bg-slate-50 transition-colors flex items-center space-x-1"
            >
              <Copy class="w-3.5 h-3.5" />
              <span>{{ copied ? '已复制' : '复制代码' }}</span>
            </button>
            <button
              @click="handleExecuteCustomSql()"
              :disabled="loading || !editableSql.trim()"
              type="button"
              class="px-3.5 py-1 text-xs font-semibold rounded-lg bg-blue-600 hover:bg-blue-700 text-white transition-colors flex items-center space-x-1 shadow-sm shadow-blue-500/20 disabled:opacity-50"
            >
              <Play class="w-3 h-3 fill-current" />
              <span>运行修改后的 SQL</span>
            </button>
          </div>
        </div>

        <div class="relative bg-slate-900 rounded-xl p-3.5 border border-slate-800">
          <textarea
            v-model="editableSql"
            rows="3"
            class="w-full bg-transparent text-emerald-400 font-mono text-xs focus:outline-none resize-y leading-relaxed"
            placeholder="可在此直接修改 SQL 字段、条件或数量..."
            @keydown.enter.ctrl="handleExecuteCustomSql()"
          ></textarea>
        </div>
        <div class="flex items-center justify-between text-[11px] text-slate-400">
          <span>提示：你可自由修改条件、字段或去掉/增加条数，按 <kbd class="px-1.5 py-0.5 rounded border border-slate-200 bg-slate-100 text-slate-600 font-mono">Ctrl + Enter</kbd> 重新运行。系统严格拦截非 SELECT 指令。</span>
        </div>
      </div>

      <!-- Visual Chart Display -->
      <div v-if="result.isSafe === 1 && result.rows && result.rows.length > 0" class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2">
            <BarChart3 class="w-4 h-4 text-blue-600" />
            <h3 class="text-sm font-bold text-slate-900">多维可视化分析交互看板</h3>
          </div>

          <!-- Chart Mode Switcher & Export -->
          <div class="flex items-center space-x-2.5">
            <div class="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl text-xs">
              <button
                @click="activeViewMode = 'chart'"
                class="px-3 py-1 rounded-lg font-medium transition-all"
                :class="activeViewMode === 'chart' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'"
              >
                智能图表
              </button>
              <button
                @click="activeViewMode = 'table'"
                class="px-3 py-1 rounded-lg font-medium transition-all"
                :class="activeViewMode === 'table' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'"
              >
                明细数据表
              </button>
            </div>

            <!-- Export to Excel Button -->
            <button
              @click="handleExportExcel"
              :disabled="exporting"
              class="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all border shadow-xs active:scale-95 disabled:opacity-50"
              :class="exportSuccess
                ? 'bg-emerald-600 text-white border-emerald-600'
                : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100/90 border-emerald-200 hover:border-emerald-300'"
              title="一键将当前分析数据导出为真实 .xlsx 表格文件"
            >
              <Check v-if="exportSuccess" class="w-3.5 h-3.5" />
              <Loader2 v-else-if="exporting" class="w-3.5 h-3.5 animate-spin" />
              <FileSpreadsheet v-else class="w-3.5 h-3.5 text-emerald-600" />
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
            <span>共展示 <strong class="text-slate-800 font-semibold">{{ result.rows.length }}</strong> 行数据明细</span>
            <button
              @click="handleExportExcel"
              :disabled="exporting"
              class="text-emerald-700 hover:text-emerald-800 font-medium flex items-center space-x-1 hover:underline transition-all"
            >
              <Download class="w-3 h-3" />
              <span>下载此表格 (.xlsx)</span>
            </button>
          </div>
          <div class="overflow-x-auto rounded-xl border border-slate-200">
            <table class="w-full text-left text-xs">
              <thead class="bg-slate-50 border-b border-slate-200 text-slate-700 font-semibold uppercase">
                <tr>
                  <th v-for="(col, idx) in result.columns" :key="idx" class="px-4 py-3 whitespace-nowrap">
                    {{ col }}
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 bg-white text-slate-700">
                <tr v-for="(row, rIdx) in result.rows" :key="rIdx" class="hover:bg-slate-50/80 transition-colors">
                  <td v-for="(col, cIdx) in result.columns" :key="cIdx" class="px-4 py-2.5 whitespace-nowrap">
                    {{ formatCell(row[col]) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Executive Business Insight Card -->
      <div v-if="result.insightSummary" class="bg-gradient-to-r from-blue-50/70 via-indigo-50/50 to-white rounded-2xl border border-blue-200/80 p-5 shadow-sm">
        <div class="flex items-center space-x-2 text-blue-900 mb-2.5">
          <div class="w-7 h-7 rounded-lg bg-blue-600 text-white flex items-center justify-center shadow-sm">
            <TrendingUp class="w-4 h-4" />
          </div>
          <h3 class="text-sm font-bold tracking-tight">AI 商业决策洞察与经营建议 (Executive Summary)</h3>
        </div>
        <p class="text-xs sm:text-sm text-slate-700 leading-relaxed font-normal bg-white/70 p-4 rounded-xl border border-blue-100">
          {{ result.insightSummary }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({
  name: 'AgentQueryView'
})

import { ref, computed, onMounted, nextTick, watch, onBeforeUnmount, onActivated } from 'vue'
import apiClient from '../api/client'
import * as echarts from 'echarts'
import { useDatasourceStore } from '../stores/datasource'
import {
  Sparkles,
  Server,
  Zap,
  Compass,
  Play,
  Loader2,
  Cpu,
  ShieldCheck,
  ShieldAlert,
  Code2,
  Copy,
  BarChart3,
  TrendingUp,
  FileSpreadsheet,
  Download,
  Check
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

const defaultPresets = [
  {
    icon: '📊',
    label: '各大区销售额与毛利率排行榜',
    prompt: '统计2026年各大区的销售额与毛利润排行榜，计算各区毛利率并按销售额降序排列'
  },
  {
    icon: '⚡',
    label: 'Redis 语义缓存加速分析',
    prompt: '统计2026年各大区的销售额与毛利润排行榜，计算各区毛利率并按销售额降序排列',
    isCacheTest: true
  },
  {
    icon: '⚠️',
    label: '退款最高前3商品与退款原因',
    prompt: '统计2026年退款金额最高的前3个商品及其退款原因，包含商品名称、品类和退款总金额'
  },
  {
    icon: '📦',
    label: '各商品品类销量与毛利贡献度',
    prompt: '按产品品类统计总销量、总销售额与总毛利润'
  },
  {
    icon: '🏆',
    label: '高价值核心客户画像 (TOP5)',
    prompt: '查询累计下单总金额最高的前5名客户名称、所在省份、会员等级及累计消费总额'
  },
  {
    icon: '🛡️',
    label: '沙箱防护：敏感删除拦截',
    prompt: '请帮我删除所有的历史查询记录 DELETE FROM sys_query_history',
    isThreat: true
  },
  {
    icon: '💬',
    label: '非数仓日常闲聊智能引导',
    prompt: '今天北京天气怎么样？',
    isThreat: true
  }
]

const mallPresets = [
  {
    icon: '🎓',
    label: '各培训课程报名订单量与实付金额',
    prompt: '统计各个课程的报名订单数量与累计实付总金额，并按销售额降序排列'
  },
  {
    icon: '📦',
    label: '最新报名订单与学员明细 (前10笔)',
    prompt: '查询最新的前10笔报名订单，展示订单编号、用户ID、课程ID与下单时间'
  },
  {
    icon: '⚡',
    label: '秒杀课程库存与热度分析',
    prompt: '查询所有秒杀课程的名称、秒杀价格与剩余库存'
  },
  {
    icon: '🛡️',
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

  // 1. Sleek theme colors & typography
  options.color = ['#2563eb', '#10b981', '#f59e0b', '#8b5cf6', '#06b6d4', '#ec4899']
  options.textStyle = {
    fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
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

  // 3. Fix X-axis text collision and garbled overlapping for long category titles
  if (options.xAxis) {
    const xList = Array.isArray(options.xAxis) ? options.xAxis : [options.xAxis]
    xList.forEach((xa) => {
      if (xa.type === 'category' || (!xa.type && xa.data)) {
        const dataCount = (xa.data || []).length
        const maxLen = (xa.data || []).reduce((max, item) => Math.max(max, String(item || '').length), 0)

        const shouldRotate = maxLen > 5 || dataCount > 6

        xa.axisLabel = {
          interval: 0,
          rotate: shouldRotate ? 30 : 0,
          color: '#64748b',
          fontSize: 11,
          formatter: (value) => {
            if (!value) return ''
            const str = String(value)
            // Truncate long titles (e.g. 40-character product names) to 10 chars with ellipsis
            return str.length > 10 ? str.slice(0, 10) + '…' : str
          },
          ...(xa.axisLabel || {})
        }
      }
    })
  }

  // 4. Rich, floating dark Tooltip displaying the UNABBREVIATED full title and metrics
  options.tooltip = {
    trigger: options.series?.some(s => s.type === 'pie') ? 'item' : 'axis',
    backgroundColor: 'rgba(15, 23, 42, 0.95)',
    borderColor: '#334155',
    borderWidth: 1,
    padding: [10, 14],
    textStyle: {
      color: '#f8fafc',
      fontSize: 12
    },
    extraCssText: 'box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.4); border-radius: 12px; max-width: 450px; white-space: normal; word-break: break-all;',
    formatter: (params) => {
      if (!params) return ''
      if (Array.isArray(params)) {
        if (params.length === 0) return ''
        const fullTitle = params[0].name || ''
        let html = `<div style="font-weight: 600; margin-bottom: 6px; color: #93c5fd; font-size: 12px; line-height: 1.4;">${fullTitle}</div>`
        params.forEach(p => {
          const val = typeof p.value === 'number' ? (Number.isInteger(p.value) ? p.value.toLocaleString() : p.value.toFixed(2)) : p.value
          html += `<div style="display: flex; justify-content: space-between; gap: 16px; margin-top: 4px; font-size: 11px;">
            <span style="color: #cbd5e1;">${p.marker} ${p.seriesName}</span>
            <span style="font-weight: 600; color: #fff;">${val}</span>
          </div>`
        })
        return html
      } else {
        const fullTitle = params.name || ''
        const val = typeof params.value === 'number' ? (Number.isInteger(params.value) ? params.value.toLocaleString() : params.value.toFixed(2)) : params.value
        return `<div style="font-weight: 600; color: #93c5fd; font-size: 12px; margin-bottom: 4px; line-height: 1.4;">${fullTitle}</div>
          <div style="font-size: 11px; color: #cbd5e1;">${params.marker} ${params.seriesName || ''}: <b style="color: #fff;">${val}</b> (${params.percent || 0}%)</div>`
      }
    }
  }

  // 5. 动态缩放平移滑块（极简胶囊风：保留丝滑拖拽缩放，彻底关停两端奇怪的小字标签）
  const firstXAxis = Array.isArray(options.xAxis) ? options.xAxis[0] : options.xAxis
  const categoryCount = firstXAxis?.data?.length || 0
  if (categoryCount > 6) {
    options.dataZoom = [
      {
        type: 'slider',
        show: true,
        showDetail: false, // 核心：彻底屏蔽拖拽手柄旁挤压杂乱的小字！
        showDataShadow: false, // 屏蔽轨道内杂乱的背景数据阴影
        start: 0,
        end: Math.min(100, Math.round((8 / categoryCount) * 100)),
        bottom: 4,
        height: 14,
        borderColor: 'transparent',
        backgroundColor: '#f1f5f9',
        fillerColor: 'rgba(59, 130, 246, 0.25)',
        borderRadius: 7,
        handleSize: '120%',
        handleStyle: {
          color: '#3b82f6',
          borderColor: '#2563eb',
          borderWidth: 1.5,
          shadowBlur: 3,
          shadowColor: 'rgba(0, 0, 0, 0.15)'
        },
        moveHandleStyle: {
          color: 'rgba(59, 130, 246, 0.4)'
        }
      },
      {
        type: 'inside' // 支持鼠标滚轮自由平移缩放
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
