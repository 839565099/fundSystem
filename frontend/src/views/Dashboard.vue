<template>
  <div class="dashboard-page page-container">
    <div class="page-header">
      <h1 class="page-title">
        <n-icon size="28"><GridOutline /></n-icon>
        投资概览
      </h1>
      <div class="header-actions">
        <div class="realtime-indicator">
          <span class="pulse-dot"></span>
          实时更新
        </div>
        <n-button @click="refreshData" :loading="refreshing">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="dashboard-grid">
      <div class="overview-card">
        <div class="label">总资产</div>
        <div class="value">{{ formatMoney(portfolio?.currentValue || 0) }}</div>
        <div class="change" :class="portfolio?.totalReturn >= 0 ? 'positive' : 'negative'">
          <n-icon>
            <TrendingUpOutline v-if="portfolio?.totalReturn >= 0" />
            <TrendingDownOutline v-else />
          </n-icon>
          {{ portfolio?.totalReturn >= 0 ? '+' : '' }}{{ portfolio?.totalProfit?.toFixed(2) || 0 }} 元
          ({{ portfolio?.totalReturn >= 0 ? '+' : '' }}{{ portfolio?.totalReturn?.toFixed(2) || 0 }}%)
        </div>
      </div>
      <div class="overview-card">
        <div class="label">今日收益</div>
        <div class="value" :class="portfolio?.dayProfit >= 0 ? 'positive' : 'negative'">
          {{ portfolio?.dayProfit >= 0 ? '+' : '' }}{{ formatMoney(portfolio?.dayProfit || 0) }}
        </div>
        <div class="change" :class="portfolio?.dayReturn >= 0 ? 'positive' : 'negative'">
          <n-icon>
            <TrendingUpOutline v-if="portfolio?.dayReturn >= 0" />
            <TrendingDownOutline v-else />
          </n-icon>
          {{ portfolio?.dayReturn >= 0 ? '+' : '' }}{{ portfolio?.dayReturn?.toFixed(2) || 0 }}%
        </div>
      </div>
      <div class="overview-card">
        <div class="label">昨日收益</div>
        <div class="value" :class="portfolio?.yesterdayProfit >= 0 ? 'positive' : 'negative'">
          {{ portfolio?.yesterdayProfit >= 0 ? '+' : '' }}{{ formatMoney(portfolio?.yesterdayProfit || 0) }}
        </div>
        <div class="change" :class="portfolio?.yesterdayReturn >= 0 ? 'positive' : 'negative'">
          <n-icon>
            <TrendingUpOutline v-if="portfolio?.yesterdayReturn >= 0" />
            <TrendingDownOutline v-else />
          </n-icon>
          {{ portfolio?.yesterdayReturn >= 0 ? '+' : '' }}{{ portfolio?.yesterdayReturn?.toFixed(2) || 0 }}%
        </div>
      </div>
      <div class="overview-card">
        <div class="label">持仓基金</div>
        <div class="value">{{ portfolio?.fundCount || 0 }}</div>
        <div class="change">
          <n-button text type="primary" @click="router.push('/portfolio')">
            管理组合 <n-icon><ChevronForwardOutline /></n-icon>
          </n-button>
        </div>
      </div>
      <div class="overview-card" @click="router.push('/alerts')" style="cursor: pointer;">
        <div class="label">预警通知</div>
        <div class="value">
          <n-badge :value="unreadAlertCount" :show="unreadAlertCount > 0">
            <n-icon size="32"><NotificationsOutline /></n-icon>
          </n-badge>
        </div>
        <div class="change">
          {{ unreadAlertCount > 0 ? `${unreadAlertCount} 条未读` : '暂无预警' }}
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="main-content">
      <div class="left-panel">
        <!-- 资产配置 -->
        <n-card title="资产配置" class="chart-card">
          <template #header-extra>
            <n-button text @click="router.push('/portfolio')">
              详情
            </n-button>
          </template>
          <div v-if="portfolio?.allocations?.length" class="chart-container" ref="allocationChartRef"></div>
          <n-empty v-else description="暂无持仓数据">
            <template #extra>
              <n-button type="primary" @click="router.push('/portfolio')">
                创建组合
              </n-button>
            </template>
          </n-empty>
        </n-card>

        <!-- 持仓列表 -->
        <n-card title="持仓明细" class="holdings-card">
          <template #header-extra>
            <n-button text @click="router.push('/portfolio')">
              管理
            </n-button>
          </template>
          <n-data-table
            v-if="portfolio?.items?.length"
            :columns="holdingColumns"
            :data="portfolio.items"
            :bordered="false"
            size="small"
          />
          <n-empty v-else description="暂无持仓" />
        </n-card>
      </div>

      <div class="right-panel">
        <!-- AI 快捷助手 -->
        <n-card title="AI 投资助手" class="ai-card">
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
                <n-icon><ChatbubbleOutline /></n-icon>
              </template>
              提问
            </n-button>
          </div>
        </n-card>

        <!-- 预警中心 -->
        <n-card title="预警中心" class="alert-card">
          <template #header-extra>
            <n-button text @click="router.push('/alerts')">
              全部
            </n-button>
          </template>
          <div v-if="recentAlerts.length" class="alert-list">
            <div
              v-for="alert in recentAlerts.slice(0, 5)"
              :key="alert.id"
              class="alert-item"
              :class="{ unread: !alert.isRead }"
            >
              <div class="alert-icon" :class="getAlertIconClass(alert.alertType)">
                <n-icon>
                  <AlertCircleOutline v-if="alert.alertType === 'GROWTH'" />
                  <NewspaperOutline v-else-if="alert.alertType === 'NEWS'" />
                  <NotificationsOutline v-else />
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

        <!-- 收藏基金 -->
        <n-card title="我的收藏" class="favorites-card">
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
                class="favorite-item"
                @click="router.push(`/fund/${fav.fundCode}`)"
              >
                <div class="fav-info">
                  <div class="fav-name">{{ fav.fundName }}</div>
                  <div class="fav-code">{{ fav.fundCode }}</div>
                </div>
                <div class="fav-growth" :class="fav.dayGrowth >= 0 ? 'positive' : 'negative'">
                  {{ fav.dayGrowth >= 0 ? '+' : '' }}{{ fav.dayGrowth?.toFixed(2) }}%
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无收藏" />
          </n-spin>
        </n-card>
      </div>
    </div>

    <!-- 底部区域 -->
    <div class="bottom-section">
      <!-- 推荐基金 -->
      <n-card title="智能推荐" class="recommend-card">
        <template #header-extra>
          <n-button text @click="router.push('/recommend')">
            更多
          </n-button>
        </template>
        <n-spin :show="recommendLoading">
          <div class="recommend-grid">
            <div
              v-for="fund in recommendFunds"
              :key="fund.fundCode"
              class="fund-card"
              @click="router.push(`/fund/${fund.fundCode}`)"
            >
              <div class="header">
                <div>
                  <div class="name">{{ fund.fundName }}</div>
                  <div class="code">{{ fund.fundCode }}</div>
                </div>
                <n-tag size="small" :type="getRiskType(fund.riskLevel)">
                  {{ getRiskText(fund.riskLevel) }}
                </n-tag>
              </div>
              <div class="nav">{{ fund.nav?.toFixed(4) }}</div>
              <div class="growth" :class="fund.dayGrowth >= 0 ? 'positive' : 'negative'">
                {{ fund.dayGrowth >= 0 ? '+' : '' }}{{ fund.dayGrowth?.toFixed(2) }}%
              </div>
            </div>
          </div>
        </n-spin>
      </n-card>

      <!-- 市场热点 -->
      <n-card title="市场热点" class="news-card">
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
import { ref, onMounted, computed, nextTick, h } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  NCard, NButton, NIcon, NBadge, NTag, NInput, NDataTable, NEmpty, NSpin,
  useMessage
} from 'naive-ui'
import {
  GridOutline, RefreshOutline, TrendingUpOutline, TrendingDownOutline,
  NotificationsOutline, ChevronForwardOutline, ChatbubbleOutline,
  AlertCircleOutline, NewspaperOutline
} from '@vicons/ionicons5'
import * as echarts from 'echarts'
import { fundApi } from '@/api/fund'
import type { PortfolioVO, AlertHistoryVO, Fund } from '@/types'

const router = useRouter()
const authStore = useAuthStore()
const message = useMessage()

// 状态
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

// 快捷问题
const quickQuestions = [
  '今日大盘走势如何？',
  '推荐几只稳健基金',
  '如何降低投资风险？'
]

// 持仓表格列
const holdingColumns = [
  { title: '基金名称', key: 'fundName', ellipsis: { tooltip: true } },
  { title: '持有金额', key: 'amount', width: 100, render: (row: any) => `¥${row.amount?.toFixed(2) || '0.00'}` },
  { title: '收益率', key: 'profitRatio', width: 90, render: (row: any) => {
    const val = row.profitRatio
    if (val === null || val === undefined) return '-'
    const cls = val >= 0 ? 'positive' : 'negative'
    return h('span', { class: cls }, `${val >= 0 ? '+' : ''}${val.toFixed(2)}%`)
  }}
]

// 加载数据
const loadData = async () => {
  if (!authStore.isLoggedIn) return

  try {
    // 并行加载数据
    const [portfolioRes, alertCountRes, alertsRes, favoritesRes, newsRes] = await Promise.all([
      fundApi.getDefaultPortfolio(),
      fundApi.getUnreadAlertCount(),
      fundApi.getAlertHistory(null, 5),
      fundApi.getFavorites(),
      fundApi.getNewsList(1, 5)
    ])

    portfolio.value = portfolioRes
    unreadAlertCount.value = alertCountRes
    recentAlerts.value = alertsRes
    topFavorites.value = favoritesRes.slice(0, 5)
    hotNews.value = newsRes.records || []

    // 加载推荐基金
    loadRecommendFunds()

    // 渲染资产配置图
    await nextTick()
    renderAllocationChart()
  } catch (error: any) {
    console.error('加载数据失败', error)
  }
}

// 加载推荐基金
const loadRecommendFunds = async () => {
  recommendLoading.value = true
  try {
    const res = await fundApi.getHotFunds(4)
    recommendFunds.value = res
  } catch (error) {
    console.error('加载推荐失败', error)
  } finally {
    recommendLoading.value = false
  }
}

// 刷新数据
const refreshData = async () => {
  refreshing.value = true
  await loadData()
  refreshing.value = false
  message.success('数据已刷新')
}

// 渲染资产配置图
const renderAllocationChart = () => {
  if (!allocationChartRef.value || !portfolio.value?.allocations?.length) return

  if (allocationChart) {
    allocationChart.dispose()
  }

  allocationChart = echarts.init(allocationChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: 'var(--text-color)' }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: 'var(--card-bg)',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
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

// AI 提问
const askAI = async (question?: string) => {
  const q = question || aiQuestion.value
  if (!q.trim()) return

  aiLoading.value = true
  try {
    // 跳转到 AI 助手页面并传递问题
    router.push({ path: '/ai-assistant', query: { q } })
  } finally {
    aiLoading.value = false
  }
}

// 工具函数
const formatMoney = (value: number) => {
  return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
    default: return 'success'
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
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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
  min-height: 350px;
}

.holdings-card {
  flex: 1;
}

.ai-quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.quick-question {
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-question:hover {
  background: var(--primary-color);
  color: white;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-secondary);
  cursor: pointer;
}

.alert-item.unread {
  background: rgba(59, 130, 246, 0.1);
  border-left: 3px solid var(--primary-color);
}

.alert-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-icon.danger {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

.alert-icon.warning {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
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
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
}

.favorite-item:hover {
  background: var(--card-bg-hover);
}

.fav-name {
  font-weight: 500;
  font-size: 14px;
}

.fav-code {
  font-size: 12px;
  color: var(--text-secondary);
}

.fav-growth {
  font-weight: 600;
}

.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

@media (max-width: 1024px) {
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-item {
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
}

.news-item:hover {
  background: var(--card-bg-hover);
}

.news-title {
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.positive {
  color: var(--up-color);
}

.negative {
  color: var(--down-color);
}
</style>
