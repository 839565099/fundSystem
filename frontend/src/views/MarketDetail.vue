<template>
  <div class="page-container">
    <div class="market-header card">
      <n-spin :show="loading">
        <div v-if="marketData" class="market-info">
          <div class="market-title">
            <h1>{{ getMarketName(marketData.marketCode) }}</h1>
            <n-tag type="info" size="small">{{ marketData.marketCode }}</n-tag>
          </div>
          
          <div class="market-stats">
            <div class="stat-card">
              <div class="stat-label">当前点位</div>
              <div class="stat-value primary">{{ marketData.currentPoint?.toFixed(2) }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">今日涨跌</div>
              <div class="stat-value" :class="marketData.changeRatio! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ marketData.changeRatio! >= 0 ? '+' : '' }}{{ marketData.changeRatio?.toFixed(2) }}%
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">涨跌点数</div>
              <div class="stat-value" :class="marketData.changePoint! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ marketData.changePoint! >= 0 ? '+' : '' }}{{ marketData.changePoint?.toFixed(2) }}
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">成交量</div>
              <div class="stat-value">{{ formatVolume(marketData.volume) }}</div>
            </div>
          </div>
          
          <div class="market-detail-info">
            <div class="detail-item">
              <span class="label">开盘</span>
              <span class="value">{{ marketData.openPoint?.toFixed(2) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">最高</span>
              <span class="value growth-positive">{{ marketData.highPoint?.toFixed(2) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">最低</span>
              <span class="value growth-negative">{{ marketData.lowPoint?.toFixed(2) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">昨收</span>
              <span class="value">{{ marketData.prevClose?.toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </n-spin>
    </div>

    <div class="chart-section card">
      <div class="chart-header">
        <h2 class="section-title">
          <n-icon size="24"><TrendingUpOutline /></n-icon>
          走势图
        </h2>
        <div class="chart-controls">
          <div class="btn-segment">
            <button
              v-for="m in chartModes"
              :key="m.value"
              class="segment-btn"
              :class="{ active: chartMode === m.value }"
              @click="changeChartMode(m.value)"
            >
              {{ m.label }}
            </button>
          </div>
        </div>
      </div>
      <div ref="mainChartRef" class="main-chart"></div>
    </div>

    <div class="indicator-section card" v-if="chartMode !== 'minute'">
      <h3 class="section-title">
        <n-icon size="20"><AnalyticsOutline /></n-icon>
        技术指标
      </h3>
      <n-tabs type="line" animated @update:value="handleIndicatorTabChange">
        <n-tab-pane name="macd" tab="MACD（异同移动平均线）">
          <div ref="macdChartRef" class="indicator-chart"></div>
        </n-tab-pane>
        <n-tab-pane name="rsi" tab="RSI（相对强弱指标）">
          <div ref="rsiChartRef" class="indicator-chart"></div>
        </n-tab-pane>
        <n-tab-pane name="kdj" tab="KDJ（随机指标）">
          <div ref="kdjChartRef" class="indicator-chart"></div>
        </n-tab-pane>
      </n-tabs>
    </div>

    <div class="history-section card">
      <h3 class="section-title">
        <n-icon size="20"><ListOutline /></n-icon>
        历史数据
      </h3>
      <n-data-table
        :columns="columns"
        :data="historyData"
        :loading="historyLoading"
        :pagination="pagination"
        size="small"
        max-height="400"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, h, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NTag, NSpin, NTabs, NTabPane, NDataTable, createDiscreteApi } from 'naive-ui'
import { TrendingUpOutline, AnalyticsOutline, ListOutline } from '@vicons/ionicons5'
import { marketApi } from '../api/fund'
import type { MarketDataVO } from '../types'
import * as echarts from 'echarts'
import { useThemeStore } from '../stores/theme'

const getCssColor = (varName: string, fallback: string) => {
  return getComputedStyle(document.documentElement).getPropertyValue(varName).trim() || fallback
}

const getThemeColors = () => ({
  upColor: getCssColor('--up-color', '#dc2626'),
  downColor: getCssColor('--down-color', '#16a34a'),
  primaryColor: getCssColor('--primary-color', '#4f46e5'),
  warningColor: getCssColor('--warning-color', '#d97706'),
  textSecondary: getCssColor('--text-secondary', '#64748b'),
  textTertiary: getCssColor('--text-tertiary', '#94a3b8'),
  textColor: getCssColor('--text-color', '#0f172a'),
  borderColor: getCssColor('--border-color', '#e2e8f0'),
  bgSecondary: getCssColor('--bg-secondary', '#f1f5f9'),
  purpleColor: '#8b5cf6',
})

const { message } = createDiscreteApi(['message'])
const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

const route = useRoute()
const router = useRouter()

const marketCode = ref('')
const marketData = ref<MarketDataVO>()
const loading = ref(false)
const historyLoading = ref(false)
const historyData = ref<any[]>([])

const chartMode = ref<string>('day')

const chartModes = [
  { label: '分时', value: 'minute' },
  { label: '日K', value: 'day' },
  { label: '周K', value: 'week' },
  { label: '月K', value: 'month' },
]

const changeChartMode = (mode: string) => {
  chartMode.value = mode
  loadHistoryData()
}

const mainChartRef = ref<HTMLElement>()
const macdChartRef = ref<HTMLElement>()
const rsiChartRef = ref<HTMLElement>()
const kdjChartRef = ref<HTMLElement>()
let mainChart: echarts.ECharts | null = null
let macdChart: echarts.ECharts | null = null
let rsiChart: echarts.ECharts | null = null
let kdjChart: echarts.ECharts | null = null

const pagination = {
  pageSize: 20,
}

const columns = [
  { title: '日期', key: 'date', width: 120 },
  { 
    title: '收盘', 
    key: 'close',
    width: 100,
    render: (row: any) => row.close?.toFixed(2),
  },
  { 
    title: '开盘', 
    key: 'open',
    width: 100,
    render: (row: any) => row.open?.toFixed(2),
  },
  { 
    title: '最高', 
    key: 'high',
    width: 100,
    render: (row: any) => row.high?.toFixed(2),
  },
  { 
    title: '最低', 
    key: 'low',
    width: 100,
    render: (row: any) => row.low?.toFixed(2),
  },
  { 
    title: '涨跌幅', 
    key: 'changeRatio',
    width: 100,
    render: (row: any) => {
      const tc = getThemeColors()
      const value = row.changeRatio || 0
      return h('span', {
        style: { color: value >= 0 ? tc.upColor : tc.downColor }
      }, `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`)
    },
  },
  { 
    title: '成交量', 
    key: 'volume',
    width: 100,
    render: (row: any) => formatVolume(row.volume),
  },
]

const getMarketName = (code: string) => {
  const names: Record<string, string> = {
    'sh000001': '上证指数',
    'sz399001': '深证成指',
    'sz399006': '创业板指',
    'sh000688': '科创50',
    '000001': '上证指数',
    '399001': '深证成指',
    '399006': '创业板指',
    '000688': '科创50',
  }
  return names[code] || code
}

const formatVolume = (volume?: number) => {
  if (!volume) return '--'
  if (volume >= 100000000) {
    return (volume / 100000000).toFixed(2) + '亿'
  }
  if (volume >= 10000) {
    return (volume / 10000).toFixed(2) + '万'
  }
  return volume.toString()
}

const loadMarketData = async () => {
  loading.value = true
  try {
    const data = await marketApi.getMarketData()
    marketData.value = data?.find((m: MarketDataVO) => m.marketCode === marketCode.value)
  } catch {
    message.error('加载市场数据失败')
  } finally {
    loading.value = false
  }
}

const loadHistoryData = async () => {
  historyLoading.value = true
  try {
    // 根据图表模式确定请求周期
    let requestPeriod = 'day'
    if (chartMode.value === 'minute') {
      requestPeriod = 'day'  // 分时图获取当日数据
    } else {
      requestPeriod = chartMode.value  // day, week, month
    }

    const data = await marketApi.getMarketHistory(marketCode.value, requestPeriod)
    historyData.value = (data || []).map((item: any) => ({
      date: item.tradeDate || item.date,
      open: item.openPoint || item.open,
      close: item.currentPoint || item.close,
      high: item.highPoint || item.high,
      low: item.lowPoint || item.low,
      volume: item.volume,
      changeRatio: item.changeRatio,
    })).sort((a: any, b: any) => b.date.localeCompare(a.date))

    initMainChart()
    if (chartMode.value !== 'minute') {
      initIndicatorCharts()
    }
  } catch {
    message.error('加载历史数据失败')
    historyData.value = []
  } finally {
    historyLoading.value = false
  }
}

const initMainChart = () => {
  if (!mainChartRef.value || historyData.value.length === 0) return

  if (!mainChart) {
    mainChart = echarts.init(mainChartRef.value)
  }

  const sortedData = [...historyData.value].sort((a, b) => a.date.localeCompare(b.date))
  const dates = sortedData.map(d => d.date)

  // 根据周期格式化日期
  const formatDate = (date: string) => {
    if (chartMode.value === 'month') {
      return date.slice(0, 7)  // YYYY-MM
    }
    return date.slice(5)  // MM-DD
  }

  // 分时图模式
  if (chartMode.value === 'minute') {
    const tc = getThemeColors()
    const closes = sortedData.map(d => d.close)
    const firstClose = closes[0] || 1
    const growths = closes.map(c => ((c - firstClose) / firstClose) * 100)
    const lastGrowth = growths[growths.length - 1] || 0
    const lineColor = lastGrowth >= 0 ? tc.upColor : tc.downColor

    mainChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          const d = sortedData[idx]
          return `
            <div style="padding: 8px;">
              <div style="font-weight: 600; margin-bottom: 8px;">${d.date}</div>
              <div>收盘: <span style="color: ${tc.primaryColor}; font-weight: 600;">${d.close?.toFixed(2)}</span></div>
              <div>涨跌幅: <span style="color: ${d.changeRatio >= 0 ? tc.upColor : tc.downColor};">${d.changeRatio >= 0 ? '+' : ''}${d.changeRatio?.toFixed(2) || '--'}%</span></div>
            </div>
          `
        },
      },
      grid: { left: 55, right: 20, top: 25, bottom: 60 },
      dataZoom: [
        {
          type: 'slider',
          bottom: 10,
          height: 20,
          start: 0,
          end: 100,
          borderColor: 'transparent',
          backgroundColor: 'rgba(0,0,0,0.02)',
          fillerColor: tc.primaryColor + '26',
          handleStyle: { color: tc.primaryColor },
          moveHandleStyle: { color: tc.primaryColor, opacity: 0.3 },
          selectedDataBackground: { lineStyle: { color: tc.primaryColor }, areaStyle: { color: tc.primaryColor } },
          textStyle: { color: tc.textSecondary, fontSize: 10 },
          labelFormatter: (value: number) => {
            const date = dates[value]
            return date ? date.slice(5) : ''
          }
        }
      ],
      xAxis: {
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 11, interval: 'auto' as const, formatter: formatDate },
      },
      yAxis: {
        type: 'value',
        name: '涨跌幅(%)',
        nameTextStyle: { color: tc.textSecondary, fontSize: 11, align: 'right' },
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: tc.textSecondary,
          fontSize: 11,
          formatter: (v: number) => `${v >= 0 ? '+' : ''}${v.toFixed(2)}%`,
        },
      },
      series: [{
        type: 'line',
        data: growths,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2, color: lineColor },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${lineColor}40` },
            { offset: 1, color: `${lineColor}05` },
          ]),
        },
      }],
      animationDuration: 1000,
      animationEasing: 'cubicOut' as const,
    })
  } else {
    // K线图模式（日K、周K、月K）
    const tc = getThemeColors()
    const klineData = sortedData.map(d => [d.open, d.close, d.low, d.high])

    // 根据周期设置dataZoom的起始位置
    let zoomStart = 80
    if (chartMode.value === 'week') {
      zoomStart = 70
    } else if (chartMode.value === 'month') {
      zoomStart = 50
    }

    mainChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          const d = sortedData[idx]
          return `
            <div style="padding: 8px;">
              <div style="font-weight: 600; margin-bottom: 8px;">${d.date}</div>
              <div>开盘: ${d.open?.toFixed(2)}</div>
              <div>收盘: ${d.close?.toFixed(2)}</div>
              <div>最高: ${d.high?.toFixed(2)}</div>
              <div>最低: ${d.low?.toFixed(2)}</div>
              <div>涨跌幅: <span style="color: ${d.changeRatio >= 0 ? tc.upColor : tc.downColor}">${d.changeRatio >= 0 ? '+' : ''}${d.changeRatio?.toFixed(2) || '--'}%</span></div>
            </div>
          `
        },
      },
      legend: { show: false },
      grid: { left: 55, right: 20, top: 25, bottom: 60 },
      dataZoom: [
        {
          type: 'slider',
          bottom: 10,
          height: 20,
          start: zoomStart,
          end: 100,
          borderColor: 'transparent',
          backgroundColor: 'rgba(0,0,0,0.02)',
          fillerColor: tc.primaryColor + '26',
          handleStyle: { color: tc.primaryColor },
          moveHandleStyle: { color: tc.primaryColor, opacity: 0.3 },
          selectedDataBackground: { lineStyle: { color: tc.primaryColor }, areaStyle: { color: tc.primaryColor } },
          textStyle: { color: tc.textSecondary, fontSize: 10 },
          labelFormatter: (value: number) => {
            const date = dates[value]
            return date ? formatDate(date) : ''
          }
        }
      ],
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 11, interval: 'auto' as const, formatter: formatDate },
      },
      yAxis: {
        type: 'value',
        name: '点位',
        nameTextStyle: { color: tc.textSecondary, fontSize: 11, align: 'right' },
        scale: true,
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 11 },
      },
      series: [
        {
          name: 'K线',
          type: 'candlestick',
          data: klineData,
          itemStyle: {
            color: tc.upColor,
            color0: tc.downColor,
            borderColor: tc.upColor,
            borderColor0: tc.downColor,
          },
        },
      ],
      animationDuration: 1000,
      animationEasing: 'cubicOut' as const,
    })
  }
}

const initIndicatorCharts = () => {
  if (historyData.value.length === 0) return

  const tc = getThemeColors()
  const sortedData = [...historyData.value].sort((a, b) => a.date.localeCompare(b.date))
  const closes = sortedData.map(d => d.close)
  const dates = sortedData.map(d => d.date)

  const macdData = calculateMACD(closes)
  const rsiData = calculateRSI(closes, 14)
  const kdjData = calculateKDJ(sortedData)

  // 日期格式化
  const formatDate = (date: string) => {
    if (chartMode.value === 'month') return date.slice(0, 7)
    return date.slice(5)
  }

  // 根据周期设置缩放起始位置
  let zoomStart = 80
  if (chartMode.value === 'week') zoomStart = 70
  if (chartMode.value === 'month') zoomStart = 50

  // 通用的 dataZoom 配置
  const dataZoomConfig = [
    {
      type: 'slider' as const,
      show: false,  // 隐藏滑块，但保留缩放功能
      xAxisIndex: 0,
      start: zoomStart,
      end: 100
    },
    {
      type: 'inside' as const,  // 支持鼠标滚轮缩放
      xAxisIndex: 0,
      start: zoomStart,
      end: 100
    }
  ]

  // X 轴配置（显示日期）
  const xAxisConfig = {
    type: 'category' as const,
    data: dates,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: tc.textTertiary, fontSize: 10, interval: 'auto' as const, formatter: formatDate }
  }

  // MACD 图表
  if (macdChartRef.value) {
    if (!macdChart) macdChart = echarts.init(macdChartRef.value)
    macdChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor, fontSize: 12 },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          return `
            <div style="padding: 8px;">
              <div style="font-weight:600;margin-bottom:6px;">${sortedData[idx].date}</div>
              <div style="color:${tc.primaryColor};">DIF: ${macdData.dif[idx]?.toFixed(4) || '--'}</div>
              <div style="color:${tc.warningColor};">DEA: ${macdData.dea[idx]?.toFixed(4) || '--'}</div>
              <div style="color:${macdData.macd[idx] >= 0 ? tc.upColor : tc.downColor};">MACD: ${(macdData.macd[idx] * 2)?.toFixed(4) || '--'}</div>
            </div>
          `
        }
      },
      legend: { data: ['DIF', 'DEA', 'MACD'], top: 5, textStyle: { color: tc.textSecondary, fontSize: 11 } },
      grid: { left: 50, right: 15, top: 35, bottom: 30 },
      dataZoom: dataZoomConfig,
      xAxis: xAxisConfig,
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 10 }
      },
      series: [
        { name: 'DIF', type: 'line', data: macdData.dif, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.primaryColor } },
        { name: 'DEA', type: 'line', data: macdData.dea, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.warningColor } },
        { name: 'MACD', type: 'bar', data: macdData.macd, barWidth: '40%', itemStyle: { color: (p: any) => p.value >= 0 ? tc.upColor : tc.downColor, borderRadius: [2, 2, 0, 0] } },
      ],
    })
  }

  // RSI 图表
  if (rsiChartRef.value) {
    if (!rsiChart) rsiChart = echarts.init(rsiChartRef.value)
    rsiChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor, fontSize: 12 },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          return `
            <div style="padding: 8px;">
              <div style="font-weight:600;margin-bottom:6px;">${sortedData[idx].date}</div>
              <div style="color:${tc.purpleColor};">RSI(14): ${rsiData[idx]?.toFixed(2) || '--'}</div>
            </div>
          `
        }
      },
      legend: { data: ['RSI(14)'], top: 5, textStyle: { color: tc.textSecondary, fontSize: 11 } },
      grid: { left: 50, right: 15, top: 35, bottom: 30 },
      dataZoom: dataZoomConfig,
      xAxis: xAxisConfig,
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 10, formatter: '{value}' }
      },
      series: [{
        name: 'RSI(14)',
        type: 'line',
        data: rsiData,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5, color: tc.purpleColor },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.15)' },
            { offset: 1, color: 'rgba(139, 92, 246, 0.02)' }
          ])
        }
      }],
      markLine: {
        silent: true,
        symbol: 'none',
        data: [
          { yAxis: 70, lineStyle: { color: tc.upColor, type: 'dashed', width: 1 }, label: { show: true, position: 'insideEndTop', formatter: '超买', color: tc.upColor, fontSize: 9 } },
          { yAxis: 50, lineStyle: { color: tc.textTertiary, type: 'dashed', width: 1 }, label: { show: true, position: 'insideEndTop', formatter: '50', color: tc.textTertiary, fontSize: 9 } },
          { yAxis: 30, lineStyle: { color: tc.downColor, type: 'dashed', width: 1 }, label: { show: true, position: 'insideEndTop', formatter: '超卖', color: tc.downColor, fontSize: 9 } },
        ]
      }
    })
  }

  // KDJ 图表
  if (kdjChartRef.value) {
    if (!kdjChart) kdjChart = echarts.init(kdjChartRef.value)
    kdjChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor, fontSize: 12 },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          return `
            <div style="padding: 8px;">
              <div style="font-weight:600;margin-bottom:6px;">${sortedData[idx].date}</div>
              <div style="color:${tc.primaryColor};">K: ${kdjData.k[idx]?.toFixed(2) || '--'}</div>
              <div style="color:${tc.warningColor};">D: ${kdjData.d[idx]?.toFixed(2) || '--'}</div>
              <div style="color:${tc.downColor};">J: ${kdjData.j[idx]?.toFixed(2) || '--'}</div>
            </div>
          `
        }
      },
      legend: { data: ['K', 'D', 'J'], top: 5, textStyle: { color: tc.textSecondary, fontSize: 11 } },
      grid: { left: 50, right: 15, top: 35, bottom: 30 },
      dataZoom: dataZoomConfig,
      xAxis: xAxisConfig,
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 10 }
      },
      series: [
        { name: 'K', type: 'line', data: kdjData.k, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.primaryColor } },
        { name: 'D', type: 'line', data: kdjData.d, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.warningColor } },
        { name: 'J', type: 'line', data: kdjData.j, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.downColor } },
      ]
    })
  }

  // 监听主图缩放事件，同步到指标图
  if (mainChart && (macdChart || rsiChart || kdjChart)) {
    mainChart.on('dataZoom', (params: any) => {
      const start = params.start ?? (params.batch?.[0]?.start)
      const end = params.end ?? (params.batch?.[0]?.end)
      if (start !== undefined && end !== undefined) {
        macdChart?.dispatchAction({ type: 'dataZoom', start, end })
        rsiChart?.dispatchAction({ type: 'dataZoom', start, end })
        kdjChart?.dispatchAction({ type: 'dataZoom', start, end })
      }
    })
  }
}

// 处理指标标签切换时重新渲染图表
const handleIndicatorTabChange = (name: string) => {
  setTimeout(() => {
    // 切换标签时，需要重新初始化图表（因为DOM可能被重建）
    if (name === 'macd' && macdChartRef.value) {
      // 销毁旧实例，重新初始化
      if (macdChart) {
        macdChart.dispose()
        macdChart = null
      }
      initSingleIndicatorChart('macd')
    } else if (name === 'rsi' && rsiChartRef.value) {
      if (rsiChart) {
        rsiChart.dispose()
        rsiChart = null
      }
      initSingleIndicatorChart('rsi')
    } else if (name === 'kdj' && kdjChartRef.value) {
      if (kdjChart) {
        kdjChart.dispose()
        kdjChart = null
      }
      initSingleIndicatorChart('kdj')
    }
  }, 50)
}

// 初始化单个指标图表（用于Tab切换时懒加载）
const initSingleIndicatorChart = (name: string) => {
  if (historyData.value.length === 0) return

  const tc = getThemeColors()
  const sortedData = [...historyData.value].sort((a, b) => a.date.localeCompare(b.date))
  const closes = sortedData.map(d => d.close)
  const dates = sortedData.map(d => d.date)

  // 日期格式化
  const formatDate = (date: string) => {
    if (chartMode.value === 'month') return date.slice(0, 7)
    return date.slice(5)
  }

  // 根据周期设置缩放起始位置
  let zoomStart = 80
  if (chartMode.value === 'week') zoomStart = 70
  if (chartMode.value === 'month') zoomStart = 50

  // 通用的 dataZoom 配置
  const dataZoomConfig = [
    {
      type: 'slider' as const,
      show: false,
      xAxisIndex: 0,
      start: zoomStart,
      end: 100
    },
    {
      type: 'inside' as const,
      xAxisIndex: 0,
      start: zoomStart,
      end: 100
    }
  ]

  // X 轴配置
  const xAxisConfig = {
    type: 'category' as const,
    data: dates,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: tc.textTertiary, fontSize: 10, interval: 'auto' as const, formatter: formatDate }
  }

  if (name === 'macd' && macdChartRef.value && !macdChart) {
    const macdData = calculateMACD(closes)
    macdChart = echarts.init(macdChartRef.value)
    macdChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor, fontSize: 12 },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          return `
            <div style="padding: 8px;">
              <div style="font-weight:600;margin-bottom:6px;">${sortedData[idx].date}</div>
              <div style="color:${tc.primaryColor};">DIF: ${macdData.dif[idx]?.toFixed(4) || '--'}</div>
              <div style="color:${tc.warningColor};">DEA: ${macdData.dea[idx]?.toFixed(4) || '--'}</div>
              <div style="color:${macdData.macd[idx] >= 0 ? tc.upColor : tc.downColor};">MACD: ${(macdData.macd[idx] * 2)?.toFixed(4) || '--'}</div>
            </div>
          `
        }
      },
      legend: { data: ['DIF', 'DEA', 'MACD'], top: 5, textStyle: { color: tc.textSecondary, fontSize: 11 } },
      grid: { left: 50, right: 15, top: 35, bottom: 30 },
      dataZoom: dataZoomConfig,
      xAxis: xAxisConfig,
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 10 }
      },
      series: [
        { name: 'DIF', type: 'line', data: macdData.dif, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.primaryColor } },
        { name: 'DEA', type: 'line', data: macdData.dea, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.warningColor } },
        { name: 'MACD', type: 'bar', data: macdData.macd, barWidth: '40%', itemStyle: { color: (p: any) => p.value >= 0 ? tc.upColor : tc.downColor, borderRadius: [2, 2, 0, 0] } },
      ],
    })
  } else if (name === 'rsi' && rsiChartRef.value && !rsiChart) {
    const rsiData = calculateRSI(closes, 14)
    rsiChart = echarts.init(rsiChartRef.value)
    rsiChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor, fontSize: 12 },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          return `<div style="padding:8px;"><div style="font-weight:600;margin-bottom:6px;">${sortedData[idx].date}</div><div style="color:${tc.purpleColor};">RSI(14): ${rsiData[idx]?.toFixed(2) || '--'}</div></div>`
        }
      },
      legend: { data: ['RSI(14)'], top: 5, textStyle: { color: tc.textSecondary, fontSize: 11 } },
      grid: { left: 50, right: 15, top: 35, bottom: 30 },
      dataZoom: dataZoomConfig,
      xAxis: xAxisConfig,
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 10 }
      },
      series: [{
        name: 'RSI(14)',
        type: 'line',
        data: rsiData,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.5, color: tc.purpleColor },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139, 92, 246, 0.15)' },
            { offset: 1, color: 'rgba(139, 92, 246, 0.02)' }
          ])
        }
      }],
      markLine: {
        silent: true,
        symbol: 'none',
        data: [
          { yAxis: 70, lineStyle: { color: tc.upColor, type: 'dashed', width: 1 }, label: { show: true, position: 'insideEndTop', formatter: '超买', color: tc.upColor, fontSize: 9 } },
          { yAxis: 50, lineStyle: { color: tc.textTertiary, type: 'dashed', width: 1 }, label: { show: true, position: 'insideEndTop', formatter: '50', color: tc.textTertiary, fontSize: 9 } },
          { yAxis: 30, lineStyle: { color: tc.downColor, type: 'dashed', width: 1 }, label: { show: true, position: 'insideEndTop', formatter: '超卖', color: tc.downColor, fontSize: 9 } },
        ]
      }
    })
  } else if (name === 'kdj' && kdjChartRef.value && !kdjChart) {
    const kdjData = calculateKDJ(sortedData)
    kdjChart = echarts.init(kdjChartRef.value)
    kdjChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: tc.borderColor,
        borderWidth: 1,
        textStyle: { color: tc.textColor, fontSize: 12 },
        formatter: (params: any) => {
          const idx = params[0].dataIndex
          return `<div style="padding:8px;"><div style="font-weight:600;margin-bottom:6px;">${sortedData[idx].date}</div><div style="color:${tc.primaryColor};">K: ${kdjData.k[idx]?.toFixed(2) || '--'}</div><div style="color:${tc.warningColor};">D: ${kdjData.d[idx]?.toFixed(2) || '--'}</div><div style="color:${tc.downColor};">J: ${kdjData.j[idx]?.toFixed(2) || '--'}</div></div>`
        }
      },
      legend: { data: ['K', 'D', 'J'], top: 5, textStyle: { color: tc.textSecondary, fontSize: 11 } },
      grid: { left: 50, right: 15, top: 35, bottom: 30 },
      dataZoom: dataZoomConfig,
      xAxis: xAxisConfig,
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: tc.textSecondary, fontSize: 10 }
      },
      series: [
        { name: 'K', type: 'line', data: kdjData.k, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.primaryColor } },
        { name: 'D', type: 'line', data: kdjData.d, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.warningColor } },
        { name: 'J', type: 'line', data: kdjData.j, smooth: true, symbol: 'none', lineStyle: { width: 1.5, color: tc.downColor } },
      ]
    })
  }
}

const calculateMACD = (closes: number[]) => {
  const ema12: number[] = []
  const ema26: number[] = []
  const dif: number[] = []
  const dea: number[] = []
  const macd: number[] = []
  
  const k12 = 2 / (12 + 1)
  const k26 = 2 / (26 + 1)
  const k9 = 2 / (9 + 1)
  
  closes.forEach((close, i) => {
    if (i === 0) {
      ema12.push(close)
      ema26.push(close)
      dif.push(0)
      dea.push(0)
      macd.push(0)
    } else {
      ema12.push(close * k12 + ema12[i - 1] * (1 - k12))
      ema26.push(close * k26 + ema26[i - 1] * (1 - k26))
      dif.push(ema12[i] - ema26[i])
      dea.push(dif[i] * k9 + dea[i - 1] * (1 - k9))
      macd.push((dif[i] - dea[i]) * 2)
    }
  })
  
  return { dif, dea, macd }
}

const calculateRSI = (closes: number[], period: number) => {
  const rsi: number[] = []
  let gains = 0
  let losses = 0
  
  closes.forEach((close, i) => {
    if (i === 0) {
      rsi.push(50)
    } else {
      const change = close - closes[i - 1]
      const gain = change > 0 ? change : 0
      const loss = change < 0 ? -change : 0
      
      if (i < period) {
        gains += gain
        losses += loss
        rsi.push(50)
      } else if (i === period) {
        gains += gain
        losses += loss
        const avgGain = gains / period
        const avgLoss = losses / period
        const rs = avgLoss === 0 ? 100 : avgGain / avgLoss
        rsi.push(100 - 100 / (1 + rs))
      } else {
        gains = (gains * (period - 1) + gain) / period
        losses = (losses * (period - 1) + loss) / period
        const rs = losses === 0 ? 100 : gains / losses
        rsi.push(100 - 100 / (1 + rs))
      }
    }
  })
  
  return rsi
}

const calculateKDJ = (data: any[]) => {
  const k: number[] = []
  const d: number[] = []
  const j: number[] = []
  const period = 9
  
  data.forEach((item, i) => {
    if (i < period - 1) {
      k.push(50)
      d.push(50)
      j.push(50)
    } else {
      const slice = data.slice(i - period + 1, i + 1)
      const high = Math.max(...slice.map(d => d.high))
      const low = Math.min(...slice.map(d => d.low))
      const rsv = high === low ? 50 : ((item.close - low) / (high - low)) * 100
      
      const kv = i === period - 1 ? 50 : (2 / 3) * k[i - 1] + (1 / 3) * rsv
      const dv = i === period - 1 ? 50 : (2 / 3) * d[i - 1] + (1 / 3) * kv
      const jv = 3 * kv - 2 * dv
      
      k.push(kv)
      d.push(dv)
      j.push(jv)
    }
  })
  
  return { k, d, j }
}

const handleResize = () => {
  mainChart?.resize()
  macdChart?.resize()
  rsiChart?.resize()
  kdjChart?.resize()
}

onMounted(() => {
  marketCode.value = route.params.marketCode as string
  if (!marketCode.value) {
    message.error('缺少市场代码')
    router.push('/')
    return
  }
  
  loadMarketData()
  loadHistoryData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  mainChart?.dispose()
  macdChart?.dispose()
  rsiChart?.dispose()
  kdjChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

watch(chartMode, () => {
  // 图表模式变化时会触发 loadHistoryData，这里只需要重新渲染
})

// 监听 historyData 变化来重新渲染图表
watch(historyData, () => {
  initMainChart()
  if (chartMode.value !== 'minute') {
    setTimeout(() => initIndicatorCharts(), 100)
  }
}, { deep: true })
</script>

<style scoped>
.market-header {
  margin-bottom: 24px;
  padding: 24px;
}

.market-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.market-title h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

.market-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 800px) {
  .market-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: var(--bg-color);
  border-radius: var(--radius-lg);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-value.primary {
  color: var(--primary-color);
}

.market-detail-info {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.detail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.detail-item .label {
  font-size: 12px;
  color: var(--text-secondary);
}

.detail-item .value {
  font-size: 16px;
  font-weight: 600;
}

.chart-section {
  padding: 20px;
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-controls {
  display: flex;
  align-items: center;
  gap: 12px;
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
  border-radius: var(--radius-md);
  padding: 3px;
  gap: 1px;
}

.segment-btn {
  padding: 6px 12px;
  font-size: 12px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
}

.segment-btn:hover {
  background: var(--bg-color);
}

.segment-btn.active {
  background: var(--primary-color);
  color: white;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.15);
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
  margin: 0 8px;
}

.main-chart {
  height: 400px;
  width: 100%;
}

.indicator-section {
  padding: 20px;
  margin-bottom: 24px;
}

.indicator-section .section-title {
  font-size: 16px;
  margin-bottom: 16px;
}

.indicator-chart {
  height: 150px;
  width: 100%;
}

.history-section {
  padding: 20px;
}

.history-section .section-title {
  font-size: 16px;
  margin-bottom: 16px;
}

.growth-positive {
  color: var(--up-color);
}

.growth-negative {
  color: var(--down-color);
}
</style>
