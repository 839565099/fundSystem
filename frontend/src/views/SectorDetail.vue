<template>
  <div class="page-container">
    <n-spin :show="loading">
      <div v-if="sector" class="sector-detail">
        <!-- 头部信息 -->
        <div class="detail-header card">
          <div class="header-top">
            <div class="title-area">
              <n-button text @click="router.back()">
                <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
              </n-button>
              <h1>{{ sector.name }}</h1>
              <n-tag :type="getTypeTagType(sector.type)">{{ getTypeLabel(sector.type) }}</n-tag>
            </div>
          </div>
          <div class="code">{{ sector.code }}</div>

          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-label">当前点位</div>
              <div class="stat-value primary">{{ sector.price?.toFixed(2) }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">今日涨跌</div>
              <div class="stat-value" :class="sector.changePercent >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ sector.changePercent >= 0 ? '+' : '' }}{{ sector.changePercent?.toFixed(2) }}%
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">成交额</div>
              <div class="stat-value">{{ sector.volume?.toFixed(2) }}亿</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">换手率</div>
              <div class="stat-value">{{ sector.turnover?.toFixed(2) }}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">上涨家数</div>
              <div class="stat-value growth-positive">{{ sector.upCount }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">下跌家数</div>
              <div class="stat-value growth-negative">{{ sector.downCount }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">领涨股</div>
              <div class="stat-value" style="font-size: 14px">{{ sector.leadingStock || '--' }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">领涨股涨幅</div>
              <div class="stat-value growth-positive">
                {{ sector.leadingStockGrowth >= 0 ? '+' : '' }}{{ sector.leadingStockGrowth?.toFixed(2) }}%
              </div>
            </div>
          </div>
        </div>

        <!-- 走势图 -->
        <div class="chart-section card">
          <div class="section-header">
            <h3 class="section-title">
              <n-icon size="20"><TrendingUpOutline /></n-icon>
              走势图
            </h3>
            <div class="chart-modes">
              <button
                v-for="m in chartModes"
                :key="m.value"
                class="mode-btn"
                :class="{ active: chartMode === m.value }"
                @click="changeChartMode(m.value)"
              >
                {{ m.label }}
              </button>
            </div>
          </div>
          <div ref="chartRef" class="chart-container"></div>
        </div>

        <!-- 成分股 -->
        <div class="stocks-section card">
          <h3 class="section-title">
            <n-icon size="20"><ListOutline /></n-icon>
            成分股 (共{{ stocks.length }}只)
          </h3>
          <n-data-table
            :columns="stockColumns"
            :data="stocks"
            :loading="stocksLoading"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
        </div>
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, h, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NTag, NSpin, NDataTable, createDiscreteApi } from 'naive-ui'
import { ArrowBackOutline, TrendingUpOutline, ListOutline } from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { sectorApi } from '../api/sector'
import type { SectorVO, SectorStockVO, SectorKLineItem } from '../types/sector'
import { useThemeStore } from '../stores/theme'

const { message } = createDiscreteApi(['message'])
const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const stocksLoading = ref(false)
const sector = ref<SectorVO>()
const stocks = ref<SectorStockVO[]>([])
const chartData = ref<SectorKLineItem[]>([])

const chartMode = ref<string>('day')
const chartModes = [
  { label: '日K', value: 'day' },
  { label: '周K', value: 'week' },
  { label: '月K', value: 'month' },
]

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const stockColumns = [
  { title: '股票代码', key: 'code', width: 100 },
  {
    title: '股票名称',
    key: 'name',
    render: (row: SectorStockVO) => h('span', { style: { fontWeight: row.isLeading ? '600' : 'normal' } }, [
      row.name,
      row.isLeading ? h(NTag, { size: 'small', type: 'warning', style: 'margin-left: 4px' }, () => '领涨') : null
    ])
  },
  {
    title: '涨跌幅',
    key: 'changePercent',
    width: 100,
    render: (row: SectorStockVO) => h('span', { class: row.changePercent >= 0 ? 'growth-positive' : 'growth-negative' },
      `${row.changePercent >= 0 ? '+' : ''}${row.changePercent?.toFixed(2)}%`)
  },
  { title: '现价', key: 'price', width: 90, render: (row: SectorStockVO) => row.price?.toFixed(2) || '--' },
  { title: '成交额(亿)', key: 'volume', width: 110, render: (row: SectorStockVO) => row.volume?.toFixed(2) || '--' }
]

const getTypeLabel = (type: string) => ({ industry: '行业', concept: '概念', region: '地域' }[type] || type)
const getTypeTagType = (type: string): 'info' | 'success' | 'warning' => ({ industry: 'info', concept: 'success', region: 'warning' }[type] as any || 'info')

const loadSectorDetail = async () => {
  const code = route.params.code as string
  loading.value = true
  try {
    sector.value = await sectorApi.getDetail(code)
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadHistory = async () => {
  const code = route.params.code as string
  try {
    const data = await sectorApi.getHistory(code, chartMode.value)
    chartData.value = data.history || []
    updateChart()
  } catch (e: any) {
    console.error('加载历史数据失败', e)
  }
}

const loadStocks = async () => {
  const code = route.params.code as string
  stocksLoading.value = true
  try {
    stocks.value = await sectorApi.getStocks(code) || []
  } catch (e: any) {
    console.error('加载成分股失败', e)
  } finally {
    stocksLoading.value = false
  }
}

const changeChartMode = (mode: string) => {
  chartMode.value = mode
  loadHistory()
}

const initChart = () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  resizeHandler = () => chart?.resize()
  window.addEventListener('resize', resizeHandler)
}

const updateChart = () => {
  if (!chart || chartData.value.length === 0) return

  const sortedData = [...chartData.value].sort((a, b) => a.date.localeCompare(b.date))
  const dates = sortedData.map(d => d.date)
  const closes = sortedData.map(d => d.close)

  // 计算涨跌幅
  const firstClose = closes[0] || 1
  const growths = closes.map(c => ((c - firstClose) / firstClose) * 100)
  const lastGrowth = growths[growths.length - 1] || 0
  const lineColor = lastGrowth >= 0 ? getComputedStyle(document.documentElement).getPropertyValue('--up-color').trim() : getComputedStyle(document.documentElement).getPropertyValue('--down-color').trim()

  // 日期格式化
  const formatDate = (date: string) => {
    if (chartMode.value === 'month') return date.slice(0, 7)
    return date.slice(5)
  }

  // 计算Y轴范围（基于涨跌幅）
  const minGrowth = Math.min(...growths)
  const maxGrowth = Math.max(...growths)
  const padding = (maxGrowth - minGrowth) * 0.1 || 1

  // 根据周期设置缩放起始位置
  let zoomStart = 70
  if (chartMode.value === 'week') zoomStart = 50
  if (chartMode.value === 'month') zoomStart = 30

  const tc = {
    upColor: getComputedStyle(document.documentElement).getPropertyValue('--up-color').trim(),
    downColor: getComputedStyle(document.documentElement).getPropertyValue('--down-color').trim(),
    primaryColor: getComputedStyle(document.documentElement).getPropertyValue('--primary-color').trim(),
    textSecondary: getComputedStyle(document.documentElement).getPropertyValue('--text-secondary').trim() || '#64748b',
    textColor: getComputedStyle(document.documentElement).getPropertyValue('--text-color').trim() || '#0f172a',
    textTertiary: getComputedStyle(document.documentElement).getPropertyValue('--text-tertiary').trim() || '#94a3b8',
  }

  const option: any = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color)',
      borderWidth: 1,
      textStyle: { color: tc.textColor, fontSize: 12 },
      formatter: (params: any[]) => {
        const idx = params[0].dataIndex
        const item = sortedData[idx]
        const growth = growths[idx]
        return `
          <div style="padding: 10px; min-width: 150px;">
            <div style="font-weight: 600; margin-bottom: 8px; color: ${tc.textColor};">${item.date}</div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
              <span style="color: ${tc.textSecondary};">收盘点位</span>
              <span style="color: ${tc.primaryColor}; font-weight: 600;">${item.close?.toFixed(2)}</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
              <span style="color: ${tc.textSecondary};">区间涨跌</span>
              <span style="color: ${item.changePercent >= 0 ? tc.upColor : tc.downColor}; font-weight: 600;">${item.changePercent >= 0 ? '+' : ''}${item.changePercent?.toFixed(2)}%</span>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
              <span style="color: ${tc.textSecondary};">累计涨跌</span>
              <span style="color: ${growth >= 0 ? tc.upColor : tc.downColor}; font-weight: 600;">${growth >= 0 ? '+' : ''}${growth?.toFixed(2)}%</span>
            </div>
            <div style="display: flex; justify-content: space-between;">
              <span style="color: ${tc.textSecondary};">成交额</span>
              <span style="color: ${tc.textColor};">${item.volume?.toFixed(2)}亿</span>
            </div>
          </div>
        `
      }
    },
    grid: { left: 60, right: 20, top: 40, bottom: 60 },
    dataZoom: [
      {
        type: 'slider',
        bottom: 10,
        height: 22,
        start: zoomStart,
        end: 100,
        borderColor: 'transparent',
        backgroundColor: 'rgba(0,0,0,0.02)',
        fillerColor: `${tc.primaryColor}26`,
        handleStyle: { color: tc.primaryColor },
        moveHandleStyle: { color: tc.primaryColor, opacity: 0.3 },
        selectedDataBackground: { lineStyle: { color: tc.primaryColor }, areaStyle: { color: tc.primaryColor } },
        textStyle: { color: tc.textSecondary, fontSize: 10 },
        labelFormatter: (value: number) => formatDate(dates[value] || '')
      }
    ],
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: tc.textSecondary, fontSize: 11, interval: 'auto' as const, formatter: formatDate }
    },
    yAxis: {
      type: 'value',
      name: '累计涨跌幅(%)',
      nameTextStyle: { color: tc.textSecondary, fontSize: 11, align: 'right' },
      min: (value: { min: number }) => Math.floor(value.min - padding),
      max: (value: { max: number }) => Math.ceil(value.max + padding),
      splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f1f5f9', type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: tc.textSecondary,
        fontSize: 11,
        formatter: (v: number) => `${v >= 0 ? '+' : ''}${v.toFixed(1)}%`
      }
    },
    series: [
      {
        type: 'line',
        data: growths,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2.5, color: lineColor },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${lineColor}30` },
            { offset: 0.5, color: `${lineColor}10` },
            { offset: 1, color: `${lineColor}02` }
          ])
        },
        markLine: {
          silent: true,
          symbol: 'none',
          data: [{ yAxis: 0, lineStyle: { color: tc.textTertiary, type: 'dashed', width: 1 } }]
        }
      }
    ],
    animationDuration: 800,
    animationEasing: 'cubicOut' as const
  }

  chart.setOption(option, true)
}

onMounted(async () => {
  await loadSectorDetail()
  await Promise.all([loadHistory(), loadStocks()])
  initChart()
  updateChart()
})

onUnmounted(() => {
  chart?.dispose()
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>
.sector-detail { max-width: 1200px; margin: 0 auto; }
.detail-header { padding: 24px; margin-bottom: 20px; }
.header-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.title-area { display: flex; align-items: center; gap: 12px; }
.title-area h1 { font-size: 24px; font-weight: 700; margin: 0; }
.code { color: var(--text-secondary); margin-bottom: 20px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 800px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-card { text-align: center; padding: 16px; background: var(--bg-color); border-radius: var(--radius-lg); }
.stat-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; }
.stat-value { font-size: 22px; font-weight: 700; }
.stat-value.primary { color: var(--primary-color); }
.chart-section { padding: 20px; margin-bottom: 20px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title { font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 8px; margin: 0; }
.chart-modes { display: flex; background: var(--bg-color); border-radius: var(--radius-md); padding: 3px; gap: 1px; }
.mode-btn { padding: 6px 16px; font-size: 13px; border: none; background: transparent; color: var(--text-secondary); cursor: pointer; transition: all 0.2s; border-radius: var(--radius-sm); }
.mode-btn:hover { background: rgba(0,0,0,0.04); }
.mode-btn.active { background: var(--primary-color); color: white; font-weight: 600; box-shadow: 0 2px 6px rgba(79, 70, 229, 0.15); }
.chart-container { height: 380px; width: 100%; }
.stocks-section { padding: 20px; }
.growth-positive { color: var(--up-color); }
.growth-negative { color: var(--down-color); }
</style>
