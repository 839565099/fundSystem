<template>
  <div class="admin-layout">
    <n-layout has-sider>
      <!-- 侧边栏 -->
      <n-layout-sider
        bordered
        :width="240"
        :native-scrollbar="false"
        class="admin-sider"
      >
        <div class="admin-logo">
          <svg viewBox="0 0 48 48" fill="none" class="logo-icon">
            <path d="M24 4L42 14V34L24 44L6 34V14L24 4Z" fill="currentColor" opacity="0.2"/>
            <path d="M24 4L42 14V34L24 44L6 34V14L24 4Z" stroke="currentColor" stroke-width="2" fill="none"/>
            <path d="M24 14L34 20V32L24 38L14 32V20L24 14Z" fill="currentColor"/>
          </svg>
          <span class="logo-text">管理后台</span>
        </div>
        <n-menu
          :options="menuOptions"
          :value="currentKey"
          @update:value="handleMenuClick"
          class="admin-menu"
        />
      </n-layout-sider>

      <!-- 主内容区 -->
      <n-layout>
        <n-layout-header bordered class="admin-header">
          <div class="header-left">
            <h2 class="page-title">{{ pageTitle }}</h2>
          </div>
          <div class="header-right">
            <n-button @click="router.push('/')">
              <template #icon>
                <n-icon><IconHome /></n-icon>
              </template>
              返回前台
            </n-button>
            <n-dropdown :options="userOptions" @select="handleUserAction">
              <n-button text class="user-btn">
                <n-avatar round size="small" class="user-avatar">
                  {{ authStore.user?.nickname?.charAt(0) || 'A' }}
                </n-avatar>
                <span class="user-name">{{ authStore.user?.nickname || '管理员' }}</span>
              </n-button>
            </n-dropdown>
          </div>
        </n-layout-header>
        <n-layout-content class="admin-content">
          <router-view />
        </n-layout-content>
      </n-layout>
    </n-layout>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NLayout,
  NLayoutSider,
  NLayoutHeader,
  NLayoutContent,
  NMenu,
  NButton,
  NAvatar,
  NDropdown,
  NIcon,
  type MenuOption,
} from 'naive-ui'
import {
  IconHome,
  IconUsers,
  IconFileText,
  IconLogout,
  IconUser,
  IconChartBar,
  IconClock,
  IconSettings,
  IconHistory,
} from '@tabler/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentKey = computed(() => route.name as string)
const pageTitle = computed(() => (route.meta.title as string) || '管理后台')

const renderIcon = (icon: any) => {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions: MenuOption[] = [
  { label: '管理概览', key: 'AdminDashboard', icon: renderIcon(IconChartBar) },
  { label: '用户管理', key: 'AdminUsers', icon: renderIcon(IconUsers) },
  { type: 'divider', key: 'd-session' },
  { label: '会话管理', key: 'AdminSessions', icon: renderIcon(IconClock) },
  { label: '会话配置', key: 'AdminSessionConfig', icon: renderIcon(IconSettings) },
  { label: '会话日志', key: 'AdminSessionLogs', icon: renderIcon(IconHistory) },
  { type: 'divider', key: 'd-logs' },
  { label: '操作日志', key: 'AdminLogs', icon: renderIcon(IconFileText) },
]

const userOptions = [
  { label: '个人中心', key: 'profile', icon: renderIcon(IconUser) },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: renderIcon(IconLogout) },
]

const handleMenuClick = (key: string) => {
  const pathMap: Record<string, string> = {
    AdminDashboard: '/admin',
    AdminUsers: '/admin/users',
    AdminSessions: '/admin/sessions',
    AdminSessionConfig: '/admin/session-config',
    AdminSessionLogs: '/admin/session-logs',
    AdminLogs: '/admin/logs',
  }
  const path = pathMap[key]
  if (path) {
    router.push(path)
  }
}

const handleUserAction = async (key: string) => {
  if (key === 'logout') {
    await authStore.logout()
    router.push('/login')
  } else if (key === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--bg-layout);
}

.admin-sider {
  background: var(--card-bg);
}

.admin-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: var(--primary-color);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.admin-menu {
  padding: 12px 8px;
}

.admin-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
}

.user-avatar {
  background: var(--gradient-primary);
  color: white;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
}

.admin-content {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: var(--bg-layout);
}
</style>
