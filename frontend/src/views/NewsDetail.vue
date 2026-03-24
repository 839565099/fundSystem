<template>
  <div class="page-container">
    <n-spin :show="loading">
      <div v-if="news" class="news-detail">
        <!-- 返回导航 -->
        <div class="back-nav">
          <n-button text @click="router.push('/news')">
            <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
            返回资讯列表
          </n-button>
        </div>

        <!-- 文章头部 -->
        <div class="news-header card">
          <div class="header-top">
            <div class="title-row">
              <h1 class="news-title">{{ news.title }}</h1>
              <div class="action-btns">
                <n-button quaternary circle size="small" @click="handleShare">
                  <template #icon><n-icon><ShareSocialOutline /></n-icon></template>
                </n-button>
              </div>
            </div>
          </div>

          <div class="news-meta">
            <div class="meta-left">
              <span v-if="news.source" class="meta-item source-item">
                <span v-if="getSourceIcon(news.source) === 'sina'" class="source-icon sina">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M9.197 15.566c1.73 1.09 3.743 1.362 5.314.705 1.569-.657 2.512-2.057 2.512-3.742 0-1.684-.943-3.084-2.512-3.741-1.571-.657-3.584-.386-5.314.705-1.73 1.09-2.782 2.71-2.782 4.446 0 1.735 1.052 3.355 2.782 4.446l-.5.785zm-1.5-7.498c2.443-1.542 5.437-1.912 7.8-.924 2.362.988 3.816 3.127 3.816 5.578 0 2.45-1.454 4.59-3.816 5.578-2.363.988-5.357.618-7.8-.924-2.442-1.541-3.935-3.893-3.935-6.437 0-2.544 1.493-4.896 3.935-6.437l.5.786z"/>
                  </svg>
                </span>
                <span v-else-if="getSourceIcon(news.source) === 'eastmoney'" class="source-icon eastmoney">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2"/>
                    <text x="12" y="16" text-anchor="middle" font-size="10" font-weight="bold">东</text>
                  </svg>
                </span>
                <n-icon v-else size="16"><LocationOutline /></n-icon>
                <span class="source-name">{{ news.source }}</span>
              </span>
              <span v-if="news.author" class="meta-item">
                <n-icon size="14"><PersonOutline /></n-icon>
                {{ news.author }}
              </span>
              <span v-if="news.publishTime" class="meta-item">
                <n-icon size="14"><TimeOutline /></n-icon>
                {{ formatTime(news.publishTime) }}
              </span>
              <span v-if="news.viewCount" class="meta-item">
                <n-icon size="14"><EyeOutline /></n-icon>
                {{ news.viewCount }} 阅读
              </span>
            </div>
            <div class="meta-right">
              <n-tag v-if="news.newsType" type="info" size="small" :bordered="false">
                {{ news.newsType }}
              </n-tag>
              <n-tag
                v-if="news.sentiment"
                :type="getSentimentType(news.sentiment)"
                size="small"
                :bordered="false"
              >
                {{ getSentimentLabel(news.sentiment) }}
              </n-tag>
            </div>
          </div>

          <!-- 影响等级 -->
          <div v-if="news.impactLevel" class="impact-section">
            <span class="impact-label">影响等级:</span>
            <div class="impact-stars">
              <n-icon
                v-for="i in 5"
                :key="i"
                size="16"
                :color="i <= news.impactLevel ? '#f59e0b' : '#e5e7eb'"
              >
                <StarOutline />
              </n-icon>
            </div>
          </div>
        </div>

        <!-- 文章内容 -->
        <div class="news-content card">
          <div v-if="news.content && news.content.length >= 100" class="content-text" v-html="news.content"></div>
          <div v-else-if="news.summary" class="content-text">
            {{ news.summary }}
            <div v-if="news.originalUrl" class="content-note">
              <n-icon size="16"><InformationCircleOutline /></n-icon>
              正文内容较少，建议查看原文获取完整内容
            </div>
            <div v-if="news.originalUrl" class="original-link">
              <n-button type="primary" @click="openOriginal(news.originalUrl)">
                <template #icon><n-icon><OpenOutline /></n-icon></template>
                查看原文
              </n-button>
            </div>
          </div>
          <n-empty v-else description="暂无详细内容">
            <template #extra>
              <p class="no-content-tip">该资讯仅提供标题信息，请查看原文获取完整内容</p>
              <n-button v-if="news.originalUrl" type="primary" @click="openOriginal(news.originalUrl)">
                查看原文
              </n-button>
            </template>
          </n-empty>

          <!-- 原文链接 -->
          <div v-if="news.originalUrl && news.content && news.content.length >= 100" class="original-link">
            <n-button type="primary" @click="openOriginal(news.originalUrl)">
              <template #icon><n-icon><OpenOutline /></n-icon></template>
              查看原文
            </n-button>
          </div>
        </div>

        <!-- 相关推荐 -->
        <div v-if="relatedNews.length > 0" class="related-section card">
          <h3 class="section-title">
            <n-icon size="20"><DocumentsOutline /></n-icon>
            相关资讯
          </h3>
          <div class="related-list">
            <div
              v-for="item in relatedNews"
              :key="item.id"
              class="related-item"
              @click="goToNews(item.id)"
            >
              <div class="related-content">
                <h4 class="related-title">{{ item.title }}</h4>
                <div class="related-meta">
                  <span>{{ item.source }}</span>
                  <span>{{ formatTimeShort(item.publishTime) }}</span>
                  <n-tag
                    v-if="item.sentiment"
                    :type="getSentimentType(item.sentiment)"
                    size="tiny"
                    :bordered="false"
                  >
                    {{ getSentimentLabel(item.sentiment) }}
                  </n-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="news-actions">
          <n-button @click="router.push('/news')">
            <template #icon><n-icon><ListOutline /></n-icon></template>
            返回列表
          </n-button>
        </div>
      </div>

      <n-empty v-else-if="!loading" description="资讯不存在" />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NSpin, NEmpty, NTag, createDiscreteApi } from 'naive-ui'
import {
  ArrowBackOutline, LocationOutline, PersonOutline, TimeOutline,
  EyeOutline, StarOutline, ShareSocialOutline, OpenOutline,
  DocumentsOutline, ListOutline, InformationCircleOutline
} from '@vicons/ionicons5'
import { newsApi } from '../api/fund'
import type { FundNews } from '../types'
import dayjs from 'dayjs'

const { message } = createDiscreteApi(['message'])
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const news = ref<FundNews>()
const relatedNews = ref<FundNews[]>([])

const formatTime = (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
const formatTimeShort = (time?: string) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

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

// 来源图标映射
const getSourceIcon = (source?: string) => {
  if (source?.includes('新浪')) {
    return 'sina'
  } else if (source?.includes('东方财富') || source?.includes('东财')) {
    return 'eastmoney'
  }
  return 'default'
}

const loadNews = async () => {
  loading.value = true
  try {
    const id = parseInt(route.params.id as string)
    news.value = await newsApi.getDetail(id)
    // 加载相关新闻
    loadRelatedNews(id)
  } catch {
    message.error('加载资讯失败')
  } finally {
    loading.value = false
  }
}

const loadRelatedNews = async (id: number) => {
  try {
    relatedNews.value = await newsApi.getRelated(id, 5) || []
  } catch {
    // 忽略错误
  }
}

const goToNews = (id: number) => {
  router.push(`/news/${id}`)
  window.scrollTo(0, 0)
}

const handleShare = () => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href)
    message.success('链接已复制到剪贴板')
  }
}

const openOriginal = (url: string) => {
  window.open(url, '_blank')
}

// 监听路由变化
watch(() => route.params.id, () => {
  if (route.params.id) {
    loadNews()
  }
})

onMounted(loadNews)
</script>

<style scoped>
.news-detail {
  max-width: 900px;
  margin: 0 auto;
}

.back-nav {
  margin-bottom: 16px;
}

.news-header {
  margin-bottom: 24px;
  padding: 24px;
}

.header-top {
  margin-bottom: 16px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.news-title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
  margin: 0;
  flex: 1;
}

.action-btns {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.news-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.meta-left, .meta-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-item.source {
  color: var(--primary-color);
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-item .source-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
}

.source-item .source-icon.sina {
  color: #ff6600;
}

.source-item .source-icon.eastmoney {
  color: #c00;
}

.source-item .source-name {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 14px;
}

.impact-section {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.impact-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.impact-stars {
  display: flex;
  gap: 2px;
}

/* 文章内容 */
.news-content {
  padding: 32px;
  margin-bottom: 24px;
}

.content-text {
  line-height: 2;
  font-size: 16px;
  color: var(--text-color);
}

.content-text :deep(p) {
  margin-bottom: 16px;
}

.content-text :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-md);
  margin: 16px 0;
}

.no-content-tip {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 8px;
}

.content-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
}

.original-link {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

/* 相关推荐 */
.related-section {
  padding: 20px 24px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-item {
  padding: 12px 16px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.related-item:hover {
  background: var(--bg-color-secondary);
}

.related-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.related-title {
  font-size: 14px;
  font-weight: 500;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 底部操作 */
.news-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

@media (max-width: 768px) {
  .news-title {
    font-size: 20px;
  }

  .news-content {
    padding: 20px;
  }

  .news-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
