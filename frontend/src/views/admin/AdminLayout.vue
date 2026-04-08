<template>
  <div class="admin-layout">
    <n-layout has-sider>
      <n-layout-sider bordered :width="248" :native-scrollbar="false" class="admin-sider">
        <div class="admin-logo">
          <div class="admin-logo__mark">
            <span class="admin-logo__bar admin-logo__bar--1"></span>
            <span class="admin-logo__bar admin-logo__bar--2"></span>
            <span class="admin-logo__bar admin-logo__bar--3"></span>
          </div>
          <div class="admin-logo__text">
            <span class="admin-logo__label">管理后台</span>
            <span class="admin-logo__sub">Admin Console</span>
          </div>
        </div>
        <n-menu
          :options="menuOptions"
          :value="currentKey"
          @update:value="handleMenuClick"
          class="admin-menu"
        />
      </n-layout-sider>

      <n-layout>
        <header class="admin-header">
          <div class="admin-header__left">
            <h2 class="admin-header__title">{{ pageTitle }}</h2>
          </div>
          <div class="admin-header__right">
            <n-button class="back-btn" @click="router.push('/')">
              <template #icon><n-icon><IconHome /></n-icon></template>
              返回前台
            </n-button>
            <n-dropdown :options="userOptions" @select="handleUserAction">
              <button class="user-btn">
                <div class="user-avatar">
                  {{ authStore.user?.nickname?.charAt(0) || 'A' }}
                </div>
                <span class="user-name">{{ authStore.user?.nickname || '管理员' }}</span>
              </button>
            </n-dropdown>
          </div>
        </header>
        <n-layout-content class="admin-content">
          <slot />
        </n-layout-content>
      </n-layout>
    </n-layout>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NLayout, NLayoutSider, NLayoutHeader, NLayoutContent,
  NMenu, NButton, NDropdown, NIcon, type MenuOption,
} from 'naive-ui'
import {
  IconHome, IconUsers, IconFileText, IconLogout, IconUser,
  IconChartBar, IconClock, IconSettings, IconHistory,
} from '@tabler/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentKey = computed(() => route.name as string)
const pageTitle = computed(() => (route.meta.title as string) || '管理后台')

const renderIcon = (icon: any) => () => h(NIcon, null, { default: () => h(icon) })

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
    AdminDashboard: '/admin', AdminUsers: '/admin/users',
    AdminSessions: '/admin/sessions', AdminSessionConfig: '/admin/session-config',
    AdminSessionLogs: '/admin/session-logs', AdminLogs: '/admin/logs',
  }
  const path = pathMap[key]
  if (path) router.push(path)
}

const handleUserAction = async (key: string) => {
  if (key === 'logout') { await authStore.logout(); router.push('/login') }
  else if (key === 'profile') router.push('/profile')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--bg-layout);
}

.admin-sider {
  background: var(--card-bg) !important;
  border-right: 1px solid var(--border-color) !important;
}

.admin-logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 14px;
  border-bottom: 1px solid var(--border-color);
}

.admin-logo__mark {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: linear-gradient(155deg, rgba(201, 169, 110, 0.12) 0%, rgba(201, 169, 110, 0.04) 100%);
  border: 1px solid rgba(201, 169, 110, 0.15);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  padding: 8px 6px;
  flex-shrink: 0;
}

.admin-logo__bar {
  width: 4px; border-radius: 2px;
  background: var(--gradient-gold-vert);
}

.admin-logo__bar--1 { height: 8px; }
.admin-logo__bar--2 { height: 14px; }
.admin-logo__bar--3 { height: 18px; }

.admin-logo__text { display: flex; flex-direction: column; }
.admin-logo__label { font-size: 16px; font-weight: 600; color: var(--text-primary); }
.admin-logo__sub { font-size: 10px; color: var(--text-tertiary); letter-spacing: 0.08em; text-transform: uppercase; }

.admin-menu { padding: 12px 8px; }

.admin-header {
  height: 64px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-bottom: 1px solid var(--border-color);
}

.admin-header__left { display: flex; align-items: center; }

.admin-header__title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.admin-header__right { display: flex; align-items: center; gap: 16px; }

.back-btn {
  border: 1px solid var(--border-color) !important;
  color: var(--text-secondary) !important;
  background: var(--bg-tertiary) !important;
  font-size: 12px !important;
}

.back-btn:hover {
  border-color: rgba(201, 169, 110, 0.3) !important;
  color: var(--primary-color) !important;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.user-btn:hover { background: var(--bg-tertiary); }

.user-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: linear-gradient(155deg, rgba(201, 169, 110, 0.15), rgba(201, 169, 110, 0.05));
  border: 1px solid rgba(201, 169, 110, 0.2);
  color: var(--primary-color);
  font-weight: 600;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name { font-weight: 500; color: var(--text-primary); font-size: 14px; }

.admin-content {
  padding: 28px;
  min-height: calc(100vh - 64px);
  background: var(--bg-layout);
}
</style>
