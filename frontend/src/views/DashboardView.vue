<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div class="flex items-center space-x-2">
            <span class="p-1.5 rounded-lg bg-blue-50 text-blue-600 border border-blue-100">
              <LayoutDashboard class="w-4 h-4" />
            </span>
            <h1 class="text-base font-bold text-slate-900 tracking-tight">数仓运营与性能监控大盘</h1>
            <span class="text-[11px] px-2 py-0.5 rounded-full font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
              实时数据流监控
            </span>
          </div>
          <p class="mt-1 text-xs text-slate-500">
            全平台 Text-to-SQL 调用频次、AST 审查拦截率、执行延时及底层数仓核心资产汇总。
          </p>
        </div>

        <button
          @click="fetchMetrics"
          :disabled="loading"
          class="flex items-center space-x-1.5 px-3.5 py-2 text-xs font-medium rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700 transition-colors"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" />
          <span>刷新大盘指标</span>
        </button>
      </div>
    </div>

    <!-- 4 Key Metric Cards -->
    <div v-if="metrics" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Card 1: Total Queries -->
      <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-500">总分析请求频次</span>
          <span class="p-2 rounded-xl bg-blue-50 text-blue-600">
            <Activity class="w-4 h-4" />
          </span>
        </div>
        <div class="mt-3 flex items-baseline space-x-2">
          <span class="text-2xl font-bold text-slate-900">{{ metrics.totalQueries }}</span>
          <span class="text-xs text-slate-400">次分析调用</span>
        </div>
        <div class="mt-2 text-xs text-slate-500 flex items-center space-x-1">
          <span>安全通过:</span>
          <span class="font-semibold text-emerald-600">{{ metrics.totalSafePass }}</span>
          <span class="text-slate-300">|</span>
          <span>沙箱拦截:</span>
          <span class="font-semibold text-rose-600">{{ metrics.totalIntercepted }}</span>
        </div>
      </div>

      <!-- Card 2: AST Safety Pass Rate -->
      <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-500">AST 沙箱安全通过率</span>
          <span class="p-2 rounded-xl bg-emerald-50 text-emerald-600">
            <ShieldCheck class="w-4 h-4" />
          </span>
        </div>
        <div class="mt-3 flex items-baseline space-x-2">
          <span class="text-2xl font-bold text-emerald-600">{{ metrics.safePassRate }}%</span>
          <span class="text-xs text-slate-400">只读语法合规</span>
        </div>
        <div class="mt-2 text-xs text-slate-500">
          严格阻断 DDL/DML 与越权表探针
        </div>
      </div>

      <!-- Card 3: Avg Execution Latency -->
      <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-500">平均执行延时</span>
          <span class="p-2 rounded-xl bg-amber-50 text-amber-600">
            <Timer class="w-4 h-4" />
          </span>
        </div>
        <div class="mt-3 flex items-baseline space-x-2">
          <span class="text-2xl font-bold text-slate-900">{{ metrics.avgExecutionTimeMs }}</span>
          <span class="text-xs text-slate-400">毫秒 (ms)</span>
        </div>
        <div class="mt-2 text-xs text-slate-500">
          包含 AST 解析与 MySQL JDBC 检索
        </div>
      </div>

      <!-- Card 4: Warehouse Rows -->
      <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold text-slate-500">数仓核心资产</span>
          <span class="p-2 rounded-xl bg-indigo-50 text-indigo-600">
            <Database class="w-4 h-4" />
          </span>
        </div>
        <div class="mt-3 flex items-baseline space-x-2">
          <span class="text-2xl font-bold text-slate-900">{{ metrics.warehouseTotalRows }}</span>
          <span class="text-xs text-slate-400">条总物理行</span>
        </div>
        <div class="mt-2 text-xs text-slate-500">
          覆盖 {{ metrics.warehouseTablesCount }} 张业务维度与事实表
        </div>
      </div>
    </div>

    <!-- Charts & Activity Row -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- Left: Safety Ratio Pie Chart -->
      <div class="lg:col-span-5 bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
        <h3 class="text-sm font-bold text-slate-900">AST 审查结果安全分布</h3>
        <p class="text-xs text-slate-500">只读合规查询与高危/越权指令拦截比例</p>
        <div ref="pieRef" class="w-full h-64"></div>
      </div>

      <!-- Right: Recent Queries Stream -->
      <div class="lg:col-span-7 bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
        <h3 class="text-sm font-bold text-slate-900">最新中台分析轨迹</h3>
        <p class="text-xs text-slate-500">跨部门最新 Text-to-SQL 分析请求流</p>

        <div v-if="metrics && metrics.recentQueries" class="divide-y divide-slate-100">
          <div
            v-for="item in metrics.recentQueries"
            :key="item.id"
            class="py-3 flex items-center justify-between gap-3 text-xs"
          >
            <div class="flex items-center space-x-2.5 min-w-0">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="item.isSafe === 1 ? 'bg-emerald-500' : 'bg-rose-500'"
              ></span>
              <div class="truncate">
                <span class="font-semibold text-slate-800">{{ item.realName || item.username }}</span>
                <span class="text-slate-400 ml-1.5 font-mono">"{{ item.userPrompt }}"</span>
              </div>
            </div>

            <div class="flex items-center space-x-2 text-slate-400 flex-shrink-0 font-mono text-[11px]">
              <span v-if="item.isSafe === 1" class="text-emerald-600 font-semibold">{{ item.executionTimeMs }}ms</span>
              <span v-else class="text-rose-600 font-semibold">阻断</span>
              <span>·</span>
              <span>{{ item.createdAt?.slice(11, 19) || item.createdAt }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import apiClient from '../api/client'
import * as echarts from 'echarts'
import {
  LayoutDashboard,
  RefreshCw,
  Activity,
  ShieldCheck,
  Timer,
  Database
} from 'lucide-vue-next'
import { useToastStore } from '../stores/toast'

const toast = useToastStore()
const metrics = ref(null)
const loading = ref(false)

const pieRef = ref(null)
let pieInstance = null

const fetchMetrics = async () => {
  loading.value = true
  try {
    const data = await apiClient.get('/dashboard/metrics')
    metrics.value = data
    await nextTick()
    renderPie()
  } catch (err) {
    toast.error(err.message || '获取大盘指标失败')
  } finally {
    loading.value = false
  }
}

const renderPie = () => {
  if (!pieRef.value || !metrics.value) return
  if (pieInstance) {
    pieInstance.dispose()
  }

  pieInstance = echarts.init(pieRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次 ({d}%)'
    },
    legend: {
      bottom: '5%',
      left: 'center'
    },
    color: ['#10b981', '#f43f5e'],
    series: [
      {
        name: '安全审核',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: metrics.value.totalSafePass || 0, name: 'AST 审核通过' },
          { value: metrics.value.totalIntercepted || 0, name: '沙箱阻断' }
        ]
      }
    ]
  }

  pieInstance.setOption(option)
}

const handleResize = () => {
  if (pieInstance) {
    pieInstance.resize()
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  fetchMetrics()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (pieInstance) {
    pieInstance.dispose()
  }
})
</script>
