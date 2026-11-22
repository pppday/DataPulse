<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div class="flex items-center space-x-2">
            <span class="p-1.5 rounded-lg bg-blue-50 text-blue-600 border border-blue-100">
              <ShieldCheck class="w-4 h-4" />
            </span>
            <h1 class="text-base font-bold text-slate-900 tracking-tight">SQL 执行与安全审计台账</h1>
            <span class="text-[11px] px-2 py-0.5 rounded-full font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
              全量链路落库可追溯
            </span>
          </div>
          <p class="mt-1 text-xs text-slate-500">
            全量记录自然语言推导的 SQL 查询、AST 语法树审查拦截详情及端到端执行耗时流水。
          </p>
        </div>

        <button
          @click="fetchLogs"
          :disabled="loading"
          class="flex items-center space-x-1.5 px-3.5 py-2 text-xs font-medium rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700 transition-colors"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" />
          <span>刷新审计流水</span>
        </button>
      </div>
    </div>

    <!-- Filter & Table Card -->
    <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
      <!-- Status Tabs -->
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <button
            @click="filterStatus = 'all'"
            class="px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"
            :class="filterStatus === 'all' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'"
          >
            全部记录 ({{ logs.length }})
          </button>
          <button
            @click="filterStatus = 'safe'"
            class="px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"
            :class="filterStatus === 'safe' ? 'bg-emerald-600 text-white' : 'text-slate-600 hover:bg-slate-100'"
          >
            审查通过 ({{ logs.filter(l => l.isSafe === 1).length }})
          </button>
          <button
            @click="filterStatus = 'intercepted'"
            class="px-3 py-1.5 text-xs font-medium rounded-lg transition-colors"
            :class="filterStatus === 'intercepted' ? 'bg-rose-600 text-white' : 'text-slate-600 hover:bg-slate-100'"
          >
            沙箱阻断 ({{ logs.filter(l => l.isSafe === 0).length }})
          </button>
        </div>

        <div class="text-xs text-slate-400">
          共收录 {{ filteredLogs.length }} 笔分析轨迹
        </div>
      </div>

      <!-- Audit Logs Table -->
      <div class="overflow-x-auto rounded-xl border border-slate-200">
        <table class="w-full text-left text-xs">
          <thead class="bg-slate-50 border-b border-slate-200 text-slate-700 font-semibold uppercase">
            <tr>
              <th class="px-4 py-3">流水ID</th>
              <th class="px-4 py-3">操作人</th>
              <th class="px-4 py-3">分析自然语言需求</th>
              <th class="px-4 py-3">AST 沙箱裁定</th>
              <th class="px-4 py-3">耗时</th>
              <th class="px-4 py-3">行数</th>
              <th class="px-4 py-3">审计时间</th>
              <th class="px-4 py-3 text-right">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-slate-700">
            <tr
              v-for="log in filteredLogs"
              :key="log.id"
              class="hover:bg-slate-50 transition-colors"
            >
              <td class="px-4 py-3 font-mono font-medium text-slate-500">
                #{{ log.id }}
              </td>
              <td class="px-4 py-3 whitespace-nowrap">
                <div class="font-medium text-slate-900">{{ log.realName || log.username || '系统用户' }}</div>
                <div class="text-[11px] text-slate-400 font-mono">@{{ log.username }}</div>
              </td>
              <td class="px-4 py-3 max-w-xs truncate text-slate-800" :title="log.userPrompt">
                {{ log.userPrompt }}
              </td>
              <td class="px-4 py-3 whitespace-nowrap">
                <span
                  class="px-2 py-0.5 text-[11px] font-bold rounded-full"
                  :class="log.isSafe === 1
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border border-rose-200'"
                >
                  {{ log.isSafe === 1 ? 'PASS 审查通过' : 'BLOCKED 阻断' }}
                </span>
              </td>
              <td class="px-4 py-3 font-mono">
                {{ log.executionTimeMs }} ms
              </td>
              <td class="px-4 py-3 font-mono">
                {{ log.rowCount }} 行
              </td>
              <td class="px-4 py-3 whitespace-nowrap text-slate-500 font-mono text-[11px]">
                {{ log.createdAt }}
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
                <button
                  @click="openDetail(log)"
                  class="text-blue-600 hover:text-blue-800 font-medium hover:underline text-xs"
                >
                  查看详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Detail Modal -->
    <div
      v-if="activeLog"
      class="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4"
      @click.self="activeLog = null"
    >
      <div class="bg-white rounded-2xl max-w-2xl w-full border border-slate-200 shadow-xl overflow-hidden p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2">
            <span
              class="px-2 py-0.5 text-xs font-bold rounded"
              :class="activeLog.isSafe === 1 ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'"
            >
              {{ activeLog.isSafe === 1 ? 'PASS 审查通过' : 'BLOCKED 安全拦截' }}
            </span>
            <h3 class="text-sm font-bold text-slate-900">审计轨迹详情 #{{ activeLog.id }}</h3>
          </div>
          <button @click="activeLog = null" class="text-slate-400 hover:text-slate-700 text-lg leading-none">
            &times;
          </button>
        </div>

        <div class="space-y-3 text-xs">
          <div>
            <div class="font-semibold text-slate-500 mb-1">用户自然语言需求</div>
            <div class="p-3 bg-slate-50 rounded-xl border border-slate-200 text-slate-800 font-medium">
              {{ activeLog.userPrompt }}
            </div>
          </div>

          <div>
            <div class="font-semibold text-slate-500 mb-1">执行/拦截原因与 AST 裁定说明</div>
            <div class="p-3 rounded-xl border font-mono" :class="activeLog.isSafe === 1 ? 'bg-emerald-50/50 border-emerald-200 text-emerald-800' : 'bg-rose-50/50 border-rose-200 text-rose-800'">
              {{ activeLog.safetyReason }}
            </div>
          </div>

          <div>
            <div class="font-semibold text-slate-500 mb-1">执行 SQL 语句</div>
            <pre class="p-3 bg-slate-900 text-slate-100 rounded-xl overflow-x-auto font-mono text-[11px] leading-relaxed">{{ activeLog.generatedSql }}</pre>
          </div>

          <div v-if="activeLog.insightSummary">
            <div class="font-semibold text-slate-500 mb-1">AI 商业决策洞察摘要</div>
            <div class="p-3 bg-blue-50/50 rounded-xl border border-blue-200 text-slate-700 leading-relaxed">
              {{ activeLog.insightSummary }}
            </div>
          </div>
        </div>

        <div class="flex justify-end pt-3 border-t border-slate-100">
          <button
            @click="activeLog = null"
            class="px-4 py-2 text-xs font-medium rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
          >
            关 闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import apiClient from '../api/client'
import { useToastStore } from '../stores/toast'
import { ShieldCheck, RefreshCw } from 'lucide-vue-next'

const toast = useToastStore()
const logs = ref([])
const loading = ref(false)
const filterStatus = ref('all')
const activeLog = ref(null)

const fetchLogs = async () => {
  loading.value = true
  try {
    const data = await apiClient.get('/agent/history?limit=50')
    logs.value = data
  } catch (err) {
    toast.error(err.message || '获取审计日志失败')
  } finally {
    loading.value = false
  }
}

const filteredLogs = computed(() => {
  if (filterStatus.value === 'safe') {
    return logs.value.filter(l => l.isSafe === 1)
  }
  if (filterStatus.value === 'intercepted') {
    return logs.value.filter(l => l.isSafe === 0)
  }
  return logs.value
})

const openDetail = (log) => {
  activeLog.value = log
}

onMounted(() => {
  fetchLogs()
})
</script>
