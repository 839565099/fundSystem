<template>
  <div class="page-container">
    <div class="search-header card">
      <n-input
        v-model:value="keyword"
        placeholder="输入基金代码或名称搜索"
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
      <n-button type="primary" size="large" @click="handleSearch" :loading="loading">
        搜索
      </n-button>
    </div>

    <div class="filter-section card">
      <div class="filter-row">
        <span class="filter-label">基金类型:</span>
        <n-select
          v-model:value="filters.fundType"
          :options="typeOptions"
          placeholder="全部"
          clearable
          style="width: 150px;"
        />
      </div>
      <div class="filter-row">
        <span class="filter-label">风险等级:</span>
        <n-select
          v-model:value="filters.riskLevel"
          :options="riskOptions"
          placeholder="全部"
          clearable
          style="width: 120px;"
        />
      </div>
      <div class="filter-row">
        <span class="filter-label">排序:</span>
        <n-select
          v-model:value="filters.sortBy"
          :options="sortOptions"
          style="width: 150px;"
        />
      </div>
    </div>

    <n-spin :show="loading">
      <div v-if="funds.length > 0" class="results-section">
        <div class="results-header">
          <span>共 {{ total }} 只基金</span>
        </div>
        <n-data-table
          :columns="columns"
          :data="funds"
          :row-key="(row: Fund) => row.fundCode"
          @update:checked-row-keys="handleRowClick"
        />
        <div class="pagination">
          <n-pagination
            v-model:page="pageNum"
            :page-count="pageCount"
            @update:page="handlePageChange"
          />
        </div>
      </div>
      <n-empty v-else-if="!loading && searched" description="未找到符合条件的基金" />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NButton, NIcon, NSelect, NSpin, NEmpty, NDataTable, NPagination, type DataTableColumns, createDiscreteApi } from 'naive-ui'
import { IconSearch as SearchOutline } from '@tabler/icons-vue'
import { fundApi } from '../api/fund'
import type { Fund } from '../types'

const { message } = createDiscreteApi(['message'])

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const funds = ref<Fund[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const pageCount = ref(1)

// 防抖定时器
let searchTimer: ReturnType<typeof setTimeout> | null = null

// 实时搜索（带防抖）
const performSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 300) // 300ms 防抖延迟
}

const filters = reactive({
  fundType: null as string | null,
  riskLevel: null as number | null,
  sortBy: 'dayGrowth',
})

const typeOptions = [
  { label: '全部', value: '' },
  { label: '股票型', value: '股票型' },
  { label: '混合型', value: '混合型' },
  { label: '债券型', value: '债券型' },
  { label: '指数型', value: '指数型' },
  { label: 'QDII', value: 'QDII' },
]

const riskOptions = [
  { label: '全部', value: 0 },
  { label: '低风险', value: 1 },
  { label: '中低风险', value: 2 },
  { label: '中风险', value: 3 },
  { label: '中高风险', value: 4 },
  { label: '高风险', value: 5 },
]

const sortOptions = [
  { label: '日涨跌幅', value: 'dayGrowth' },
  { label: '近一周', value: 'weekGrowth' },
  { label: '近一月', value: 'monthGrowth' },
  { label: '近一年', value: 'yearGrowth' },
  { label: '基金规模', value: 'fundScale' },
]

const formatGrowth = (value?: number) => {
  if (value === undefined || value === null) return '--'
  const cls = value >= 0 ? 'growth-positive' : 'growth-negative'
  return h('span', { class: cls }, `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`)
}

const columns: DataTableColumns<Fund> = [
  {
    title: '基金代码',
    key: 'fundCode',
    width: 100,
    render: (row) => h('a', {
      style: 'color: var(--primary-color); cursor: pointer;',
      onClick: () => router.push(`/fund/${row.fundCode}`)
    }, row.fundCode),
  },
  {
    title: '基金名称',
    key: 'fundName',
    ellipsis: { tooltip: true },
  },
  {
    title: '类型',
    key: 'fundType',
    width: 100,
  },
  {
    title: '净值',
    key: 'nav',
    width: 100,
    render: (row) => row.nav?.toFixed(4) || '--',
  },
  {
    title: '日涨跌',
    key: 'dayGrowth',
    width: 100,
    render: (row) => formatGrowth(row.dayGrowth),
  },
  {
    title: '近一周',
    key: 'weekGrowth',
    width: 100,
    render: (row) => formatGrowth(row.weekGrowth),
  },
  {
    title: '近一年',
    key: 'yearGrowth',
    width: 100,
    render: (row) => formatGrowth(row.yearGrowth),
  },
]

const handleSearch = async () => {
  // 实时搜索时，如果没有关键词也可以搜索（根据筛选条件）
  if (!keyword.value && !filters.fundType && !filters.riskLevel) {
    // 清空结果
    funds.value = []
    total.value = 0
    searched.value = false
    return
  }

  loading.value = true
  searched.value = true
  pageNum.value = 1

  try {
    const data = await fundApi.search({
      keyword: keyword.value || undefined,
      fundType: filters.fundType || undefined,
      riskLevel: filters.riskLevel || undefined,
      sortBy: filters.sortBy,
      sortOrder: 'desc',
      pageNum: pageNum.value,
      pageSize,
    })
    funds.value = data.records || []
    total.value = data.total || 0
    pageCount.value = data.pages || 1
  } catch {
    message.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 监听关键词输入变化，实现实时搜索
watch(keyword, () => {
  performSearch()
})

// 监听筛选条件变化
watch([() => filters.fundType, () => filters.riskLevel, () => filters.sortBy], () => {
  if (keyword.value || filters.fundType || filters.riskLevel) {
    performSearch()
  }
})

const handlePageChange = async (page: number) => {
  pageNum.value = page
  await handleSearch()
}

const handleRowClick = (keys: Array<string | number>) => {
  if (keys.length > 0) {
    router.push(`/fund/${keys[0]}`)
  }
}
</script>

<style scoped>
.search-header {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 24px;
}

.search-header :deep(.n-input) {
  flex: 1;
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 24px;
  padding: 16px 24px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.results-section {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.results-header {
  margin-bottom: 16px;
  color: var(--text-secondary);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
