<template>
  <!-- 全站统一页头骨架：左侧展示眉标/主标题/状态徽章/说明，右侧为 #actions 操作区，底部默认插槽放汇总指标或快捷预设 -->
  <section class="border-b border-slate-200/80 pb-6 dp-rise">
    <div class="flex flex-col lg:flex-row lg:items-end justify-between gap-5">
      <div class="min-w-0">
        <div v-if="eyebrow" class="dp-eyebrow">
          <span class="w-4 h-px bg-accent-400"></span>
          <span>{{ eyebrow }}</span>
        </div>
        <div class="mt-1.5 flex flex-wrap items-center gap-2.5">
          <h1 class="dp-title">{{ title }}</h1>
          <span v-if="badge" :class="`dp-badge-${badgeTone}`">
            <span class="w-1.5 h-1.5 rounded-full bg-current opacity-75"></span>
            {{ badge }}
          </span>
        </div>
        <p v-if="description" class="dp-subtitle mt-1.5 max-w-2xl">{{ description }}</p>
      </div>

      <!-- 右侧操作按钮插槽（如刷新按钮、数据源切换器、新增入口） -->
      <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2.5 flex-shrink-0">
        <slot name="actions" />
      </div>
    </div>

    <!-- 可选底部扩展插槽（如工作台推荐提问标签、字典页表数量概览） -->
    <div v-if="$slots.default" class="mt-5 pt-4 border-t border-slate-200/60">
      <slot />
    </div>
  </section>
</template>

<script setup>
/**
 * 通用页面标题栏组件（PageHeader）。
 * 被分析工作台、数据字典、数据源管理、安全审计、运营大盘 5 个主页面复用，
 * 保证全平台每个一级页面的顶部视觉层次和间距完全一致。
 */
defineProps({
  // 可选页面图标组件（保留用于扩展）
  icon: {
    type: [Object, Function],
    default: null
  },
  // 页面主标题（必填）
  title: {
    type: String,
    required: true
  },
  // 标题上方英文小眉标，用于强化板块分区感
  eyebrow: {
    type: String,
    default: ''
  },
  // 紧跟标题右侧的状态胶囊文案（如“AST 只读沙箱防护”）
  badge: {
    type: String,
    default: ''
  },
  // 胶囊颜色主题：映射到 style.css 中对应的 dp-badge-* 样式类
  badgeTone: {
    type: String,
    default: 'success',
    validator: (v) => ['success', 'accent', 'warning', 'danger', 'neutral'].includes(v)
  },
  // 页面一句话功能描述
  description: {
    type: String,
    default: ''
  }
})
</script>
