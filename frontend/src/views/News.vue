<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="search-header card">
      <h2 class="section-title">
        <n-icon size="24"><IconNews /></n-icon>
        资讯中心
      </h2>
      <div class="search-box">
        <n-input
          v-model:value="searchKeyword"
          placeholder="搜索资讯..."
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <n-icon><IconSearch /></n-icon>
          </template>
        </n-input>
        <n-button type="primary" @click="handleSearch">搜索</n-button>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs card">
      <div class="tabs-wrapper">
        <n-radio-group v-model:value="selectedType" @update:value="handleTypeChange">
          <n-radio-button value="">全部</n-radio-button>
          <n-radio-button value="财经要闻">财经要闻</n-radio-button>
          <n-radio-button value="基金动态">基金动态</n-radio-button>
          <n-radio-button value="市场分析">市场分析</n-radio-button>
          <n-radio-button value="政策法规">政策法规</n-radio-button>
          <n-radio-button value="行业动态">行业动态</n-radio-button>
          <n-radio-button value="资金流向">资金流向</n-radio-button>
        </n-radio-group>
      </div>
      <div class="sentiment-filter">
        <span class="filter-label">情感筛选:</span>
        <n-button
          :type="selectedSentiment === '' ? 'primary' : 'default'"
          size="small"
          @click="selectedSentiment = ''; loadNews()"
        >全部</n-button>
        <n-button
          :type="selectedSentiment === 'BULLISH' ? 'success' : 'default'"
          size="small"
          @click="selectedSentiment = 'BULLISH'; loadNews()"
        >利好</n-button>
        <n-button
          :type="selectedSentiment === 'BEARISH' ? 'error' : 'default'"
          size="small"
          @click="selectedSentiment = 'BEARISH'; loadNews()"
        >利空</n-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 新闻列表 -->
      <n-spin :show="loading">
        <div class="news-list">
          <div v-if="newsList.length > 0" class="news-grid">
            <div
              v-for="item in newsList"
              :key="item.id"
              class="news-card card"
              @click="router.push(`/news/${item.id}`)"
            >
              <div class="card-content">
                <h3 class="card-title">{{ item.title }}</h3>
                <p v-if="item.summary" class="card-summary">{{ item.summary }}</p>
                <div class="card-meta">
                  <span class="source">
                    <span v-if="getSourceIcon(item.source) === 'sina'" class="source-icon sina">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                        <path d="M9.197 15.566c1.73 1.09 3.743 1.362 5.314.705 1.569-.657 2.512-2.057 2.512-3.742 0-1.684-.943-3.084-2.512-3.741-1.571-.657-3.584-.386-5.314.705-1.73 1.09-2.782 2.71-2.782 4.446 0 1.735 1.052 3.355 2.782 4.446l-.5.785zm-1.5-7.498c2.443-1.542 5.437-1.912 7.8-.924 2.362.988 3.816 3.127 3.816 5.578 0 2.45-1.454 4.59-3.816 5.578-2.363.988-5.357.618-7.8-.924-2.442-1.541-3.935-3.893-3.935-6.437 0-2.544 1.493-4.896 3.935-6.437l.5.786z"/>
                      </svg>
                    </span>
                    <span v-else-if="getSourceIcon(item.source) === 'eastmoney'" class="source-icon eastmoney">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                        <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"/>
                        <text x="12" y="16" text-anchor="middle" font-size="10" font-weight="bold">东</text>
                      </svg>
                    </span>
                    <n-icon v-else size="14"><IconMapPin /></n-icon>
                    <span class="source-name">{{ item.source || '未知来源' }}</span>
                  </span>
                  <span class="time">
                    <n-icon size="14"><IconClock /></n-icon>
                    {{ formatTime(item.publishTime || '') }}
                  </span>
                  <n-tag
                    v-if="item.sentiment"
                    :type="getSentimentType(item.sentiment)"
                    size="small"
                    :bordered="false"
                  >
                    {{ getSentimentLabel(item.sentiment) }}
                  </n-tag>
                </div>
              </div>
            </div>
          </div>
          <n-empty v-else description="暂无资讯" />

          <!-- 分页 -->
          <div v-if="newsList.length > 0" class="pagination">
            <n-pagination
              v-model:page="pageNum"
              :page-count="pageCount"
              @update:page="loadNews"
            />
          </div>
        </div>
      </n-spin>

      <!-- 侧边栏 -->
      <div class="sidebar">
        <!-- 热门资讯 -->
        <div class="sidebar-card card">
          <h3 class="sidebar-title">
            <n-icon size="18"><IconFlame /></n-icon>
            热门资讯
          </h3>
          <div class="hot-list">
            <div
              v-for="(item, index) in hotNews"
              :key="item.id"
              class="hot-item"
              @click="router.push(`/news/${item.id}`)"
            >
              <span class="hot-rank" :class="getHotRankClass(index)">{{ index + 1 }}</span>
              <span class="hot-title">{{ item.title }}</span>
            </div>
          </div>
        </div>

        <!-- 情感风向 -->
        <div class="sidebar-card card">
          <h3 class="sidebar-title">
            <n-icon size="18"><IconChartBar /></n-icon>
            情感风向
          </h3>
          <div class="sentiment-stats">
            <div class="stat-item bullish">
              <span class="stat-label">利好</span>
              <div class="stat-bar">
                <div class="bar-fill" :style="{ width: sentimentOverview?.bullishPercent + '%' }"></div>
              </div>
              <span class="stat-value">{{ sentimentOverview?.bullishPercent?.toFixed(1) || 0 }}%</span>
            </div>
            <div class="stat-item neutral">
              <span class="stat-label">中性</span>
              <div class="stat-bar">
                <div class="bar-fill" :style="{ width: sentimentOverview?.neutralPercent + '%' }"></div>
              </div>
              <span class="stat-value">{{ sentimentOverview?.neutralPercent?.toFixed(1) || 0 }}%</span>
            </div>
            <div class="stat-item bearish">
              <span class="stat-label">利空</span>
              <div class="stat-bar">
                <div class="bar-fill" :style="{ width: sentimentOverview?.bearishPercent + '%' }"></div>
              </div>
              <span class="stat-value">{{ sentimentOverview?.bearishPercent?.toFixed(1) || 0 }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NIcon, NInput, NButton, NSpin, NEmpty, NTag, NPagination,
  NRadioGroup, NRadioButton, createDiscreteApi
} from 'naive-ui'
import {
  IconNews, IconSearch, IconMapPin,
  IconClock, IconFlame, IconChartBar
} from '@tabler/icons-vue'
import { newsApi } from '../api/fund'
import type { FundNews, NewsSentimentOverview } from '../types'

const { message, notification } = createDiscreteApi(['message', 'notification'])
const router = useRouter()

// 状态
const loading = ref(false)
const searchKeyword = ref('')
const selectedType = ref('')
const selectedSentiment = ref('')
const newsList = ref<FundNews[]>([])
const hotNews = ref<FundNews[]>([])
const sentimentOverview = ref<NewsSentimentOverview>()

const pageNum = ref(1)
const pageSize = 10
const pageCount = ref(1)

// SSE 连接
let eventSource: EventSource | null = null

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return time.slice(0, 10)
}

// 情感标签
const getSentimentType = (sentiment: string) => {
  switch (sentiment) {
    case 'BULLISH': return 'success'
    case 'BEARISH': return 'error'
    default: return 'default'
  }
}

const getSentimentLabel = (sentiment: string) => {
  switch (sentiment) {
    case 'BULLISH': return '利好'
    case 'BEARISH': return '利空'
    default: return '中性'
  }
}

// 热门排名样式
const getHotRankClass = (index: number) => {
  if (index === 0) return 'gold'
  if (index === 1) return 'silver'
  if (index === 2) return 'bronze'
  return ''
}

// 来源图标映射
const getSourceIcon = (source?: string) => {
  if (source?.includes('新浪')) {
    return 'sina'
  } else if (source?.includes('东方财富') || source?.includes('东财')) {
    return 'eastmoney'
  }
  return 'default'
}

// 加载新闻列表
const loadNews = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (selectedType.value) params.newsType = selectedType.value
    if (selectedSentiment.value) params.sentiment = selectedSentiment.value

    let data
    // 优先使用搜索接口
    if (searchKeyword.value || selectedType.value || selectedSentiment.value) {
      data = await newsApi.search(params)
    } else {
      data = await newsApi.getList(pageNum.value, pageSize)
    }
    newsList.value = data?.records || []
    pageCount.value = data?.pages || 1
  } catch (error) {
    console.error('加载新闻失败:', error)
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载热门资讯
const loadHotNews = async () => {
  try {
    hotNews.value = await newsApi.getHot(5) || []
  } catch {
    // 忽略错误
  }
}

// 加载情感统计
const loadSentiment = async () => {
  try {
    sentimentOverview.value = await newsApi.getSentimentOverview()
  } catch {
    // 忽略错误
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadNews()
}

// 分类切换
const handleTypeChange = () => {
  pageNum.value = 1
  loadNews()
}

// SSE 实时推送
const connectSSE = () => {
  try {
    eventSource = new EventSource('/api/news/stream')

    eventSource.onmessage = (event) => {
      try {
        const news = JSON.parse(event.data) as FundNews
        showNewsNotification(news)
        loadNews()
        loadHotNews()
      } catch {
        // 忽略解析错误
      }
    }

    eventSource.onerror = () => {
      eventSource?.close()
      setTimeout(connectSSE, 10000)
    }
  } catch {
    setTimeout(connectSSE, 10000)
  }
}

const showNewsNotification = (news: FundNews) => {
  notification.info({
    title: '📢 新资讯推送',
    content: news.title,
    meta: news.source,
    duration: 5000
  })
}

onMounted(() => {
  loadNews()
  loadHotNews()
  loadSentiment()
  connectSSE()
})

onUnmounted(() => {
  eventSource?.close()
})
</script>

<style scoped>
.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.search-box {
  display: flex;
  gap: 12px;
}

.search-box :deep(.n-input) {
  width: 300px;
}

.category-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.sentiment-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.main-content {
  display: flex;
  gap: 24px;
}

.news-list {
  flex: 1;
  min-width: 0;
}

.news-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.news-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.news-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.card-meta .source,
.card-meta .time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.source {
  display: flex;
  align-items: center;
  gap: 6px;
}

.source-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
}

.source-icon.sina {
  color: #ff6600;
}

.source-icon.eastmoney {
  color: #c00;
}

.source-name {
  color: var(--primary-color);
  font-weight: 500;
}

.tabs-wrapper {
  flex: 1;
  overflow-x: auto;
}

.tabs-wrapper :deep(.n-radio-group) {
  flex-wrap: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 侧边栏 */
.sidebar {
  width: 300px;
  flex-shrink: 0;
}

.sidebar-card {
  padding: 16px;
  margin-bottom: 20px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

/* 热门资讯 */
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}

.hot-item:hover {
  background: var(--bg-color);
}

.hot-rank {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: var(--bg-color);
  color: var(--text-secondary);
  flex-shrink: 0;
}

.hot-rank.gold {
  background: #fbbf24;
  color: #78350f;
}

.hot-rank.silver {
  background: #9ca3af;
  color: #374151;
}

.hot-rank.bronze {
  background: #d97706;
  color: #451a03;
}

.hot-title {
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 情感统计 */
.sentiment-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-label {
  width: 40px;
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-bar {
  flex: 1;
  height: 8px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.stat-item.bullish .bar-fill {
  background: var(--down-color);
  height: 100%;
  border-radius: var(--radius-sm);
  transition: width 0.5s ease;
}

.stat-item.neutral .bar-fill {
  background: var(--text-tertiary);
  height: 100%;
  border-radius: var(--radius-sm);
  transition: width 0.5s ease;
}

.stat-item.bearish .bar-fill {
  background: var(--up-color);
  height: 100%;
  border-radius: var(--radius-sm);
  transition: width 0.5s ease;
}

.stat-value {
  width: 45px;
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.stat-item.bullish .stat-value { color: var(--down-color); }
.stat-item.neutral .stat-value { color: var(--text-secondary); }
.stat-item.bearish .stat-value { color: var(--up-color); }

/* 响应式 */
@media (max-width: 900px) {
  .main-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .search-header {
    flex-direction: column;
    gap: 16px;
  }

  .search-box :deep(.n-input) {
    width: 100%;
  }
}
</style>
