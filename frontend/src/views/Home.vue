<template>
  <div class="home-container">
    <!-- Hero 区域 + 搜索 -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="hero-title">智能基金投资平台</h1>
        <p class="hero-subtitle">专业的基金数据分析与投资决策工具</p>
        <div class="search-box">
          <n-auto-complete
            v-model:value="searchKeyword"
            :options="searchOptions"
            :loading="searchLoading"
            placeholder="搜索基金代码或名称..."
            size="large"
            clearable
            @select="handleSearchSelect"
            @update:value="handleSearchInput"
          >
            <template #prefix>
              <n-icon><IconSearch /></n-icon>
            </template>
          </n-auto-complete>
        </div>
        <!-- 统计数字直接放在 hero 底部 -->
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-value">{{ hotFundsCount }}</span>
            <span class="hero-stat-label">热门基金</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value positive">{{ upCount }}</span>
            <span class="hero-stat-label">今日上涨</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value negative">{{ downCount }}</span>
            <span class="hero-stat-label">今日下跌</span>
          </div>
          <div class="hero-stat-divider"></div>
          <div class="hero-stat">
            <span class="hero-stat-value">{{ favoritesCount }}</span>
            <span class="hero-stat-label">我的收藏</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 市场行情 - 横向滚动 -->
    <div class="market-section">
      <div class="section-header">
        <h2 class="section-title">
          <n-icon size="24"><IconChartDots3 /></n-icon>
          市场行情
        </h2>
        <n-button text type="primary" @click="router.push('/ranking')">
          查看更多 <n-icon><IconChevronRight /></n-icon>
        </n-button>
      </div>
      <n-spin :show="marketLoading">
        <div class="market-scroll">
          <div
            v-for="market in marketData"
            :key="market.marketCode"
            class="market-card card"
            @click="router.push(`/market/${market.marketCode}`)"
          >
            <div class="market-header">
              <span class="market-name">{{ getMarketName(market.marketCode) }}</span>
              <span class="market-code">{{ market.marketCode }}</span>
            </div>
            <div class="market-body">
              <div class="market-point">{{ market.currentPoint?.toFixed(2) }}</div>
              <div class="market-change" :class="market.changeRatio! >= 0 ? 'positive' : 'negative'">
                <n-icon size="16">
                  <IconTrendingUp v-if="market.changeRatio! >= 0" />
                  <IconTrendingDown v-else />
                </n-icon>
                {{ market.changeRatio! >= 0 ? '+' : '' }}{{ market.changeRatio?.toFixed(2) }}%
                <span class="change-value">({{ market.changePoint! >= 0 ? '+' : '' }}{{ market.changePoint?.toFixed(2) }})</span>
              </div>
            </div>
            <div class="market-chart" :ref="el => setMiniChartRef(el, market.marketCode)"></div>
            <div class="market-footer">
              <div class="market-detail">
                <span>开: {{ market.openPoint?.toFixed(2) }}</span>
                <span>高: {{ market.highPoint?.toFixed(2) }}</span>
                <span>低: {{ market.lowPoint?.toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </n-spin>
    </div>

    <div class="content-section">
      <div class="content-grid">
        <div class="hot-funds-section">
          <div class="section-header">
            <h2 class="section-title">
              <n-icon size="24"><IconFlame /></n-icon>
              热门基金
            </h2>
            <n-button text type="primary" @click="router.push('/ranking')">
              更多 <n-icon><IconChevronRight /></n-icon>
            </n-button>
          </div>
          <n-spin :show="hotFundsLoading">
            <div class="hot-funds-list">
              <div
                v-for="(fund, index) in hotFunds"
                :key="fund.fundCode"
                class="hot-fund-item card"
                @click="router.push(`/fund/${fund.fundCode}`)"
              >
                <div class="fund-rank" :class="getRankClass(index)">{{ index + 1 }}</div>
                <div class="fund-info">
                  <div class="fund-name">{{ fund.fundName }}</div>
                  <div class="fund-code">{{ fund.fundCode }} · {{ fund.fundType }}</div>
                </div>
                <div class="fund-growth" :class="fund.dayGrowth! >= 0 ? 'positive' : 'negative'">
                  {{ fund.dayGrowth! >= 0 ? '+' : '' }}{{ fund.dayGrowth?.toFixed(2) }}%
                </div>
              </div>
              <n-empty v-if="!hotFundsLoading && hotFunds.length === 0" description="暂无数据" />
            </div>
          </n-spin>
        </div>

        <div class="favorites-section" v-if="authStore.isLoggedIn">
          <div class="section-header">
            <h2 class="section-title">
              <n-icon size="24"><IconStar /></n-icon>
              我的收藏
            </h2>
            <n-button text type="primary" @click="router.push('/favorites')">
              全部 <n-icon><IconChevronRight /></n-icon>
            </n-button>
          </div>
          <n-spin :show="favoritesLoading">
            <div class="favorites-list">
              <div
                v-for="fav in favorites.slice(0, 5)"
                :key="fav.fundCode"
                class="favorite-item card"
                @click="router.push(`/fund/${fav.fundCode}`)"
              >
                <div class="fav-info">
                  <div class="fav-name">{{ fav.fundName }}</div>
                  <div class="fav-code">{{ fav.fundCode }}</div>
                </div>
                <div class="fav-data">
                  <div class="fav-nav">{{ fav.nav?.toFixed(4) || '--' }}</div>
                  <div class="fav-growth" :class="fav.dayGrowth! >= 0 ? 'positive' : 'negative'">
                    {{ fav.dayGrowth! >= 0 ? '+' : '' }}{{ fav.dayGrowth?.toFixed(2) || '--' }}%
                  </div>
                </div>
              </div>
              <n-empty v-if="!favoritesLoading && favorites.length === 0" description="暂无收藏" />
            </div>
          </n-spin>
        </div>
      </div>
    </div>

    <div class="news-section">
      <div class="section-header">
        <h2 class="section-title">
          <n-icon size="24"><IconNews /></n-icon>
          资讯动态
        </h2>
        <n-button text type="primary" @click="router.push('/news')">
          更多 <n-icon><IconChevronRight /></n-icon>
        </n-button>
      </div>
      <n-spin :show="newsLoading">
        <div class="news-grid">
          <div
            v-for="news in newsList.slice(0, 4)"
            :key="news.id"
            class="news-card card"
            @click="router.push(`/news/${news.id}`)"
          >
            <div class="news-content">
              <div class="news-title">{{ news.title }}</div>
              <div class="news-meta">
                <span v-if="news.source">{{ news.source }}</span>
                <span>{{ formatTime(news.publishTime) }}</span>
              </div>
            </div>
            <n-tag v-if="news.sentiment" :type="getSentimentType(news.sentiment)" size="small">
              {{ getSentimentLabel(news.sentiment) }}
            </n-tag>
          </div>
        </div>
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NAutoComplete, NIcon, NSpin, NEmpty, NButton, NTag, createDiscreteApi } from 'naive-ui'
import {
  IconSearch,
  IconTrendingUp,
  IconTrendingDown,
  IconStar,
  IconChartDots3,
  IconFlame,
  IconNews,
  IconChevronRight,
  IconChartBar
} from '@tabler/icons-vue'
import { useAuthStore } from '../stores/auth'
import { fundApi, favoriteApi, marketApi, newsApi } from '../api/fund'
import type { Fund, UserFavorite, MarketDataVO, FundNews } from '../types'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')
const searchOptions = ref<{ label: string; value: string }[]>([])
const searchLoading = ref(false)

const marketData = ref<MarketDataVO[]>([])
const marketLoading = ref(false)
const favorites = ref<UserFavorite[]>([])
const favoritesLoading = ref(false)
const hotFunds = ref<Fund[]>([])
const hotFundsLoading = ref(false)
const newsList = ref<FundNews[]>([])
const newsLoading = ref(false)

const miniChartRefs = new Map<string, any>()
const chartInstances = new Map<string, echarts.ECharts>()

const favoritesCount = computed(() => favorites.value.length)
const upCount = computed(() => hotFunds.value.filter(f => (f.dayGrowth || 0) >= 0).length)
const downCount = computed(() => hotFunds.value.filter(f => (f.dayGrowth || 0) < 0).length)
const hotFundsCount = computed(() => hotFunds.value.length)

const setMiniChartRef = (el: any, code: string) => {
  if (el) {
    miniChartRefs.set(code, el)
  }
}

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

const getRankClass = (index: number) => {
  if (index === 0) return 'gold'
  if (index === 1) return 'silver'
  if (index === 2) return 'bronze'
  return ''
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

const getSentimentType = (sentiment: string) => {
  if (sentiment === 'BULLISH') return 'success'
  if (sentiment === 'BEARISH') return 'error'
  return 'default'
}

const getSentimentLabel = (sentiment: string) => {
  if (sentiment === 'BULLISH') return '利好'
  if (sentiment === 'BEARISH') return '利空'
  return '中性'
}

const handleSearchInput = async (value: string) => {
  if (!value || value.length < 1) {
    searchOptions.value = []
    return
  }
  
  searchLoading.value = true
  try {
    const data = await fundApi.searchByKeyword(value, 10)
    searchOptions.value = data.map((f: Fund) => ({
      label: `${f.fundCode} - ${f.fundName}`,
      value: f.fundCode,
    }))
  } catch {
    searchOptions.value = []
  } finally {
    searchLoading.value = false
  }
}

const handleSearchSelect = (value: string) => {
  router.push(`/fund/${value}`)
}

const loadMarketData = async () => {
  marketLoading.value = true
  try {
    marketData.value = await marketApi.getMarketData() || []
    
    setTimeout(() => {
      marketData.value.forEach(m => {
        const el = miniChartRefs.get(m.marketCode)
        if (el) {
          initMiniChart(el, m)
        }
      })
    }, 100)
  } catch {
    message.error('加载市场数据失败')
  } finally {
    marketLoading.value = false
  }
}

const initMiniChart = (el: HTMLElement, market: MarketDataVO) => {
  if (chartInstances.has(market.marketCode)) {
    chartInstances.get(market.marketCode)?.dispose()
  }
  
  const chart = echarts.init(el)
  chartInstances.set(market.marketCode, chart)
  
  const isUp = (market.changeRatio || 0) >= 0
  const rawColor = isUp ? getComputedStyle(document.documentElement).getPropertyValue('--up-color').trim() : getComputedStyle(document.documentElement).getPropertyValue('--down-color').trim()
  const basePoint = market.currentPoint || 3000
  const changeRatio = market.changeRatio || 0
  
  const data = []
  const points = 30
  for (let i = 0; i < points; i++) {
    const progress = i / (points - 1)
    const trend = changeRatio * progress * 0.01 * basePoint
    const noise = (Math.random() - 0.5) * basePoint * 0.003
    const value = basePoint - trend * (points - 1 - i) / (points - 1) + noise
    data.push(value)
  }
  
  chart.setOption({
    grid: { left: 0, right: 0, top: 5, bottom: 5 },
    xAxis: { type: 'category', show: false, data: data.map((_, i) => i) },
    yAxis: { 
      type: 'value', 
      show: false,
      min: (value: { min: number; max: number }) => value.min * 0.999,
      max: (value: { min: number; max: number }) => value.max * 1.001
    },
    series: [{
      type: 'line',
      data: data,
      smooth: true,
      symbol: 'none',
      lineStyle: { width: 2, color: rawColor },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: `${rawColor}60` },
          { offset: 1, color: `${rawColor}05` },
        ]),
      },
    }],
  })
}

const loadFavorites = async () => {
  if (!authStore.isLoggedIn) return
  
  favoritesLoading.value = true
  try {
    favorites.value = await favoriteApi.getList() || []
  } catch {
    // ignore
  } finally {
    favoritesLoading.value = false
  }
}

const loadHotFunds = async () => {
  hotFundsLoading.value = true
  try {
    const recommendHot = await fundApi.getHotFunds(10)
    if (recommendHot?.length) {
      hotFunds.value = recommendHot as Fund[]
      return
    }

    const classicHot = await fundApi.getClassicHotFunds(10)
    if (classicHot?.length) {
      hotFunds.value = classicHot as Fund[]
      return
    }

    const result = await fundApi.getRanking('growth', 'day', 1, 10)
    hotFunds.value = result.records || []
  } catch {
    hotFunds.value = []
  } finally {
    hotFundsLoading.value = false
  }
}

const loadNews = async () => {
  newsLoading.value = true
  try {
    const result = await newsApi.getList(1, 8)
    newsList.value = result.records || []
  } catch {
    // ignore
  } finally {
    newsLoading.value = false
  }
}

onMounted(() => {
  loadMarketData()
  loadFavorites()
  loadHotFunds()
  loadNews()
})
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* Hero 区域 */
.hero-section {
  position: relative;
  border-radius: var(--radius-xl);
  padding: 56px 40px 40px;
  margin-bottom: 28px;
  color: white;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #18181B 0%, #27272A 40%, #3F3F46 100%);
  z-index: 0;
}

.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 70% 30%, rgba(212, 168, 67, 0.25) 0%, transparent 50%),
    radial-gradient(ellipse at 10% 90%, rgba(212, 168, 67, 0.1) 0%, transparent 40%);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 640px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 38px;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: 0.02em;
}

.hero-subtitle {
  font-size: 15px;
  opacity: 0.8;
  margin-bottom: 32px;
  letter-spacing: 0.06em;
}

.search-box {
  padding: 8px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
  margin-bottom: 36px;
}

/* Hero 内嵌统计 */
.hero-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.hero-stat-value {
  font-size: 26px;
  font-weight: 700;
  font-family: var(--font-number);
  line-height: 1.2;
}

.hero-stat-value.positive { color: var(--up-color); }
.hero-stat-value.negative { color: var(--down-color); }

.hero-stat-label {
  font-size: 12px;
  opacity: 0.7;
  letter-spacing: 0.04em;
}

.hero-stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.15);
}

@media (max-width: 600px) {
  .hero-section { padding: 36px 20px 28px; }
  .hero-title { font-size: 26px; }
  .hero-stats { gap: 16px; }
  .hero-stat-value { font-size: 20px; }
}

.section-header {
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

.market-section {
  margin-bottom: 28px;
}

.market-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-snap-type: x mandatory;
}

.market-scroll::-webkit-scrollbar {
  height: 4px;
}

.market-scroll::-webkit-scrollbar-thumb {
  background: var(--text-tertiary);
  border-radius: 4px;
}

.market-card {
  min-width: 280px;
  flex-shrink: 0;
  padding: 20px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  background: var(--gradient-card);
  scroll-snap-align: start;
  transition: all var(--transition-base);
}

.market-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.market-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.market-name {
  font-size: 16px;
  font-weight: 600;
}

.market-code {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.market-body {
  margin-bottom: 12px;
}

.market-point {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  font-family: var(--font-number);
  letter-spacing: 0.02em;
}

.market-change {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.market-change.positive {
  color: var(--up-color);
}

.market-change.negative {
  color: var(--down-color);
}

.change-value {
  font-size: 13px;
  opacity: 0.8;
}

.market-chart {
  height: 60px;
  width: 100%;
  margin-bottom: 12px;
}

.market-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.market-detail {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}

.content-section {
  margin-bottom: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

@media (max-width: 1000px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

.hot-funds-list, .favorites-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-fund-item, .favorite-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  cursor: pointer;
}

.fund-rank {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.fund-rank.gold {
  background: #fbbf24;
  color: white;
}

.fund-rank.silver {
  background: #9ca3af;
  color: white;
}

.fund-rank.bronze {
  background: #d97706;
  color: white;
}

.fund-info {
  flex: 1;
  min-width: 0;
}

.fund-name {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fund-code {
  font-size: 12px;
  color: var(--text-secondary);
}

.fund-growth {
  font-size: 16px;
  font-weight: 600;
}

.fund-growth.positive, .fav-growth.positive {
  color: var(--up-color);
}

.fund-growth.negative, .fav-growth.negative {
  color: var(--down-color);
}

.favorite-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fav-info {
  min-width: 0;
}

.fav-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.fav-code {
  font-size: 12px;
  color: var(--text-secondary);
}

.fav-data {
  text-align: right;
}

.fav-nav {
  font-size: 14px;
  font-weight: 500;
}

.fav-growth {
  font-size: 13px;
}

.news-section {
  margin-bottom: 24px;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@media (max-width: 800px) {
  .news-grid {
    grid-template-columns: 1fr;
  }
}

.news-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  cursor: pointer;
  gap: 12px;
}

.news-content {
  flex: 1;
  min-width: 0;
}

.news-title {
  font-size: 15px;
  font-weight: 500;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  gap: 12px;
}
</style>
