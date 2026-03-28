<template>
  <n-popover v-model:show="showPopover" trigger="click" placement="bottom-end" :width="360">
    <template #trigger>
      <n-badge :value="unreadCount" :max="99" :show="unreadCount > 0">
        <n-button quaternary circle>
          <template #icon>
            <n-icon size="20">
              <IconBell />
            </n-icon>
          </template>
        </n-button>
      </n-badge>
    </template>

    <div class="notification-popover">
      <div class="popover-header">
        <span class="popover-title">通知</span>
        <n-button v-if="unreadCount > 0" text size="small" type="primary" @click="handleMarkAllRead">
          全部已读
        </n-button>
      </div>
      <n-spin :show="loading">
        <div class="popover-content">
          <template v-if="notifications.length > 0">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="popover-item"
              :class="{ unread: item.isRead === 0 }"
              @click="handleItemClick(item)"
            >
              <div class="item-icon">
                <n-icon size="18" :color="getIconColor(item.type)">
                  <component :is="getIcon(item.type)" />
                </n-icon>
              </div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-time">{{ formatTime(item.createTime) }}</div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="empty-state">
              <n-empty description="暂无通知" size="small" />
            </div>
          </template>
        </div>
      </n-spin>
      <div class="popover-footer">
        <n-button text block @click="goToNotifications">查看全部通知</n-button>
      </div>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NBadge, NButton, NIcon, NSpin, NEmpty, NPopover, createDiscreteApi } from 'naive-ui'
import { IconBell, IconAlertCircle, IconInfoCircle, IconStar } from '@tabler/icons-vue'
import { notificationApi, type Notification } from '../api/notification'

const { message } = createDiscreteApi(['message'])

const router = useRouter()

const showPopover = ref(false)
const loading = ref(false)
const unreadCount = ref(0)
const notifications = ref<Notification[]>([])

const getIcon = (type: string) => {
  switch (type) {
    case 'ALERT': return IconAlertCircle
    case 'SYSTEM': return IconInfoCircle
    case 'ACTIVITY': return IconStar
    default: return IconBell
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

const fetchUnreadCount = async () => {
  try {
    unreadCount.value = await notificationApi.getUnreadCount()
  } catch {
    // ignore
  }
}

const fetchNotifications = async () => {
  loading.value = true
  try {
    const res = await notificationApi.getNotifications(undefined, 1, 5)
    notifications.value = res.records
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

const handleItemClick = async (item: Notification) => {
  if (item.isRead === 0) {
    try {
      await notificationApi.markAsRead(item.id)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e: any) {
      message.error(e.message || '操作失败')
    }
  }
  showPopover.value = false
  if (item.link) {
    router.push(item.link)
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

const goToNotifications = () => {
  showPopover.value = false
  router.push('/notifications')
}

// 定时刷新未读数量
let timer: number | null = null

onMounted(() => {
  fetchUnreadCount()
  fetchNotifications()
  timer = window.setInterval(() => {
    fetchUnreadCount()
    if (showPopover.value) {
      fetchNotifications()
    }
  }, 60000) // 每分钟刷新
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.notification-popover {
  max-height: 400px;
  display: flex;
  flex-direction: column;
}

.popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.popover-title {
  font-weight: 600;
  font-size: 15px;
}

.popover-content {
  flex: 1;
  overflow-y: auto;
  max-height: 300px;
}

.popover-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.popover-item:hover {
  background: var(--hover-bg);
}

.popover-item.unread {
  background: var(--bg-color-secondary);
}

.item-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color-secondary);
  border-radius: 50%;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.empty-state {
  padding: 40px 16px;
}

.popover-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  text-align: center;
}
</style>
