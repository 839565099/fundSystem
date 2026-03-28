<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <n-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--users">
            <n-icon size="28"><IconUsers /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
      </n-card>

      <n-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--active">
            <n-icon size="28"><IconCircleCheck /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.activeUsers || 0 }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
      </n-card>

      <n-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--admin">
            <n-icon size="28"><IconShieldCheck /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.adminCount || 0 }}</div>
            <div class="stat-label">管理员数</div>
          </div>
        </div>
      </n-card>

      <n-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--today">
            <n-icon size="28"><IconCalendar /></n-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayNewUsers || 0 }}</div>
            <div class="stat-label">今日新增</div>
          </div>
        </div>
      </n-card>
    </div>

    <!-- 最近用户 -->
    <n-card title="最近注册用户" class="recent-users">
      <template #header-extra>
        <n-button text type="primary" @click="router.push('/admin/users')">
          查看全部
        </n-button>
      </template>
      <n-data-table
        :columns="userColumns"
        :data="recentUsers"
        :loading="loading"
        :bordered="false"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NDataTable,
  NIcon,
  NTag,
  type DataTableColumns,
} from 'naive-ui'
import {
  IconUsers,
  IconCircleCheck,
  IconShieldCheck,
  IconCalendar,
} from '@tabler/icons-vue'
import { adminApi } from '@/api/admin'
import type { UserVO } from '@/types'

const router = useRouter()

interface Stats {
  totalUsers: number
  activeUsers: number
  adminCount: number
  todayNewUsers: number
}

const loading = ref(false)
const stats = ref<Stats>({
  totalUsers: 0,
  activeUsers: 0,
  adminCount: 0,
  todayNewUsers: 0,
})
const recentUsers = ref<UserVO[]>([])

const userColumns: DataTableColumns<UserVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'username' },
  { title: '昵称', key: 'nickname' },
  { title: '邮箱', key: 'email' },
  {
    title: '角色',
    key: 'role',
    render: (row) => h(NTag, {
      type: row.role === 'ADMIN' ? 'warning' : 'default',
      size: 'small',
    }, { default: () => row.role === 'ADMIN' ? '管理员' : '用户' })
  },
  {
    title: '状态',
    key: 'status',
    render: (row) => h(NTag, {
      type: row.status === 1 ? 'success' : 'error',
      size: 'small',
    }, { default: () => row.status === 1 ? '正常' : '禁用' })
  },
  {
    title: '注册时间',
    key: 'createTime',
    render: (row) => row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-'
  },
]

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const [statsRes, usersRes] = await Promise.all([
      adminApi.getStats(),
      adminApi.getUsers({ page: 1, pageSize: 10 })
    ])
    stats.value = statsRes
    recentUsers.value = usersRes.list || []
  } catch (e) {
    console.error('加载数据失败', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon--users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon--active {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon--admin {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon--today {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.recent-users {
  border-radius: 12px;
}
</style>
