<template>
  <div class="home-container">
    <div class="hero-section">
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
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-auto-complete>
        </div>
      </div>
    </div>

    <div class="stats-section">
      <div class="stats-grid">
        <div class="stat-card card">
          <div class="stat-icon stat-icon--primary">
            <n-icon size="28"><TrendingUpOutline /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ hotFundsCount }}</div>
            <div class="stat-label">热门基金</div>
          </div>
        </div>
        <div class="stat-card card">
          <div class="stat-icon stat-icon--success">
            <n-icon size="28"><StatsChartOutline /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ upCount }}</div>
            <div class="stat-label">今日上涨</div>
          </div>
        </div>
        <div class="stat-card card">
          <div class="stat-icon stat-icon--danger">
            <n-icon size="28"><TrendingDownOutline /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ downCount }}</div>
            <div class="stat-label">今日下跌</div>
          </div>
        </div>
        <div class="stat-card card">
          <div class="stat-icon stat-icon--purple">
            <n-icon size="28"><StarOutline /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ favoritesCount }}</div>
            <div class="stat-label">我的收藏</div>
          </div>
        </div>
      </div>
    </div>

    <div class="market-section">
      <div class="section-header">
        <h2 class="section-title">
          <n-icon size="24"><AnalyticsOutline /></n-icon>
          市场行情
        </h2>
        <n-button text type="primary" @click="router.push('/ranking')">
          查看更多 <n-icon><ChevronForwardOutline /></n-icon>
        </n-button>
      </div>
      <n-spin :show="marketLoading">
        <div class="market-grid">
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
                  <TrendingUpOutline v-if="market.changeRatio! >= 0" />
                  <TrendingDownOutline v-else />
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
              <n-icon size="24"><FlameOutline /></n-icon>
              热门基金
            </h2>
            <n-button text type="primary" @click="router.push('/ranking')">
              更多 <n-icon><ChevronForwardOutline /></n-icon>
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
              <n-icon size="24"><StarOutline /></n-icon>
              我的收藏
            </h2>
            <n-button text type="primary" @click="router.push('/favorites')">
              全部 <n-icon><ChevronForwardOutline /></n-icon>
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
          <n-icon size="24"><NewspaperOutline /></n-icon>
          资讯动态
        </h2>
        <n-button text type="primary" @click="router.push('/news')">
          更多 <n-icon><ChevronForwardOutline /></n-icon>
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
  SearchOutline, 
  TrendingUpOutline, 
  TrendingDownOutline,
  StarOutline,
  AnalyticsOutline,
  FlameOutline,
  NewspaperOutline,
  ChevronForwardOutline,
  StatsChartOutline
} from '@vicons/ionicons5'
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
  background:
    radial-gradient(circle at 100% 0, rgba(0, 183, 201, 0.08), transparent 40%),
    radial-gradient(circle at 0 5%, rgba(6, 58, 102, 0.08), transparent 45%);
  border-radius: var(--radius-xl);
}

.hero-section {
  background:
    linear-gradient(130deg, rgba(6, 58, 102, 0.96), rgba(14, 94, 140, 0.88)),
    var(--gradient-brand);
  border-radius: var(--radius-xl);
  padding: 48px 32px;
  margin-bottom: 24px;
  color: white;
  border: 1px solid rgba(52, 204, 219, 0.25);
  box-shadow: var(--shadow-xl);
}

.hero-content {
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
}

.hero-subtitle {
  font-size: 15px;
  opacity: 0.86;
  margin-bottom: 32px;
  letter-spacing: 0.04em;
}

.search-box {
  padding: 8px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.24);
  backdrop-filter: blur(10px);
}

.stats-section {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1000px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid var(--border-color);
  background: var(--gradient-card);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon--primary {
  background: var(--primary-color);
}

.stat-icon--success {
  background: var(--success-color);
}

.stat-icon--danger {
  background: var(--danger-color);
}

.stat-icon--purple {
  background: var(--info-color);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-number);
  letter-spacing: 0.02em;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
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
  margin-bottom: 24px;
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1000px) {
  .market-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .market-grid {
    grid-template-columns: 1fr;
  }
}

.market-card {
  padding: 20px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  background: var(--gradient-card);
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
