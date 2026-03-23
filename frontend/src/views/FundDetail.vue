<template>
  <div class="page-container fund-detail-page">
    <n-spin :show="loading">
      <template v-if="fund">
        <section class="hero card">
          <div class="hero__main">
            <div class="hero__title-row">
              <h1>{{ fund.fundName }}</h1>
              <n-tag type="info" size="small">{{ fund.fundType || '未知类型' }}</n-tag>
              <n-tag v-if="fund.riskLevel" size="small" :type="riskTagType">{{ riskLevelName }}</n-tag>
            </div>
            <div class="hero__meta">
              <span>{{ fund.fundCode }}</span>
              <span v-if="fund.fundCompany">{{ fund.fundCompany }}</span>
              <span v-if="fund.establishDate">成立于 {{ fund.establishDate }}</span>
              <span v-if="fund.navDate">净值日期 {{ fund.navDate }}</span>
            </div>
          </div>
          <div class="hero__actions">
            <n-button
              :type="isFavorite ? 'warning' : 'default'"
              @click="toggleFavorite"
              :loading="favoriteLoading"
            >
              <template #icon>
                <n-icon><StarOutline /></n-icon>
              </template>
              {{ isFavorite ? '已收藏' : '添加收藏' }}
            </n-button>
          </div>
        </section>

        <section class="kpi-grid">
          <div class="kpi-card card">
            <div class="kpi-label">最新净值</div>
            <div class="kpi-value kpi-value--primary">{{ formatNav(fund.nav) }}</div>
          </div>
          <div class="kpi-card card">
            <div class="kpi-label">日涨跌</div>
            <div class="kpi-value" :class="growthClass(fund.dayGrowth)">{{ formatGrowth(fund.dayGrowth) }}</div>
          </div>
          <div class="kpi-card card">
            <div class="kpi-label">周涨跌</div>
            <div class="kpi-value" :class="growthClass(fund.weekGrowth)">{{ formatGrowth(fund.weekGrowth) }}</div>
          </div>
          <div class="kpi-card card">
            <div class="kpi-label">月涨跌</div>
            <div class="kpi-value" :class="growthClass(fund.monthGrowth)">{{ formatGrowth(fund.monthGrowth) }}</div>
          </div>
          <div class="kpi-card card">
            <div class="kpi-label">年涨跌</div>
            <div class="kpi-value" :class="growthClass(fund.yearGrowth)">{{ formatGrowth(fund.yearGrowth) }}</div>
          </div>
          <div class="kpi-card card">
            <div class="kpi-label">基金规模(亿)</div>
            <div class="kpi-value">{{ formatNumber(fund.fundScale) }}</div>
          </div>
        </section>

        <section class="chart-section">
          <h2 class="section-title">
            <n-icon size="22"><TrendingUpOutline /></n-icon>
            净值走势
          </h2>
          <FundTrendChart
            :data="navHistory"
            :fund-name="fund.fundName"
            @period-change="loadNavHistory"
          />
        </section>

        <section class="info-tabs card">
          <n-tabs v-model:value="activeTab" type="line" animated>
            <n-tab-pane name="overview" tab="概览">
              <div class="overview-grid">
                <div class="overview-item">
                  <span class="label">基金代码</span>
                  <span class="value">{{ fund.fundCode }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">基金公司</span>
                  <span class="value">{{ fund.fundCompany || '--' }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">风险等级</span>
                  <span class="value">{{ riskLevelName }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">最低申购</span>
                  <span class="value">{{ fund.minPurchase != null ? `¥${fund.minPurchase}` : '--' }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">累计净值</span>
                  <span class="value">{{ formatNav(fund.accNav) }}</span>
                </div>
                <div class="overview-item">
                  <span class="label">成立以来</span>
                  <span class="value" :class="growthClass(fund.totalGrowth)">{{ formatGrowth(fund.totalGrowth) }}</span>
                </div>
              </div>
            </n-tab-pane>

            <n-tab-pane name="performance" tab="业绩表现">
              <div class="performance-grid">
                <div v-for="item in performanceItems" :key="item.label" class="perf-item">
                  <span class="label">{{ item.label }}</span>
                  <span class="value" :class="growthClass(item.value)">{{ formatGrowth(item.value) }}</span>
                </div>
              </div>
            </n-tab-pane>

            <n-tab-pane name="manager" tab="基金经理">
              <n-spin :show="managerLoading">
                <div v-if="managers.length > 0" class="manager-list">
                  <div v-for="m in managers" :key="m.managerId" class="manager-card">
                    <n-avatar v-if="m.photo" round size="large" :src="m.photo" :fallback-src="undefined">
                      {{ m.managerName?.charAt(0) }}
                    </n-avatar>
                    <n-avatar v-else round size="large">{{ m.managerName?.charAt(0) }}</n-avatar>

                    <div class="manager-info">
                      <div class="manager-header">
                        <div class="manager-name">{{ m.managerName }}</div>
                        <n-tag size="small" type="info">{{ m.company || '基金经理' }}</n-tag>
                      </div>

                      <div class="manager-meta">
                        <span>任职: {{ m.startDate || '--' }}</span>
                        <span>从业: {{ m.workYears != null ? `${m.workYears}年` : '--' }}</span>
                        <span>管理规模: {{ m.totalAsset != null ? `${m.totalAsset}亿` : '--' }}</span>
                        <span>在管基金: {{ m.fundCount != null ? `${m.fundCount}只` : '--' }}</span>
                      </div>

                      <div v-if="m.bestReturn != null" class="manager-return">
                        任职回报:
                        <span :class="growthClass(m.bestReturn)">{{ formatGrowth(m.bestReturn) }}</span>
                      </div>

                      <div v-if="m.investmentIdea" class="expand-block">
                        <div
                          class="expand-content"
                          :class="{ 'expand-content--collapsed': !expandedIdeas[m.managerId] && shouldCollapse(m.investmentIdea, 90) }"
                        >
                          <span class="expand-label">投资理念：</span>{{ m.investmentIdea }}
                        </div>
                        <n-button
                          v-if="shouldCollapse(m.investmentIdea, 90)"
                          text
                          size="tiny"
                          class="expand-btn"
                          @click="expandedIdeas[m.managerId] = !expandedIdeas[m.managerId]"
                        >
                          {{ expandedIdeas[m.managerId] ? '收起' : '展开' }}
                        </n-button>
                      </div>

                      <div v-if="m.resume" class="expand-block">
                        <div
                          class="expand-content"
                          :class="{ 'expand-content--collapsed': !expandedResumes[m.managerId] && shouldCollapse(m.resume, 120) }"
                        >
                          <span class="expand-label">简介：</span>{{ m.resume }}
                        </div>
                        <n-button
                          v-if="shouldCollapse(m.resume, 120)"
                          text
                          size="tiny"
                          class="expand-btn"
                          @click="expandedResumes[m.managerId] = !expandedResumes[m.managerId]"
                        >
                          {{ expandedResumes[m.managerId] ? '收起' : '展开' }}
                        </n-button>
                      </div>
                    </div>
                  </div>
                </div>
                <n-empty v-else description="暂无基金经理信息" />
              </n-spin>
            </n-tab-pane>

            <n-tab-pane name="holdings" tab="重仓持仓">
              <n-spin :show="holdingsLoading">
                <div v-if="holdings.length > 0" class="holdings-section">
                  <div class="holdings-header">
                    <span>报告日期: {{ holdings[0]?.reportDate || '--' }}</span>
                    <n-button v-if="holdings.length > 5" text size="small" @click="expandHoldings = !expandHoldings">
                      {{ expandHoldings ? '收起' : `查看更多(${holdings.length})` }}
                    </n-button>
                  </div>

                  <div class="holdings-list">
                    <div v-for="(h, index) in visibleHoldings" :key="`${h.stockCode}-${index}`" class="holding-item">
                      <div class="holding-rank">{{ index + 1 }}</div>
                      <div class="holding-info">
                        <div class="stock-name">{{ h.stockName || '--' }}</div>
                        <div class="stock-code">{{ h.stockCode || '--' }}</div>
                      </div>
                      <div class="holding-metric">
                        <span class="label">持仓比例</span>
                        <span class="value">{{ h.holdingRatio != null ? `${h.holdingRatio.toFixed(2)}%` : '--' }}</span>
                      </div>
                      <div class="holding-metric">
                        <span class="label">今日涨跌</span>
                        <span class="value" :class="growthClass(h.dayGrowth)">{{ formatGrowth(h.dayGrowth) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <n-empty v-else description="暂无重仓持仓信息" />
              </n-spin>
            </n-tab-pane>

            <n-tab-pane name="fee" tab="费率信息">
              <div class="fee-grid">
                <div class="fee-item">
                  <span class="label">管理费</span>
                  <span class="value">{{ fund.managementRate != null ? `${fund.managementRate}%` : '--' }}</span>
                </div>
                <div class="fee-item">
                  <span class="label">托管费</span>
                  <span class="value">{{ fund.custodyRate != null ? `${fund.custodyRate}%` : '--' }}</span>
                </div>
                <div class="fee-item">
                  <span class="label">申购费</span>
                  <span class="value">{{ fund.purchaseRate != null ? `${fund.purchaseRate}%` : '--' }}</span>
                </div>
                <div class="fee-item">
                  <span class="label">赎回费</span>
                  <span class="value">{{ fund.redemptionRate != null ? `${fund.redemptionRate}%` : '--' }}</span>
                </div>
              </div>
            </n-tab-pane>
          </n-tabs>
        </section>
      </template>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NAvatar,
  NButton,
  NEmpty,
  NIcon,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
  createDiscreteApi,
} from 'naive-ui'
import { StarOutline, TrendingUpOutline } from '@vicons/ionicons5'
import { favoriteApi, fundApi } from '../api/fund'
import { useAuthStore } from '../stores/auth'
import FundTrendChart from '../components/FundTrendChart.vue'
import type { FundDetailVO, FundHoldingVO, FundManagerVO, FundNavHistoryVO } from '../types'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref('overview')
const fund = ref<FundDetailVO>()
const navHistory = ref<FundNavHistoryVO[]>([])
const managers = ref<FundManagerVO[]>([])
const holdings = ref<FundHoldingVO[]>([])

const loading = ref(false)
const managerLoading = ref(false)
const holdingsLoading = ref(false)
const favoriteLoading = ref(false)
const isFavorite = ref(false)
const expandHoldings = ref(false)

const expandedIdeas = reactive<Record<string, boolean>>({})
const expandedResumes = reactive<Record<string, boolean>>({})

const fundCode = computed(() => route.params.fundCode as string)

const performanceItems = computed(() => [
  { label: '近一月', value: fund.value?.monthGrowth },
  { label: '近三月', value: fund.value?.threeMonthGrowth },
  { label: '近六月', value: fund.value?.sixMonthGrowth },
  { label: '近一年', value: fund.value?.yearGrowth },
  { label: '成立以来', value: fund.value?.totalGrowth },
])

const riskLevelName = computed(() => {
  const level = fund.value?.riskLevel
  if (!level) return '未知'
  const mapping: Record<number, string> = {
    1: '低风险',
    2: '中低风险',
    3: '中风险',
    4: '中高风险',
    5: '高风险',
  }
  return mapping[level] || '未知'
})

const riskTagType = computed(() => {
  const level = fund.value?.riskLevel || 3
  if (level <= 2) return 'success'
  if (level === 3) return 'warning'
  return 'error'
})

const visibleHoldings = computed(() => {
  return expandHoldings.value ? holdings.value : holdings.value.slice(0, 5)
})

const resetExpandState = () => {
  expandHoldings.value = false
  Object.keys(expandedIdeas).forEach((k) => delete expandedIdeas[k])
  Object.keys(expandedResumes).forEach((k) => delete expandedResumes[k])
}

const formatNav = (value?: number) => {
  if (value == null) return '--'
  return value.toFixed(4)
}

const formatNumber = (value?: number) => {
  if (value == null) return '--'
  return value.toFixed(2)
}

const formatGrowth = (value?: number) => {
  if (value == null) return '--'
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

const growthClass = (value?: number) => {
  if (value == null) return ''
  return value >= 0 ? 'growth-positive' : 'growth-negative'
}

const shouldCollapse = (text?: string, limit: number = 120) => {
  return !!text && text.length > limit
}

const loadFundDetail = async () => {
  loading.value = true
  try {
    fund.value = await fundApi.getDetail(fundCode.value)
    await loadNavHistory('month')
    await checkFavorite()
  } catch (e: any) {
    message.error(e.message || '加载基金详情失败')
    fund.value = undefined
  } finally {
    loading.value = false
  }
}

const loadNavHistory = async (period: string) => {
  try {
    navHistory.value = (await fundApi.getNavHistory(fundCode.value, period)) || []
  } catch {
    navHistory.value = []
  }
}

const loadManagers = async () => {
  managerLoading.value = true
  try {
    managers.value = (await fundApi.getManager(fundCode.value)) || []
    managers.value.forEach((m) => {
      expandedIdeas[m.managerId] = false
      expandedResumes[m.managerId] = false
    })
  } catch {
    managers.value = []
  } finally {
    managerLoading.value = false
  }
}

const loadHoldings = async () => {
  holdingsLoading.value = true
  try {
    holdings.value = (await fundApi.getHoldings(fundCode.value, 10)) || []
  } catch {
    holdings.value = []
  } finally {
    holdingsLoading.value = false
  }
}

const checkFavorite = async () => {
  if (!authStore.isLoggedIn) {
    isFavorite.value = false
    return
  }
  try {
    isFavorite.value = await favoriteApi.check(fundCode.value)
  } catch {
    isFavorite.value = false
  }
}

const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    message.warning('请先登录')
    router.push('/login')
    return
  }

  favoriteLoading.value = true
  try {
    if (isFavorite.value) {
      await favoriteApi.remove(fundCode.value)
      isFavorite.value = false
      message.success('取消收藏成功')
    } else {
      await favoriteApi.add(fundCode.value)
      isFavorite.value = true
      message.success('收藏成功')
    }
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    favoriteLoading.value = false
  }
}

watch(
  () => fundCode.value,
  async (code) => {
    if (!code) return
    resetExpandState()
    activeTab.value = 'overview'
    await Promise.all([loadFundDetail(), loadManagers(), loadHoldings()])
  },
  { immediate: true }
)
</script>

<style scoped>
.fund-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero {
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hero__title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}

.hero__title-row h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}

.hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--text-secondary);
  font-size: 13px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.kpi-card {
  padding: 14px;
  border: 1px solid var(--border-color);
  background: var(--gradient-card);
}

.kpi-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-number);
}

.kpi-value--primary {
  color: var(--primary-color);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 650;
}

.info-tabs {
  padding: 18px;
}

.overview-grid,
.performance-grid,
.fee-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 12px 0;
}

.overview-item,
.perf-item,
.fee-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-item .label,
.perf-item .label,
.fee-item .label {
  color: var(--text-secondary);
  font-size: 12px;
}

.overview-item .value,
.perf-item .value,
.fee-item .value {
  font-size: 18px;
  font-weight: 600;
}

.manager-list,
.holdings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 10px;
}

.manager-card,
.holding-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px;
}

.manager-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.manager-info {
  flex: 1;
  min-width: 0;
}

.manager-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.manager-name {
  font-size: 16px;
  font-weight: 600;
}

.manager-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 12px;
}

.manager-return {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.expand-block {
  margin-top: 8px;
}

.expand-content {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.expand-content--collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.expand-label {
  color: var(--text-color);
  font-weight: 600;
}

.expand-btn {
  margin-top: 2px;
}

.holdings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 4px;
}

.holding-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.holding-rank {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--primary-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.holding-info {
  flex: 1;
  min-width: 0;
}

.stock-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stock-code {
  font-size: 12px;
  color: var(--text-secondary);
}

.holding-metric {
  min-width: 92px;
  text-align: right;
}

.holding-metric .label {
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.holding-metric .value {
  font-size: 15px;
  font-weight: 600;
}

.growth-positive {
  color: var(--up-color);
}

.growth-negative {
  color: var(--down-color);
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .overview-grid,
  .performance-grid,
  .fee-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero__title-row h1 {
    font-size: 22px;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid,
  .performance-grid,
  .fee-grid {
    grid-template-columns: 1fr;
  }

  .holding-item {
    flex-wrap: wrap;
  }

  .holding-metric {
    min-width: 0;
    width: calc(50% - 7px);
    text-align: left;
  }
}
</style>
