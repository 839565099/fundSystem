<template>
  <div class="page-container">
    <PageHeader title="我的收藏" icon="⭐">
      <template #actions>
        <n-button v-if="favorites.length > 0" @click="handleExport">
          <template #icon><n-icon><DownloadOutline /></n-icon></template>
          导出
        </n-button>
      </template>
    </PageHeader>

    <n-spin :show="loading">
      <div v-if="favorites.length > 0">
        <div class="stats-card card">
          <div class="stat-item">
            <span class="stat-value">{{ favorites.length }}</span>
            <span class="stat-label">收藏基金</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">
              <GrowthText :value="avgGrowth" size="lg" />
            </span>
            <span class="stat-label">平均日涨跌</span>
          </div>
          <div class="stat-item">
            <span class="stat-value growth-positive">{{ upCount }}</span>
            <span class="stat-label">上涨基金</span>
          </div>
          <div class="stat-item">
            <span class="stat-value growth-negative">{{ downCount }}</span>
            <span class="stat-label">下跌基金</span>
          </div>
        </div>

        <div class="trend-section card" v-if="favorites.length > 0">
          <div class="section-header">
            <h3 class="section-title">
              <n-icon size="20"><TrendingUpOutline /></n-icon>
              收藏组合趋势
            </h3>
            <div class="trend-controls">
              <n-popover trigger="click" placement="bottom">
                <template #trigger>
                  <n-button size="small">
                    选择基金 ({{ selectedTrendFunds.length }}/{{ favorites.length }})
                  </n-button>
                </template>
                <div class="fund-selector">
                  <n-checkbox-group v-model:value="selectedTrendFunds" @update:value="loadTrendChart">
                    <n-space vertical>
                      <n-checkbox
                        v-for="fav in favorites"
                        :key="fav.fundCode"
                        :value="fav.fundCode"
                        :label="`${fav.fundName} (${fav.fundCode})`"
                      />
                    </n-space>
                  </n-checkbox-group>
                  <div class="selector-actions">
                    <n-button text size="small" @click="selectAllFunds">全选</n-button>
                    <n-button text size="small" @click="selectTopFunds">前5只</n-button>
                    <n-button text size="small" @click="clearSelection">清空</n-button>
                  </div>
                </div>
              </n-popover>
              <n-button-group size="small">
                <n-button
                  v-for="p in trendPeriods"
                  :key="p.value"
                  :type="trendPeriod === p.value ? 'primary' : 'default'"
                  @click="changeTrendPeriod(p.value)"
                >
                  {{ p.label }}
                </n-button>
              </n-button-group>
            </div>
          </div>
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>

        <div class="favorites-grid">
          <div 
            v-for="fav in favorites" 
            :key="fav.fundCode" 
            class="favorite-card card"
            @click="router.push(`/fund/${fav.fundCode}`)"
          >
            <div class="fav-header">
              <n-icon size="20" color="#faad14"><StarOutline /></n-icon>
              <span class="fav-name">{{ fav.fundName }}</span>
            </div>
            <div class="fav-code">{{ fav.fundCode }}</div>
            <div class="fav-nav">净值: {{ fav.nav?.toFixed(4) || '--' }}</div>
            <div class="fav-growth">
              <div class="growth-item">
                <span class="label">今日</span>
                <GrowthText :value="fav.dayGrowth" size="sm" />
              </div>
              <div class="growth-item">
                <span class="label">7日</span>
                <GrowthText :value="fav.weekGrowth" size="sm" />
              </div>
              <div class="growth-item">
                <span class="label">30日</span>
                <GrowthText :value="fav.monthGrowth" size="sm" />
              </div>
            </div>
            <n-button 
              text 
              type="error" 
              size="small"
              @click.stop="handleRemove(fav.fundCode)"
            >
              取消收藏
            </n-button>
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无收藏">
        <template #extra>
          <n-button type="primary" @click="router.push('/search')">
            去搜索
          </n-button>
        </template>
      </n-empty>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NButton, NSpin, NEmpty, NButtonGroup, NPopover, NCheckboxGroup, NCheckbox, NSpace, createDiscreteApi } from 'naive-ui'
import { IconStar as StarOutline, IconDownload as DownloadOutline, IconTrendingUp as TrendingUpOutline } from '@tabler/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import GrowthText from '../components/GrowthText.vue'
import { favoriteApi, exportApi, fundApi } from '../api/fund'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import type { UserFavorite, FundNavHistoryVO } from '../types'
import * as echarts from 'echarts'

const getCssColor = (varName: string, fallback: string) => {
  return getComputedStyle(document.documentElement).getPropertyValue(varName).trim() || fallback
}

const { message } = createDiscreteApi(['message'])
const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const favorites = ref<UserFavorite[]>([])
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const trendPeriod = ref('month')
const trendPeriods = [
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '三月', value: 'threeMonth' },
  { label: '六月', value: 'sixMonth' },
]

// 选中的基金代码列表（用于趋势图显示）
const selectedTrendFunds = ref<string[]>([])

// 基于索引生成区分度高的颜色（HSL色彩空间）
const PALETTE = [
  '#6366f1', '#f43f5e', '#10b981', '#f59e0b', '#3b82f6',
  '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#06b6d4',
  '#a855f7', '#84cc16', '#e11d48', '#0ea5e9', '#eab308',
]

const generateColors = (count: number): string[] => {
  return Array.from({ length: count }, (_, i) => PALETTE[i % PALETTE.length])
}

// 初始化选中基金（默认前5只或全部）
const initSelectedFunds = () => {
  selectedTrendFunds.value = favorites.value.slice(0, 5).map(f => f.fundCode)
}

// 全选
const selectAllFunds = () => {
  selectedTrendFunds.value = favorites.value.map(f => f.fundCode)
  loadTrendChart()
}

// 选择前5只
const selectTopFunds = () => {
  initSelectedFunds()
  loadTrendChart()
}

// 清空选择
const clearSelection = () => {
  selectedTrendFunds.value = []
}

const avgGrowth = computed(() => {
  if (favorites.value.length === 0) return 0
  const total = favorites.value.reduce((sum, f) => sum + (f.dayGrowth || 0), 0)
  return total / favorites.value.length
})

const upCount = computed(() => favorites.value.filter(f => (f.dayGrowth || 0) > 0).length)
const downCount = computed(() => favorites.value.filter(f => (f.dayGrowth || 0) < 0).length)

const loadFavorites = async () => {
  loading.value = true
  try {
    favorites.value = await favoriteApi.getList() || []
    initSelectedFunds()
  } catch {
    message.error('加载收藏失败')
  } finally {
    loading.value = false
  }
}

const changeTrendPeriod = (period: string) => {
  trendPeriod.value = period
  loadTrendChart()
}

const loadTrendChart = async () => {
  if (!trendChartRef.value || selectedTrendFunds.value.length === 0) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const navHistories: Map<string, FundNavHistoryVO[]> = new Map()
  const selectedFunds = favorites.value.filter(f => selectedTrendFunds.value.includes(f.fundCode))

  const loadPromises = selectedFunds.map(async (fav) => {
    try {
      const data = await fundApi.getNavHistory(fav.fundCode, trendPeriod.value)
      navHistories.set(fav.fundCode, data || [])
    } catch {
      navHistories.set(fav.fundCode, [])
    }
  })

  await Promise.all(loadPromises)

  const allDates = new Set<string>()
  navHistories.forEach(history => {
    history.forEach(h => allDates.add(h.navDate))
  })
  const sortedDates = Array.from(allDates).sort()

  const series: any[] = []
  const colors = generateColors(selectedFunds.length)

  // 解析实际颜色值，用于 tooltip
  const upColor = getCssColor('--up-color', '#ef4444')
  const downColor = getCssColor('--down-color', '#22c55e')
  const textColor = getCssColor('--text-primary', '#1e293b')
  const textSecondary = getCssColor('--text-secondary', '#64748b')
  const cardBg = getCssColor('--card-bg', '#ffffff')

  selectedFunds.forEach((fav, index) => {
    const history = navHistories.get(fav.fundCode) || []
    if (history.length === 0) return

    const firstNav = history[0]?.nav || 1
    const growthData = sortedDates.map(date => {
      const item = history.find(h => h.navDate === date)
      if (!item) return null
      return firstNav > 0 ? ((item.nav - firstNav) / firstNav) * 100 : 0
    })

    series.push({
      name: fav.fundName?.substring(0, 6),
      type: 'line',
      data: growthData,
      smooth: 0.4,
      symbol: 'circle',
      symbolSize: 4,
      showSymbol: false,
      lineStyle: { width: 2.5, color: colors[index] },
      itemStyle: { color: colors[index] },
      emphasis: {
        focus: 'series',
        lineStyle: { width: 3.5 },
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: colors[index] + '30' },
          { offset: 1, color: colors[index] + '05' },
        ]),
      },
    })
  })

  if (series.length === 0) {
    trendChart.clear()
    return
  }

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: cardBg,
      borderColor: isDark.value ? 'rgba(255,255,255,0.12)' : '#e2e8f0',
      borderWidth: 1,
      padding: [12, 16],
      textStyle: { color: textColor, fontSize: 12 },
      confine: true,
      axisPointer: {
        type: 'cross',
        crossStyle: { color: textSecondary + '40' },
        lineStyle: { color: textSecondary + '40', type: 'dashed' },
      },
      formatter: (params: any[]) => {
        if (!params || params.length === 0) return ''
        const date = params[0].axisValue
        let html = `<div style="font-weight:600;margin-bottom:10px;font-size:13px;color:${textColor};">${date}</div>`
        params.forEach((p: any) => {
          if (p.value !== null && p.value !== undefined) {
            const valColor = p.value >= 0 ? upColor : downColor
            html += `<div style="display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:4px;">
              <span style="display:flex;align-items:center;gap:6px;">
                <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color};"></span>
                <span style="color:${textColor};font-size:12px;">${p.seriesName}</span>
              </span>
              <span style="color:${valColor};font-weight:600;font-size:12px;">${p.value >= 0 ? '+' : ''}${p.value.toFixed(2)}%</span>
            </div>`
          }
        })
        return html
      },
    },
    legend: {
      data: series.map(s => s.name),
      bottom: 0,
      type: 'scroll',
      textStyle: { fontSize: 11, color: textSecondary },
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 3,
    },
    grid: {
      left: '3%',
      right: '3%',
      bottom: '14%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: sortedDates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: textSecondary,
        fontSize: 11,
        formatter: (value: string) => value.slice(5),
      },
    },
    yAxis: {
      type: 'value',
      name: '涨跌幅(%)',
      nameTextStyle: {
        color: textSecondary,
        fontSize: 11,
        padding: [0, 0, 0, 30],
      },
      splitLine: {
        lineStyle: {
          color: isDark.value ? 'rgba(255, 255, 255, 0.06)' : '#f1f5f9',
          type: 'dashed',
        },
      },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: textSecondary,
        fontSize: 11,
        formatter: (value: number) => `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`,
      },
    },
    series,
    animationDuration: 600,
    animationEasing: 'cubicOut' as const,
  }, true)
}

const handleRemove = async (fundCode: string) => {
  try {
    await favoriteApi.remove(fundCode)
    favorites.value = favorites.value.filter(f => f.fundCode !== fundCode)
    message.success('取消收藏成功')
    if (favorites.value.length > 0) {
      loadTrendChart()
    }
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

const handleExport = async () => {
  try {
    const res = await exportApi.exportFavorites('xlsx')
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'favorites.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch {
    message.error('导出失败')
  }
}

const handleResize = () => {
  trendChart?.resize()
}

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await loadFavorites()
  if (favorites.value.length > 0) {
    await nextTick()
    loadTrendChart()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.stats-card {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24px;
  padding: 24px;
}

.stat-item {
  text-align: center;
}

.stat-item .stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-item .stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.trend-section {
  padding: 20px;
  margin-bottom: 24px;
}

.trend-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.trend-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fund-selector {
  max-height: 300px;
  overflow-y: auto;
  padding: 12px;
}

.selector-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.trend-section .section-title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-chart {
  height: 300px;
  width: 100%;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1000px) {
  .favorites-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .favorites-grid {
    grid-template-columns: 1fr;
  }
}

.favorite-card {
  padding: 16px;
  cursor: pointer;
}

.fav-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.fav-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fav-code {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.fav-nav {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.fav-growth {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.growth-item {
  text-align: center;
}

.growth-item .label {
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.growth-item span:last-child {
  font-size: 14px;
  font-weight: 600;
}
</style>
