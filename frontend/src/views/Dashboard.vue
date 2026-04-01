<template>
  <div class="dashboard-page page-container">
    <PageHeader title="投资概览" icon="📈">
      <template #actions>
        <div class="header-actions">
          <div class="realtime-indicator">
            <span class="pulse-dot"></span>
            实时更新
          </div>
          <n-button @click="refreshData" :loading="refreshing">
            <template #icon>
              <n-icon><IconRefresh /></n-icon>
            </template>
            刷新
          </n-button>
        </div>
      </template>
    </PageHeader>

    <Skeleton v-if="initialLoading" type="stat" />
    
    <div v-else class="dashboard-grid">
      <StatCard
        label="总资产"
        :value="portfolio?.currentValue || 0"
        prefix="¥"
        icon="AUM"
        :change="portfolio?.totalReturn"
        trend-label="总收益率"
        variant="primary"
      />
      
      <StatCard
        label="今日收益"
        :value="portfolio?.dayProfit || 0"
        prefix="¥"
        icon="DPNL"
        :change="portfolio?.dayReturn"
        :change-type="portfolio?.dayReturn >= 0 ? 'up' : 'down'"
        show-sign
      />
      
      <StatCard
        label="昨日收益"
        :value="portfolio?.yesterdayProfit || 0"
        prefix="¥"
        icon="YPNL"
        :change="portfolio?.yesterdayReturn"
        :change-type="portfolio?.yesterdayReturn >= 0 ? 'up' : 'down'"
        show-sign
      />
      
      <StatCard
        label="持仓基金"
        :value="portfolio?.fundCount || 0"
        icon="POS"
        clickable
        @click="router.push('/portfolio')"
      />
      
      <StatCard
        label="预警通知"
        :value="unreadAlertCount"
        icon="ALT"
        clickable
        variant="danger"
        @click="router.push('/alerts')"
      />
    </div>

    <div class="main-content">
      <div class="left-panel">
        <n-card title="资产配置" class="chart-card card--elevated">
          <template #header-extra>
            <n-button text @click="router.push('/portfolio')">
              详情
            </n-button>
          </template>
          <Skeleton v-if="loading" type="chart" />
          <div v-else-if="portfolio?.allocations?.length" class="chart-container" ref="allocationChartRef"></div>
          <n-empty v-else description="暂无持仓数据">
            <template #extra>
              <n-button type="primary" @click="router.push('/portfolio')">
                创建组合
              </n-button>
            </template>
          </n-empty>
        </n-card>

        <n-card title="持仓明细" class="holdings-card card--elevated">
          <template #header-extra>
            <n-button text @click="router.push('/portfolio')">
              管理
            </n-button>
          </template>
          <Skeleton v-if="loading" type="table" :rows="5" :columns="3" />
          <n-data-table
            v-else-if="portfolio?.items?.length"
            :columns="holdingColumns"
            :data="portfolio.items"
            :bordered="false"
            size="small"
          />
          <n-empty v-else description="暂无持仓" />
        </n-card>
      </div>

      <div class="right-panel">
        <n-card title="AI 投资助手" class="ai-card card--gradient">
          <div class="ai-quick-questions">
            <n-tag
              v-for="q in quickQuestions"
              :key="q"
              class="quick-question"
              @click="askAI(q)"
              style="cursor: pointer;"
            >
              {{ q }}
            </n-tag>
          </div>
          <n-input
            v-model:value="aiQuestion"
            type="textarea"
            placeholder="输入您的投资问题..."
            :autosize="{ minRows: 2, maxRows: 4 }"
            @keydown.enter.ctrl="askAI()"
          />
          <div style="margin-top: 12px; text-align: right;">
            <n-button type="primary" @click="askAI()" :loading="aiLoading">
              <template #icon>
                <n-icon><IconMessage /></n-icon>
              </template>
              提问
            </n-button>
          </div>
        </n-card>

        <n-card title="预警中心" class="alert-card card--elevated">
          <template #header-extra>
            <n-button text @click="router.push('/alerts')">
              全部
            </n-button>
          </template>
          <Skeleton v-if="loading" type="list" :rows="3" />
          <div v-else-if="recentAlerts.length" class="alert-list">
            <div
              v-for="alert in recentAlerts.slice(0, 5)"
              :key="alert.id"
              class="alert-item"
              :class="{ unread: !alert.isRead }"
            >
              <div class="alert-icon" :class="getAlertIconClass(alert.alertType)">
                <n-icon>
                  <IconAlertCircle v-if="alert.alertType === 'GROWTH'" />
                  <IconNews v-else-if="alert.alertType === 'NEWS'" />
                  <IconBell v-else />
                </n-icon>
              </div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.alertTitle }}</div>
                <div class="alert-message">{{ alert.alertMessage }}</div>
                <div class="alert-time">{{ formatTime(alert.triggeredTime) }}</div>
              </div>
            </div>
          </div>
          <n-empty v-else description="暂无预警" />
        </n-card>
      </div>
    </div>

    <!-- 智能推荐 - 全宽横向滚动 -->
    <n-card class="recommend-card card--elevated">
      <template #header>
        <div class="section-header">
          <span class="section-icon">✨</span>
          <span class="section-title">智能推荐</span>
        </div>
      </template>
      <template #header-extra>
        <n-button text @click="router.push('/recommend')">
          查看更多
          <template #icon><n-icon><IconChevronRight /></n-icon></template>
        </n-button>
      </template>
      <n-spin :show="recommendLoading">
        <div class="recommend-scroll">
          <div
            v-for="fund in recommendFunds"
            :key="fund.fundCode"
            class="fund-card"
            @click="router.push(`/fund/${fund.fundCode}`)"
          >
            <div class="fund-card-top">
              <div class="fund-info">
                <div class="fund-name">{{ fund.fundName }}</div>
                <div class="fund-code">{{ fund.fundCode }}</div>
              </div>
              <n-tag size="small" :type="getRiskType(fund.riskLevel)" round>
                {{ getRiskText(fund.riskLevel) }}
              </n-tag>
            </div>
            <div class="fund-card-bottom">
              <div class="fund-metrics">
                <div class="metric">
                  <span class="metric-label">最新净值</span>
                  <span class="metric-value">{{ fund.nav?.toFixed(4) || '-' }}</span>
                </div>
                <div class="metric">
                  <span class="metric-label">日涨跌</span>
                  <span class="metric-value growth">
                    <GrowthText :value="fund.dayGrowth" />
                  </span>
                </div>
                <div class="metric">
                  <span class="metric-label">近一年</span>
                  <span class="metric-value growth">
                    <GrowthText :value="fund.yearGrowth" />
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </n-spin>
    </n-card>

    <!-- 底部并排：我的收藏 + 市场热点 -->
    <div class="bottom-section">
      <n-card title="我的收藏" class="card--elevated">
        <template #header-extra>
          <n-button text @click="router.push('/favorites')">
            全部
          </n-button>
        </template>
        <n-spin :show="favoritesLoading">
          <div v-if="topFavorites.length" class="favorites-list">
            <div
              v-for="fav in topFavorites.slice(0, 5)"
              :key="fav.fundCode"
              class="favorite-item hover-lift"
              @click="router.push(`/fund/${fav.fundCode}`)"
            >
              <div class="fav-info">
                <div class="fav-name">{{ fav.fundName }}</div>
                <div class="fav-code">{{ fav.fundCode }}</div>
              </div>
              <div class="fav-growth">
                <GrowthText :value="fav.dayGrowth" />
              </div>
            </div>
          </div>
          <n-empty v-else description="暂无收藏" />
        </n-spin>
      </n-card>

      <n-card class="card--elevated">
        <template #header>
          <div class="section-header">
            <span class="section-icon">📰</span>
            <span class="section-title">市场热点</span>
          </div>
        </template>
        <template #header-extra>
          <n-button text @click="router.push('/news')">
            更多
          </n-button>
        </template>
        <n-spin :show="newsLoading">
          <div class="news-list">
            <div
              v-for="news in hotNews"
              :key="news.id"
              class="news-item"
              @click="router.push(`/news/${news.id}`)"
            >
              <div class="news-title">{{ news.title }}</div>
              <div class="news-meta">
                <span>{{ news.source }}</span>
                <span>{{ formatTime(news.publishTime) }}</span>
              </div>
            </div>
          </div>
        </n-spin>
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, h } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  NCard, NButton, NIcon, NTag, NInput, NDataTable, NEmpty, NSpin,
  useMessage
} from 'naive-ui'
import {
  IconRefresh,
  IconBell, IconMessage,
  IconAlertCircle, IconNews, IconChevronRight
} from '@tabler/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import StatCard from '../components/StatCard.vue'
import GrowthText from '../components/GrowthText.vue'
import Skeleton from '../components/Skeleton.vue'
import * as echarts from 'echarts'
import { fundApi } from '@/api/fund'
import type { PortfolioVO, AlertHistoryVO, Fund } from '@/types'

const router = useRouter()
const authStore = useAuthStore()
const message = useMessage()

const loading = ref(true)
const initialLoading = ref(true)
const refreshing = ref(false)
const portfolio = ref<PortfolioVO | null>(null)
const unreadAlertCount = ref(0)
const recentAlerts = ref<AlertHistoryVO[]>([])
const topFavorites = ref<any[]>([])
const recommendFunds = ref<Fund[]>([])
const hotNews = ref<any[]>([])
const favoritesLoading = ref(false)
const recommendLoading = ref(false)
const newsLoading = ref(false)
const aiQuestion = ref('')
const aiLoading = ref(false)
const allocationChartRef = ref<HTMLElement | null>(null)
let allocationChart: echarts.ECharts | null = null

const quickQuestions = [
  '今日大盘走势如何？',
  '推荐几只稳健基金',
  '如何降低投资风险？'
]

const holdingColumns = [
  { title: '基金名称', key: 'fundName', ellipsis: { tooltip: true } },
  {
    title: '成本', key: 'amount', width: 100, render: (row: any) => {
      if (!row.amount) return '-'
      return row.amount >= 10000
        ? `¥${(row.amount / 10000).toFixed(2)}万`
        : `¥${row.amount.toFixed(2)}`
    }
  },
  {
    title: '市值', key: 'currentValue', width: 100, render: (row: any) => {
      if (!row.currentValue) return '-'
      return row.currentValue >= 10000
        ? `¥${(row.currentValue / 10000).toFixed(2)}万`
        : `¥${row.currentValue.toFixed(2)}`
    }
  },
  {
    title: '占比', key: 'actualRatio', width: 70, render: (row: any) => {
      if (row.actualRatio === null || row.actualRatio === undefined) return '-'
      return `${row.actualRatio.toFixed(1)}%`
    }
  },
  {
    title: '收益率', key: 'profitRatio', width: 80, render: (row: any) => {
      const val = row.profitRatio
      if (val === null || val === undefined) return '-'
      const cls = val >= 0 ? 'growth-positive' : 'growth-negative'
      return h('span', { class: cls }, `${val >= 0 ? '+' : ''}${val.toFixed(2)}%`)
    }
  },
  {
    title: '今日', key: 'dayGrowth', width: 70, render: (row: any) => {
      const val = row.dayGrowth
      if (val === null || val === undefined) return '-'
      const cls = val >= 0 ? 'growth-positive' : 'growth-negative'
      return h('span', { class: cls }, `${val >= 0 ? '+' : ''}${val.toFixed(2)}%`)
    }
  }
]

const loadData = async () => {
  if (!authStore.isLoggedIn) return

  try {
    loading.value = true
    const [portfolioRes, alertCountRes, alertsRes, favoritesRes, newsRes] = await Promise.all([
      fundApi.getDefaultPortfolio(),
      fundApi.getUnreadAlertCount(),
      fundApi.getAlertHistory(undefined, 5),
      fundApi.getFavorites(),
      fundApi.getNewsList(1, 10)
    ])

    portfolio.value = portfolioRes
    unreadAlertCount.value = alertCountRes
    recentAlerts.value = alertsRes
    topFavorites.value = favoritesRes.slice(0, 5)
    hotNews.value = (newsRes.records || []).slice(0, 10)

    loadRecommendFunds()
  } catch (error: any) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
    initialLoading.value = false
  }
  
  await nextTick()
  renderAllocationChart()
}

const loadRecommendFunds = async () => {
  recommendLoading.value = true
  try {
    const res = await fundApi.getClassicHotFunds(4)
    recommendFunds.value = res
  } catch (error) {
    console.error('加载推荐失败', error)
  } finally {
    recommendLoading.value = false
  }
}

const refreshData = async () => {
  refreshing.value = true
  await loadData()
  refreshing.value = false
  message.success('数据已刷新')
}

const renderAllocationChart = () => {
  if (!allocationChartRef.value || !portfolio.value?.allocations?.length) return

  if (allocationChart) {
    allocationChart.dispose()
  }

  allocationChart = echarts.init(allocationChartRef.value)
  
  const style = getComputedStyle(document.documentElement)
  const textColor = style.getPropertyValue('--text-color').trim() || '#475569'
  const cardBg = style.getPropertyValue('--card-bg').trim() || '#ffffff'

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'transparent',
      borderRadius: 12,
      padding: [12, 16],
      textStyle: {
        color: '#fff',
        fontSize: 13
      }
    },
    legend: {
      orient: 'vertical',
      right: 20,
      top: 'center',
      textStyle: { 
        color: textColor,
        fontSize: 13
      },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 16
    },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['30%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: cardBg,
        borderWidth: 3
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 20,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      data: portfolio.value.allocations.map((a: any) => ({
        value: a.value,
        name: a.name,
        itemStyle: { color: a.color }
      }))
    }]
  }

  allocationChart.setOption(option)
}

const askAI = async (question?: string) => {
  const q = question || aiQuestion.value
  if (!q.trim()) return

  aiLoading.value = true
  try {
    router.push({ path: '/ai-assistant', query: { q } })
  } finally {
    aiLoading.value = false
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
}

const getAlertIconClass = (type: string) => {
  switch (type) {
    case 'GROWTH': return 'danger'
    case 'NEWS': return 'warning'
    default: return 'info'
  }
}

const getRiskType = (level: number) => {
  if (level <= 2) return 'success'
  if (level <= 4) return 'warning'
  return 'error'
}

const getRiskText = (level: number) => {
  const texts = ['低风险', '中低风险', '中风险', '中高风险', '高风险']
  return texts[level - 1] || '未知'
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  loadData()
})
</script>

<style scoped>
.dashboard-page {
  padding: 24px;
  background: linear-gradient(180deg, rgba(6, 58, 102, 0.06), transparent 240px);
  border-radius: var(--radius-xl);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
  margin-bottom: 24px;
  align-items: start;
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-card {
  min-height: 300px;
  border: 1px solid var(--border-color);
}

.chart-container {
  width: 100%;
  height: 240px;
}

.holdings-card {
}

.ai-quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.quick-question {
  cursor: pointer;
  transition: all var(--transition-fast);
  border-radius: var(--radius-full);
}

.quick-question:hover {
  background: var(--primary-color);
  color: white;
  transform: translateY(-2px);
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.alert-item:hover {
  background: var(--bg-tertiary);
  transform: translateX(4px);
}

.alert-item.unread {
  background: rgba(23, 99, 146, 0.12);
  border-left: 3px solid var(--primary-color);
}

.alert-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.alert-icon.danger {
  background: var(--up-bg);
  color: var(--danger-color);
}

.alert-icon.warning {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

.alert-icon.info {
  background: rgba(0, 183, 201, 0.14);
  color: var(--info-color);
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.alert-message {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-time {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.favorite-item:hover {
  background: var(--bg-tertiary);
}

.fav-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
}

.fav-code {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.fav-growth {
  font-weight: 600;
  font-size: 15px;
}

.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.recommend-card {
  margin-bottom: 24px;
}

.recommend-scroll {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.recommend-scroll::-webkit-scrollbar {
  height: 4px;
}

.recommend-scroll::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 2px;
}

.recommend-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}

.fund-card {
  flex: 0 0 280px;
  padding: 24px;
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.25s ease;
  scroll-snap-align: start;
}

.fund-card:hover {
  border-color: var(--primary-color);
  background: var(--bg-tertiary);
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.fund-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.fund-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.4;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fund-code {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
  font-family: 'SF Mono', 'Menlo', monospace;
}

.fund-card-bottom {
  margin-top: 4px;
}

.fund-metrics {
  display: flex;
  gap: 20px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.metric-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-value.growth {
  font-size: 18px;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow: hidden;
}

.news-item {
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.news-item:hover {
  background: var(--bg-tertiary);
}

.news-title {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 8px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-page {
    padding: 16px;
  }

  .main-content {
    gap: 16px;
  }

  .left-panel,
  .right-panel {
    gap: 16px;
  }

  .bottom-section {
    gap: 16px;
  }
}
</style>
