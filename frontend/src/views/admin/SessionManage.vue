<template>
  <div class="session-manage">
    <n-card>
      <!-- 顶部操作栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <n-tag type="info" size="medium">
            在线会话数：{{ sessions.length }}
          </n-tag>
        </div>
        <div class="toolbar-right">
          <n-button @click="loadSessions">
            <template #icon>
              <n-icon><IconRefresh /></n-icon>
            </template>
            刷新
          </n-button>
          <n-button
            :type="autoRefresh ? 'success' : 'default'"
            @click="toggleAutoRefresh"
          >
            <template #icon>
              <n-icon>
                <IconClock v-if="!autoRefresh" />
                <IconClockPause v-else />
              </n-icon>
            </template>
            {{ autoRefresh ? `自动刷新中（${countdown}s）` : '开启自动刷新' }}
          </n-button>
        </div>
      </div>

      <!-- 会话列表 -->
      <n-data-table
        :columns="columns"
        :data="sessions"
        :loading="loading"
        :row-key="(row: SessionInfo) => row.sessionId"
        :row-props="rowProps"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, onUnmounted } from 'vue'
import {
  NCard,
  NDataTable,
  NButton,
  NIcon,
  NTag,
  NSpace,
  type DataTableColumns,
  useDialog,
  useMessage,
} from 'naive-ui'
import {
  IconRefresh,
  IconClock,
  IconClockPause,
  IconLogout,
} from '@tabler/icons-vue'
import { sessionApi, type SessionInfo } from '@/api/session'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const sessions = ref<SessionInfo[]>([])
const autoRefresh = ref(false)
const countdown = ref(30)
let timer: ReturnType<typeof setInterval> | null = null

const columns: DataTableColumns<SessionInfo> = [
  {
    title: '用户名',
    key: 'username',
  },
  {
    title: '角色',
    key: 'role',
    width: 100,
    render: (row) => h(NTag, {
      type: row.role === 'ADMIN' ? 'warning' : 'default',
      size: 'small',
    }, { default: () => row.role === 'ADMIN' ? '管理员' : '用户' }),
  },
  {
    title: '登录时间',
    key: 'loginTime',
    width: 180,
    render: (row) => row.loginTime ? new Date(row.loginTime).toLocaleString('zh-CN') : '-',
  },
  {
    title: '过期时间',
    key: 'expireTime',
    width: 180,
    render: (row) => row.expireTime ? new Date(row.expireTime).toLocaleString('zh-CN') : '-',
  },
  {
    title: '剩余时间',
    key: 'remainingMinutes',
    width: 120,
    render: (row) => {
      const minutes = Math.round(row.remainingMinutes)
      const type = minutes < 5 ? 'error' : minutes < 15 ? 'warning' : 'success'
      return h(NTag, { type, size: 'small' }, {
        default: () => minutes >= 60
          ? `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`
          : `${minutes}分钟`,
      })
    },
  },
  {
    title: 'IP 地址',
    key: 'ipAddress',
    width: 140,
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) => h(NSpace, null, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: 'error',
          secondary: true,
          onClick: () => handleKick(row),
        }, {
          icon: () => h(NIcon, null, { default: () => h(IconLogout) }),
          default: () => '强制下线',
        }),
      ],
    }),
  },
]

const rowProps = (row: SessionInfo) => {
  return {
    style: row.remainingMinutes < 5 ? 'background-color: var(--n-td-color-error);' : '',
  }
}

onMounted(() => {
  loadSessions()
})

onUnmounted(() => {
  stopAutoRefresh()
})

const loadSessions = async () => {
  loading.value = true
  try {
    sessions.value = await sessionApi.getActiveSessions()
  } catch (e) {
    message.error('加载会话列表失败')
  } finally {
    loading.value = false
  }
}

const handleKick = (session: SessionInfo) => {
  dialog.error({
    title: '确认强制下线',
    content: `确定要将用户 "${session.username}" 强制下线吗？该用户将被立即踢出系统。`,
    positiveText: '确定下线',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await sessionApi.kickUser(session.userId)
        message.success(`用户 "${session.username}" 已被强制下线`)
        loadSessions()
      } catch (e) {
        message.error('强制下线失败')
      }
    },
  })
}

const toggleAutoRefresh = () => {
  if (autoRefresh.value) {
    stopAutoRefresh()
  } else {
    startAutoRefresh()
  }
}

const startAutoRefresh = () => {
  autoRefresh.value = true
  countdown.value = 30
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      countdown.value = 30
      loadSessions()
    }
  }, 1000)
}

const stopAutoRefresh = () => {
  autoRefresh.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  countdown.value = 30
}
</script>

<style scoped>
.session-manage {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}
</style>
