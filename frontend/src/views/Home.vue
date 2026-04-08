<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="hero__backdrop">
        <canvas ref="heroCanvas" class="hero__canvas"></canvas>
        <div class="hero__orb hero__orb--1"></div>
        <div class="hero__orb hero__orb--2"></div>
      </div>
      <div class="hero__content">
        <h1 class="hero__title">
          <span class="hero__title-line">智能基金投资</span>
          <span class="hero__title-line hero__title-line--gold">分析平台</span>
        </h1>
        <p class="hero__subtitle">专业的数据分析与投资决策工具</p>
        <div class="hero__search">
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
        <div class="hero__stats">
          <div class="hero__stat">
            <span class="hero__stat-val">{{ hotFundsCount }}</span>
            <span class="hero__stat-label">热门基金</span>
          </div>
          <div class="hero__stat-sep"></div>
          <div class="hero__stat">
            <span class="hero__stat-val hero__stat-val--up">{{ upCount }}</span>
            <span class="hero__stat-label">今日上涨</span>
          </div>
          <div class="hero__stat-sep"></div>
          <div class="hero__stat">
            <span class="hero__stat-val hero__stat-val--down">{{ downCount }}</span>
            <span class="hero__stat-label">今日下跌</span>
          </div>
          <div class="hero__stat-sep"></div>
          <div class="hero__stat">
            <span class="hero__stat-val">{{ favoritesCount }}</span>
            <span class="hero__stat-label">我的收藏</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Market -->
    <section class="section">
      <div class="section__head">
        <h2 class="section__title">
          <n-icon size="20"><IconChartDots3 /></n-icon>
          市场行情
        </h2>
        <n-button text type="primary" @click="router.push('/ranking')">
          更多 <n-icon><IconChevronRight /></n-icon>
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
            <div class="market-card__head">
              <span class="market-card__name">{{ getMarketName(market.marketCode) }}</span>
              <span class="market-card__code">{{ market.marketCode }}</span>
            </div>
            <div class="market-card__body">
              <div class="market-card__point">{{ market.currentPoint?.toFixed(2) }}</div>
              <div class="market-card__change" :class="market.changeRatio! >= 0 ? 'up' : 'down'">
                <n-icon size="14">
                  <IconTrendingUp v-if="market.changeRatio! >= 0" />
                  <IconTrendingDown v-else />
                </n-icon>
                {{ market.changeRatio! >= 0 ? '+' : '' }}{{ market.changeRatio?.toFixed(2) }}%
                <span class="market-card__change-val">({{ market.changePoint! >= 0 ? '+' : '' }}{{ market.changePoint?.toFixed(2) }})</span>
              </div>
            </div>
            <div class="market-card__chart" :ref="el => setMiniChartRef(el, market.marketCode)"></div>
            <div class="market-card__footer">
              <span>开 {{ market.openPoint?.toFixed(2) }}</span>
              <span>高 {{ market.highPoint?.toFixed(2) }}</span>
              <span>低 {{ market.lowPoint?.toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </n-spin>
    </section>

    <!-- Hot funds + Favorites -->
    <section class="section">
      <div class="content-grid">
        <div>
          <div class="section__head">
            <h2 class="section__title">
              <n-icon size="20"><IconFlame /></n-icon>
              热门基金
            </h2>
            <n-button text type="primary" @click="router.push('/ranking')">
              更多 <n-icon><IconChevronRight /></n-icon>
            </n-button>
          </div>
          <n-spin :show="hotFundsLoading">
            <div class="fund-list">
              <div
                v-for="(fund, index) in hotFunds"
                :key="fund.fundCode"
                class="fund-item card card--interactive"
                @click="router.push(`/fund/${fund.fundCode}`)"
              >
                <div class="fund-item__rank" :class="getRankClass(index)">{{ index + 1 }}</div>
                <div class="fund-item__info">
                  <div class="fund-item__name">{{ fund.fundName }}</div>
                  <div class="fund-item__meta">{{ fund.fundCode }} · {{ fund.fundType }}</div>
                </div>
                <div class="fund-item__growth" :class="fund.dayGrowth! >= 0 ? 'up' : 'down'">
                  {{ fund.dayGrowth! >= 0 ? '+' : '' }}{{ fund.dayGrowth?.toFixed(2) }}%
                </div>
              </div>
              <n-empty v-if="!hotFundsLoading && hotFunds.length === 0" description="暂无数据" />
            </div>
          </n-spin>
        </div>

        <div v-if="authStore.isLoggedIn">
          <div class="section__head">
            <h2 class="section__title">
              <n-icon size="20"><IconStar /></n-icon>
              我的收藏
            </h2>
            <n-button text type="primary" @click="router.push('/favorites')">
              全部 <n-icon><IconChevronRight /></n-icon>
            </n-button>
          </div>
          <n-spin :show="favoritesLoading">
            <div class="fund-list">
              <div
                v-for="fav in favorites.slice(0, 5)"
                :key="fav.fundCode"
                class="fav-item card card--interactive"
                @click="router.push(`/fund/${fav.fundCode}`)"
              >
                <div class="fav-item__info">
                  <div class="fav-item__name">{{ fav.fundName }}</div>
                  <div class="fav-item__code">{{ fav.fundCode }}</div>
                </div>
                <div class="fav-item__data">
                  <div class="fav-item__nav">{{ fav.nav?.toFixed(4) || '--' }}</div>
                  <div class="fav-item__growth" :class="fav.dayGrowth! >= 0 ? 'up' : 'down'">
                    {{ fav.dayGrowth! >= 0 ? '+' : '' }}{{ fav.dayGrowth?.toFixed(2) || '--' }}%
                  </div>
                </div>
              </div>
              <n-empty v-if="!favoritesLoading && favorites.length === 0" description="暂无收藏" />
            </div>
          </n-spin>
        </div>
      </div>
    </section>

    <!-- News -->
    <section class="section">
      <div class="section__head">
        <h2 class="section__title">
          <n-icon size="20"><IconNews /></n-icon>
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
            class="news-card card card--interactive"
            @click="router.push(`/news/${news.id}`)"
          >
            <div class="news-card__content">
              <div class="news-card__title">{{ news.title }}</div>
              <div class="news-card__meta">
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
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NAutoComplete, NIcon, NSpin, NEmpty, NButton, NTag, createDiscreteApi } from 'naive-ui'
import {
  IconSearch, IconTrendingUp, IconTrendingDown, IconStar,
  IconChartDots3, IconFlame, IconNews, IconChevronRight,
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

const heroCanvas = ref<HTMLCanvasElement>()
let heroAnimId: number | null = null

interface HeroParticle {
  x: number; y: number; vx: number; vy: number;
  radius: number; alpha: number; alphaSpeed: number;
}

const initHeroParticles = () => {
  const canvas = heroCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const parent = canvas.parentElement!
  let w = parent.clientWidth, h = parent.clientHeight
  canvas.width = w; canvas.height = h
  const particles: HeroParticle[] = Array.from({ length: 40 }, () => ({
    x: Math.random() * w, y: Math.random() * h,
    vx: (Math.random() - 0.5) * 0.25, vy: (Math.random() - 0.5) * 0.25,
    radius: Math.random() * 1.5 + 0.5,
    alpha: Math.random() * 0.4 + 0.1,
    alphaSpeed: (Math.random() - 0.5) * 0.003,
  }))
  const maxDist = 90
  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < maxDist) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(201, 169, 110, ${(1 - dist / maxDist) * 0.08})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    for (const p of particles) {
      p.x += p.vx; p.y += p.vy
      p.alpha += p.alphaSpeed
      if (p.alpha <= 0.1 || p.alpha >= 0.5) p.alphaSpeed *= -1
      p.alpha = Math.max(0.1, Math.min(0.5, p.alpha))
      if (p.x < 0) p.x = w; if (p.x > w) p.x = 0
      if (p.y < 0) p.y = h; if (p.y > h) p.y = 0
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(201, 169, 110, ${p.alpha})`
      ctx.fill()
    }
    heroAnimId = requestAnimationFrame(draw)
  }
  draw()
  const onResize = () => { w = parent.clientWidth; h = parent.clientHeight; canvas.width = w; canvas.height = h }
  window.addEventListener('resize', onResize)
  return () => window.removeEventListener('resize', onResize)
}

const setMiniChartRef = (el: any, code: string) => { if (el) miniChartRefs.set(code, el) }

const getMarketName = (code: string) => {
  const names: Record<string, string> = {
    'sh000001': '上证指数', 'sz399001': '深证成指', 'sz399006': '创业板指', 'sh000688': '科创50',
    '000001': '上证指数', '399001': '深证成指', '399006': '创业板指', '000688': '科创50',
  }
  return names[code] || code
}

const getRankClass = (index: number) => {
  if (index === 0) return 'gold'
  if (index === 1) return 'silver'
  if (index === 2) return 'bronze'
  return ''
}

const formatTime = (time?: string) => time ? dayjs(time).format('MM-DD HH:mm') : ''
const getSentimentType = (s: string) => s === 'BULLISH' ? 'success' : s === 'BEARISH' ? 'error' : 'default'
const getSentimentLabel = (s: string) => s === 'BULLISH' ? '利好' : s === 'BEARISH' ? '利空' : '中性'

const handleSearchInput = async (value: string) => {
  if (!value || value.length < 1) { searchOptions.value = []; return }
  searchLoading.value = true
  try {
    const data = await fundApi.searchByKeyword(value, 10)
    searchOptions.value = data.map((f: Fund) => ({ label: `${f.fundCode} - ${f.fundName}`, value: f.fundCode }))
  } catch { searchOptions.value = [] }
  finally { searchLoading.value = false }
}

const handleSearchSelect = (value: string) => { router.push(`/fund/${value}`) }

const loadMarketData = async () => {
  marketLoading.value = true
  try {
    marketData.value = await marketApi.getMarketData() || []
    // 并行加载每个大盘的当日分时数据
    const trendsPromises = marketData.value.map(async (m) => {
      try {
        const trends = await marketApi.getMarketTrends(m.marketCode)
        return { code: m.marketCode, trends: trends || [] }
      } catch {
        return { code: m.marketCode, trends: [] }
      }
    })
    const allTrends = await Promise.all(trendsPromises)
    const trendsMap = new Map(allTrends.map(h => [h.code, h.trends]))

    setTimeout(() => {
      marketData.value.forEach(m => {
        const el = miniChartRefs.get(m.marketCode)
        const trends = trendsMap.get(m.marketCode) || []
        if (el) initMiniChart(el, m, trends)
      })
    }, 100)
  } catch { message.error('加载市场数据失败') }
  finally { marketLoading.value = false }
}

const initMiniChart = (el: HTMLElement, market: MarketDataVO, trends: any[]) => {
  if (chartInstances.has(market.marketCode)) chartInstances.get(market.marketCode)?.dispose()
  const chart = echarts.init(el)
  chartInstances.set(market.marketCode, chart)

  // 使用分时数据（当日分钟级价格），无数据时 fallback
  const data = trends.length > 0
    ? trends.map((item: any) => Number(item.price))
    : (() => {
        const arr: number[] = []
        const base = market.currentPoint || 3000
        for (let i = 0; i < 30; i++) {
          const noise = (Math.random() - 0.5) * base * 0.003
          arr.push(base + noise)
        }
        return arr
      })()

  // 以昨收为基准判断涨跌
  const basePrice = market.prevClose || data[0]
  const lastVal = data[data.length - 1]
  const isUp = lastVal >= basePrice
  const rawColor = isUp
    ? getComputedStyle(document.documentElement).getPropertyValue('--up-color').trim()
    : getComputedStyle(document.documentElement).getPropertyValue('--down-color').trim()

  chart.setOption({
    grid: { left: 0, right: 0, top: 5, bottom: 5 },
    xAxis: { type: 'category', show: false, data: data.map((_, i) => i) },
    yAxis: { type: 'value', show: false, min: (v: { min: number; max: number }) => v.min * 0.999, max: (v: { min: number; max: number }) => v.max * 1.001 },
    series: [{
      type: 'line', data, smooth: false, symbol: 'none',
      lineStyle: { width: 1.5, color: rawColor },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: `${rawColor}40` }, { offset: 1, color: `${rawColor}05` }]) },
    }],
  })
}

const loadFavorites = async () => {
  if (!authStore.isLoggedIn) return
  favoritesLoading.value = true
  try { favorites.value = await favoriteApi.getList() || [] }
  catch { /* ignore */ }
  finally { favoritesLoading.value = false }
}

const loadHotFunds = async () => {
  hotFundsLoading.value = true
  try {
    const recommendHot = await fundApi.getHotFunds(10)
    if (recommendHot?.length) { hotFunds.value = recommendHot as Fund[]; return }
    const classicHot = await fundApi.getClassicHotFunds(10)
    if (classicHot?.length) { hotFunds.value = classicHot as Fund[]; return }
    const result = await fundApi.getRanking('growth', 'day', 1, 10)
    hotFunds.value = result.records || []
  } catch { hotFunds.value = [] }
  finally { hotFundsLoading.value = false }
}

const loadNews = async () => {
  newsLoading.value = true
  try { const result = await newsApi.getList(1, 8); newsList.value = result.records || [] }
  catch { /* ignore */ }
  finally { newsLoading.value = false }
}

let cleanupResize: (() => void) | undefined

onMounted(() => {
  loadMarketData(); loadFavorites(); loadHotFunds(); loadNews()
  cleanupResize = initHeroParticles()
})

onUnmounted(() => {
  if (heroAnimId !== null) cancelAnimationFrame(heroAnimId)
  if (cleanupResize) cleanupResize()
  chartInstances.forEach(chart => chart.dispose())
  chartInstances.clear()
})
</script>

<style scoped>
.home { max-width: 1400px; margin: 0 auto; }

/* ═══ HERO ═══ */
.hero {
  position: relative;
  border-radius: var(--radius-2xl);
  padding: 56px 40px 40px;
  margin-bottom: 32px;
  color: white;
  overflow: hidden;
}

.hero__backdrop {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #050507 0%, #0A0A0D 35%, #111115 70%);
  z-index: 0;
}

.hero__backdrop::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 75% 25%, rgba(201, 169, 110, 0.2) 0%, transparent 50%),
    radial-gradient(ellipse at 10% 85%, rgba(201, 169, 110, 0.12) 0%, transparent 45%);
}

.hero__canvas { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 1; pointer-events: none; }

.hero__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  z-index: 0;
  pointer-events: none;
}

.hero__orb--1 {
  width: 240px; height: 240px;
  background: radial-gradient(circle, rgba(201, 169, 110, 0.25) 0%, transparent 70%);
  top: -60px; right: 10%;
  animation: orbDrift1 7s ease-in-out infinite;
}

.hero__orb--2 {
  width: 180px; height: 180px;
  background: radial-gradient(circle, rgba(176, 141, 78, 0.18) 0%, transparent 70%);
  bottom: -40px; left: 8%;
  animation: orbDrift2 9s ease-in-out infinite;
}

@keyframes orbDrift1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, 25px); }
}

@keyframes orbDrift2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(25px, -15px); }
}

.hero__content {
  position: relative;
  z-index: 2;
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
}

.hero__title { margin: 0 0 10px; line-height: 1.15; }

.hero__title-line {
  display: block;
  font-family: var(--font-display);
  font-size: 40px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.hero__title-line--gold {
  background: var(--gradient-gold);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero__subtitle {
  font-size: 14px;
  opacity: 0.5;
  margin: 0 0 32px;
  letter-spacing: 0.08em;
}

.hero__search {
  padding: 6px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  margin-bottom: 32px;
}

.hero__search :deep(.n-input) {
  background: rgba(255, 255, 255, 0.08) !important;
  border: none !important;
  color: white !important;
  --n-border: none !important;
}

.hero__search :deep(.n-input .n-input__input-el) {
  color: white !important;
  caret-color: var(--primary-color) !important;
}

.hero__search :deep(.n-input .n-input__input-el::placeholder) {
  color: rgba(255, 255, 255, 0.35) !important;
}

.hero__stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 28px;
}

.hero__stat { display: flex; flex-direction: column; align-items: center; gap: 4px; }

.hero__stat-val {
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-mono);
  line-height: 1.2;
}

.hero__stat-val--up { color: var(--up-color); }
.hero__stat-val--down { color: var(--down-color); }
.hero__stat-label { font-size: 11px; opacity: 0.45; letter-spacing: 0.06em; }
.hero__stat-sep { width: 1px; height: 32px; background: rgba(255, 255, 255, 0.1); }

@media (max-width: 600px) {
  .hero { padding: 36px 20px 28px; }
  .hero__title-line { font-size: 28px; }
  .hero__stats { gap: 14px; }
  .hero__stat-val { font-size: 18px; }
}

/* ═══ SECTIONS ═══ */
.section { margin-bottom: 28px; }

.section__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section__title {
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

/* ═══ MARKET ═══ */
.market-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.market-card {
  padding: 20px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.market-card:hover { transform: translateY(-2px); }

.market-card__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.market-card__name { font-size: 15px; font-weight: 600; }
.market-card__code { font-size: 11px; color: var(--text-tertiary); background: var(--bg-tertiary); padding: 2px 8px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); }

.market-card__body { margin-bottom: 10px; }
.market-card__point { font-size: 26px; font-weight: 700; margin-bottom: 6px; font-family: var(--font-mono); letter-spacing: 0.02em; }
.market-card__change { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 4px; }
.market-card__change.up { color: var(--up-color); }
.market-card__change.down { color: var(--down-color); }
.market-card__change-val { font-size: 12px; opacity: 0.7; }

.market-card__chart { height: 50px; width: 100%; margin-bottom: 10px; }

.market-card__footer {
  border-top: 1px solid var(--border-color);
  padding-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

@media (max-width: 1000px) { .market-grid { grid-template-columns: 1fr; } }

/* ═══ CONTENT GRID ═══ */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

@media (max-width: 1000px) { .content-grid { grid-template-columns: 1fr; } }

/* ═══ FUND LIST ═══ */
.fund-list { display: flex; flex-direction: column; gap: 8px; }

.fund-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
}

.fund-item__rank {
  width: 26px; height: 26px;
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 600; font-size: 13px;
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.fund-item__rank.gold { background: var(--gradient-gold); color: #0A0A0D; }
.fund-item__rank.silver { background: linear-gradient(135deg, #94A3B8, #CBD5E1); color: #0A0A0D; }
.fund-item__rank.bronze { background: linear-gradient(135deg, #B08D4E, #C9A96E); color: #0A0A0D; }

.fund-item__info { flex: 1; min-width: 0; }
.fund-item__name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fund-item__meta { font-size: 12px; color: var(--text-tertiary); }
.fund-item__growth { font-size: 15px; font-weight: 600; font-family: var(--font-mono); }
.fund-item__growth.up { color: var(--up-color); }
.fund-item__growth.down { color: var(--down-color); }

/* ═══ FAVORITE ITEM ═══ */
.fav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
}

.fav-item__name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 140px; }
.fav-item__code { font-size: 12px; color: var(--text-tertiary); }
.fav-item__data { text-align: right; }
.fav-item__nav { font-size: 14px; font-weight: 500; font-family: var(--font-mono); }
.fav-item__growth { font-size: 13px; font-family: var(--font-mono); }
.fav-item__growth.up { color: var(--up-color); }
.fav-item__growth.down { color: var(--down-color); }

/* ═══ NEWS ═══ */
.news-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
@media (max-width: 800px) { .news-grid { grid-template-columns: 1fr; } }

.news-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  cursor: pointer;
  gap: 12px;
}

.news-card__content { flex: 1; min-width: 0; }
.news-card__title {
  font-size: 14px; font-weight: 500; line-height: 1.5; margin-bottom: 8px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.news-card__meta { font-size: 12px; color: var(--text-tertiary); display: flex; gap: 12px; }
</style>
