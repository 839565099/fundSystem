<template>
  <div class="notifications-page">
    <div class="page-header">
      <h1>消息中心</h1>
      <div class="header-actions">
        <n-button v-if="unreadCount > 0" type="primary" ghost @click="handleMarkAllRead">
          全部已读
        </n-button>
      </div>
    </div>

    <div class="tabs-wrapper">
      <n-tabs v-model:value="activeTab" type="line" @update:value="handleTabChange">
        <n-tab-pane name="all" tab="全部" />
        <n-tab-pane name="ALERT" tab="预警" />
        <n-tab-pane name="SYSTEM" tab="系统" />
        <n-tab-pane name="ACTIVITY" tab="活动" />
      </n-tabs>
    </div>

    <div class="notification-list">
      <n-spin :show="loading">
        <template v-if="notifications.length > 0">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: item.isRead === 0 }"
            @click="handleClick(item)"
          >
            <div class="notification-icon">
              <n-icon size="24" :color="getIconColor(item.type)">
                <component :is="getIcon(item.type)" />
              </n-icon>
            </div>
            <div class="notification-content">
              <div class="notification-title">{{ item.title }}</div>
              <div class="notification-text">{{ item.content }}</div>
              <div class="notification-time">{{ formatTime(item.createTime) }}</div>
            </div>
            <div class="notification-actions">
              <n-button
                v-if="item.isRead === 0"
                text
                size="small"
                @click.stop="handleMarkRead(item.id)"
              >
                标记已读
              </n-button>
              <n-button text size="small" type="error" @click.stop="handleDelete(item.id)">
                删除
              </n-button>
            </div>
          </div>
        </template>
        <template v-else>
          <n-empty description="暂无通知" />
        </template>
      </n-spin>

      <div v-if="hasMore" class="load-more">
        <n-button :loading="loadingMore" @click="loadMore">加载更多</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NTabs, NTabPane, NButton, NSpin, NEmpty, NIcon, createDiscreteApi } from 'naive-ui'
import { NotificationsOutline, AlertCircleOutline, InformationCircleOutline, StarOutline } from '@vicons/ionicons5'
import { notificationApi, type Notification } from '../api/notification'

const { message } = createDiscreteApi(['message'])

const router = useRouter()

const loading = ref(true)
const loadingMore = ref(false)
const activeTab = ref('all')
const notifications = ref<Notification[]>([])
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const unreadCount = ref(0)

const hasMore = computed(() => notifications.value.length < total.value)

const getIcon = (type: string) => {
  switch (type) {
    case 'ALERT': return AlertCircleOutline
    case 'SYSTEM': return InformationCircleOutline
    case 'ACTIVITY': return StarOutline
    default: return NotificationsOutline
  }
}

const getIconColor = (type: string) => {
  switch (type) {
    case 'ALERT': return '#f0a020'
    case 'SYSTEM': return '#2080f0'
    case 'ACTIVITY': return '#18a058'
    default: return '#666'
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

const fetchNotifications = async (reset = true) => {
  if (reset) {
    loading.value = true
    pageNum.value = 1
  } else {
    loadingMore.value = true
  }

  try {
    const type = activeTab.value === 'all' ? undefined : activeTab.value
    const res = await notificationApi.getNotifications(type, pageNum.value, pageSize)
    if (reset) {
      notifications.value = res.records
    } else {
      notifications.value.push(...res.records)
    }
    total.value = res.total
  } catch (e: any) {
    message.error(e.message || '获取通知失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    unreadCount.value = await notificationApi.getUnreadCount()
  } catch {
    // ignore
  }
}

const handleTabChange = () => {
  fetchNotifications(true)
}

const loadMore = () => {
  pageNum.value++
  fetchNotifications(false)
}

const handleClick = async (item: Notification) => {
  if (item.isRead === 0) {
    await handleMarkRead(item.id)
  }
  if (item.link) {
    router.push(item.link)
  }
}

const handleMarkRead = async (id: number) => {
  try {
    await notificationApi.markAsRead(id)
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

const handleMarkAllRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
    message.success('已全部标记为已读')
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await notificationApi.delete(id)
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      const item = notifications.value[index]
      if (item.isRead === 0) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      notifications.value.splice(index, 1)
      total.value--
    }
    message.success('删除成功')
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})
</script>

<style scoped>
.notifications-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

.tabs-wrapper {
  margin-bottom: 16px;
}

.notification-list {
  min-height: 300px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--card-bg);
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.notification-item:hover {
  background: var(--hover-bg);
}

.notification-item.unread {
  background: var(--bg-color-secondary);
  border-left: 3px solid var(--primary-color);
}

.notification-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color-secondary);
  border-radius: 50%;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.notification-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.notification-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}

.load-more {
  text-align: center;
  margin-top: 24px;
}
</style>
