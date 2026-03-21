<template>
  <span class="growth-text" :class="[colorClass, sizeClass]">
    <span v-if="showPrefix && resolvedType !== 'flat'" class="growth-text__prefix">{{ prefixSymbol }}</span>
    <span>{{ formattedValue }}</span>
    <span v-if="suffix" class="growth-text__suffix">{{ suffix }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  value: number | string | null | undefined
  showPrefix?: boolean
  suffix?: string
  size?: 'sm' | 'md' | 'lg'
}>(), {
  showPrefix: true,
  suffix: '%',
  size: 'md',
})

const numericValue = computed(() => {
  if (props.value === null || props.value === undefined) return 0
  return typeof props.value === 'string' ? parseFloat(props.value) || 0 : props.value
})

const resolvedType = computed(() => {
  if (numericValue.value > 0) return 'up'
  if (numericValue.value < 0) return 'down'
  return 'flat'
})

const colorClass = computed(() => {
  if (resolvedType.value === 'up') return 'growth-text--up'
  if (resolvedType.value === 'down') return 'growth-text--down'
  return 'growth-text--flat'
})

const sizeClass = computed(() => `growth-text--${props.size}`)

const prefixSymbol = computed(() => {
  if (resolvedType.value === 'up') return '+'
  return ''
})

const formattedValue = computed(() => {
  if (props.value === null || props.value === undefined) return '0.00'
  const num = numericValue.value
  return num.toFixed(2)
})
</script>

<style scoped>
.growth-text {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.growth-text--up {
  color: var(--up-color);
}

.growth-text--down {
  color: var(--down-color);
}

.growth-text--flat {
  color: var(--text-tertiary);
}

.growth-text--sm {
  font-size: 12px;
}

.growth-text--md {
  font-size: 14px;
}

.growth-text--lg {
  font-size: 18px;
}

.growth-text__suffix {
  margin-left: 1px;
}
</style>
