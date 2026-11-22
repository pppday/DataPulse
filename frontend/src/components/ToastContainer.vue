<template>
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
        <span class="absolute left-0 inset-y-0 w-[3px]" :class="getAccentClass(toast.type)"></span>

        <!-- Icon -->
        <div class="flex-shrink-0 mt-0.5">
          <CheckCircle2 v-if="toast.type === 'success'" class="w-4 h-4 text-pine-600" />
          <AlertCircle v-else-if="toast.type === 'error'" class="w-4 h-4 text-crimson-500" />
          <AlertTriangle v-else-if="toast.type === 'warning'" class="w-4 h-4 text-accent-600" />
          <Info v-else class="w-4 h-4 text-ink-900" />
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <h4 class="text-xs font-semibold text-ink-900 leading-tight">
            {{ toast.title }}
          </h4>
          <p class="text-xs text-slate-600 mt-1 leading-relaxed break-words">
            {{ toast.message }}
          </p>
        </div>

        <!-- Close Button -->
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
import { useToastStore } from '../stores/toast'
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

const toastStore = useToastStore()

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
