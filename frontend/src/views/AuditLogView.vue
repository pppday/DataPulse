<template>
  <div class="space-y-6">
    <!-- Header -->
    <PageHeader
      :icon="ShieldCheck"
      eyebrow="Security Audit"
      title="SQL 执行与安全审计台账"
      badge="全量链路落库可追溯"
      badge-tone="success"
      description="全量记录自然语言推导的 SQL 查询、AST 语法树审查拦截详情及端到端执行耗时流水。"
    >
      <template #actions>
        <button
          @click="fetchLogs"
          :disabled="loading"
          class="dp-btn-secondary"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loading }" />
          <span>刷新审计流水</span>
        </button>
      </template>
    </PageHeader>

    <!-- Filter & Table Card -->
    <div class="dp-card p-5 space-y-4 dp-rise" style="animation-delay: 60ms">
      <!-- Status Tabs -->
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="dp-segment">
          <button
            @click="filterStatus = 'all'"
            class="dp-segment-item"
            :class="{ 'dp-segment-item-active': filterStatus === 'all' }"
          >
            <span>全部记录</span>
            <span class="ml-1 rounded-full bg-slate-200/70 px-1.5 text-[10px] tabular-nums">{{ logs.length }}</span>
          </button>
          <button
            @click="filterStatus = 'safe'"
            class="dp-segment-item"
            :class="{ 'dp-segment-item-active text-pine-700': filterStatus === 'safe' }"
          >
            <span>审查通过</span>
            <span class="ml-1 rounded-full bg-slate-200/70 px-1.5 text-[10px] tabular-nums">{{ logs.filter(l => l.isSafe === 1).length }}</span>
          </button>
          <button
            @click="filterStatus = 'intercepted'"
            class="dp-segment-item"
            :class="{ 'dp-segment-item-active text-crimson-600': filterStatus === 'intercepted' }"
          >
            <span>沙箱阻断</span>
            <span class="ml-1 rounded-full bg-slate-200/70 px-1.5 text-[10px] tabular-nums">{{ logs.filter(l => l.isSafe === 0).length }}</span>
          </button>
        </div>

        <div class="text-xs text-slate-400 tabular-nums">
          共收录 {{ filteredLogs.length }} 笔分析轨迹
        </div>
      </div>

      <!-- Audit Logs Table -->
      <div class="dp-table-wrap">
        <table class="dp-table">
          <thead>
            <tr>
              <th>流水ID</th>
              <th>操作人</th>
              <th>分析自然语言需求</th>
              <th>AST 沙箱裁定</th>
              <th>耗时</th>
              <th>行数</th>
              <th>审计时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody class="text-slate-700">
            <tr v-for="log in filteredLogs" :key="log.id">
              <td class="font-mono font-medium text-slate-400 tabular-nums">
                #{{ log.id }}
              </td>
              <td class="whitespace-nowrap">
                <div class="font-medium text-ink-900">{{ log.realName || log.username || '系统用户' }}</div>
                <div class="text-[11px] text-slate-400 font-mono">@{{ log.username }}</div>
              </td>
              <td class="max-w-xs truncate text-slate-800" :title="log.userPrompt">
                {{ log.userPrompt }}
              </td>
              <td class="whitespace-nowrap">
                <span :class="log.isSafe === 1 ? 'dp-badge-success' : 'dp-badge-danger'">
                  <span class="w-1.5 h-1.5 rounded-full bg-current"></span>
                  <span>{{ log.isSafe === 1 ? 'PASS 审查通过' : 'BLOCKED 阻断' }}</span>
                </span>
              </td>
              <td class="font-mono tabular-nums">
                {{ log.executionTimeMs }} ms
              </td>
              <td class="font-mono tabular-nums">
                {{ log.rowCount }} 行
              </td>
              <td class="whitespace-nowrap text-slate-500 font-mono text-[11px] tabular-nums">
                {{ log.createdAt }}
              </td>
              <td class="text-right whitespace-nowrap">
                <button
                  @click="openDetail(log)"
                  class="dp-btn-ghost text-ink-900 font-medium px-2.5 py-1"
                >
                  查看详情
                </button>
              </td>
            </tr>

            <tr v-if="!loading && filteredLogs.length === 0">
              <td colspan="8" class="py-14 text-center">
                <div class="dp-icon-mark w-12 h-12 mx-auto mb-3 text-slate-400">
                  <ShieldCheck class="w-5 h-5" />
                </div>
                <div class="text-xs text-slate-400">暂无审计记录</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Detail Modal -->
    <div
      v-if="activeLog"
      class="dp-modal-mask"
      @click.self="activeLog = null"
    >
      <div class="dp-modal max-w-2xl space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center space-x-2">
            <span :class="activeLog.isSafe === 1 ? 'dp-badge-success' : 'dp-badge-danger'">
              {{ activeLog.isSafe === 1 ? 'PASS 审查通过' : 'BLOCKED 安全拦截' }}
            </span>
            <h3 class="dp-section-title">审计轨迹详情 #{{ activeLog.id }}</h3>
          </div>
          <button @click="activeLog = null" class="dp-btn-ghost p-1.5 rounded-full">
            <X class="w-4 h-4" />
          </button>
        </div>

        <div class="space-y-3 text-xs">
          <div>
            <div class="font-semibold text-slate-500 mb-1">用户自然语言需求</div>
            <div class="p-3 bg-slate-50/80 rounded-xl border border-slate-200/70 text-ink-900 font-medium">
              {{ activeLog.userPrompt }}
            </div>
          </div>

          <div>
            <div class="font-semibold text-slate-500 mb-1">执行/拦截原因与 AST 裁定说明</div>
            <div
              class="p-3 rounded-xl border font-mono"
              :class="activeLog.isSafe === 1
                ? 'bg-pine-50/70 border-pine-200 text-pine-800'
                : 'bg-crimson-50/70 border-crimson-200 text-crimson-700'"
            >
              {{ activeLog.safetyReason }}
            </div>
          </div>

          <div>
            <div class="font-semibold text-slate-500 mb-1">执行 SQL 语句</div>
            <pre class="dp-code p-4 overflow-x-auto font-mono text-[11px] leading-relaxed">{{ activeLog.generatedSql }}</pre>
          </div>

          <div v-if="activeLog.insightSummary">
            <div class="font-semibold text-slate-500 mb-1">AI 商业决策洞察摘要</div>
            <div class="p-3.5 bg-[#FAF7F1] border border-accent-200/80 rounded-xl text-slate-700 leading-relaxed">
              {{ activeLog.insightSummary }}
            </div>
          </div>
        </div>

        <div class="flex justify-end pt-3 border-t border-slate-100">
          <button
            @click="activeLog = null"
            class="dp-btn-secondary px-4"
          >
            关 闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 【SQL 执行与安全审计台账页（AuditLogView）】
 * 展示平台所有历史查询与安全拦截流水，支持按“全部 / 审查通过 / 沙箱拦截”筛选，并弹窗查看完整 SQL、拦截原因与执行耗时。
 */
import { ref, computed, onMounted } from 'vue'
import apiClient from '../api/client'
import { useToastStore } from '../stores/toast'
import PageHeader from '../components/PageHeader.vue'
import { ShieldCheck, RefreshCw, X } from 'lucide-vue-next'

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
