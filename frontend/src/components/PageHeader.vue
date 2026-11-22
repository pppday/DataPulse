<template>
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

      <div v-if="$slots.actions" class="flex flex-wrap items-center gap-2.5 flex-shrink-0">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.default" class="mt-5 pt-4 border-t border-slate-200/60">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  icon: {
    type: [Object, Function],
    default: null
  },
  title: {
    type: String,
    required: true
  },
  eyebrow: {
    type: String,
    default: ''
  },
  badge: {
    type: String,
    default: ''
  },
  badgeTone: {
    type: String,
    default: 'success',
    validator: (v) => ['success', 'accent', 'warning', 'danger', 'neutral'].includes(v)
  },
  description: {
    type: String,
    default: ''
  }
})
</script>
