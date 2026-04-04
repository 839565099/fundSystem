<template>
  <div class="session-log">
    <n-card title="会话审计日志">
      <!-- 搜索筛选栏 -->
      <div class="filter-bar">
        <n-select
          v-model:value="filter.eventType"
          placeholder="事件类型"
          clearable
          :options="eventTypeOptions"
          style="width: 150px"
        />
        <n-input
          v-model:value="filter.username"
          placeholder="用户名"
          clearable
          style="width: 150px"
        />
        <n-date-picker
          v-model:value="timeRange"
          type="datetimerange"
          clearable
          :default-time="['00:00:00', '23:59:59']"
          style="width: 380px"
        />
        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <n-icon><IconSearch /></n-icon>
          </template>
          搜索
        </n-button>
        <n-button @click="handleReset">重置</n-button>
      </div>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="logs"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: SessionLog) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import {
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NButton,
  NIcon,
  NTag,
  NDatePicker,
  type DataTableColumns,
  type PaginationProps,
  useMessage,
} from 'naive-ui'
import { IconSearch } from '@tabler/icons-vue'
import { sessionApi, type SessionLog } from '@/api/session'

const message = useMessage()

// 加载状态
const loading = ref(false)
const logs = ref<SessionLog[]>([])
const timeRange = ref<[number, number] | null>(null)

// 筛选条件
const filter = reactive({
  eventType: null as string | null,
  username: '',
})

// 分页
const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

// 事件类型选项
const eventTypeOptions = [
  { label: '登录', value: 'LOGIN' },
  { label: '警告', value: 'WARNING' },
  { label: '过期', value: 'EXPIRED' },
  { label: '登出', value: 'LOGOUT' },
  { label: '踢出', value: 'KICKED' },
]

// 事件类型标签映射
const eventTypeMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' }> = {
  'LOGIN': { label: '登录', type: 'success' },
  'WARNING': { label: '警告', type: 'warning' },
  'EXPIRED': { label: '过期', type: 'error' },
  'LOGOUT': { label: '登出', type: 'info' },
  'KICKED': { label: '踢出', type: 'error' },
}

// 格式化时间
const formatTime = (time: string | null | undefined): string => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 表格列定义
const columns: DataTableColumns<SessionLog> = [
  { title: '用户名', key: 'username', width: 120 },
  {
    title: '事件类型',
    key: 'eventType',
    width: 100,
    render: (row) => {
      const info = eventTypeMap[row.eventType] || { label: row.eventType, type: 'info' as const }
      return h(NTag, { type: info.type, size: 'small' }, { default: () => info.label })
    },
  },
  { title: 'IP地址', key: 'ipAddress', width: 140 },
  {
    title: '登录时间',
    key: 'loginTime',
    width: 180,
    render: (row) => formatTime(row.loginTime),
  },
  {
    title: '过期时间',
    key: 'expireTime',
    width: 180,
    render: (row) => formatTime(row.expireTime),
  },
  {
    title: '事件时间',
    key: 'eventTime',
    width: 180,
    render: (row) => formatTime(row.eventTime),
  },
  {
    title: '备注',
    key: 'remark',
    ellipsis: { tooltip: true },
    render: (row) => row.remark || '-',
  },
]

// 加载数据
const loadLogs = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      eventType: filter.eventType || undefined,
      username: filter.username || undefined,
    }
    if (timeRange.value) {
      const pad = (n: number) => String(n).padStart(2, '0')
      const fmt = (ts: number) => {
        const d = new Date(ts)
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      }
      params.startTime = fmt(timeRange.value[0])
      params.endTime = fmt(timeRange.value[1])
    }
    const res = await sessionApi.getSessionLogs(params as Parameters<typeof sessionApi.getSessionLogs>[0])
    logs.value = res.records || []
    pagination.itemCount = res.total || 0
  } catch {
    message.error('加载会话审计日志失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadLogs()
}

// 重置
const handleReset = () => {
  filter.eventType = null
  filter.username = ''
  timeRange.value = null
  pagination.page = 1
  loadLogs()
}

// 分页
const handlePageChange = (page: number) => {
  pagination.page = page
  loadLogs()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadLogs()
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.session-log {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
