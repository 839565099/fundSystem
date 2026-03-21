<template>
  <div class="page-container">
    <PageHeader title="板块涨跌排行" icon="📊">
      <template #actions>
        <n-button @click="loadSectors" :loading="loading">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          刷新
        </n-button>
      </template>
    </PageHeader>

    <!-- 筛选条件 -->
    <div class="filter-card card">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">板块类型</span>
          <n-button-group size="small">
            <n-button
              v-for="t in typeOptions"
              :key="t.value"
              :type="queryParams.type === t.value ? 'primary' : 'default'"
              @click="changeType(t.value)"
            >
              {{ t.label }}
            </n-button>
          </n-button-group>
        </div>

        <div class="filter-item">
          <span class="filter-label">排序方式</span>
          <n-select
            v-model:value="queryParams.sortBy"
            :options="sortOptions"
            size="small"
            style="width: 120px"
            @update:value="loadSectors"
          />
        </div>

        <div class="filter-item">
          <span class="filter-label">搜索</span>
          <n-input
            v-model:value="queryParams.keyword"
            placeholder="板块名称"
            size="small"
            clearable
            style="width: 150px"
            @keyup.enter="loadSectors"
            @clear="loadSectors"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
        </div>
      </div>
    </div>

    <!-- 涨跌榜概览 -->
    <div class="overview-section" v-if="sectors.length > 0">
      <div class="overview-card up">
        <div class="overview-header">
          <n-icon size="18" color="var(--up-color)"><TrendingUpOutline /></n-icon>
          <span>涨幅榜 TOP 5</span>
        </div>
        <div class="overview-list">
          <div
            v-for="(item, index) in topGainers"
            :key="item.code"
            class="overview-item"
            @click="goToDetail(item.code)"
          >
            <span class="rank">{{ index + 1 }}</span>
            <span class="name">{{ item.name }}</span>
            <span class="growth">+{{ item.changePercent?.toFixed(2) }}%</span>
          </div>
        </div>
      </div>

      <div class="overview-card down">
        <div class="overview-header">
          <n-icon size="18" color="var(--down-color)"><TrendingDownOutline /></n-icon>
          <span>跌幅榜 TOP 5</span>
        </div>
        <div class="overview-list">
          <div
            v-for="(item, index) in topLosers"
            :key="item.code"
            class="overview-item"
            @click="goToDetail(item.code)"
          >
            <span class="rank">{{ index + 1 }}</span>
            <span class="name">{{ item.name }}</span>
            <span class="growth">{{ item.changePercent?.toFixed(2) }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 板块列表 -->
    <div class="sector-table card">
      <n-data-table
        :columns="columns"
        :data="sectors"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: SectorVO) => row.code"
        @update:page="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NButton, NDataTable, NButtonGroup, NSelect, NInput, NTag, createDiscreteApi } from 'naive-ui'
import { RefreshOutline, SearchOutline, TrendingUpOutline, TrendingDownOutline } from '@vicons/ionicons5'
import PageHeader from '../components/PageHeader.vue'
import { sectorApi } from '../api/sector'
import type { SectorVO, SectorType } from '../types/sector'

const { message } = createDiscreteApi(['message'])

const router = useRouter()

const loading = ref(false)
const sectors = ref<SectorVO[]>([])

const queryParams = reactive({
  type: 'all' as SectorType,
  sortBy: 'dayGrowth' as 'dayGrowth' | 'weekGrowth' | 'monthGrowth' | 'volume' | 'turnover',
  sortOrder: 'desc' as 'asc' | 'desc',
  keyword: '',
  pageNum: 1,
  pageSize: 20
})

const typeOptions = [
  { label: '全部', value: 'all' as SectorType },
  { label: '行业', value: 'industry' as SectorType },
  { label: '概念', value: 'concept' as SectorType },
  { label: '地域', value: 'region' as SectorType }
]

const sortOptions = [
  { label: '今日涨幅', value: 'dayGrowth' as const },
  { label: '周涨幅', value: 'weekGrowth' as const },
  { label: '月涨幅', value: 'monthGrowth' as const },
  { label: '成交额', value: 'volume' as const },
  { label: '换手率', value: 'turnover' as const }
]

const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => {
    queryParams.pageNum = page
    loadSectors()
  },
  onUpdatePageSize: (size: number) => {
    queryParams.pageSize = size
    queryParams.pageNum = 1
    loadSectors()
  }
})

// 涨幅榜前5
const topGainers = computed(() => {
  return [...sectors.value]
    .filter(s => s.changePercent != null)
    .sort((a, b) => (b.changePercent || 0) - (a.changePercent || 0))
    .slice(0, 5)
})

// 跌幅榜前5
const topLosers = computed(() => {
  return [...sectors.value]
    .filter(s => s.changePercent != null)
    .sort((a, b) => (a.changePercent || 0) - (b.changePercent || 0))
    .slice(0, 5)
})

const columns = [
  {
    title: '板块名称',
    key: 'name',
    render: (row: SectorVO) => h('div', { class: 'sector-name-cell' }, [
      h('span', { class: 'name', onClick: () => goToDetail(row.code) }, row.name),
      h(NTag, { size: 'small', type: 'info', style: 'margin-left: 8px' }, () => getTypeLabel(row.type))
    ])
  },
  {
    title: '板块代码',
    key: 'code',
    width: 100
  },
  {
    title: '今日涨跌',
    key: 'changePercent',
    width: 100,
    render: (row: SectorVO) => h('span', {
      class: row.changePercent >= 0 ? 'growth-positive' : 'growth-negative'
    }, `${row.changePercent >= 0 ? '+' : ''}${row.changePercent?.toFixed(2)}%`)
  },
  {
    title: '成交额(亿)',
    key: 'volume',
    width: 110,
    render: (row: SectorVO) => row.volume?.toFixed(2) || '--'
  },
  {
    title: '换手率',
    key: 'turnover',
    width: 90,
    render: (row: SectorVO) => row.turnover ? `${row.turnover.toFixed(2)}%` : '--'
  },
  {
    title: '领涨股',
    key: 'leadingStock',
    width: 120,
    ellipsis: { tooltip: true },
    render: (row: SectorVO) => row.leadingStock || '--'
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
    render: (row: SectorVO) => h(NButton, {
      size: 'small',
      onClick: () => goToDetail(row.code)
    }, () => '详情')
  }
]

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    industry: '行业',
    concept: '概念',
    region: '地域'
  }
  return map[type] || type
}

const changeType = (type: SectorType) => {
  queryParams.type = type
  queryParams.pageNum = 1
  loadSectors()
}

const handlePageChange = (page: number) => {
  queryParams.pageNum = page
  loadSectors()
}

const loadSectors = async () => {
  loading.value = true
  try {
    const data = await sectorApi.getList(queryParams)
    sectors.value = data || []
    pagination.itemCount = data.length
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const goToDetail = (code: string) => {
  router.push(`/sector/${code}`)
}

onMounted(() => {
  loadSectors()
})
</script>

<style scoped>
.filter-card {
  padding: 16px;
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.overview-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 800px) {
  .overview-section {
    grid-template-columns: 1fr;
  }
}

.overview-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow);
}

.overview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
}

.overview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: var(--bg-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.overview-item:hover {
  background: var(--hover-bg);
}

.overview-item .rank {
  width: 20px;
  font-weight: 600;
  color: var(--text-secondary);
}

.overview-item .name {
  flex: 1;
  margin-left: 12px;
}

.overview-card.up .overview-item .growth {
  color: var(--up-color);
  font-weight: 600;
}

.overview-card.down .overview-item .growth {
  color: var(--down-color);
  font-weight: 600;
}

.sector-table {
  padding: 16px;
}

.sector-name-cell .name {
  font-weight: 500;
  cursor: pointer;
  color: var(--primary-color);
}

.sector-name-cell .name:hover {
  text-decoration: underline;
}

.growth-positive {
  color: var(--up-color);
}

.growth-negative {
  color: var(--down-color);
}
</style>
