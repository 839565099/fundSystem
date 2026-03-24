<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="currentThemeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <div class="app-container" :class="{ 'dark-mode': isDark }">
            <n-layout has-sider class="main-layout">
              <n-layout-sider
                bordered
                collapse-mode="width"
                :collapsed-width="64"
                :width="240"
                :collapsed="collapsed"
                show-trigger
                @collapse="collapsed = true"
                @expand="collapsed = false"
                class="layout-sider hide-mobile"
              >
                <div class="logo">
                  <div class="logo__icon">
                    <span class="logo__mark logo__mark--a"></span>
                    <span class="logo__mark logo__mark--b"></span>
                    <span class="logo__mark logo__mark--c"></span>
                  </div>
                  <transition name="logo-text">
                    <span v-if="!collapsed" class="logo__text">FundSystem</span>
                  </transition>
                </div>
                <n-menu
                  :collapsed="collapsed"
                  :collapsed-width="64"
                  :collapsed-icon-size="22"
                  :options="menuOptions"
                  :value="currentKey"
                  @update:value="handleMenuClick"
                  class="side-menu"
                />
              </n-layout-sider>
              <n-layout class="content-layout">
                <n-layout-header bordered class="layout-header">
                  <div class="header-content">
                    <div class="header-left">
                      <n-button 
                        quaternary 
                        class="mobile-menu-btn hide-desktop" 
                        @click="showMobileMenu = true"
                      >
                        <template #icon>
                          <n-icon size="24"><MenuOutline /></n-icon>
                        </template>
                      </n-button>
                      <n-breadcrumb class="hide-mobile">
                        <n-breadcrumb-item>
                          <router-link to="/">首页</router-link>
                        </n-breadcrumb-item>
                        <n-breadcrumb-item>{{ currentTitle }}</n-breadcrumb-item>
                      </n-breadcrumb>
                      <span class="mobile-title hide-desktop">{{ currentTitle }}</span>
                    </div>
                    <div class="header-actions">
                      <div
                        class="market-status hide-mobile"
                        :class="{ 'market-status--open': marketIsOpen }"
                      >
                        <span class="market-status__dot"></span>
                        {{ marketStatusLabel }} · {{ marketClock }}
                      </div>
                      <NotificationBell v-if="authStore.isLoggedIn" />
                      <n-tooltip trigger="hover">
                        <template #trigger>
                          <n-switch v-model:value="isDark" @update:value="toggleTheme" size="small">
                            <template #checked>
                              <n-icon><MoonOutline /></n-icon>
                            </template>
                            <template #unchecked>
                              <n-icon><SunnyOutline /></n-icon>
                            </template>
                          </n-switch>
                        </template>
                        {{ isDark ? '切换亮色模式' : '切换深色模式' }}
                      </n-tooltip>
                      
                      <n-dropdown v-if="authStore.isLoggedIn" :options="userOptions" @select="handleUserAction">
                        <n-button text class="user-btn">
                          <n-avatar round size="small" class="user-avatar">
                            {{ authStore.user?.nickname || authStore.user?.username?.charAt(0) || 'U' }}
                          </n-avatar>
                          <span class="user-name hide-mobile">{{ authStore.user?.nickname || authStore.user?.username }}</span>
                        </n-button>
                      </n-dropdown>
                      <n-button v-else type="primary" size="small" @click="router.push('/login')">
                        登录
                      </n-button>
                    </div>
                  </div>
                </n-layout-header>
                <n-layout-content class="layout-content" :class="{ 'with-mobile-nav': isMobile }">
                  <router-view v-slot="{ Component }">
                    <transition name="page" mode="out-in">
                      <keep-alive :include="cachedViews">
                        <component :is="Component" :key="route.path" />
                      </keep-alive>
                    </transition>
                  </router-view>
                </n-layout-content>
              </n-layout>
            </n-layout>
            
            <MobileNav :alert-count="unreadAlertCount" />
            
            <n-drawer v-model:show="showMobileMenu" placement="left" :width="280">
              <n-drawer-content title="菜单" closable>
                <n-menu
                  :options="menuOptions"
                  :value="currentKey"
                  @update:value="handleMobileMenuClick"
                  class="mobile-menu"
                />
              </n-drawer-content>
            </n-drawer>
          </div>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted, onUnmounted, provide } from 'vue'
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
  NTooltip,
  NDrawer,
  NDrawerContent,
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
  AnalyticsOutline,
  SparklesOutline,
  MenuOutline,
  MailOutline,
} from '@vicons/ionicons5'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { lightThemeOverrides, darkThemeOverrides } from './styles/naive-theme'
import MobileNav from './components/MobileNav.vue'
import NotificationBell from './components/NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const collapsed = ref(false)
const isDark = ref(themeStore.theme === 'dark')
const showMobileMenu = ref(false)
const isMobile = ref(false)
const unreadAlertCount = ref(0)
const nowTime = ref(new Date())
let timeTicker: number | undefined

const cachedViews = ['Home', 'Search', 'Ranking']

provide('isMobile', isMobile)

const naiveTheme = computed(() => isDark.value ? darkTheme : null)
const currentThemeOverrides = computed(() => isDark.value ? darkThemeOverrides : lightThemeOverrides)

const currentKey = computed(() => route.name as string)
const currentTitle = computed(() => (route.meta.title as string) || '首页')
const marketClock = computed(() => nowTime.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }))

const marketIsOpen = computed(() => {
  const now = nowTime.value
  const day = now.getDay()
  if (day === 0 || day === 6) return false
  const minutes = now.getHours() * 60 + now.getMinutes()
  const am = minutes >= 570 && minutes < 690
  const pm = minutes >= 780 && minutes < 900
  return am || pm
})

const marketStatusLabel = computed(() => {
  const now = nowTime.value
  const minutes = now.getHours() * 60 + now.getMinutes()
  if (marketIsOpen.value) return '交易中'
  if (minutes >= 900 && minutes < 930) return '收盘整理'
  if (minutes >= 540 && minutes < 570) return '开盘前'
  return '非交易时段'
})

const checkMobile = () => {
  isMobile.value = window.innerWidth < 769
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  timeTicker = window.setInterval(() => {
    nowTime.value = new Date()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (timeTicker) {
    clearInterval(timeTicker)
  }
})

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
  Notifications: '/notifications',
  AIAssistant: '/ai-assistant',
  Analytics: '/analytics',
  News: '/news',
  Profile: '/profile',
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
  { label: '消息中心', key: 'Notifications', icon: renderIcon(MailOutline) },
  { label: '基金对比', key: 'Compare', icon: renderIcon(GitCompareOutline) },
  { type: 'divider', key: 'd2' },
  { label: 'AI 助手', key: 'AIAssistant', icon: renderIcon(SparklesOutline) },
  { label: '高级分析', key: 'Analytics', icon: renderIcon(AnalyticsOutline) },
  { label: '资讯中心', key: 'News', icon: renderIcon(NewspaperOutline) },
  { type: 'divider', key: 'd3' },
  { label: '个人中心', key: 'Profile', icon: renderIcon(PersonOutline) },
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

const handleMobileMenuClick = (key: string) => {
  handleMenuClick(key)
  showMobileMenu.value = false
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
  background: var(--bg-layout);
  transition: background-color var(--transition-slow);
}

.main-layout {
  min-height: 100vh;
}

.layout-sider {
  border-right: 1px solid var(--border-color);
  transition: width var(--transition-base);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
  padding: 0 16px;
}

.logo__icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: linear-gradient(150deg, rgba(0, 183, 201, 0.18), rgba(23, 99, 146, 0.18));
  border: 1px solid rgba(0, 183, 201, 0.22);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 2px;
  padding: 4px 3px;
  flex-shrink: 0;
}

.logo__mark {
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, #57d7e4, #1d8ead);
}

.logo__mark--a {
  height: 9px;
}

.logo__mark--b {
  height: 13px;
}

.logo__mark--c {
  height: 17px;
}

.logo__text {
  font-size: 18px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.logo-text-enter-active,
.logo-text-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.logo-text-enter-from,
.logo-text-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.side-menu {
  padding: 8px;
}

.layout-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  background: var(--card-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mobile-menu-btn {
  padding: 8px;
}

.mobile-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.market-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
}

.market-status__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--danger-color);
  box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.5);
  animation: market-pulse 1.8s infinite;
}

.market-status--open .market-status__dot {
  background: var(--success-color);
  box-shadow: 0 0 0 0 rgba(18, 183, 106, 0.5);
}

@keyframes market-pulse {
  0% {
    transform: scale(0.9);
    box-shadow: 0 0 0 0 currentColor;
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 8px rgba(0, 0, 0, 0);
  }
  100% {
    transform: scale(0.9);
    box-shadow: 0 0 0 0 rgba(0, 0, 0, 0);
  }
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: var(--radius-lg);
  transition: background-color var(--transition-fast);
}

.user-btn:hover {
  background: var(--bg-tertiary);
}

.user-avatar {
  background: var(--gradient-primary);
  color: white;
  font-weight: 600;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
}

.layout-content {
  padding: 24px;
  min-height: calc(100vh - 64px);
  transition: padding var(--transition-base);
}

.layout-content.with-mobile-nav {
  padding-bottom: 88px;
}

.mobile-menu {
  padding: 8px 0;
}

.page-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.page-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@media (max-width: 768px) {
  .layout-header {
    height: 56px;
    padding: 0 16px;
  }
  
  .layout-content {
    padding: 16px;
    min-height: calc(100vh - 56px);
  }
  
  .layout-content.with-mobile-nav {
    padding-bottom: 80px;
  }
  
  .header-actions {
    gap: 12px;
  }
}

@media (min-width: 769px) {
  .hide-desktop {
    display: none !important;
  }
}

@media (max-width: 768px) {
  .hide-mobile {
    display: none !important;
  }
}
</style>
