<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="currentThemeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <div class="app-container" :class="{ 'dark-mode': isDark }">
            <!-- 认证页面（登录/注册等）直接显示 router-view -->
            <router-view v-if="isAuthPage" v-slot="{ Component }">
              <transition name="page" mode="out-in">
                <component :is="Component" :key="route.path" />
              </transition>
            </router-view>

            <!-- 正常布局页面 -->
            <n-layout v-else has-sider class="main-layout">
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
                          <n-icon size="24"><IconMenu2 /></n-icon>
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
                      <!-- 管理员入口按钮 -->
                      <n-button 
                        v-if="authStore.isAdmin" 
                        type="primary" 
                        size="small"
                        @click="router.push('/admin')"
                        class="hide-mobile"
                      >
                        <template #icon>
                          <n-icon><IconSettings /></n-icon>
                        </template>
                        管理后台
                      </n-button>
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
                              <n-icon><IconMoon /></n-icon>
                            </template>
                            <template #unchecked>
                              <n-icon><IconSun /></n-icon>
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
                <!-- 管理员入口（移动端） -->
                <div v-if="authStore.isAdmin" class="mobile-admin-entry">
                  <n-button 
                    type="primary" 
                    block
                    @click="() => { router.push('/admin'); showMobileMenu = false }"
                  >
                    <template #icon>
                      <n-icon><IconSettings /></n-icon>
                    </template>
                    管理后台
                  </n-button>
                </div>
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
  IconHome,
  IconSearch,
  IconTrophy,
  IconStar,
  IconGitCompare,
  IconNews,
  IconUser,
  IconSun,
  IconMoon,
  IconLogout,
  IconLayoutGrid,
  IconWallet,
  IconBell,
  IconChartDots3,
  IconSparkles,
  IconMenu2,
  IconMail,
  IconSettings,
} from '@tabler/icons-vue'
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

// 判断是否是认证相关页面（登录、注册、忘记密码等）
const isAuthPage = computed(() => {
  const authPages = ['Login', 'Register', 'ForgotPassword', 'ResetPassword']
  return authPages.includes(route.name as string)
})

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

const menuOptions = computed<MenuOption[]>(() => [
  { label: '首页', key: 'Home', icon: renderIcon(IconHome) },
  { label: '投资概览', key: 'Dashboard', icon: renderIcon(IconLayoutGrid) },
  { label: '基金搜索', key: 'Search', icon: renderIcon(IconSearch) },
  { label: '基金排行', key: 'Ranking', icon: renderIcon(IconTrophy) },
  { label: '板块排行', key: 'SectorRanking', icon: renderIcon(IconLayoutGrid) },
  { type: 'divider', key: 'd1' },
  { label: '我的收藏', key: 'Favorites', icon: renderIcon(IconStar) },
  { label: '投资组合', key: 'Portfolio', icon: renderIcon(IconWallet) },
  { label: '预警管理', key: 'Alerts', icon: renderIcon(IconBell) },
  { label: '消息中心', key: 'Notifications', icon: renderIcon(IconMail) },
  { label: '基金对比', key: 'Compare', icon: renderIcon(IconGitCompare) },
  { type: 'divider', key: 'd2' },
  { label: 'AI 助手', key: 'AIAssistant', icon: renderIcon(IconSparkles) },
  { label: '高级分析', key: 'Analytics', icon: renderIcon(IconChartDots3) },
  { label: '资讯中心', key: 'News', icon: renderIcon(IconNews) },
  { type: 'divider', key: 'd3' },
  { label: '个人中心', key: 'Profile', icon: renderIcon(IconUser) },
])

const userOptions = computed(() => {
  const options: any[] = [
    { label: '个人中心', key: 'profile', icon: renderIcon(IconUser) },
  ]
  options.push({ type: 'divider', key: 'd1' })
  options.push({ label: '退出登录', key: 'logout', icon: renderIcon(IconLogout) })
  return options
})

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
    router.push('/login')
  } else if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'admin') {
    router.push('/admin')
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
  background: linear-gradient(150deg, rgba(201, 169, 110, 0.15), rgba(39, 39, 46, 0.12));
  border: 1px solid rgba(201, 169, 110, 0.22);
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
  background: linear-gradient(180deg, #D4BA82, #B08D4E);
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
  box-shadow: 0 0 0 0 rgba(229, 77, 66, 0.5);
  animation: market-pulse 1.8s infinite;
}

.market-status--open .market-status__dot {
  background: var(--success-color);
  box-shadow: 0 0 0 0 rgba(43, 164, 113, 0.5);
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

/* 移动端管理员入口 */
.mobile-admin-entry {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  margin-top: 8px;
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
