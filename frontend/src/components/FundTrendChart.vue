<template>
  <div class="fund-trend-chart">
    <div class="chart-toolbar">
      <div class="toolbar-group">
        <span class="group-label">周期</span>
        <div class="btn-segment">
          <button 
            v-for="p in periods" 
            :key="p.value"
            class="segment-btn"
            :class="{ active: period === p.value }"
            @click="changePeriod(p.value)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>
      
      <div class="toolbar-divider"></div>
      
      <div class="toolbar-group">
        <span class="group-label">图表</span>
        <div class="btn-segment">
          <button 
            v-for="t in chartTypes" 
            :key="t.value"
            class="segment-btn"
            :class="{ active: chartType === t.value }"
            @click="chartType = t.value as 'line' | 'area' | 'column'"
          >
            <n-icon size="14" style="margin-right: 4px;">
              <TrendingUpOutline v-if="t.value === 'line'" />
              <CashOutline v-else-if="t.value === 'area'" />
              <BarChartOutline v-else />
            </n-icon>
            {{ t.label }}
          </button>
        </div>
      </div>
      
      <div class="toolbar-divider"></div>
      
      <div class="toolbar-group">
        <span class="group-label">显示</span>
        <div class="btn-segment">
          <button 
            class="segment-btn"
            :class="{ active: displayMode === 'nav' }"
            @click="displayMode = 'nav'"
          >
            <n-icon size="14" style="margin-right: 4px;"><CashOutline /></n-icon>
            净值
          </button>
          <button 
            class="segment-btn"
            :class="{ active: displayMode === 'growth' }"
            @click="displayMode = 'growth'"
          >
            <n-icon size="14" style="margin-right: 4px;"><TrendingUpOutline /></n-icon>
            涨跌幅
          </button>
        </div>
      </div>
    </div>
    
    <div ref="chartRef" class="chart-container"></div>
    
    <div class="chart-stats" v-if="stats">
      <div class="stat-item">
        <span class="stat-label">期间涨跌</span>
        <span class="stat-value" :class="stats.periodGrowth >= 0 ? 'growth-positive' : 'growth-negative'">
          {{ stats.periodGrowth >= 0 ? '+' : '' }}{{ stats.periodGrowth?.toFixed(2) }}%
        </span>
      </div>
      <div class="stat-item" v-if="displayMode === 'nav'">
        <span class="stat-label">最高净值</span>
        <span class="stat-value">{{ stats.maxNav?.toFixed(4) }}</span>
      </div>
      <div class="stat-item" v-if="displayMode === 'nav'">
        <span class="stat-label">最低净值</span>
        <span class="stat-value">{{ stats.minNav?.toFixed(4) }}</span>
      </div>
      <div class="stat-item" v-if="displayMode === 'growth'">
        <span class="stat-label">最大涨幅</span>
        <span class="stat-value growth-positive">{{ stats.maxGrowth >= 0 ? '+' : '' }}{{ stats.maxGrowth?.toFixed(2) }}%</span>
      </div>
      <div class="stat-item" v-if="displayMode === 'growth'">
        <span class="stat-label">最大跌幅</span>
        <span class="stat-value growth-negative">{{ stats.minGrowth?.toFixed(2) }}%</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">上涨天数</span>
        <span class="stat-value growth-positive">{{ stats.upDays }}天</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">下跌天数</span>
        <span class="stat-value growth-negative">{{ stats.downDays }}天</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { NIcon } from 'naive-ui'
import { TrendingUpOutline, CashOutline, BarChartOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import type { FundNavHistoryVO } from '../types'
import { useThemeStore } from '../stores/theme'

const props = defineProps<{
  data: FundNavHistoryVO[]
  fundName?: string
}>()

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

// 根据主题获取颜色
const getChartColors = () => ({
  splitLine: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6',
  text: isDark.value ? 'rgba(255, 255, 255, 0.6)' : '#6b7280',
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const period = ref('month')
const chartType = ref<'line' | 'area' | 'column'>('area')
const displayMode = ref<'nav' | 'growth'>('nav')

const periods = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '三月', value: 'threeMonth' },
  { label: '六月', value: 'sixMonth' },
  { label: '年', value: 'year' },
  { label: '三年', value: 'threeYear' },
]

const chartTypes = [
  { label: '折线', value: 'line' },
  { label: '面积', value: 'area' },
  { label: '柱状', value: 'column' },
]

const emit = defineEmits<{
  (e: 'periodChange', period: string): void
}>()

const stats = computed(() => {
  if (!props.data || props.data.length === 0) return null
  
  const navs = props.data.map(d => d.nav)
  const maxNav = Math.max(...navs)
  const minNav = Math.min(...navs)
  const firstNav = props.data[0]?.nav || 0
  const lastNav = props.data[props.data.length - 1]?.nav || 0
  const periodGrowth = firstNav > 0 ? ((lastNav - firstNav) / firstNav) * 100 : 0
  
  const growths = props.data.map(d => d.dayGrowth || 0).filter(g => g !== 0)
  const maxGrowth = growths.length > 0 ? Math.max(...growths) : 0
  const minGrowth = growths.length > 0 ? Math.min(...growths) : 0
  
  let upDays = 0
  let downDays = 0
  props.data.forEach(d => {
    if (d.dayGrowth !== undefined && d.dayGrowth !== null) {
      if (d.dayGrowth > 0) upDays++
      else if (d.dayGrowth < 0) downDays++
    }
  })
  
  return { maxNav, minNav, periodGrowth, upDays, downDays, maxGrowth, minGrowth }
})

const changePeriod = (p: string) => {
  period.value = p
  emit('periodChange', p)
}

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()

  // 保存函数引用以便正确移除
  resizeHandler = () => chart?.resize()
  window.addEventListener('resize', resizeHandler)
}

const updateChart = () => {
  if (!chart || !props.data || props.data.length === 0) return

  const colors = getChartColors()
  const dates = props.data.map(d => d.navDate)
  const navs = props.data.map(d => d.nav)
  const growths = props.data.map(d => d.dayGrowth || 0)
  
  const firstNav = props.data[0]?.nav || 1
  const cumulativeGrowths = props.data.map((d, index) => {
    if (index === 0) return 0
    const currentNav = d.nav
    return firstNav > 0 ? ((currentNav - firstNav) / firstNav) * 100 : 0
  })
  
  let series: any[]
  
  if (chartType.value === 'column') {
    series = [{
      type: 'bar',
      data: growths,
      itemStyle: {
        color: (params: any) => params.value >= 0 ? '#ef4444' : '#22c55e',
        borderRadius: [4, 4, 0, 0],
      },
      barWidth: '60%',
    }]
  } else {
    const dataToUse = displayMode.value === 'growth' ? cumulativeGrowths : navs
    const isGrowthMode = displayMode.value === 'growth'
    const lastValue = dataToUse[dataToUse.length - 1]
    const lineColor = isGrowthMode 
      ? (lastValue >= 0 ? '#ef4444' : '#22c55e')
      : '#3b82f6'
    
    series = [{
      type: 'line',
      data: dataToUse,
      smooth: true,
      symbol: 'none',
      lineStyle: {
        width: 2,
        color: lineColor,
      },
      areaStyle: chartType.value === 'area' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: `${lineColor}40` },
          { offset: 1, color: `${lineColor}05` },
        ]),
      } : undefined,
    }]
  }
  
  const isGrowthMode = displayMode.value === 'growth'
  
  const option: any = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#1f2937' },
      formatter: (params: any) => {
        if (!params || !params[0]) return ''
        const data = params[0]
        const idx = data.dataIndex
        const item = props.data[idx]
        if (!item) return ''
        if (isGrowthMode && chartType.value !== 'column') {
          const cumGrowth = cumulativeGrowths[idx]
          return `
            <div style="padding: 8px;">
              <div style="font-weight: 600; margin-bottom: 8px;">${item.navDate}</div>
              <div>累计涨跌: <span style="color: ${cumGrowth >= 0 ? '#ef4444' : '#22c55e'}; font-weight: 600;">${cumGrowth >= 0 ? '+' : ''}${cumGrowth.toFixed(2)}%</span></div>
              ${item.dayGrowth !== undefined ? `<div>日涨跌: <span style="color: ${item.dayGrowth >= 0 ? '#ef4444' : '#22c55e'};">${item.dayGrowth >= 0 ? '+' : ''}${item.dayGrowth.toFixed(2)}%</span></div>` : ''}
              <div>净值: <span style="color: #3b82f6;">${item.nav?.toFixed(4)}</span></div>
            </div>
          `
        }
        return `
          <div style="padding: 8px;">
            <div style="font-weight: 600; margin-bottom: 8px;">${item.navDate}</div>
            <div>净值: <span style="color: #3b82f6; font-weight: 600;">${item.nav?.toFixed(4)}</span></div>
            ${item.dayGrowth !== undefined ? `<div>日涨跌: <span style="color: ${item.dayGrowth >= 0 ? '#ef4444' : '#22c55e'};">${item.dayGrowth >= 0 ? '+' : ''}${item.dayGrowth.toFixed(2)}%</span></div>` : ''}
          </div>
        `
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: chartType.value === 'column' ? dates : dates,
      boundaryGap: chartType.value === 'column',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: colors.text,
        fontSize: 11,
        formatter: (value: string) => value.slice(5),
      },
    },
    yAxis: {
      type: 'value',
      name: isGrowthMode ? '涨跌幅(%)' : '净值',
      nameTextStyle: {
        color: colors.text,
        fontSize: 12,
        padding: [0, 0, 0, 40],
      },
      splitLine: {
        lineStyle: { color: colors.splitLine, type: 'dashed' },
      },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: colors.text,
        fontSize: 11,
        formatter: isGrowthMode 
          ? (value: number) => `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
          : (value: number) => value.toFixed(4),
      },
    },
    series,
    animationDuration: 1000,
    animationEasing: 'cubicOut' as const,
  }
  
  chart.setOption(option)
}

watch(() => props.data, updateChart, { deep: true })
watch(chartType, updateChart)
watch(displayMode, updateChart)
watch(isDark, updateChart)  // 主题变化时更新图表

onMounted(initChart)

onUnmounted(() => {
  chart?.dispose()
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})
</script>

<style scoped>
.fund-trend-chart {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--shadow);
}

.chart-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
  min-width: 36px;
}

.btn-segment {
  display: flex;
  background: var(--bg-color);
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.segment-btn {
  padding: 6px 12px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 6px;
  display: flex;
  align-items: center;
}

.segment-btn:hover {
  background: var(--bg-color);
}

.segment-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
  margin: 0 8px;
}

.chart-container {
  height: 350px;
  width: 100%;
}

.chart-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
}

.growth-positive {
  color: #ef4444;
}

.growth-negative {
  color: #22c55e;
}
</style>
