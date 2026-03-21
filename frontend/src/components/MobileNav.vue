<template>
  <nav class="mobile-nav hide-desktop">
    <div
      v-for="item in navItems"
      :key="item.key"
      class="mobile-nav__item"
      :class="{ 'mobile-nav__item--active': isActive(item) }"
      @click="handleNavClick(item)"
    >
      <component :is="item.icon" class="mobile-nav__icon" />
      <span class="mobile-nav__label">{{ item.label }}</span>
      <div v-if="item.badge && item.badge > 0" class="mobile-nav__badge">
        {{ item.badge > 99 ? '99+' : item.badge }}
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  HomeOutline,
  StatsChartOutline,
  SearchOutline,
  HeartOutline,
  NotificationsOutline
} from '@vicons/ionicons5'

interface NavItem {
  key: string
  label: string
  path: string
  icon: any
  badge?: number
  authRequired?: boolean
}

interface Props {
  alertCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  alertCount: 0
})

const router = useRouter()
const route = useRoute()

const navItems = computed<NavItem[]>(() => [
  {
    key: 'home',
    label: '首页',
    path: '/',
    icon: h(HomeOutline)
  },
  {
    key: 'dashboard',
    label: '概览',
    path: '/dashboard',
    icon: h(StatsChartOutline),
    authRequired: true
  },
  {
    key: 'search',
    label: '搜索',
    path: '/search',
    icon: h(SearchOutline)
  },
  {
    key: 'favorites',
    label: '收藏',
    path: '/favorites',
    icon: h(HeartOutline),
    authRequired: true
  },
  {
    key: 'alerts',
    label: '预警',
    path: '/alerts',
    icon: h(NotificationsOutline),
    badge: props.alertCount,
    authRequired: true
  }
])

const isActive = (item: NavItem) => {
  if (item.path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(item.path)
}

const handleNavClick = (item: NavItem) => {
  if (isActive(item)) return
  router.push(item.path)
}
</script>

<style scoped>
.mobile-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: var(--card-bg);
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: var(--z-fixed);
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
}

.mobile-nav__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  color: var(--text-tertiary);
  font-size: 11px;
  transition: all var(--transition-fast);
  cursor: pointer;
  border-radius: var(--radius-lg);
  min-width: 64px;
}

.mobile-nav__item:active {
  transform: scale(0.95);
}

.mobile-nav__item--active {
  color: var(--primary-color);
  background: rgba(59, 130, 246, 0.1);
}

.mobile-nav__item:hover:not(.mobile-nav__item--active) {
  color: var(--text-secondary);
}

.mobile-nav__icon {
  font-size: 22px;
  transition: transform var(--transition-fast);
}

.mobile-nav__item--active .mobile-nav__icon {
  transform: scale(1.1);
}

.mobile-nav__label {
  font-weight: 500;
  line-height: 1;
}

.mobile-nav__badge {
  position: absolute;
  top: 2px;
  right: 8px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  color: white;
  background: var(--danger-color);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
}

@media (max-width: 768px) {
  .mobile-nav {
    display: flex;
  }
}

@media (min-width: 769px) {
  .mobile-nav {
    display: none;
  }
}
</style>
