<template>
  <div 
    class="stat-card card card-stagger" 
    :class="{ 
      'stat-card--clickable': clickable,
      [`stat-card--${variant}`]: variant 
    }"
    @click="handleClick"
  >
    <div class="stat-card__header">
      <div class="stat-card__icon-wrapper" v-if="icon" :style="iconStyle">
        <span class="stat-card__icon">{{ icon }}</span>
      </div>
      <span class="stat-card__label">{{ label }}</span>
      <div class="stat-card__extra" v-if="$slots.extra">
        <slot name="extra"></slot>
      </div>
    </div>
    
    <div class="stat-card__body">
      <div class="stat-card__value-wrapper">
        <span v-if="prefix" class="stat-card__prefix" :class="valueColorClass">{{ prefix }}</span>
        <span class="stat-card__value number-animate" :class="valueColorClass">{{ formattedDisplayValue }}</span>
        <span v-if="suffix" class="stat-card__suffix" :class="valueColorClass">{{ suffix }}</span>
      </div>
      
      <div 
        v-if="change !== undefined && change !== null" 
        class="stat-card__trend" 
        :class="trendClass"
      >
        <span class="stat-card__trend-icon">{{ trendIcon }}</span>
        <span class="stat-card__trend-value">{{ formattedChange }}</span>
        <span class="stat-card__trend-label" v-if="trendLabel">{{ trendLabel }}</span>
      </div>
    </div>
    
    <div v-if="$slots.footer" class="stat-card__footer">
      <slot name="footer"></slot>
    </div>
    
    <div v-if="loading" class="stat-card__loading">
      <NSpin size="small" />
    </div>
    
    <div v-if="sparklineData && sparklineData.length" class="stat-card__sparkline">
      <div 
        v-for="(point, index) in sparklineData" 
        :key="index" 
        class="stat-card__sparkline-bar"
        :style="{ height: `${point}%` }"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NSpin } from 'naive-ui'

interface Props {
  label: string
  value: string | number
  change?: number | null
  changeType?: 'up' | 'down' | 'flat'
  icon?: string
  iconBgColor?: string
  prefix?: string
  suffix?: string
  trendLabel?: string
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger'
  clickable?: boolean
  loading?: boolean
  sparklineData?: number[]
  animateValue?: boolean
  showSign?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  change: null,
  changeType: undefined,
  icon: '',
  iconBgColor: '',
  prefix: '',
  suffix: '',
  trendLabel: '',
  variant: 'default',
  clickable: false,
  loading: false,
  sparklineData: undefined,
  animateValue: true,
  showSign: false
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const displayValue = ref(props.value)
const formattedDisplayValue = computed(() => {
  if (!props.showSign) return displayValue.value
  const val = Number(displayValue.value)
  if (isNaN(val) || val === 0) return displayValue.value
  if (val > 0) return `+${displayValue.value}`
  return displayValue.value
})

watch(() => props.value, (newVal) => {
  if (props.animateValue && typeof newVal === 'number') {
    animateNumber(newVal)
  } else {
    displayValue.value = newVal
  }
})

const animateNumber = (target: number) => {
  const start = Number(displayValue.value) || 0
  const duration = 500
  const startTime = performance.now()
  
  const animate = (currentTime: number) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    const easeOutQuart = 1 - Math.pow(1 - progress, 4)
    const current = start + (target - start) * easeOutQuart
    
    if (typeof props.value === 'number' && !Number.isInteger(props.value)) {
      displayValue.value = current.toFixed(2)
    } else {
      displayValue.value = Math.round(current)
    }
    
    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }
  
  requestAnimationFrame(animate)
}

const resolvedChangeType = computed(() => {
  if (props.changeType) return props.changeType
  if (props.change === null || props.change === undefined) return 'flat'
  if (props.change > 0) return 'up'
  if (props.change < 0) return 'down'
  return 'flat'
})

const trendClass = computed(() => {
  const type = resolvedChangeType.value
  return {
    'stat-card__trend--up': type === 'up',
    'stat-card__trend--down': type === 'down',
    'stat-card__trend--flat': type === 'flat'
  }
})

const valueColorClass = computed(() => {
  const type = resolvedChangeType.value
  return {
    'stat-card__value--up': type === 'up',
    'stat-card__value--down': type === 'down'
  }
})

const trendIcon = computed(() => {
  const type = resolvedChangeType.value
  if (type === 'up') return '↑'
  if (type === 'down') return '↓'
  return '→'
})

const formattedChange = computed(() => {
  if (props.change === null || props.change === undefined) return ''
  const abs = Math.abs(props.change)
  return `${abs.toFixed(2)}%`
})

const iconStyle = computed(() => {
  if (props.iconBgColor) {
    return { backgroundColor: props.iconBgColor }
  }
  
  const type = resolvedChangeType.value
  if (type === 'up') return { backgroundColor: 'var(--up-bg)' }
  if (type === 'down') return { backgroundColor: 'var(--down-bg)' }
  return { backgroundColor: 'var(--bg-tertiary)' }
})

const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}
</script>

<style scoped>
.stat-card {
  padding: 24px;
  position: relative;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  background-image: linear-gradient(155deg, rgba(255, 255, 255, 0.04), rgba(0, 0, 0, 0));
}

.stat-card--clickable {
  cursor: pointer;
}

.stat-card--clickable:active {
  transform: translateY(0) !important;
}

.stat-card--primary {
  background: var(--gradient-primary);
  border: none;
  color: white;
}

.stat-card--primary .stat-card__label,
.stat-card--primary .stat-card__prefix,
.stat-card--primary .stat-card__suffix {
  color: rgba(255, 255, 255, 0.9);
}

.stat-card--primary .stat-card__value {
  color: white;
}

.stat-card--primary .stat-card__trend--up {
  color: #fca5a5;
}

.stat-card--primary .stat-card__trend--down {
  color: #86efac;
}

.stat-card--success {
  border-left: 4px solid var(--success-color);
}

.stat-card--warning {
  border-left: 4px solid var(--warning-color);
}

.stat-card--danger {
  border-left: 4px solid var(--danger-color);
}

.stat-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card__icon-wrapper {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
}

.stat-card__icon {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.stat-card__label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  flex: 1;
}

.stat-card__extra {
  flex-shrink: 0;
}

.stat-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-card__value-wrapper {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-card__value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  font-family: var(--font-number);
  letter-spacing: 0.02em;
}

.stat-card__value--up {
  color: var(--up-color);
}

.stat-card__value--down {
  color: var(--down-color);
}

.stat-card__prefix {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-secondary);
}

.stat-card__suffix {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-left: 4px;
}

.stat-card__trend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  margin-top: 8px;
  width: fit-content;
}

.stat-card__trend--up {
  background: var(--up-bg);
  color: var(--up-color);
}

.stat-card__trend--down {
  background: var(--down-bg);
  color: var(--down-color);
}

.stat-card__trend--flat {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
}

.stat-card__trend-icon {
  font-size: 14px;
  font-weight: 700;
}

.stat-card__trend-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.stat-card--primary .stat-card__icon-wrapper {
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.2);
}

.stat-card--primary .stat-card__label,
.stat-card--primary .stat-card__trend-label {
  color: rgba(255, 255, 255, 0.86);
}

.stat-card__footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.stat-card__loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: inherit;
  z-index: 10;
}

[data-theme='dark'] .stat-card__loading {
  background: rgba(30, 41, 59, 0.8);
}

.stat-card__sparkline {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  display: flex;
  align-items: flex-end;
  gap: 2px;
  padding: 0 12px 8px;
  opacity: 0.3;
  pointer-events: none;
}

.stat-card__sparkline-bar {
  flex: 1;
  background: var(--primary-color);
  border-radius: 2px 2px 0 0;
  min-height: 4px;
  transition: height 0.3s ease;
}

@media (max-width: 768px) {
  .stat-card {
    padding: 20px;
    min-height: 100px;
  }
  
  .stat-card__value {
    font-size: 26px;
  }
  
  .stat-card__icon-wrapper {
    min-width: 34px;
    height: 38px;
  }
  
  .stat-card__icon {
    font-size: 11px;
  }
}
</style>
