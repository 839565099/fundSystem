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
              <span v-if="news.source" class="meta-item source">
                <n-icon size="14"><LocationOutline /></n-icon>
                {{ news.source }}
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
          <div v-if="news.content" class="content-text" v-html="news.content"></div>
          <div v-else-if="news.summary" class="content-text">{{ news.summary }}</div>
          <n-empty v-else description="暂无详细内容">
            <template #extra>
              <p class="no-content-tip">该资讯仅提供标题信息，暂无正文内容</p>
            </template>
          </n-empty>

          <!-- 原文链接 -->
          <div v-if="news.originalUrl" class="original-link">
            <n-button text type="primary" @click="openOriginal(news.originalUrl)">
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
  DocumentsOutline, ListOutline
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
