<template>
  <div class="portfolio-detail-page page-container">
    <n-spin :show="loading">
      <template v-if="portfolio">
        <!-- 顶部操作栏 -->
        <div class="page-top-bar">
          <div class="top-bar-left">
            <n-button text size="large" @click="router.back()">
              <template #icon><n-icon size="20"><IconArrowLeft /></n-icon></template>
            </n-button>
            <div>
              <div class="portfolio-title">
                {{ portfolio.name }}
                <n-tag v-if="portfolio.isDefault" type="primary" size="small" round>默认</n-tag>
              </div>
              <p v-if="portfolio.description" class="portfolio-desc">{{ portfolio.description }}</p>
            </div>
          </div>
          <div class="top-bar-actions">
            <n-button type="primary" @click="showAddFundModal = true">
              <template #icon><n-icon><IconPlus /></n-icon></template>
              添加基金
            </n-button>
            <n-button @click="refreshPortfolio" :loading="refreshing">
              <template #icon><n-icon><IconRefresh /></n-icon></template>
              刷新
            </n-button>
          </div>
        </div>

        <!-- 核心指标区 -->
        <div class="metrics-hero">
          <div class="hero-top">
            <div class="hero-asset">
              <span class="hero-tag">总资产</span>
              <span class="hero-amount">{{ formatMoney(portfolio.currentValue) }}</span>
            </div>
            <div class="hero-meta">
              成本 {{ formatMoney(portfolio.totalAmount) }}&emsp;持仓 {{ portfolio.fundCount }} 只基金
            </div>
          </div>
          <div class="hero-metrics-row">
            <div class="hero-chip">
              <span class="chip-label">总收益</span>
              <span class="chip-val" :class="portfolio.totalProfit >= 0 ? 'up' : 'down'">
                {{ portfolio.totalProfit >= 0 ? '+' : '' }}{{ formatMoney(portfolio.totalProfit) }}
              </span>
            </div>
            <div class="hero-chip">
              <span class="chip-label">收益率</span>
              <span class="chip-val" :class="portfolio.totalReturn >= 0 ? 'up' : 'down'">
                {{ portfolio.totalReturn >= 0 ? '+' : '' }}{{ portfolio.totalReturn?.toFixed(2) }}%
              </span>
            </div>
            <div class="hero-chip">
              <span class="chip-label">今日收益</span>
              <span class="chip-val" :class="portfolio.dayProfit >= 0 ? 'up' : 'down'">
                {{ portfolio.dayProfit >= 0 ? '+' : '' }}{{ formatMoney(portfolio.dayProfit) }}
              </span>
            </div>
            <div class="hero-chip">
              <span class="chip-label">日收益率</span>
              <span class="chip-val" :class="portfolio.dayReturn >= 0 ? 'up' : 'down'">
                {{ portfolio.dayReturn >= 0 ? '+' : '' }}{{ portfolio.dayReturn?.toFixed(2) }}%
              </span>
            </div>
          </div>
        </div>

        <!-- 资产配置 -->
        <n-card v-if="portfolio.allocations && portfolio.allocations.length" class="allocation-card card--elevated" title="资产配置">
          <div class="allocation-content">
            <div class="pie-chart" ref="pieChartRef"></div>
            <div class="allocation-legend">
              <div v-for="item in portfolio.allocations" :key="item.name" class="legend-item">
                <span class="legend-color" :style="{ background: item.color }"></span>
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-value">{{ item.ratio?.toFixed(1) }}%</span>
                <span class="legend-amount">{{ formatMoneyShort(item.value) }}</span>
              </div>
            </div>
          </div>
        </n-card>

        <!-- 持仓列表 -->
        <n-card class="holdings-card card--elevated" title="持仓明细">
          <n-data-table
            :columns="columns"
            :data="portfolio.items || []"
            :pagination="false"
            :bordered="false"
            size="small"
          />
          <n-empty v-if="!portfolio.items?.length" description="暂无持仓" />
        </n-card>
      </template>

      <n-empty v-else-if="!loading" description="组合不存在" />
    </n-spin>

    <!-- 添加基金弹窗 -->
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
import { IconArrowLeft, IconPlus, IconRefresh, IconTrash, IconPencil } from '@tabler/icons-vue'
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
  yesterdayProfit: number
  yesterdayReturn: number
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

// 添加基金相关
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

const existingFundCodes = computed(() => {
  return new Set(portfolio.value?.items?.map((item: PortfolioItem) => item.fundCode) || [])
})

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
  if (show) loadFavoriteFunds()
}

const handleFundSearch = async (query: string) => {
  if (!query) {
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

const getCssColor = (varName: string, fallback: string) => {
  return getComputedStyle(document.documentElement).getPropertyValue(varName).trim() || fallback
}

const renderPieChart = () => {
  if (!pieChartRef.value || !portfolio.value?.allocations?.length) return

  if (pieChart) {
    pieChart.dispose()
  }

  const cardBg = getCssColor('--card-bg', '#ffffff')
  const textColor = getCssColor('--text-primary', '#1e293b')
  const textSecondary = getCssColor('--text-secondary', '#64748b')

  pieChart = echarts.init(pieChartRef.value)
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: cardBg,
      borderColor: '#e2e8f0',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: textColor, fontSize: 12 },
      formatter: (params: any) => {
        return `<div style="font-weight:600;margin-bottom:4px;">${params.name}</div>
          <div style="color:${textSecondary}">市值: ¥${params.value?.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
          <div style="color:${textSecondary}">占比: ${params.percent?.toFixed(1)}%</div>`
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: cardBg,
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold', color: textColor },
          itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.15)' }
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

const formatMoney = (value: number | undefined | null) => {
  if (value === undefined || value === null) return '¥0.00'
  return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatMoneyShort = (value: number | undefined | null) => {
  if (value === undefined || value === null) return '-'
  if (value >= 10000) return `¥${(value / 10000).toFixed(2)}万`
  return `¥${value.toFixed(2)}`
}

const columns: DataTableColumns<PortfolioItem> = [
  {
    title: '基金名称', key: 'fundName', ellipsis: { tooltip: true },
    render: (row) => h('div', {}, [
      h('div', { style: 'font-weight:500' }, row.fundName),
      h('div', { style: 'font-size:11px;color:var(--text-tertiary)' }, row.fundCode)
    ])
  },
  {
    title: '类型', key: 'fundType', width: 80,
    render: (row) => h(NTag, { size: 'small', round: true, bordered: false }, { default: () => row.fundType || '-' })
  },
  {
    title: '成本', key: 'amount', width: 100,
    render: (row) => formatMoneyShort(row.amount)
  },
  {
    title: '市值', key: 'currentValue', width: 100,
    render: (row) => formatMoneyShort(row.currentValue)
  },
  {
    title: '占比', key: 'actualRatio', width: 70,
    render: (row) => {
      if (row.actualRatio === null || row.actualRatio === undefined) return '-'
      return `${row.actualRatio.toFixed(1)}%`
    }
  },
  {
    title: '收益', key: 'profit', width: 100,
    render: (row) => h('span', { class: row.profit >= 0 ? 'positive' : 'negative' },
      `${row.profit >= 0 ? '+' : ''}${formatMoneyShort(row.profit)}`)
  },
  {
    title: '收益率', key: 'profitRatio', width: 80,
    render: (row) => h('span', { class: row.profitRatio >= 0 ? 'positive' : 'negative' },
      `${row.profitRatio >= 0 ? '+' : ''}${row.profitRatio?.toFixed(2)}%`)
  },
  {
    title: '今日', key: 'dayGrowth', width: 70,
    render: (row) => {
      const growth = row.dayGrowth
      if (growth === undefined || growth === null) return '-'
      return h('span', { class: growth >= 0 ? 'positive' : 'negative' },
        `${growth >= 0 ? '+' : ''}${growth.toFixed(2)}%`)
    }
  },
  {
    title: '操作', key: 'actions', width: 80,
    render: (row) => h('div', { style: { display: 'flex', gap: '4px' } }, [
      h(NButton, {
        text: true, type: 'primary', size: 'small',
        onClick: () => openEditModal(row)
      }, { icon: () => h(NIcon, { size: 16 }, { default: () => h(IconPencil) }) }),
      h(NButton, {
        text: true, type: 'error', size: 'small',
        onClick: () => deleteItem(row.id)
      }, { icon: () => h(NIcon, { size: 16 }, { default: () => h(IconTrash) }) })
    ])
  }
]

onMounted(loadPortfolio)

watch(() => route.params.id, () => {
  if (route.params.id) loadPortfolio()
})
</script>

<style scoped>
.portfolio-detail-page {
  padding: 24px;
  border-radius: var(--radius-xl);
}

/* 顶部操作栏 */
.page-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.top-bar-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.portfolio-title {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.portfolio-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 4px;
}

.top-bar-actions {
  display: flex;
  gap: 8px;
}

/* 核心指标区 */
.metrics-hero {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border-radius: 16px;
  color: #fff;
  padding: 24px 28px 20px;
  margin-bottom: 20px;
}

.hero-top {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.hero-asset {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 6px;
}

.hero-tag {
  font-size: 13px;
  background: rgba(255, 255, 255, 0.18);
  padding: 2px 10px;
  border-radius: 10px;
  letter-spacing: 0.5px;
}

.hero-amount {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.5px;
  font-variant-numeric: tabular-nums;
}

.hero-meta {
  font-size: 13px;
  opacity: 0.55;
  margin-left: 62px;
}

.hero-metrics-row {
  display: flex;
  gap: 0;
}

.hero-chip {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 0;
  position: relative;
}

.hero-chip + .hero-chip::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  height: 60%;
  width: 1px;
  background: rgba(255, 255, 255, 0.12);
}

.chip-label {
  font-size: 12px;
  opacity: 0.55;
}

.chip-val {
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.chip-val.up { color: #86efac; }
.chip-val.down { color: #fca5a5; }

@media (max-width: 768px) {
  .metrics-hero { padding: 20px; }
  .hero-amount { font-size: 24px; }
  .hero-meta { margin-left: 0; }
  .hero-metrics-row { flex-wrap: wrap; }
  .hero-chip { min-width: 50%; }
}

/* 资产配置 */
.allocation-card {
  margin-bottom: 20px;
}

.allocation-content {
  display: flex;
  align-items: center;
  gap: 24px;
}

.pie-chart {
  width: 280px;
  height: 280px;
  flex-shrink: 0;
}

.allocation-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 280px;
  overflow-y: auto;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background 0.15s;
}

.legend-item:hover {
  background: var(--bg-secondary);
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.legend-value {
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
}

.legend-amount {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

/* 持仓表格 */
.holdings-card {
  margin-bottom: 24px;
}

.positive { color: var(--up-color); }
.negative { color: var(--down-color); }

@media (max-width: 768px) {
  .portfolio-detail-page {
    padding: 16px;
  }

  .page-top-bar {
    flex-direction: column;
    gap: 12px;
  }

  .allocation-content {
    flex-direction: column;
  }

  .pie-chart {
    width: 100%;
    height: 240px;
  }
}
</style>
