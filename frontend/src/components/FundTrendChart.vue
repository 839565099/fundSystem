<template>
  <div class="fund-trend-chart card card--elevated">
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

      <template v-if="period !== 'intraday'">
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
                <IconTrendingUp v-if="t.value === 'line'" />
                <IconCoin v-else-if="t.value === 'area'" />
                <IconChartBar v-else />
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
              <n-icon size="14" style="margin-right: 4px;"><IconCoin /></n-icon>
              净值
            </button>
            <button
              class="segment-btn"
              :class="{ active: displayMode === 'growth' }"
              @click="displayMode = 'growth'"
            >
              <n-icon size="14" style="margin-right: 4px;"><IconTrendingUp /></n-icon>
              涨跌幅
            </button>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="toolbar-divider"></div>
        <div class="toolbar-group" v-if="intradayData">
          <span class="intraday-badge" :class="intradayData.isEtf ? 'badge-etf' : 'badge-estimate'">
            {{ intradayData.isEtf ? '实时行情' : '估值走势' }}
          </span>
        </div>
      </template>

      <div class="toolbar-actions">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary size="small" @click="toggleFullscreen">
              <template #icon>
                <n-icon><IconArrowsMaximize /></n-icon>
              </template>
            </n-button>
          </template>
          全屏
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary size="small" @click="downloadChart">
              <template #icon>
                <n-icon><IconDownload /></n-icon>
              </template>
            </n-button>
          </template>
          下载图表
        </n-tooltip>
      </div>
    </div>

    <div ref="chartRef" class="chart-container" :class="{ 'chart-container--fullscreen': isFullscreen }"></div>

    <!-- 分时图统计 -->
    <div class="chart-stats" v-if="period === 'intraday' && intradayStats">
      <div class="stat-item card-stagger">
        <span class="stat-label">昨收净值</span>
        <span class="stat-value">{{ intradayStats.prevNav?.toFixed(4) }}</span>
      </div>
      <div class="stat-item card-stagger">
        <span class="stat-label">当前估值</span>
        <span class="stat-value">{{ intradayStats.currentPrice?.toFixed(4) }}</span>
      </div>
      <div class="stat-item card-stagger">
        <span class="stat-label">估算涨跌</span>
        <span class="stat-value" :class="intradayStats.changeRatio >= 0 ? 'growth-positive' : 'growth-negative'">
          {{ intradayStats.changeRatio >= 0 ? '+' : '' }}{{ intradayStats.changeRatio?.toFixed(2) }}%
        </span>
      </div>
      <div class="stat-item card-stagger">
        <span class="stat-label">最高</span>
        <span class="stat-value">{{ intradayStats.maxPrice?.toFixed(4) }}</span>
      </div>
      <div class="stat-item card-stagger">
        <span class="stat-label">最低</span>
        <span class="stat-value">{{ intradayStats.minPrice?.toFixed(4) }}</span>
      </div>
    </div>

    <!-- 常规统计 -->
    <div class="chart-stats" v-else-if="stats">
      <div class="stat-item card-stagger">
        <span class="stat-label">期间涨跌</span>
        <span class="stat-value" :class="stats.periodGrowth >= 0 ? 'growth-positive' : 'growth-negative'">
          {{ stats.periodGrowth >= 0 ? '+' : '' }}{{ stats.periodGrowth?.toFixed(2) }}%
        </span>
      </div>
      <div class="stat-item card-stagger" v-if="displayMode === 'nav'">
        <span class="stat-label">最高净值</span>
        <span class="stat-value">{{ stats.maxNav?.toFixed(4) }}</span>
      </div>
      <div class="stat-item card-stagger" v-if="displayMode === 'nav'">
        <span class="stat-label">最低净值</span>
        <span class="stat-value">{{ stats.minNav?.toFixed(4) }}</span>
      </div>
      <div class="stat-item card-stagger" v-if="displayMode === 'growth'">
        <span class="stat-label">最大涨幅</span>
        <span class="stat-value growth-positive">{{ stats.maxGrowth >= 0 ? '+' : '' }}{{ stats.maxGrowth?.toFixed(2) }}%</span>
      </div>
      <div class="stat-item card-stagger" v-if="displayMode === 'growth'">
        <span class="stat-label">最大跌幅</span>
        <span class="stat-value growth-negative">{{ stats.minGrowth?.toFixed(2) }}%</span>
      </div>
      <div class="stat-item card-stagger">
        <span class="stat-label">上涨天数</span>
        <span class="stat-value growth-positive">{{ stats.upDays }}天</span>
      </div>
      <div class="stat-item card-stagger">
        <span class="stat-label">下跌天数</span>
        <span class="stat-value growth-negative">{{ stats.downDays }}天</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { NIcon, NButton, NTooltip, useMessage } from 'naive-ui'
import { IconTrendingUp, IconCoin, IconChartBar, IconArrowsMaximize, IconDownload } from '@tabler/icons-vue'
import * as echarts from 'echarts'
import type { FundNavHistoryVO } from '../types'
import { useThemeStore } from '../stores/theme'

interface IntradayTrendPoint {
  time: string
  price: number
  changeRatio: number
}

interface IntradayData {
  fundCode: string
  prevNav: number
  isEtf: boolean
  trends: IntradayTrendPoint[]
}

const props = defineProps<{
  data: FundNavHistoryVO[]
  fundName?: string
  intradayData?: IntradayData | null
}>()

const themeStore = useThemeStore()
const message = useMessage()
const isDark = computed(() => themeStore.theme === 'dark')

const getChartColors = () => {
  const style = getComputedStyle(document.documentElement)
  return {
    splitLine: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#e4e7ed',
    text: isDark.value ? 'rgba(255, 255, 255, 0.6)' : '#909399',
    upColor: style.getPropertyValue('--up-color').trim() || '#ef4444',
    downColor: style.getPropertyValue('--down-color').trim() || '#22c55e',
    primaryColor: style.getPropertyValue('--primary-color').trim() || '#3b82f6',
    cardBg: style.getPropertyValue('--card-bg').trim() || '#ffffff',
    goldColor: style.getPropertyValue('--primary-color').trim() || '#C9A96E',
  }
}

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const period = ref('month')
const chartType = ref<'line' | 'area' | 'column'>('area')
const displayMode = ref<'nav' | 'growth'>('nav')
const isFullscreen = ref(false)

const periods = [
  { label: '分时', value: 'intraday' },
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

const intradayStats = computed(() => {
  if (!props.intradayData || !props.intradayData.trends || props.intradayData.trends.length === 0) return null
  const trends = props.intradayData.trends
  const prices = trends.map(t => t.price)
  const last = trends[trends.length - 1]
  return {
    prevNav: props.intradayData.prevNav,
    currentPrice: last?.price,
    changeRatio: last?.changeRatio,
    maxPrice: Math.max(...prices),
    minPrice: Math.min(...prices),
  }
})

const changePeriod = (p: string) => {
  period.value = p
  emit('periodChange', p)
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  setTimeout(() => chart?.resize(), 100)
}

const downloadChart = () => {
  if (!chart) return

  const url = chart.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: isDark.value ? '#1e293b' : '#fff'
  })

  const link = document.createElement('a')
  link.href = url
  link.download = `${props.fundName || '基金'}_走势图_${new Date().toLocaleDateString()}.png`
  link.click()

  message.success('图表已下载')
}

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value, isDark.value ? 'dark' : undefined)
  updateChart()

  resizeObserver = new ResizeObserver(() => {
    chart?.resize()
  })
  resizeObserver.observe(chartRef.value)
}

const updateChart = () => {
  if (!chart) return

  if (period.value === 'intraday') {
    updateIntradayChart()
  } else {
    updateRegularChart()
  }
}

const updateIntradayChart = () => {
  if (!chart) return
  const colors = getChartColors()
  const trends = props.intradayData?.trends || []
  const prevNav = props.intradayData?.prevNav || 0

  if (trends.length === 0) {
    chart.setOption({
      title: {
        text: '暂无分时数据',
        left: 'center',
        top: 'center',
        textStyle: { color: colors.text, fontSize: 14 }
      }
    }, true)
    return
  }

  const times = trends.map(t => t.time)
  const prices = trends.map(t => t.price)
  const changeRatios = trends.map(t => t.changeRatio)

  const lastChange = changeRatios[changeRatios.length - 1] || 0
  const lineColor = lastChange >= 0 ? colors.upColor : colors.downColor

  const option: any = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: isDark.value ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
      borderColor: isDark.value ? 'rgba(255, 255, 255, 0.1)' : '#e5e7eb',
      borderWidth: 1,
      borderRadius: 12,
      padding: [12, 16],
      textStyle: {
        color: isDark.value ? '#f8fafc' : '#1f2937',
        fontSize: 13
      },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);',
      formatter: (params: any) => {
        if (!params || !params[0]) return ''
        const idx = params[0].dataIndex
        const point = trends[idx]
        if (!point) return ''
        return `
          <div style="padding: 4px;">
            <div style="font-weight: 600; margin-bottom: 10px; font-size: 14px;">${point.time}</div>
            <div style="margin-bottom: 6px;">估值: <span style="color: ${colors.goldColor}; font-weight: 600;">${point.price?.toFixed(4)}</span></div>
            <div>涨跌幅: <span style="color: ${point.changeRatio >= 0 ? colors.upColor : colors.downColor}; font-weight: 600;">${point.changeRatio >= 0 ? '+' : ''}${point.changeRatio?.toFixed(2)}%</span></div>
          </div>
        `
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: times,
      boundaryGap: false,
      axisLine: {
        show: true,
        lineStyle: { color: colors.splitLine }
      },
      axisTick: { show: false },
      axisLabel: {
        color: colors.text,
        fontSize: 11,
        interval: (index: number) => {
          // 大约每30分钟显示一个标签
          const step = Math.max(1, Math.floor(times.length / 8))
          return index === 0 || index === times.length - 1 || index % step === 0
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '涨跌幅(%)',
      nameTextStyle: {
        color: colors.text,
        fontSize: 12,
        padding: [0, 0, 0, 50],
      },
      splitLine: {
        lineStyle: {
          color: colors.splitLine,
          type: 'dashed'
        },
      },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: colors.text,
        fontSize: 11,
        formatter: (value: number) => `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`,
      },
    },
    series: [
      {
        type: 'line',
        data: changeRatios,
        smooth: false,
        symbol: 'none',
        lineStyle: {
          width: 2,
          color: lineColor,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${lineColor}25` },
            { offset: 1, color: `${lineColor}02` },
          ]),
        },
        markLine: {
          silent: true,
          symbol: 'none',
          label: { show: false },
          lineStyle: {
            color: colors.text,
            type: 'dashed',
            width: 1,
          },
          data: [{ yAxis: 0 }],
        },
      },
    ],
    animationDuration: 600,
    animationEasing: 'cubicOut' as const,
  }

  chart.setOption(option, true)
}

const updateRegularChart = () => {
  if (!chart || !props.data || props.data.length === 0) return

  const colors = getChartColors()
  const dates = props.data.map(d => d.navDate)
  const navs = props.data.map(d => d.nav)
  const growths = props.data.map(d => d.dayGrowth || 0)

  const firstNav = props.data[0]?.nav || 1
  const cumulativeGrowths = props.data.map((d) => {
    const currentNav = d.nav
    return firstNav > 0 ? ((currentNav - firstNav) / firstNav) * 100 : 0
  })

  let series: any[]

  if (chartType.value === 'column') {
    series = [{
      type: 'bar',
      data: growths,
      itemStyle: {
        color: (params: any) => params.value >= 0 ? colors.upColor : colors.downColor,
        borderRadius: [4, 4, 0, 0],
      },
      barWidth: '60%',
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      }
    }]
  } else {
    const dataToUse = displayMode.value === 'growth' ? cumulativeGrowths : navs
    const isGrowthMode = displayMode.value === 'growth'
    const lastValue = dataToUse[dataToUse.length - 1]
    const lineColor = isGrowthMode
      ? (lastValue >= 0 ? colors.upColor : colors.downColor)
      : colors.primaryColor

    series = [{
      type: 'line',
      data: dataToUse,
      smooth: 0.6,
      symbol: 'none',
      lineStyle: {
        width: 2.5,
        color: lineColor,
        shadowColor: 'rgba(0, 0, 0, 0.1)',
        shadowBlur: 8,
        shadowOffsetY: 4
      },
      areaStyle: chartType.value === 'area' ? {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: `${lineColor}30` },
          { offset: 0.5, color: `${lineColor}10` },
          { offset: 1, color: `${lineColor}00` },
        ]),
      } : undefined,
      emphasis: {
        focus: 'series',
        lineStyle: {
          width: 3.5
        }
      }
    }]
  }

  const isGrowthMode = displayMode.value === 'growth'

  const option: any = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: isDark.value ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
      borderColor: isDark.value ? 'rgba(255, 255, 255, 0.1)' : '#e5e7eb',
      borderWidth: 1,
      borderRadius: 12,
      padding: [12, 16],
      textStyle: {
        color: isDark.value ? '#f8fafc' : '#1f2937',
        fontSize: 13
      },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);',
      formatter: (params: any) => {
        if (!params || !params[0]) return ''
        const data = params[0]
        const idx = data.dataIndex
        const item = props.data[idx]
        if (!item) return ''
        if (isGrowthMode && chartType.value !== 'column') {
          const cumGrowth = cumulativeGrowths[idx]
          return `
            <div style="padding: 4px;">
              <div style="font-weight: 600; margin-bottom: 10px; font-size: 14px;">${item.navDate}</div>
              <div style="margin-bottom: 6px;">累计涨跌: <span style="color: ${cumGrowth >= 0 ? colors.upColor : colors.downColor}; font-weight: 600;">${cumGrowth >= 0 ? '+' : ''}${cumGrowth.toFixed(2)}%</span></div>
              ${item.dayGrowth !== undefined ? `<div style="margin-bottom: 6px;">日涨跌: <span style="color: ${item.dayGrowth >= 0 ? colors.upColor : colors.downColor}; font-weight: 500;">${item.dayGrowth >= 0 ? '+' : ''}${item.dayGrowth.toFixed(2)}%</span></div>` : ''}
              <div>净值: <span style="color: ${colors.primaryColor}; font-weight: 500;">${item.nav?.toFixed(4)}</span></div>
            </div>
          `
        }
        return `
          <div style="padding: 4px;">
            <div style="font-weight: 600; margin-bottom: 10px; font-size: 14px;">${item.navDate}</div>
            <div style="margin-bottom: 6px;">净值: <span style="color: ${colors.primaryColor}; font-weight: 600;">${item.nav?.toFixed(4)}</span></div>
            ${item.dayGrowth !== undefined ? `<div>日涨跌: <span style="color: ${item.dayGrowth >= 0 ? colors.upColor : colors.downColor}; font-weight: 500;">${item.dayGrowth >= 0 ? '+' : ''}${item.dayGrowth.toFixed(2)}%</span></div>` : ''}
          </div>
        `
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: chartType.value === 'column',
      axisLine: {
        show: true,
        lineStyle: { color: colors.splitLine }
      },
      axisTick: { show: false },
      axisLabel: {
        color: colors.text,
        fontSize: 11,
        formatter: (value: string) => value.slice(5),
      },
    },
    yAxis: {
      type: 'value',
      scale: !isGrowthMode,
      name: isGrowthMode ? '涨跌幅(%)' : '净值',
      nameTextStyle: {
        color: colors.text,
        fontSize: 12,
        padding: [0, 0, 0, 40],
      },
      splitLine: {
        lineStyle: {
          color: colors.splitLine,
          type: 'dashed'
        },
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
    dataZoom: props.data.length > 50 ? [
      {
        type: 'inside',
        start: 70,
        end: 100,
        zoomOnMouseWheel: true,
        moveOnMouseMove: true,
      },
      {
        type: 'slider',
        start: 70,
        end: 100,
        height: 20,
        bottom: 10,
        borderColor: 'transparent',
        backgroundColor: isDark.value ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.02)',
        fillerColor: isDark.value ? 'rgba(59, 130, 246, 0.2)' : 'rgba(59, 130, 246, 0.15)',
        handleStyle: {
          color: colors.primaryColor,
          borderColor: colors.primaryColor,
        },
        textStyle: {
          color: colors.text,
          fontSize: 10
        }
      }
    ] : undefined,
    series,
    animationDuration: 800,
    animationEasing: 'cubicOut' as const,
  }

  chart.setOption(option, true)
}

watch(() => props.data, () => { if (period.value !== 'intraday') updateChart() }, { deep: true })
watch(() => props.intradayData, () => { if (period.value === 'intraday') updateChart() }, { deep: true })
watch(chartType, () => { if (period.value !== 'intraday') updateChart() })
watch(displayMode, () => { if (period.value !== 'intraday') updateChart() })
watch(isDark, () => {
  chart?.dispose()
  initChart()
})

onMounted(initChart)

onUnmounted(() => {
  chart?.dispose()
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})
</script>

<style scoped>
.fund-trend-chart {
  padding: 24px;
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
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 3px;
  gap: 2px;
}

.segment-btn {
  padding: 7px 14px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  font-weight: 500;
}

.segment-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.segment-btn.active {
  background: var(--primary-color);
  color: white;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

.toolbar-divider {
  width: 1px;
  height: 28px;
  background: var(--border-color);
  margin: 0 8px;
}

.toolbar-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.intraday-badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  letter-spacing: 0.04em;
}

.badge-etf {
  background: rgba(201, 169, 110, 0.15);
  color: var(--primary-color);
  border: 1px solid rgba(201, 169, 110, 0.3);
}

.badge-estimate {
  background: rgba(59, 130, 246, 0.1);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.chart-container {
  height: 380px;
  width: 100%;
  transition: height var(--transition-base);
}

.chart-container--fullscreen {
  height: 70vh;
}

.chart-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
  flex-wrap: wrap;
  gap: 16px;
}

.stat-item {
  text-align: center;
  min-width: 80px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.growth-positive {
  color: var(--up-color);
}

.growth-negative {
  color: var(--down-color);
}

@media (max-width: 768px) {
  .fund-trend-chart {
    padding: 16px;
  }

  .chart-toolbar {
    gap: 8px;
  }

  .toolbar-divider {
    display: none;
  }

  .chart-container {
    height: 300px;
  }

  .stat-value {
    font-size: 16px;
  }
}
</style>
