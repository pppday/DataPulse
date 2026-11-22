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
        class="pointer-events-auto w-full bg-white rounded-xl shadow-lg border p-4 flex items-start space-x-3 transition-all"
        :class="getBorderClass(toast.type)"
      >
        <!-- Icon -->
        <div class="flex-shrink-0 mt-0.5">
          <CheckCircle2 v-if="toast.type === 'success'" class="w-5 h-5 text-emerald-500" />
          <AlertCircle v-else-if="toast.type === 'error'" class="w-5 h-5 text-rose-500" />
          <AlertTriangle v-else-if="toast.type === 'warning'" class="w-5 h-5 text-amber-500" />
          <Info v-else class="w-5 h-5 text-blue-500" />
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <h4 class="text-xs font-bold text-slate-900 leading-tight">
            {{ toast.title }}
          </h4>
          <p class="text-xs text-slate-600 mt-1 leading-relaxed break-words">
            {{ toast.message }}
          </p>
        </div>

        <!-- Close Button -->
        <button
          @click="toastStore.remove(toast.id)"
          class="flex-shrink-0 text-slate-400 hover:text-slate-600 p-0.5 rounded transition-colors"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToastStore } from '../stores/toast'
import { CheckCircle2, AlertCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

const toastStore = useToastStore()

const getBorderClass = (type) => {
  switch (type) {
    case 'success':
      return 'border-emerald-200/80 shadow-emerald-500/5'
    case 'error':
      return 'border-rose-200/80 shadow-rose-500/5'
    case 'warning':
      return 'border-amber-200/80 shadow-amber-500/5'
    default:
      return 'border-blue-200/80 shadow-blue-500/5'
  }
}
</script>
