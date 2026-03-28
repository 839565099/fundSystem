<template>
  <div class="operation-log">
    <n-card>
      <n-tabs v-model:value="activeTab" type="line" @update:value="handleTabChange">
        <n-tab-pane name="operation" tab="操作日志">
          <!-- 操作日志筛选 -->
          <div class="filter-bar">
            <n-input
              v-model:value="opFilter.username"
              placeholder="操作人"
              clearable
              style="width: 150px"
            />
            <n-select
              v-model:value="opFilter.operation"
              placeholder="操作类型"
              clearable
              :options="operationOptions"
              style="width: 150px"
            />
            <n-button type="primary" @click="loadOperationLogs">
              <template #icon>
                <n-icon><IconSearch /></n-icon>
              </template>
              搜索
            </n-button>
            <n-button @click="resetOpFilter">重置</n-button>
          </div>

          <!-- 操作日志表格 -->
          <n-data-table
            :columns="opColumns"
            :data="opLogs"
            :loading="opLoading"
            :pagination="opPagination"
            :row-key="(row: OperationLogItem) => row.id"
            @update:page="handleOpPageChange"
            @update:page-size="handleOpPageSizeChange"
          />
        </n-tab-pane>

        <n-tab-pane name="system" tab="系统日志">
          <!-- 系统日志筛选 -->
          <div class="filter-bar">
            <n-select
              v-model:value="sysFilter.logType"
              placeholder="日志类型"
              clearable
              :options="logTypeOptions"
              style="width: 150px"
            />
            <n-button type="primary" @click="loadSystemLogs">
              <template #icon>
                <n-icon><IconSearch /></n-icon>
              </template>
              搜索
            </n-button>
            <n-button @click="resetSysFilter">重置</n-button>
          </div>

          <!-- 系统日志表格 -->
          <n-data-table
            :columns="sysColumns"
            :data="sysLogs"
            :loading="sysLoading"
            :pagination="sysPagination"
            :row-key="(row: SystemLogItem) => row.id"
            @update:page="handleSysPageChange"
            @update:page-size="handleSysPageSizeChange"
          />
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import {
  NCard,
  NTabs,
  NTabPane,
  NDataTable,
  NInput,
  NSelect,
  NButton,
  NIcon,
  NTag,
  type DataTableColumns,
  type PaginationProps,
  useMessage,
} from 'naive-ui'
import { IconSearch } from '@tabler/icons-vue'
import { adminApi, type OperationLogItem, type SystemLogItem } from '@/api/admin'

const message = useMessage()

// Tab 状态
const activeTab = ref('operation')

// 操作日志状态
const opLoading = ref(false)
const opLogs = ref<OperationLogItem[]>([])
const opFilter = reactive({
  username: '',
  operation: null as string | null,
})
const opPagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

// 系统日志状态
const sysLoading = ref(false)
const sysLogs = ref<SystemLogItem[]>([])
const sysFilter = reactive({
  logType: null as string | null,
})
const sysPagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

// 操作类型选项
const operationOptions = [
  { label: '启用用户', value: 'ENABLE_USER' },
  { label: '禁用用户', value: 'DISABLE_USER' },
  { label: '变更角色', value: 'CHANGE_ROLE' },
  { label: '删除用户', value: 'DELETE_USER' },
]

// 日志类型选项
const logTypeOptions = [
  { label: '数据同步', value: 'DATA_SYNC' },
  { label: '数据清理', value: 'DATA_CLEANUP' },
  { label: '定时任务', value: 'SCHEDULED_TASK' },
]

// 操作日志表格列
const opColumns: DataTableColumns<OperationLogItem> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '操作人', key: 'username', width: 120 },
  {
    title: '操作类型',
    key: 'operation',
    width: 120,
    render: (row) => {
      const typeMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' }> = {
        'ENABLE_USER': { label: '启用用户', type: 'success' },
        'DISABLE_USER': { label: '禁用用户', type: 'error' },
        'CHANGE_ROLE': { label: '变更角色', type: 'warning' },
        'DELETE_USER': { label: '删除用户', type: 'error' },
      }
      const info = typeMap[row.operation] || { label: row.operation, type: 'info' }
      return h(NTag, { type: info.type, size: 'small' }, { default: () => info.label })
    }
  },
  { title: '目标类型', key: 'targetType', width: 100 },
  { title: '目标ID', key: 'targetId', width: 100 },
  {
    title: '详情',
    key: 'detail',
    ellipsis: { tooltip: true },
    render: (row) => row.detail || '-'
  },
  { title: 'IP', key: 'ip', width: 130 },
  {
    title: '操作时间',
    key: 'createTime',
    width: 180,
    render: (row) => row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-'
  },
]

// 系统日志表格列
const sysColumns: DataTableColumns<SystemLogItem> = [
  { title: 'ID', key: 'id', width: 80 },
  {
    title: '日志类型',
    key: 'logType',
    width: 120,
    render: (row) => {
      const typeMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' }> = {
        'DATA_SYNC': { label: '数据同步', type: 'info' },
        'DATA_CLEANUP': { label: '数据清理', type: 'warning' },
        'SCHEDULED_TASK': { label: '定时任务', type: 'default' },
      }
      const info = typeMap[row.logType] || { label: row.logType, type: 'default' }
      return h(NTag, { type: info.type, size: 'small' }, { default: () => info.label })
    }
  },
  {
    title: '内容',
    key: 'content',
    ellipsis: { tooltip: true },
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => h(NTag, {
      type: row.status === 'SUCCESS' ? 'success' : 'error',
      size: 'small',
    }, { default: () => row.status === 'SUCCESS' ? '成功' : '失败' })
  },
  {
    title: '耗时(ms)',
    key: 'durationMs',
    width: 100,
    render: (row) => row.durationMs ?? '-'
  },
  {
    title: '错误信息',
    key: 'errorMsg',
    ellipsis: { tooltip: true },
    render: (row) => row.errorMsg || '-'
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
    render: (row) => row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-'
  },
]

// 加载操作日志
const loadOperationLogs = async () => {
  opLoading.value = true
  try {
    const res = await adminApi.getOperationLogs({
      page: opPagination.page!,
      pageSize: opPagination.pageSize!,
      username: opFilter.username || undefined,
      operation: opFilter.operation || undefined,
    })
    opLogs.value = res.list || []
    opPagination.itemCount = res.total || 0
  } catch (e) {
    message.error('加载操作日志失败')
  } finally {
    opLoading.value = false
  }
}

// 加载系统日志
const loadSystemLogs = async () => {
  sysLoading.value = true
  try {
    const res = await adminApi.getSystemLogs({
      page: sysPagination.page!,
      pageSize: sysPagination.pageSize!,
      logType: sysFilter.logType || undefined,
    })
    sysLogs.value = res.list || []
    sysPagination.itemCount = res.total || 0
  } catch (e) {
    message.error('加载系统日志失败')
  } finally {
    sysLoading.value = false
  }
}

// 重置操作日志筛选
const resetOpFilter = () => {
  opFilter.username = ''
  opFilter.operation = null
  opPagination.page = 1
  loadOperationLogs()
}

// 重置系统日志筛选
const resetSysFilter = () => {
  sysFilter.logType = null
  sysPagination.page = 1
  loadSystemLogs()
}

// Tab 切换
const handleTabChange = (tab: string) => {
  if (tab === 'operation' && opLogs.value.length === 0) {
    loadOperationLogs()
  } else if (tab === 'system' && sysLogs.value.length === 0) {
    loadSystemLogs()
  }
}

// 操作日志分页
const handleOpPageChange = (page: number) => {
  opPagination.page = page
  loadOperationLogs()
}

const handleOpPageSizeChange = (pageSize: number) => {
  opPagination.pageSize = pageSize
  opPagination.page = 1
  loadOperationLogs()
}

// 系统日志分页
const handleSysPageChange = (page: number) => {
  sysPagination.page = page
  loadSystemLogs()
}

const handleSysPageSizeChange = (pageSize: number) => {
  sysPagination.pageSize = pageSize
  sysPagination.page = 1
  loadSystemLogs()
}

onMounted(() => {
  loadOperationLogs()
})
</script>

<style scoped>
.operation-log {
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
