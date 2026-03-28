<template>
  <div class="page-container">
    <!-- 选择区域 -->
    <div class="compare-header card">
      <h2 class="section-title">
        <n-icon size="24"><GitCompareOutline /></n-icon>
        基金对比
      </h2>
      <div class="fund-selectors">
        <div v-for="(code, index) in selectedCodes" :key="index" class="selector-item">
          <n-select
            :value="code"
            :options="getOptions(index)"
            :loading="getLoading(index)"
            placeholder="选择或搜索基金"
            filterable
            remote
            clearable
            style="width: 280px"
            @search="(query: string) => handleSearch(query, index)"
            @update:value="(val: string) => handleSelect(val, index)"
          />
          <n-button
            v-if="selectedCodes.length > 2"
            text
            type="error"
            @click="removeSelector(index)"
          >
            <n-icon size="18"><CloseOutline /></n-icon>
          </n-button>
        </div>
        <n-button
          v-if="selectedCodes.length < 4"
          dashed
          @click="addSelector"
        >
          <template #icon><n-icon><AddOutline /></n-icon></template>
          添加基金
        </n-button>
      </div>
      <n-button
        type="primary"
        :disabled="validCodes.length < 2"
        :loading="loading"
        @click="handleCompare"
      >
        开始对比
      </n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="compareData.length > 0" class="compare-result">
        <!-- 趋势图对比 -->
        <div class="chart-section card">
          <div class="section-header">
            <h3 class="section-title">
              <n-icon size="20"><TrendingUpOutline /></n-icon>
              累计涨跌幅走势对比
            </h3>
            <div class="chart-controls">
              <n-radio-group v-model:value="chartPeriod" @update:value="updateChart">
                <n-radio-button value="month">近一月</n-radio-button>
                <n-radio-button value="threeMonth">近三月</n-radio-button>
                <n-radio-button value="sixMonth">近六月</n-radio-button>
                <n-radio-button value="year">近一年</n-radio-button>
              </n-radio-group>
            </div>
          </div>
          <div ref="chartRef" class="compare-chart"></div>
        </div>

        <!-- 基金卡片对比 -->
        <div class="funds-comparison">
          <div v-for="(fund, index) in compareData" :key="fund.fundCode" class="fund-card card" :style="{ borderTopColor: colors[index] }">
            <div class="fund-card-header" :style="{ backgroundColor: colors[index] + '15' }">
              <div class="fund-title">
                <span class="fund-name">{{ fund.fundName }}</span>
                <n-tag v-if="fund.fundType" size="small" type="info">{{ fund.fundType }}</n-tag>
              </div>
              <div class="fund-code">{{ fund.fundCode }}</div>
            </div>

            <div class="fund-card-body">
              <!-- 核心数据 -->
              <div class="core-stats">
                <div class="stat-item">
                  <span class="stat-label">最新净值</span>
                  <span class="stat-value primary">{{ fund.nav?.toFixed(4) || '--' }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">日涨跌</span>
                  <span class="stat-value" :class="fund.dayGrowth! >= 0 ? 'positive' : 'negative'">
                    {{ fund.dayGrowth! >= 0 ? '+' : '' }}{{ fund.dayGrowth?.toFixed(2) }}%
                  </span>
                </div>
              </div>

              <!-- 业绩表现 -->
              <div class="performance-section">
                <h4>业绩表现</h4>
                <div class="performance-bars">
                  <div class="bar-item">
                    <span class="bar-label">近一周</span>
                    <div class="bar-wrapper">
                      <div class="bar" :style="getBarStyle(fund.weekGrowth, maxValues.week)"></div>
                    </div>
                    <span class="bar-value" :class="fund.weekGrowth! >= 0 ? 'positive' : 'negative'">
                      {{ fund.weekGrowth! >= 0 ? '+' : '' }}{{ fund.weekGrowth?.toFixed(2) }}%
                    </span>
                  </div>
                  <div class="bar-item">
                    <span class="bar-label">近一月</span>
                    <div class="bar-wrapper">
                      <div class="bar" :style="getBarStyle(fund.monthGrowth, maxValues.month)"></div>
                    </div>
                    <span class="bar-value" :class="fund.monthGrowth! >= 0 ? 'positive' : 'negative'">
                      {{ fund.monthGrowth! >= 0 ? '+' : '' }}{{ fund.monthGrowth?.toFixed(2) }}%
                    </span>
                  </div>
                  <div class="bar-item">
                    <span class="bar-label">近三月</span>
                    <div class="bar-wrapper">
                      <div class="bar" :style="getBarStyle(fund.threeMonthGrowth, maxValues.threeMonth)"></div>
                    </div>
                    <span class="bar-value" :class="fund.threeMonthGrowth! >= 0 ? 'positive' : 'negative'">
                      {{ fund.threeMonthGrowth! >= 0 ? '+' : '' }}{{ fund.threeMonthGrowth?.toFixed(2) }}%
                    </span>
                  </div>
                  <div class="bar-item">
                    <span class="bar-label">近六月</span>
                    <div class="bar-wrapper">
                      <div class="bar" :style="getBarStyle(fund.sixMonthGrowth, maxValues.sixMonth)"></div>
                    </div>
                    <span class="bar-value" :class="fund.sixMonthGrowth! >= 0 ? 'positive' : 'negative'">
                      {{ fund.sixMonthGrowth! >= 0 ? '+' : '' }}{{ fund.sixMonthGrowth?.toFixed(2) }}%
                    </span>
                  </div>
                  <div class="bar-item">
                    <span class="bar-label">近一年</span>
                    <div class="bar-wrapper">
                      <div class="bar" :style="getBarStyle(fund.yearGrowth, maxValues.year)"></div>
                    </div>
                    <span class="bar-value" :class="fund.yearGrowth! >= 0 ? 'positive' : 'negative'">
                      {{ fund.yearGrowth! >= 0 ? '+' : '' }}{{ fund.yearGrowth?.toFixed(2) }}%
                    </span>
                  </div>
                  <div class="bar-item">
                    <span class="bar-label">成立以来</span>
                    <div class="bar-wrapper">
                      <div class="bar" :style="getBarStyle(fund.totalGrowth, maxValues.total)"></div>
                    </div>
                    <span class="bar-value" :class="fund.totalGrowth! >= 0 ? 'positive' : 'negative'">
                      {{ fund.totalGrowth! >= 0 ? '+' : '' }}{{ fund.totalGrowth?.toFixed(2) }}%
                    </span>
                  </div>
                </div>
              </div>

              <!-- 风险指标 -->
              <div class="risk-section">
                <h4>风险指标</h4>
                <div class="risk-items">
                  <div class="risk-item">
                    <span class="risk-label">最大回撤</span>
                    <span class="risk-value negative">{{ fund.maxDrawdown ? fund.maxDrawdown.toFixed(2) + '%' : '--' }}</span>
                  </div>
                  <div class="risk-item">
                    <span class="risk-label">夏普比率</span>
                    <span class="risk-value">{{ fund.sharpeRatio?.toFixed(2) || '--' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 详细数据表格 -->
        <div class="detail-table card">
          <h3 class="section-title">
            <n-icon size="20"><GridOutline /></n-icon>
            详细指标对比
          </h3>
          <n-data-table
            :columns="tableColumns"
            :data="tableData"
            :bordered="false"
            striped
          />
        </div>
      </div>

      <n-empty v-else-if="!loading" description="请选择至少2只基金进行对比" />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { NIcon, NButton, NSelect, NSpin, NEmpty, NDataTable, NTag, NRadioButton, NRadioGroup, createDiscreteApi } from 'naive-ui'
import { IconGitCompare as GitCompareOutline, IconX as CloseOutline, IconPlus as AddOutline, IconTrendingUp as TrendingUpOutline, IconLayoutGrid as GridOutline } from '@tabler/icons-vue'
import { fundApi, compareApi, favoriteApi } from '../api/fund'
import type { Fund, FundCompareVO } from '../types'
import * as echarts from 'echarts'
import { useThemeStore } from '../stores/theme'

const { message } = createDiscreteApi(['message'])
const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

const getChartColors = () => ({
  splitLine: isDark.value ? 'rgba(255, 255, 255, 0.08)' : '#f3f4f6',
  text: isDark.value ? 'rgba(255, 255, 255, 0.6)' : '#6b7280',
})

const selectedCodes = ref(['', ''])
const selectOptions = ref(new Map<number, { label: string; value: string }[]>())
const searchLoading = ref(new Map<number, boolean>())
const loading = ref(false)
const compareData = ref<FundCompareVO[]>([])
const chartRef = ref<HTMLElement>()
const chartPeriod = ref('sixMonth')
let chart: echarts.ECharts | null = null

// 收藏的基金
const favoriteOptions = ref<{ label: string; value: string }[]>([])

// 加载收藏的基金
const loadFavorites = async () => {
  try {
    const list = await favoriteApi.getList()
    favoriteOptions.value = (list || []).map((f: any) => ({
      label: `⭐ ${f.fundCode} - ${f.fundName}`,
      value: f.fundCode
    }))
    // 初始化所有选择器的选项
    selectedCodes.value.forEach((_, index) => {
      selectOptions.value.set(index, [...favoriteOptions.value])
    })
  } catch {
    // 忽略错误
  }
}

const validCodes = computed(() => {
  return selectedCodes.value.filter(c => c && c.length > 0)
})

const colors = ['#4f46e5', '#16a34a', '#d97706', '#dc2626']

// 计算各指标的最大值（用于条形图比例）
const maxValues = computed(() => {
  const getValue = (arr: (number | undefined)[]) => {
    const values = arr.filter(v => v !== undefined && v !== null) as number[]
    if (values.length === 0) return 1
    const max = Math.max(...values.map(v => Math.abs(v)))
    return max || 1
  }

  return {
    week: getValue(compareData.value.map(f => f.weekGrowth)),
    month: getValue(compareData.value.map(f => f.monthGrowth)),
    threeMonth: getValue(compareData.value.map(f => f.threeMonthGrowth)),
    sixMonth: getValue(compareData.value.map(f => f.sixMonthGrowth)),
    year: getValue(compareData.value.map(f => f.yearGrowth)),
    total: getValue(compareData.value.map(f => f.totalGrowth))
  }
})

let searchTimer: ReturnType<typeof setTimeout> | null = null

// 搜索基金
const handleSearch = async (query: string, index: number) => {
  if (searchTimer) clearTimeout(searchTimer)

  // 获取已选的基金代码（排除当前选择器）
  const selectedCodesSet = new Set(selectedCodes.value.filter((c, i) => i !== index && c))
  const availableFavorites = favoriteOptions.value.filter(f => !selectedCodesSet.has(f.value))

  if (!query) {
    selectOptions.value.set(index, availableFavorites)
    return
  }

  searchLoading.value.set(index, true)
  searchTimer = setTimeout(async () => {
    try {
      const data = await fundApi.searchByKeyword(query, 10)
      const searchResults = (data || [])
        .filter((f: Fund) => !selectedCodesSet.has(f.fundCode))
        .map((f: Fund) => ({
          label: `${f.fundCode} - ${f.fundName}`,
          value: f.fundCode
        }))
      selectOptions.value.set(index, [...availableFavorites, ...searchResults])
    } catch {
      selectOptions.value.set(index, availableFavorites)
    } finally {
      searchLoading.value.set(index, false)
    }
  }, 300)
}

// 选择基金
const handleSelect = (value: string, index: number) => {
  const newCodes = [...selectedCodes.value]
  newCodes[index] = value
  selectedCodes.value = newCodes
}

// 获取选项
const getOptions = (index: number) => selectOptions.value.get(index) || favoriteOptions.value

// 获取加载状态
const getLoading = (index: number) => searchLoading.value.get(index) || false

const addSelector = () => {
  if (selectedCodes.value.length < 4) {
    const newIndex = selectedCodes.value.length
    selectedCodes.value.push('')
    // 初始化新选择器的选项
    selectOptions.value.set(newIndex, [...favoriteOptions.value])
  }
}

const removeSelector = (index: number) => {
  selectedCodes.value.splice(index, 1)
}

const handleCompare = async () => {
  if (validCodes.value.length < 2) {
    message.warning('请选择至少2只基金')
    return
  }

  loading.value = true
  try {
    const result = await compareApi.compare(validCodes.value)
    compareData.value = result || []
    setTimeout(initChart, 100)
  } catch {
    message.error('对比失败')
  } finally {
    loading.value = false
  }
}

// 生成条形图样式
const getBarStyle = (value: number | undefined, maxValue: number) => {
  if (value === undefined || value === null) {
    return { width: '0%', backgroundColor: 'var(--bg-secondary)' }
  }
  const percentage = Math.min(Math.abs(value) / maxValue * 100, 100)
  const color = value >= 0 ? 'var(--up-color)' : 'var(--down-color)'
  return {
    width: `${percentage}%`,
    backgroundColor: color
  }
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value || compareData.value.length === 0) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  updateChart()
}

const updateChart = () => {
  if (!chart || compareData.value.length === 0) return

  // 根据周期过滤数据
  const now = new Date()
  let startDate: Date
  switch (chartPeriod.value) {
    case 'month':
      startDate = new Date(now.getFullYear(), now.getMonth() - 1, now.getDate())
      break
    case 'threeMonth':
      startDate = new Date(now.getFullYear(), now.getMonth() - 3, now.getDate())
      break
    case 'sixMonth':
      startDate = new Date(now.getFullYear(), now.getMonth() - 6, now.getDate())
      break
    case 'year':
      startDate = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate())
      break
    default:
      startDate = new Date(now.getFullYear(), now.getMonth() - 6, now.getDate())
  }

  // 处理数据：计算累计涨跌幅
  const series: any[] = []
  let allDates = new Set<string>()

  compareData.value.forEach((fund, index) => {
    if (!fund.navHistory || fund.navHistory.length === 0) return

    const history = fund.navHistory
      .filter(h => new Date(h.navDate) >= startDate)
      .sort((a, b) => a.navDate.localeCompare(b.navDate))

    if (history.length === 0) return

    const baseNav = history[0].nav
    const data = history.map(h => {
      allDates.add(h.navDate)
      return {
        date: h.navDate,
        value: baseNav ? ((h.nav - baseNav) / baseNav * 100) : 0
      }
    })

    series.push({
      name: fund.fundName,
      type: 'line',
      smooth: true,
      symbol: 'none',
      data: data.map(d => d.value),
      lineStyle: { width: 2.5, color: colors[index] },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: colors[index] + '30' },
          { offset: 1, color: colors[index] + '05' }
        ])
      }
    })
  })

  const dates = Array.from(allDates).sort()

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'var(--border-color)',
      borderWidth: 1,
      textStyle: { color: 'var(--text-color)', fontSize: 12 },
      formatter: (params: any[]) => {
        const date = dates[params[0].dataIndex]
        let html = `<div style="font-weight:600;margin-bottom:8px;">${date}</div>`
        params.forEach((p) => {
          const color = p.color
          const value = p.value
          html += `<div style="display:flex;justify-content:space-between;gap:20px;margin:4px 0;">
            <span><span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${color};margin-right:6px;"></span>${p.seriesName}</span>
            <span style="font-weight:600;color:${value >= 0 ? 'var(--up-color)' : 'var(--down-color)'}">${value >= 0 ? '+' : ''}${value.toFixed(2)}%</span>
          </div>`
        })
        return html
      }
    },
    legend: {
      data: compareData.value.map(f => f.fundName),
      bottom: 0,
      textStyle: { color: 'var(--text-secondary)' }
    },
    grid: {
      left: 60,
      right: 20,
      top: 40,
      bottom: 60
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#6b7280',
        fontSize: 11,
        formatter: (v: string) => v.slice(5)
      }
    },
    yAxis: {
      type: 'value',
      name: '累计涨跌幅(%)',
      nameTextStyle: { color: '#6b7280', fontSize: 11 },
      splitLine: { lineStyle: { color: getChartColors().splitLine, type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#6b7280',
        fontSize: 11,
        formatter: (v: number) => `${v >= 0 ? '+' : ''}${v.toFixed(1)}%`
      }
    },
    series,
    animationDuration: 800,
    animationEasing: 'cubicOut' as const
  }

  chart.setOption(option, true)
}

// 表格数据
const tableColumns = computed(() => {
  const cols: any[] = [
    { title: '指标', key: 'metric', width: 120, fixed: 'left' }
  ]
  compareData.value.forEach((fund, index) => {
    cols.push({
      title: fund.fundName,
      key: `fund${index}`,
      render: (row: any) => {
        const value = row[`fund${index}`]
        if (typeof value === 'number') {
          const color = value >= 0 ? 'var(--up-color)' : 'var(--down-color)'
          return h('span', { style: { color, fontWeight: '600' } },
            `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`)
        }
        return value ?? '--'
      }
    })
  })
  return cols
})

const tableData = computed(() => {
  const metrics = [
    { key: 'dayGrowth', label: '日涨跌幅' },
    { key: 'weekGrowth', label: '近一周' },
    { key: 'monthGrowth', label: '近一月' },
    { key: 'threeMonthGrowth', label: '近三月' },
    { key: 'sixMonthGrowth', label: '近六月' },
    { key: 'yearGrowth', label: '近一年' },
    { key: 'totalGrowth', label: '成立以来' }
  ]

  return metrics.map(metric => {
    const row: any = { metric: metric.label }
    compareData.value.forEach((fund, index) => {
      row[`fund${index}`] = fund[metric.key as keyof FundCompareVO]
    })
    return row
  })
})

const handleResize = () => chart?.resize()

onMounted(() => {
  loadFavorites()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.compare-header {
  margin-bottom: 24px;
  padding: 24px;
}

.fund-selectors {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 20px 0;
}

.selector-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compare-result {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 图表区域 */
.chart-section {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.compare-chart {
  height: 400px;
  width: 100%;
}

/* 基金卡片对比 */
.funds-comparison {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.fund-card {
  border-top: 4px solid;
  overflow: hidden;
}

.fund-card-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.fund-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.fund-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
}

.fund-code {
  font-size: 13px;
  color: var(--text-secondary);
}

.fund-card-body {
  padding: 16px 20px;
}

.core-stats {
  display: flex;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
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
  font-size: 20px;
  font-weight: 700;
}

.stat-value.primary {
  color: var(--primary-color);
}

.stat-value.positive {
  color: var(--up-color);
}

.stat-value.negative {
  color: var(--down-color);
}

/* 业绩表现条形图 */
.performance-section {
  margin-bottom: 16px;
}

.performance-section h4,
.risk-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
}

.performance-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-item {
  display: grid;
  grid-template-columns: 60px 1fr 60px;
  align-items: center;
  gap: 8px;
}

.bar-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.bar-wrapper {
  height: 8px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.bar {
  height: 100%;
  border-radius: var(--radius-sm);
  transition: width 0.5s ease;
}

.bar-value {
  font-size: 12px;
  font-weight: 600;
  text-align: right;
}

.bar-value.positive {
  color: var(--up-color);
}

.bar-value.negative {
  color: var(--down-color);
}

/* 风险指标 */
.risk-items {
  display: flex;
  gap: 20px;
}

.risk-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.risk-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.risk-value {
  font-size: 14px;
  font-weight: 600;
}

/* 详细表格 */
.detail-table {
  padding: 20px;
}

.detail-table .section-title {
  margin-bottom: 16px;
}
</style>
