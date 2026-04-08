<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="currentThemeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <div class="app-container" :class="{ 'dark-mode': isDark }">
            <SessionCountdown />

            <!-- Auth pages -->
            <router-view v-if="isAuthPage" v-slot="{ Component }">
              <transition name="page" mode="out-in">
                <component :is="Component" :key="route.path" />
              </transition>
            </router-view>

            <!-- Admin layout -->
            <AdminLayout v-else-if="isAdminRoute">
              <router-view v-slot="{ Component }">
                <transition name="page" mode="out-in">
                  <component :is="Component" :key="route.path" />
                </transition>
              </router-view>
            </AdminLayout>

            <!-- Main layout -->
            <n-layout v-else has-sider class="main-layout">
              <n-layout-sider
                bordered
                collapse-mode="width"
                :collapsed-width="72"
                :width="248"
                :collapsed="collapsed"
                show-trigger
                @collapse="collapsed = true"
                @expand="collapsed = false"
                class="layout-sider hide-mobile"
              >
                <div class="sidebar">
                  <!-- Logo -->
                  <div class="logo" :class="{ 'logo--collapsed': collapsed }">
                    <div class="logo__mark">
                      <span class="logo__bar logo__bar--1"></span>
                      <span class="logo__bar logo__bar--2"></span>
                      <span class="logo__bar logo__bar--3"></span>
                    </div>
                    <transition name="logo-text">
                      <div v-if="!collapsed" class="logo__text">
                        <span class="logo__name">Fund</span>
                        <span class="logo__name logo__name--light">Terminal</span>
                      </div>
                    </transition>
                  </div>

                  <!-- Navigation -->
                  <nav class="sidebar-nav">
                    <div class="nav-group" v-for="group in menuGroups" :key="group.key">
                      <transition name="logo-text">
                        <div v-if="!collapsed && group.label" class="nav-group__label">{{ group.label }}</div>
                      </transition>
                      <n-menu
                        :collapsed="collapsed"
                        :collapsed-width="72"
                        :collapsed-icon-size="20"
                        :options="group.items"
                        :value="currentKey"
                        @update:value="handleMenuClick"
                        class="side-menu"
                      />
                      <div v-if="!collapsed" class="nav-group__divider"></div>
                    </div>
                  </nav>
                </div>
              </n-layout-sider>

              <n-layout class="content-layout">
                <!-- Header -->
                <header class="layout-header">
                  <div class="header-content">
                    <div class="header-left">
                      <n-button quaternary class="mobile-menu-btn hide-desktop" @click="showMobileMenu = true">
                        <template #icon><n-icon size="22"><IconMenu2 /></n-icon></template>
                      </n-button>
                      <div class="breadcrumb hide-mobile">
                        <span class="breadcrumb__root">Fund Terminal</span>
                        <span class="breadcrumb__sep">/</span>
                        <span class="breadcrumb__current">{{ currentTitle }}</span>
                      </div>
                      <span class="mobile-title hide-desktop">{{ currentTitle }}</span>
                    </div>

                    <div class="header-actions">
                      <!-- Market Status -->
                      <div class="market-status hide-mobile" :class="{ 'market-status--open': marketIsOpen }">
                        <span class="market-status__dot"></span>
                        <span class="market-status__label">{{ marketStatusLabel }}</span>
                        <span class="market-status__time">{{ marketClock }}</span>
                      </div>

                      <!-- Admin entry -->
                      <n-button
                        v-if="authStore.isAdmin"
                        size="small"
                        class="admin-entry hide-mobile"
                        @click="router.push('/admin')"
                      >
                        <template #icon><n-icon><IconSettings /></n-icon></template>
                        管理后台
                      </n-button>

                      <NotificationBell v-if="authStore.isLoggedIn" />

                      <!-- Theme toggle -->
                      <button class="theme-toggle" @click="toggleTheme(!isDark)" :title="isDark ? '切换亮色' : '切换暗色'">
                        <n-icon size="18">
                          <IconSun v-if="isDark" />
                          <IconMoon v-else />
                        </n-icon>
                      </button>

                      <!-- User -->
                      <n-dropdown v-if="authStore.isLoggedIn" :options="userOptions" @select="handleUserAction">
                        <button class="user-btn">
                          <div class="user-avatar">
                            {{ authStore.user?.nickname?.charAt(0) || authStore.user?.username?.charAt(0) || 'U' }}
                          </div>
                          <span
                            v-if="authStore.user?.email && authStore.user.emailVerified !== 1"
                            class="email-unverified-dot"
                            title="邮箱未验证"
                          ></span>
                        </button>
                      </n-dropdown>
                      <n-button v-else size="small" class="btn-gold" @click="router.push('/login')">
                        登录
                      </n-button>
                    </div>
                  </div>
                </header>

                <!-- Content -->
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

            <!-- Mobile drawer -->
            <n-drawer v-model:show="showMobileMenu" placement="left" :width="280">
              <n-drawer-content title="导航" closable>
                <n-menu
                  :options="allMenuOptions"
                  :value="currentKey"
                  @update:value="handleMobileMenuClick"
                  class="mobile-menu"
                />
                <div v-if="authStore.isAdmin" class="mobile-admin-entry">
                  <n-button type="primary" block @click="() => { router.push('/admin'); showMobileMenu = false }">
                    <template #icon><n-icon><IconSettings /></n-icon></template>
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
  NConfigProvider, NLayout, NLayoutSider, NLayoutContent,
  NMenu, NButton, NDropdown, NIcon,
  NMessageProvider, NDialogProvider, NNotificationProvider,
  NDrawer, NDrawerContent, darkTheme, type MenuOption,
} from 'naive-ui'
import {
  IconHome, IconSearch, IconTrophy, IconStar, IconGitCompare,
  IconNews, IconUser, IconSun, IconMoon, IconLogout, IconLayoutGrid,
  IconWallet, IconBell, IconChartDots3, IconSparkles, IconMenu2,
  IconMail, IconSettings,
} from '@tabler/icons-vue'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { lightThemeOverrides, darkThemeOverrides } from './styles/naive-theme'
import MobileNav from './components/MobileNav.vue'
import NotificationBell from './components/NotificationBell.vue'
import SessionCountdown from './components/SessionCountdown.vue'
import AdminLayout from './views/admin/AdminLayout.vue'

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

const isAuthPage = computed(() => {
  const authPages = ['Login', 'Register', 'ForgotPassword', 'ResetPassword', 'EmailLogin']
  return authPages.includes(route.name as string)
})

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

const marketClock = computed(() => nowTime.value.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }))

const marketIsOpen = computed(() => {
  const now = nowTime.value
  const day = now.getDay()
  if (day === 0 || day === 6) return false
  const minutes = now.getHours() * 60 + now.getMinutes()
  return (minutes >= 570 && minutes < 690) || (minutes >= 780 && minutes < 900)
})

const marketStatusLabel = computed(() => {
  const minutes = nowTime.value.getHours() * 60 + nowTime.value.getMinutes()
  if (marketIsOpen.value) return '交易中'
  if (minutes >= 900 && minutes < 930) return '收盘整理'
  if (minutes >= 540 && minutes < 570) return '盘前准备'
  return '休市'
})

const checkMobile = () => { isMobile.value = window.innerWidth < 769 }

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  timeTicker = window.setInterval(() => { nowTime.value = new Date() }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (timeTicker) clearInterval(timeTicker)
})

const renderIcon = (icon: any) => () => h(NIcon, null, { default: () => h(icon) })

const menuKeyToPath: Record<string, string> = {
  Home: '/', Dashboard: '/dashboard', Search: '/search', Ranking: '/ranking',
  SectorRanking: '/sector', Favorites: '/favorites', Compare: '/compare',
  Portfolio: '/portfolio', Alerts: '/alerts', Notifications: '/notifications',
  AIAssistant: '/ai-assistant', Analytics: '/analytics', News: '/news', Profile: '/profile',
}

interface MenuGroup {
  key: string
  label?: string
  items: MenuOption[]
}

const menuGroups = computed<MenuGroup[]>(() => [
  {
    key: 'main',
    label: '概览',
    items: [
      { label: '首页', key: 'Home', icon: renderIcon(IconHome) },
      { label: '投资概览', key: 'Dashboard', icon: renderIcon(IconLayoutGrid) },
    ],
  },
  {
    key: 'market',
    label: '市场',
    items: [
      { label: '基金搜索', key: 'Search', icon: renderIcon(IconSearch) },
      { label: '基金排行', key: 'Ranking', icon: renderIcon(IconTrophy) },
      { label: '板块排行', key: 'SectorRanking', icon: renderIcon(IconLayoutGrid) },
    ],
  },
  {
    key: 'portfolio',
    label: '投资',
    items: [
      { label: '我的收藏', key: 'Favorites', icon: renderIcon(IconStar) },
      { label: '投资组合', key: 'Portfolio', icon: renderIcon(IconWallet) },
      { label: '预警管理', key: 'Alerts', icon: renderIcon(IconBell) },
      { label: '消息中心', key: 'Notifications', icon: renderIcon(IconMail) },
      { label: '基金对比', key: 'Compare', icon: renderIcon(IconGitCompare) },
    ],
  },
  {
    key: 'tools',
    label: '工具',
    items: [
      { label: 'AI 助手', key: 'AIAssistant', icon: renderIcon(IconSparkles) },
      { label: '高级分析', key: 'Analytics', icon: renderIcon(IconChartDots3) },
      { label: '资讯中心', key: 'News', icon: renderIcon(IconNews) },
    ],
  },
  {
    key: 'account',
    items: [
      { label: '个人中心', key: 'Profile', icon: renderIcon(IconUser) },
    ],
  },
])

// Flat menu for mobile drawer
const allMenuOptions = computed<MenuOption[]>(() => {
  const options: MenuOption[] = []
  menuGroups.value.forEach((group, i) => {
    options.push(...group.items)
    if (i < menuGroups.value.length - 1) {
      options.push({ type: 'divider', key: `d-${group.key}` })
    }
  })
  return options
})

const userOptions = computed(() => [
  { label: '个人中心', key: 'profile', icon: renderIcon(IconUser) },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: renderIcon(IconLogout) },
])

const handleMenuClick = (key: string) => {
  const path = menuKeyToPath[key]
  if (path) router.push(path)
}

const handleMobileMenuClick = (key: string) => {
  handleMenuClick(key)
  showMobileMenu.value = false
}

const handleUserAction = async (key: string) => {
  if (key === 'logout') { await authStore.logout(); router.push('/login') }
  else if (key === 'profile') router.push('/profile')
}

const toggleTheme = (value: boolean) => {
  isDark.value = value
  themeStore.theme = value ? 'dark' : 'light'
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: var(--bg-layout);
  transition: background-color var(--transition-slow);
}

.main-layout { min-height: 100vh; }

/* ═══ SIDEBAR ═══ */
.layout-sider {
  background: var(--card-bg) !important;
  border-right: 1px solid var(--border-color) !important;
  transition: width var(--transition-base);
  overflow: hidden;
}

.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Logo */
.logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 14px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  transition: padding var(--transition-base);
}

.logo--collapsed {
  justify-content: center;
  padding: 0;
}

.logo__mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(155deg, rgba(201, 169, 110, 0.12) 0%, rgba(201, 169, 110, 0.04) 100%);
  border: 1px solid rgba(201, 169, 110, 0.15);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  padding: 8px 6px;
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.logo__bar {
  width: 4px;
  border-radius: 2px;
  background: var(--gradient-gold-vert);
  transition: height var(--transition-base);
}

.logo__bar--1 { height: 8px; }
.logo__bar--2 { height: 14px; }
.logo__bar--3 { height: 18px; }

.logo__text {
  display: flex;
  align-items: baseline;
  gap: 4px;
  white-space: nowrap;
}

.logo__name {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.logo__name--light {
  color: var(--primary-color);
  font-weight: 400;
}

.logo-text-enter-active,
.logo-text-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.logo-text-enter-from,
.logo-text-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* Navigation groups */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.nav-group__label {
  padding: 16px 20px 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.nav-group__divider {
  height: 1px;
  margin: 8px 20px;
  background: linear-gradient(90deg, transparent, var(--border-color), transparent);
}

.nav-group:last-child .nav-group__divider { display: none; }

.side-menu {
  padding: 0 8px;
}

.side-menu :deep(.n-menu-item) {
  border-radius: 8px !important;
  margin: 2px 0 !important;
}

.side-menu :deep(.n-menu-item-content) {
  padding-left: 12px !important;
}

.side-menu :deep(.n-menu-item-content-header) {
  font-weight: 500 !important;
  font-size: 13.5px !important;
}

/* ═══ HEADER ═══ */
.layout-header {
  height: 64px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
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

.header-left { display: flex; align-items: center; gap: 16px; }
.mobile-menu-btn { padding: 8px; color: var(--text-secondary) !important; }

.mobile-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

/* Breadcrumb */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.breadcrumb__root {
  color: var(--text-tertiary);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.breadcrumb__sep { color: var(--text-disabled); }

.breadcrumb__current {
  color: var(--text-primary);
  font-weight: 600;
}

/* Header actions */
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Market status */
.market-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  letter-spacing: 0.02em;
}

.market-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--danger-color);
  box-shadow: 0 0 0 0 rgba(248, 113, 113, 0.4);
  animation: market-pulse 2s infinite;
}

.market-status--open .market-status__dot {
  background: var(--success-color);
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.4);
}

.market-status__label { color: var(--text-tertiary); }
.market-status__time { color: var(--text-secondary); font-family: var(--font-mono); font-weight: 500; }

@keyframes market-pulse {
  0% { transform: scale(0.9); box-shadow: 0 0 0 0 currentColor; }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(0, 0, 0, 0); }
  100% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 0, 0, 0); }
}

/* Admin entry button */
.admin-entry {
  border: 1px solid var(--border-color) !important;
  color: var(--text-secondary) !important;
  background: var(--bg-tertiary) !important;
  font-size: 12px !important;
  transition: all var(--transition-fast) !important;
}

.admin-entry:hover {
  border-color: rgba(201, 169, 110, 0.3) !important;
  color: var(--primary-color) !important;
}

/* Theme toggle */
.theme-toggle {
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.theme-toggle:hover {
  border-color: rgba(201, 169, 110, 0.3);
  color: var(--primary-color);
  background: var(--card-bg-hover);
}

/* User button */
.user-btn {
  position: relative;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(201, 169, 110, 0.2);
  background: linear-gradient(155deg, rgba(201, 169, 110, 0.12), rgba(201, 169, 110, 0.04));
  border-radius: var(--radius-full);
  color: var(--primary-color);
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.user-btn:hover {
  border-color: rgba(201, 169, 110, 0.4);
  box-shadow: var(--shadow-gold-sm);
}

.email-unverified-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--warning-color);
  border: 2px solid var(--card-bg);
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Content */
.layout-content {
  padding: 28px;
  min-height: calc(100vh - 64px);
  background: var(--bg-layout);
}

.layout-content.with-mobile-nav { padding-bottom: 88px; }

.mobile-menu { padding: 8px 0; }
.mobile-admin-entry {
  padding: 16px;
  border-top: 1px solid var(--border-color);
  margin-top: 8px;
}

/* Page transitions */
.page-enter-active { transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1); }
.page-leave-active { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.page-enter-from { opacity: 0; transform: translateX(12px); }
.page-leave-to { opacity: 0; transform: translateX(-12px); }

/* ═══ RESPONSIVE ═══ */
@media (max-width: 768px) {
  .layout-header { height: 56px; padding: 0 16px; }
  .layout-content { padding: 16px; min-height: calc(100vh - 56px); }
  .layout-content.with-mobile-nav { padding-bottom: 80px; }
  .header-actions { gap: 8px; }
  .market-status { display: none !important; }
}

@media (min-width: 769px) {
  .hide-desktop { display: none !important; }
}

@media (max-width: 768px) {
  .hide-mobile { display: none !important; }
}
</style>
