<template>
  <div class="space-y-6">
    <!-- Header -->
    <PageHeader
      :icon="LayoutDashboard"
      eyebrow="Observability"
      title="数仓运营与性能监控大盘"
      badge="实时数据流监控"
      badge-tone="success"
      description="全平台 Text-to-SQL 调用频次、AST 审查拦截率、执行延时及底层数仓核心资产汇总。"
    >
      <template #actions>
        <button
          @click="fetchMetrics"
          :disabled="loading"
          class="dp-btn-secondary"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" />
          <span>刷新大盘指标</span>
        </button>
      </template>
    </PageHeader>

    <!-- 4 Key Metric Cards (Restrained Editorial Style, No Blur Orbs) -->
    <div v-if="metrics" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Card 1: Total Queries -->
      <div class="dp-card dp-card-hover dp-rise p-5" style="animation-delay: 40ms">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">总分析请求频次</span>
          <span class="dp-icon-mark w-8 h-8">
            <Activity class="w-4 h-4 text-ink-900" />
          </span>
        </div>
        <div class="mt-4 flex items-baseline space-x-2">
          <span class="dp-stat-number">{{ fmt(metrics.totalQueries) }}</span>
          <span class="text-xs text-slate-400">次分析调用</span>
        </div>
        <div class="pt-3 mt-3 border-t border-slate-100 text-xs text-slate-500 flex items-center space-x-1.5">
          <span>安全通过:</span>
          <span class="font-semibold text-pine-600 tabular-nums">{{ fmt(metrics.totalSafePass) }}</span>
          <span class="text-slate-300">|</span>
          <span>沙箱拦截:</span>
          <span class="font-semibold text-crimson-600 tabular-nums">{{ fmt(metrics.totalIntercepted) }}</span>
        </div>
      </div>

      <!-- Card 2: AST Safety Pass Rate -->
      <div class="dp-card dp-card-hover dp-rise p-5" style="animation-delay: 90ms">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">AST 沙箱安全通过率</span>
          <span class="dp-icon-mark w-8 h-8">
            <ShieldCheck class="w-4 h-4 text-pine-600" />
          </span>
        </div>
        <div class="mt-4 flex items-baseline space-x-2">
          <span class="dp-stat-number text-pine-600">{{ metrics.safePassRate }}%</span>
          <span class="text-xs text-slate-400">只读语法合规</span>
        </div>
        <div class="pt-3 mt-3 border-t border-slate-100 text-xs text-slate-500">
          严格阻断 DDL/DML 与越权表探针
        </div>
      </div>

      <!-- Card 3: Avg Execution Latency -->
      <div class="dp-card dp-card-hover dp-rise p-5" style="animation-delay: 140ms">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">平均执行延时</span>
          <span class="dp-icon-mark w-8 h-8">
            <Timer class="w-4 h-4 text-accent-600" />
          </span>
        </div>
        <div class="mt-4 flex items-baseline space-x-2">
          <span class="dp-stat-number">{{ fmt(metrics.avgExecutionTimeMs) }}</span>
          <span class="text-xs text-slate-400">毫秒 (ms)</span>
        </div>
        <div class="pt-3 mt-3 border-t border-slate-100 text-xs text-slate-500">
          包含 AST 解析与 MySQL JDBC 检索
        </div>
      </div>

      <!-- Card 4: Warehouse Rows -->
      <div class="dp-card dp-card-hover dp-rise p-5" style="animation-delay: 190ms">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">数仓核心资产</span>
          <span class="dp-icon-mark w-8 h-8">
            <Database class="w-4 h-4 text-ink-900" />
          </span>
        </div>
        <div class="mt-4 flex items-baseline space-x-2">
          <span class="dp-stat-number">{{ fmt(metrics.warehouseTotalRows) }}</span>
          <span class="text-xs text-slate-400">条总物理行</span>
        </div>
        <div class="pt-3 mt-3 border-t border-slate-100 text-xs text-slate-500">
          覆盖 {{ metrics.warehouseTablesCount }} 张业务维度与事实表
        </div>
      </div>
    </div>

    <!-- Loading Skeleton -->
    <div v-else-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="i in 4" :key="i" class="dp-card h-[142px] animate-pulse"></div>
    </div>

    <!-- Charts & Activity Row -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- Left: Safety Ratio Pie Chart -->
      <div class="lg:col-span-5 dp-card p-5 space-y-3 dp-rise" style="animation-delay: 230ms">
        <h3 class="dp-section-title">AST 审查结果安全分布</h3>
        <p class="text-xs text-slate-500">只读合规查询与高危/越权指令拦截比例</p>
        <div ref="pieRef" class="w-full h-64"></div>
      </div>

      <!-- Right: Recent Queries Stream -->
      <div class="lg:col-span-7 dp-card p-5 space-y-3 dp-rise" style="animation-delay: 280ms">
        <h3 class="dp-section-title">最新中台分析轨迹</h3>
        <p class="text-xs text-slate-500">跨部门最新 Text-to-SQL 分析请求流</p>

        <div v-if="metrics && metrics.recentQueries" class="divide-y divide-slate-100">
          <div
            v-for="item in metrics.recentQueries"
            :key="item.id"
            class="py-3 flex items-center justify-between gap-3 text-xs hover:bg-slate-50/70 -mx-2 px-2 rounded-lg transition-colors"
          >
            <div class="flex items-center space-x-3 min-w-0">
              <span
                class="w-1.5 h-1.5 rounded-full flex-shrink-0"
                :class="item.isSafe === 1 ? 'bg-pine-600' : 'bg-crimson-500'"
              ></span>
              <div class="truncate">
                <span class="font-semibold text-ink-900">{{ item.realName || item.username }}</span>
                <span class="text-slate-500 ml-1.5">"{{ item.userPrompt }}"</span>
              </div>
            </div>

            <div class="flex items-center space-x-2 text-slate-400 flex-shrink-0 font-mono text-[11px] tabular-nums">
              <span v-if="item.isSafe === 1" class="text-pine-600 font-semibold">{{ item.executionTimeMs }}ms</span>
              <span v-else class="text-crimson-600 font-semibold">阻断</span>
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
import PageHeader from '../components/PageHeader.vue'
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

const fmt = (v) => {
  if (v === null || v === undefined || v === '') return '-'
  return isNaN(Number(v)) ? v : Number(v).toLocaleString()
}

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
    title: {
      text: (metrics.value.safePassRate ?? 0) + '%',
      subtext: '安全通过率',
      left: 'center',
      top: '34%',
      textStyle: {
        fontSize: 26,
        fontWeight: 600,
        color: '#181715',
        fontFamily: '"Plus Jakarta Sans", Inter, sans-serif'
      },
      subtextStyle: {
        fontSize: 11,
        color: '#706B63'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次 ({d}%)',
      backgroundColor: '#FFFFFF',
      borderColor: '#E7E4DC',
      borderWidth: 1,
      textStyle: {
        color: '#181715',
        fontSize: 12
      },
      extraCssText: 'box-shadow:0 12px 28px -12px rgba(24,23,21,.16);border-radius:10px;'
    },
    legend: {
      bottom: '2%',
      left: 'center',
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: {
        color: '#545049'
      }
    },
    color: ['#1F6E56', '#C24B54'],
    series: [
      {
        name: '安全审核',
        type: 'pie',
        radius: ['64%', '80%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#FFFFFF',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          scale: true,
          label: {
            show: false
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
