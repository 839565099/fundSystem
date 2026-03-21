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
                  <div class="logo__icon">📈</div>
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
} from '@vicons/ionicons5'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { lightThemeOverrides, darkThemeOverrides } from './styles/naive-theme'
import MobileNav from './components/MobileNav.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const collapsed = ref(false)
const isDark = ref(themeStore.theme === 'dark')
const showMobileMenu = ref(false)
const isMobile = ref(false)
const unreadAlertCount = ref(0)

const cachedViews = ['Home', 'Search', 'Ranking']

provide('isMobile', isMobile)

const naiveTheme = computed(() => isDark.value ? darkTheme : null)
const currentThemeOverrides = computed(() => isDark.value ? darkThemeOverrides : lightThemeOverrides)

const currentKey = computed(() => route.name as string)
const currentTitle = computed(() => (route.meta.title as string) || '首页')

const checkMobile = () => {
  isMobile.value = window.innerWidth < 769
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
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
  font-size: 24px;
  flex-shrink: 0;
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
