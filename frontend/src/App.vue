<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <div class="app-container" :class="{ 'dark-mode': isDark }">
            <n-layout has-sider>
              <n-layout-sider
                bordered
                collapse-mode="width"
                :collapsed-width="64"
                :width="220"
                :collapsed="collapsed"
                show-trigger
                @collapse="collapsed = true"
                @expand="collapsed = false"
                class="layout-sider"
              >
                <div class="logo">
                  <span v-if="!collapsed">📈 基金系统</span>
                  <span v-else>📈</span>
                </div>
                <n-menu
                  :collapsed="collapsed"
                  :collapsed-width="64"
                  :collapsed-icon-size="22"
                  :options="menuOptions"
                  :value="currentKey"
                  @update:value="handleMenuClick"
                />
              </n-layout-sider>
              <n-layout>
                <n-layout-header bordered class="layout-header">
                  <div class="header-content">
                    <n-breadcrumb>
                      <n-breadcrumb-item>首页</n-breadcrumb-item>
                      <n-breadcrumb-item>{{ currentTitle }}</n-breadcrumb-item>
                    </n-breadcrumb>
                    <div class="header-actions">
                      <n-switch v-model:value="isDark" @update:value="toggleTheme">
                        <template #checked>
                          <n-icon><MoonOutline /></n-icon>
                        </template>
                        <template #unchecked>
                          <n-icon><SunnyOutline /></n-icon>
                        </template>
                      </n-switch>
                      <n-dropdown v-if="authStore.isLoggedIn" :options="userOptions" @select="handleUserAction">
                        <n-button text>
                          <n-avatar round size="small" style="margin-right: 8px;">
                            {{ authStore.user?.nickname || authStore.user?.username?.charAt(0) || 'U' }}
                          </n-avatar>
                          {{ authStore.user?.nickname || authStore.user?.username }}
                        </n-button>
                      </n-dropdown>
                      <n-button v-else type="primary" @click="router.push('/login')">
                        登录
                      </n-button>
                    </div>
                  </div>
                </n-layout-header>
                <n-layout-content class="layout-content">
                  <router-view v-slot="{ Component }">
                    <transition name="fade" mode="out-in">
                      <component :is="Component" />
                    </transition>
                  </router-view>
                </n-layout-content>
              </n-layout>
            </n-layout>
          </div>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NConfigProvider,
  NLayout,
  NLayoutSider,
  NLayoutHeader,
  NLayoutContent,
  NMenu,
  NButton,
  NAvatar,
  NDropdown,
  NBreadcrumb,
  NBreadcrumbItem,
  NSwitch,
  NIcon,
  NMessageProvider,
  NDialogProvider,
  NNotificationProvider,
  darkTheme,
  type MenuOption,
} from 'naive-ui'
import {
  HomeOutline,
  SearchOutline,
  TrophyOutline,
  StarOutline,
  GitCompareOutline,
  NewspaperOutline,
  PersonOutline,
  SunnyOutline,
  MoonOutline,
  LogOutOutline,
  GridOutline,
  WalletOutline,
  NotificationsOutline,
  ChatbubbleOutline,
  AnalyticsOutline,
  SparklesOutline,
} from '@vicons/ionicons5'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const collapsed = ref(false)
const isDark = ref(themeStore.theme === 'dark')

const naiveTheme = computed(() => isDark.value ? darkTheme : null)

const themeOverrides = {
  common: {
    primaryColor: '#3b82f6',
    primaryColorHover: '#60a5fa',
    primaryColorPressed: '#2563eb',
  },
}

const currentKey = computed(() => route.name as string)
const currentTitle = computed(() => (route.meta.title as string) || '首页')

const renderIcon = (icon: any) => {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuKeyToPath: Record<string, string> = {
  Home: '/',
  Dashboard: '/dashboard',
  Search: '/search',
  Ranking: '/ranking',
  SectorRanking: '/sector',
  Favorites: '/favorites',
  Compare: '/compare',
  Portfolio: '/portfolio',
  Alerts: '/alerts',
  AIAssistant: '/ai-assistant',
  Analytics: '/analytics',
  News: '/news',
}

const menuOptions: MenuOption[] = [
  { label: '首页', key: 'Home', icon: renderIcon(HomeOutline) },
  { label: '投资概览', key: 'Dashboard', icon: renderIcon(GridOutline) },
  { label: '基金搜索', key: 'Search', icon: renderIcon(SearchOutline) },
  { label: '基金排行', key: 'Ranking', icon: renderIcon(TrophyOutline) },
  { label: '板块排行', key: 'SectorRanking', icon: renderIcon(GridOutline) },
  { type: 'divider', key: 'd1' },
  { label: '我的收藏', key: 'Favorites', icon: renderIcon(StarOutline) },
  { label: '投资组合', key: 'Portfolio', icon: renderIcon(WalletOutline) },
  { label: '预警管理', key: 'Alerts', icon: renderIcon(NotificationsOutline) },
  { label: '基金对比', key: 'Compare', icon: renderIcon(GitCompareOutline) },
  { type: 'divider', key: 'd2' },
  { label: 'AI 助手', key: 'AIAssistant', icon: renderIcon(SparklesOutline) },
  { label: '高级分析', key: 'Analytics', icon: renderIcon(AnalyticsOutline) },
  { label: '资讯中心', key: 'News', icon: renderIcon(NewspaperOutline) },
]

const userOptions = [
  { label: '个人中心', key: 'profile', icon: renderIcon(PersonOutline) },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: renderIcon(LogOutOutline) },
]

const handleMenuClick = (key: string) => {
  const path = menuKeyToPath[key]
  if (path) {
    router.push(path)
  }
}

const handleUserAction = (key: string) => {
  if (key === 'logout') {
    authStore.logout()
    router.push('/')
  } else if (key === 'profile') {
    router.push('/profile')
  }
}

const toggleTheme = (value: boolean) => {
  themeStore.theme = value ? 'dark' : 'light'
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  transition: all 0.3s ease;
}

.app-container.dark-mode {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}

.layout-sider {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
}

.dark-mode .layout-sider {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.layout-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
}

.dark-mode .layout-header {
  background: rgba(30, 30, 46, 0.8);
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.layout-content {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
