<template>
  <!-- 右上角全局通知浮层：外层用 pointer-events-none 防止透明空白区挡住底下页面点击，单条消息卡片再开启 pointer-events-auto -->
  <div class="fixed top-5 right-5 z-50 flex flex-col space-y-2.5 max-w-sm w-full pointer-events-none">
    <transition-group
      enter-active-class="transform ease-out duration-300 transition"
      enter-from-class="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-4"
      enter-to-class="translate-y-0 opacity-100 sm:translate-x-0"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="pointer-events-auto relative w-full overflow-hidden rounded-xl bg-white border border-slate-200 shadow-lift p-4 pl-5 flex items-start gap-3 transition-all"
      >
        <!-- 卡片左侧 3px 状态强调色条 -->
        <span class="absolute left-0 inset-y-0 w-[3px]" :class="getAccentClass(toast.type)"></span>

        <!-- 按通知类型展示对应图标 -->
        <div class="flex-shrink-0 mt-0.5">
          <CheckCircle2 v-if="toast.type === 'success'" class="w-4 h-4 text-pine-600" />
          <AlertCircle v-else-if="toast.type === 'error'" class="w-4 h-4 text-crimson-500" />
          <AlertTriangle v-else-if="toast.type === 'warning'" class="w-4 h-4 text-accent-600" />
          <Info v-else class="w-4 h-4 text-ink-900" />
        </div>

        <!-- 标题与正文内容 -->
        <div class="flex-1 min-w-0">
          <h4 class="text-xs font-semibold text-ink-900 leading-tight">
            {{ toast.title }}
          </h4>
          <p class="text-xs text-slate-600 mt-1 leading-relaxed break-words">
            {{ toast.message }}
          </p>
        </div>

        <!-- 手动关闭按钮 -->
        <button
          @click="toastStore.remove(toast.id)"
          class="flex-shrink-0 text-slate-400 hover:text-ink-900 p-0.5 rounded transition-colors cursor-pointer"
        >
          <X class="w-3.5 h-3.5" />
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
/**
 * 全局轻提示（Toast）悬浮渲染组件。
 * 挂载于 App.vue 根节点，监听 Pinia 的 toastStore.toasts 队列并带动画渲染成功/错误/警告/普通消息。
 */
import { useToastStore } from '../stores/toast'
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

const toastStore = useToastStore()

// 根据提示类型返回卡片左侧竖条对应的 Tailwind 背景色类名
const getAccentClass = (type) => {
  switch (type) {
    case 'success':
      return 'bg-pine-600'
    case 'error':
      return 'bg-crimson-500'
    case 'warning':
      return 'bg-accent-500'
    default:
      return 'bg-ink-900'
  }
}
</script>
