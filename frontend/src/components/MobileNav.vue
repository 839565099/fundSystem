<template>
  <nav class="mobile-nav hide-desktop">
    <div
      v-for="item in navItems"
      :key="item.key"
      class="nav-item"
      :class="{ 'nav-item--active': isActive(item) }"
      @click="handleNavClick(item)"
    >
      <component :is="item.icon" class="nav-item__icon" />
      <span class="nav-item__label">{{ item.label }}</span>
      <div v-if="item.badge && item.badge > 0" class="nav-item__badge">
        {{ item.badge > 99 ? '99+' : item.badge }}
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { IconHome, IconChartBar, IconSearch, IconHeart, IconBell } from '@tabler/icons-vue'

interface NavItem {
  key: string; label: string; path: string; icon: any; badge?: number; authRequired?: boolean;
}

interface Props { alertCount?: number }
const props = withDefaults(defineProps<Props>(), { alertCount: 0 })

const router = useRouter()
const route = useRoute()

const navItems = computed<NavItem[]>(() => [
  { key: 'home', label: '首页', path: '/', icon: h(IconHome) },
  { key: 'dashboard', label: '概览', path: '/dashboard', icon: h(IconChartBar), authRequired: true },
  { key: 'search', label: '搜索', path: '/search', icon: h(IconSearch) },
  { key: 'favorites', label: '收藏', path: '/favorites', icon: h(IconHeart), authRequired: true },
  { key: 'alerts', label: '预警', path: '/alerts', icon: h(IconBell), badge: props.alertCount, authRequired: true },
])

const isActive = (item: NavItem) => item.path === '/' ? route.path === '/' : route.path.startsWith(item.path)
const handleNavClick = (item: NavItem) => { if (!isActive(item)) router.push(item.path) }
</script>

<style scoped>
.mobile-nav {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  height: 64px;
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-top: 1px solid var(--glass-border);
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: var(--z-fixed);
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  color: var(--text-tertiary);
  font-size: 10px;
  transition: all var(--transition-fast);
  cursor: pointer;
  border-radius: var(--radius-lg);
  min-width: 60px;
}

.nav-item:active { transform: scale(0.95); }

.nav-item--active { color: var(--primary-color); }
.nav-item--active .nav-item__icon { filter: drop-shadow(0 0 6px rgba(201, 169, 110, 0.4)); }

.nav-item:hover:not(.nav-item--active) { color: var(--text-secondary); }

.nav-item__icon {
  font-size: 22px;
  transition: transform var(--transition-fast);
}

.nav-item--active .nav-item__icon { transform: scale(1.1); }

.nav-item__label { font-weight: 500; line-height: 1; }

.nav-item__badge {
  position: absolute;
  top: 2px; right: 6px;
  min-width: 16px; height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  color: white;
  background: var(--danger-color);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(248, 113, 113, 0.3);
}

@media (min-width: 769px) { .mobile-nav { display: none; } }
</style>
