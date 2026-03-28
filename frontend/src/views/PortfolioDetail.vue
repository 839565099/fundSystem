<template>
  <div class="portfolio-detail-page page-container">
    <n-spin :show="loading">
      <template v-if="portfolio">
        <!-- 头部信息 -->
        <div class="page-header">
          <div class="header-left">
            <n-button text @click="router.back()">
              <template #icon><n-icon><IconArrowLeft /></n-icon></template>
            </n-button>
            <PageHeader :title="portfolio.name" style="margin-bottom: 0;">
              <template #actions>
                <n-tag v-if="portfolio.isDefault" type="primary" size="small">默认</n-tag>
              </template>
            </PageHeader>
          </div>
          <div class="header-actions">
            <n-button @click="showAddFundModal = true">
              <template #icon><n-icon><IconPlus /></n-icon></template>
              添加基金
            </n-button>
            <n-button @click="refreshPortfolio" :loading="refreshing">
              <template #icon><n-icon><IconRefresh /></n-icon></template>
              刷新
            </n-button>
          </div>
        </div>

        <p v-if="portfolio.description" class="portfolio-desc">{{ portfolio.description }}</p>

        <!-- 概览卡片 -->
        <div class="overview-cards">
          <n-card class="overview-card">
            <div class="stat-label">总资产</div>
            <div class="stat-value">{{ formatMoney(portfolio.currentValue) }}</div>
          </n-card>
          <n-card class="overview-card">
            <div class="stat-label">总成本</div>
            <div class="stat-value">{{ formatMoney(portfolio.totalAmount) }}</div>
          </n-card>
          <n-card class="overview-card">
            <div class="stat-label">总收益</div>
            <div class="stat-value" :class="portfolio.totalProfit >= 0 ? 'positive' : 'negative'">
              {{ portfolio.totalProfit >= 0 ? '+' : '' }}{{ formatMoney(portfolio.totalProfit) }}
            </div>
          </n-card>
          <n-card class="overview-card">
            <div class="stat-label">收益率</div>
            <div class="stat-value" :class="portfolio.totalReturn >= 0 ? 'positive' : 'negative'">
              {{ portfolio.totalReturn >= 0 ? '+' : '' }}{{ portfolio.totalReturn?.toFixed(2) }}%
            </div>
          </n-card>
          <n-card class="overview-card">
            <div class="stat-label">今日收益</div>
            <div class="stat-value" :class="portfolio.dayProfit >= 0 ? 'positive' : 'negative'">
              {{ portfolio.dayProfit >= 0 ? '+' : '' }}{{ formatMoney(portfolio.dayProfit) }}
            </div>
          </n-card>
          <n-card class="overview-card">
            <div class="stat-label">持仓基金</div>
            <div class="stat-value">{{ portfolio.fundCount }} 只</div>
          </n-card>
        </div>

        <!-- 资产配置饼图 -->
        <n-card v-if="portfolio.allocations && portfolio.allocations.length" class="allocation-card" title="资产配置">
          <div class="allocation-content">
            <div class="pie-chart" ref="pieChartRef"></div>
            <div class="allocation-legend">
              <div v-for="item in portfolio.allocations" :key="item.name" class="legend-item">
                <span class="legend-color" :style="{ background: item.color }"></span>
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-value">{{ item.ratio?.toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </n-card>

        <!-- 持仓列表 -->
        <n-card class="holdings-card" title="持仓明细">
          <n-data-table
            :columns="columns"
            :data="portfolio.items || []"
            :pagination="false"
            :bordered="false"
          />
          <n-empty v-if="!portfolio.items?.length" description="暂无持仓" />
        </n-card>
      </template>

      <n-empty v-else-if="!loading" description="组合不存在" />
    </n-spin>

    <!-- 添加基金弹窗 - 支持收藏基金下拉和搜索 -->
    <n-modal v-model:show="showAddFundModal" preset="dialog" title="添加基金">
      <n-form ref="addFormRef" :model="addFundForm" label-placement="left" label-width="80">
        <n-form-item label="选择基金" path="fundCode" required>
          <n-select
            v-model:value="addFundForm.fundCode"
            :options="fundOptions"
            placeholder="点击下拉选择收藏基金，或输入基金代码搜索"
            filterable
            clearable
            :loading="searchingFund"
            remote
            @search="handleFundSearch"
            @update:show="handleSelectDropdown"
          />
        </n-form-item>
        <n-form-item label="买入金额" path="amount" required>
          <n-input-number
            v-model:value="addFundForm.amount"
            placeholder="投入了多少钱"
            :min="0"
            :precision="2"
            style="width: 100%"
          >
            <template #prefix>¥</template>
          </n-input-number>
        </n-form-item>
        <n-form-item label="买入净值" path="buyNav">
          <n-input-number
            v-model:value="addFundForm.buyNav"
            placeholder="买入时的净值（可选，默认使用当前净值）"
            :min="0"
            :precision="4"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showAddFundModal = false">取消</n-button>
        <n-button type="primary" @click="addFund" :loading="adding">确定</n-button>
      </template>
    </n-modal>

    <!-- 编辑持仓弹窗 -->
    <n-modal v-model:show="showEditModal" preset="dialog" title="编辑持仓">
      <n-form ref="editFormRef" :model="editForm" label-placement="left" label-width="80">
        <n-form-item label="基金">
          <span>{{ editForm.fundName }} ({{ editForm.fundCode }})</span>
        </n-form-item>
        <n-form-item label="买入金额" path="amount" required>
          <n-input-number
            v-model:value="editForm.amount"
            placeholder="投入了多少钱"
            :min="0"
            :precision="2"
            style="width: 100%"
          >
            <template #prefix>¥</template>
          </n-input-number>
        </n-form-item>
        <n-form-item label="买入净值" path="buyNav">
          <n-input-number
            v-model:value="editForm.buyNav"
            placeholder="买入时的净值"
            :min="0"
            :precision="4"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="份额" path="shares">
          <n-input-number
            v-model:value="editForm.shares"
            placeholder="持有份额（可选，根据金额/净值自动计算）"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showEditModal = false">取消</n-button>
        <n-button type="primary" @click="updateItem" :loading="updating">确定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, h, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NButton, NIcon, NTag, NSpin, NEmpty, NModal, NForm, NFormItem, NInputNumber, NDataTable, NSelect, useMessage, type DataTableColumns } from 'naive-ui'
import { IconArrowLeft, IconPlus, IconRefresh, IconTrash, IconPencil, IconWallet, IconCoin, IconChartLine, IconTrendingUp, IconCalendarStats, IconBasket } from '@tabler/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import { fundApi, favoriteApi } from '../api/fund'
import type { UserFavorite, Fund } from '../types'
import * as echarts from 'echarts'

interface PortfolioItem {
  id: number
  fundCode: string
  fundName: string
  fundType: string
  shares: number
  amount: number
  targetRatio: number
  actualRatio: number
  buyNav: number
  currentNav: number
  currentValue: number
  profit: number
  profitRatio: number
  dayProfit: number
  dayGrowth: number
  buyDate: string
}

interface Allocation {
  name: string
  value: number
  ratio: number
  color: string
}

interface Portfolio {
  id: number
  name: string
  description: string
  totalAmount: number
  currentValue: number
  totalProfit: number
  totalReturn: number
  dayProfit: number
  dayReturn: number
  fundCount: number
  isDefault: number
  items: PortfolioItem[]
  allocations: Allocation[]
}

const router = useRouter()
const route = useRoute()
const message = useMessage()

const loading = ref(false)
const refreshing = ref(false)
const portfolio = ref<Portfolio | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null

// 添加基金相关 - 支持收藏基金下拉和搜索
const showAddFundModal = ref(false)
const adding = ref(false)
const addFundForm = ref({
  fundCode: '',
  amount: null as number | null,
  buyNav: null as number | null
})

const fundOptions = ref<Array<{ label: string; value: string }>>([])
const favoriteFunds = ref<Array<{ fundCode: string; fundName: string }>>([])
const searchingFund = ref(false)

// 编辑持仓相关
const showEditModal = ref(false)
const updating = ref(false)
const editForm = ref({
  itemId: 0,
  fundCode: '',
  fundName: '',
  amount: null as number | null,
  buyNav: null as number | null,
  shares: null as number | null
})

// 获取已添加基金的代码集合
const existingFundCodes = computed(() => {
  return new Set(portfolio.value?.items?.map((item: PortfolioItem) => item.fundCode) || [])
})

// 过滤掉已添加的基金
const filterExistingFunds = (funds: Array<{ fundCode: string; fundName: string }>) => {
  const existing = existingFundCodes.value
  return funds.filter(f => !existing.has(f.fundCode))
}

const loadFavoriteFunds = async () => {
  try {
    const favorites = await favoriteApi.getList()
    favoriteFunds.value = (favorites || []).map((f: UserFavorite) => ({
      fundCode: f.fundCode,
      fundName: f.fundName || ''
    }))
    // 过滤掉已添加的基金
    const availableFunds = filterExistingFunds(favoriteFunds.value)
    fundOptions.value = availableFunds.map(f => ({
      label: `${f.fundName} (${f.fundCode})`,
      value: f.fundCode
    }))
  } catch (e) {
    console.error('加载收藏基金失败', e)
  }
}

const handleSelectDropdown = (show: boolean) => {
  if (show) {
    // 每次打开下拉框时重新加载并过滤
    loadFavoriteFunds()
  }
}

const handleFundSearch = async (query: string) => {
  if (!query) {
    // 清空搜索时显示收藏基金（过滤已添加的）
    const availableFunds = filterExistingFunds(favoriteFunds.value)
    fundOptions.value = availableFunds.map(f => ({
      label: `${f.fundName} (${f.fundCode})`,
      value: f.fundCode
    }))
    return
  }

  searchingFund.value = true
  try {
    const results = await fundApi.searchByKeyword(query, 20)
    const existing = existingFundCodes.value
    // 搜索结果也要过滤已添加的基金
    fundOptions.value = (results || [])
      .filter((f: Fund) => !existing.has(f.fundCode))
      .map((f: Fund) => ({
        label: `${f.fundName} (${f.fundCode})`,
        value: f.fundCode
      }))
  } catch (e) {
    console.error('搜索基金失败', e)
  } finally {
    searchingFund.value = false
  }
}

const loadPortfolio = async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    portfolio.value = await fundApi.getPortfolioDetail(id)
    await nextTick()
    renderPieChart()
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const refreshPortfolio = async () => {
  const id = Number(route.params.id)
  if (!id) return

  refreshing.value = true
  try {
    await fundApi.refreshPortfolio(id)
    await loadPortfolio()
    message.success('刷新成功')
  } catch (e: any) {
    message.error(e.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
}

const addFund = async () => {
  const id = Number(route.params.id)

  // 验证
  if (!addFundForm.value.fundCode || addFundForm.value.fundCode.length !== 6) {
    message.warning('请输入6位基金代码')
    return
  }
  if (!addFundForm.value.amount || addFundForm.value.amount <= 0) {
    message.warning('请输入买入金额')
    return
  }

  adding.value = true
  try {
    await fundApi.addPortfolioItem(id, {
      fundCode: addFundForm.value.fundCode,
      amount: addFundForm.value.amount,
      buyNav: addFundForm.value.buyNav
    })
    message.success('添加成功')
    showAddFundModal.value = false
    addFundForm.value = { fundCode: '', amount: null, buyNav: null }
    await loadPortfolio()
  } catch (e: any) {
    message.error(e.message || '添加失败')
  } finally {
    adding.value = false
  }
}

const deleteItem = async (itemId: number) => {
  const id = Number(route.params.id)
  if (!id) return

  try {
    await fundApi.deletePortfolioItem(id, itemId)
    message.success('删除成功')
    await loadPortfolio()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

// 打开编辑弹窗
const openEditModal = (item: PortfolioItem) => {
  editForm.value = {
    itemId: item.id,
    fundCode: item.fundCode,
    fundName: item.fundName,
    amount: item.amount,
    buyNav: item.buyNav,
    shares: item.shares
  }
  showEditModal.value = true
}

// 更新持仓
const updateItem = async () => {
  const id = Number(route.params.id)
  if (!id || !editForm.value.itemId) return

  if (!editForm.value.amount || editForm.value.amount <= 0) {
    message.warning('请输入买入金额')
    return
  }

  updating.value = true
  try {
    await fundApi.updatePortfolioItem(id, editForm.value.itemId, {
      amount: editForm.value.amount,
      buyNav: editForm.value.buyNav,
      shares: editForm.value.shares
    })
    message.success('更新成功')
    showEditModal.value = false
    await loadPortfolio()
  } catch (e: any) {
    message.error(e.message || '更新失败')
  } finally {
    updating.value = false
  }
}

const renderPieChart = () => {
  if (!pieChartRef.value || !portfolio.value?.allocations?.length) return

  if (pieChart) {
    pieChart.dispose()
  }

  pieChart = echarts.init(pieChartRef.value)
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: 'var(--card-bg)',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: portfolio.value.allocations.map(a => ({
          name: a.name,
          value: a.value,
          itemStyle: { color: a.color }
        }))
      }
    ]
  }
  pieChart.setOption(option)
}

const columns: DataTableColumns<PortfolioItem> = [
  { title: '基金代码', key: 'fundCode', width: 100 },
  { title: '基金名称', key: 'fundName', ellipsis: { tooltip: true } },
  { title: '基金类型', key: 'fundType', width: 100 },
  {
    title: '份额',
    key: 'shares',
    width: 100,
    render: (row) => row.shares?.toFixed(2) || '-'
  },
  {
    title: '成本',
    key: 'amount',
    width: 110,
    render: (row) => formatMoney(row.amount)
  },
  {
    title: '市值',
    key: 'currentValue',
    width: 110,
    render: (row) => formatMoney(row.currentValue)
  },
  {
    title: '收益',
    key: 'profit',
    width: 110,
    render: (row) => h('span', { class: row.profit >= 0 ? 'positive' : 'negative' },
      `${row.profit >= 0 ? '+' : ''}${formatMoney(row.profit)}`)
  },
  {
    title: '收益率',
    key: 'profitRatio',
    width: 90,
    render: (row) => h('span', { class: row.profitRatio >= 0 ? 'positive' : 'negative' },
      `${row.profitRatio >= 0 ? '+' : ''}${row.profitRatio?.toFixed(2)}%`)
  },
  {
    title: '今日涨跌',
    key: 'dayGrowth',
    width: 90,
    render: (row) => {
      const growth = row.dayGrowth
      if (growth === undefined || growth === null) return '-'
      return h('span', { class: growth >= 0 ? 'positive' : 'negative' },
        `${growth >= 0 ? '+' : ''}${growth.toFixed(2)}%`)
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) => h('div', { style: { display: 'flex', gap: '8px' } }, [
      h(NButton, {
        text: true,
        type: 'primary',
        onClick: () => openEditModal(row)
      }, { icon: () => h(NIcon, null, { default: () => h(IconPencil) }) }),
      h(NButton, {
        text: true,
        type: 'error',
        onClick: () => deleteItem(row.id)
      }, { icon: () => h(NIcon, null, { default: () => h(IconTrash) }) })
    ])
  }
]

const formatMoney = (value: number | undefined | null) => {
  if (value === undefined || value === null) return '¥0.00'
  return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(loadPortfolio)

watch(() => route.params.id, () => {
  if (route.params.id) {
    loadPortfolio()
  }
})
</script>

<style scoped>
.portfolio-detail-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.portfolio-desc {
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

.overview-card {
  text-align: center;
  padding: 12px 8px !important;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 15px;
  font-weight: 600;
  word-break: break-all;
}

.allocation-card {
  margin-bottom: 16px;
}

.allocation-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.pie-chart {
  width: 500px;
  height: 500px;
  flex-shrink: 0;
}

.allocation-legend {
  flex: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-name {
  white-space: nowrap;
  margin-right: auto;
}

.legend-value {
  font-weight: 500;
  white-space: nowrap;
  margin-left: 16px;
}

.holdings-card {
  margin-bottom: 24px;
}

.positive { color: var(--up-color); }
.negative { color: var(--down-color); }
</style>
