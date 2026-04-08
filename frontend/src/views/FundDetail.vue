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
                <n-icon><IconStar /></n-icon>
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
            <n-icon size="22"><IconTrendingUp /></n-icon>
            净值走势
          </h2>
          <FundTrendChart
            :data="navHistory"
            :fund-name="fund.fundName"
            :intraday-data="intradayData"
            @period-change="loadNavHistory"
          />
        </section>

        <!-- 概览卡片 -->
        <section class="info-card card">
          <h3 class="card-title">
            <n-icon size="18"><IconInfoCircle /></n-icon>
            概览
          </h3>
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
              <span class="value">
                <n-tag v-if="fund.riskLevel" size="small" :type="riskTagType">{{ riskLevelName }}</n-tag>
                <span v-else>--</span>
              </span>
            </div>
            <div class="overview-item">
              <span class="label">基金规模</span>
              <span class="value">{{ fund.fundScale != null ? `${fund.fundScale.toFixed(2)}亿` : '--' }}</span>
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
        </section>

        <!-- 业绩表现卡片 -->
        <section class="info-card card">
          <h3 class="card-title">
            <n-icon size="18"><IconChartBar /></n-icon>
            业绩表现
          </h3>
          <div class="performance-row">
            <div v-for="item in performanceItems" :key="item.label" class="perf-block" :class="growthBgClass(item.value)">
              <span class="perf-label">{{ item.label }}</span>
              <span class="perf-value" :class="growthClass(item.value)">{{ formatGrowth(item.value) }}</span>
            </div>
          </div>
        </section>

        <!-- 基金经理卡片 -->
        <section class="info-card card">
          <h3 class="card-title">
            <n-icon size="18"><IconUser /></n-icon>
            基金经理
          </h3>
          <n-spin :show="managerLoading">
            <div v-if="managers.length > 0" class="manager-list">
              <div v-for="m in managers" :key="m.managerId" class="manager-card">
                <n-avatar v-if="m.photo" round size="large" :src="m.photo">
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
                    <div class="expand-content" :class="{ 'expand-content--collapsed': !expandedIdeas[m.managerId] && shouldCollapse(m.investmentIdea, 90) }">
                      <span class="expand-label">投资理念：</span>{{ m.investmentIdea }}
                    </div>
                    <n-button v-if="shouldCollapse(m.investmentIdea, 90)" text size="tiny" class="expand-btn" @click="expandedIdeas[m.managerId] = !expandedIdeas[m.managerId]">
                      {{ expandedIdeas[m.managerId] ? '收起' : '展开' }}
                    </n-button>
                  </div>

                  <div v-if="m.resume" class="expand-block">
                    <div class="expand-content" :class="{ 'expand-content--collapsed': !expandedResumes[m.managerId] && shouldCollapse(m.resume, 120) }">
                      <span class="expand-label">简介：</span>{{ m.resume }}
                    </div>
                    <n-button v-if="shouldCollapse(m.resume, 120)" text size="tiny" class="expand-btn" @click="expandedResumes[m.managerId] = !expandedResumes[m.managerId]">
                      {{ expandedResumes[m.managerId] ? '收起' : '展开' }}
                    </n-button>
                  </div>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无基金经理信息" />
          </n-spin>
        </section>

        <!-- 重仓持仓卡片 -->
        <section class="info-card card">
          <h3 class="card-title">
            <n-icon size="18"><IconBriefcase /></n-icon>
            重仓持仓
            <span v-if="holdings.length > 0" class="card-subtitle">报告期: {{ holdings[0]?.reportDate || '--' }}</span>
          </h3>
          <n-spin :show="holdingsLoading">
            <div v-if="holdings.length > 0">
              <div class="holdings-table">
                <div class="holdings-table-header">
                  <span class="col-rank">排名</span>
                  <span class="col-name">股票名称</span>
                  <span class="col-code">代码</span>
                  <span class="col-ratio">持仓占比</span>
                  <span class="col-growth">今日涨跌</span>
                </div>
                <div v-for="(h, index) in visibleHoldings" :key="`${h.stockCode}-${index}`" class="holding-row">
                  <span class="col-rank">
                    <span class="rank-badge" :class="{ 'rank-badge--top3': index < 3 }">{{ index + 1 }}</span>
                  </span>
                  <span class="col-name">{{ h.stockName || '--' }}</span>
                  <span class="col-code">{{ h.stockCode || '--' }}</span>
                  <span class="col-ratio">
                    <div class="ratio-bar-wrap">
                      <div class="ratio-bar" :style="{ width: Math.min(h.holdingRatio || 0, 10) / 10 * 100 + '%' }"></div>
                      <span class="ratio-text">{{ h.holdingRatio != null ? h.holdingRatio.toFixed(2) + '%' : '--' }}</span>
                    </div>
                  </span>
                  <span class="col-growth" :class="growthClass(h.dayGrowth)">{{ formatGrowth(h.dayGrowth) }}</span>
                </div>
              </div>
              <div v-if="holdings.length > 5" class="holdings-expand">
                <n-button text size="small" @click="expandHoldings = !expandHoldings">
                  {{ expandHoldings ? '收起' : `展开全部 ${holdings.length} 只` }}
                  <template #icon>
                    <n-icon><IconChevronDown /></n-icon>
                  </template>
                </n-button>
              </div>
            </div>
            <n-empty v-else description="暂无重仓持仓信息" />
          </n-spin>
        </section>

        <!-- 费率信息卡片 -->
        <section class="info-card card">
          <h3 class="card-title">
            <n-icon size="18"><IconCoin /></n-icon>
            费率信息
          </h3>
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
  NTag,
  createDiscreteApi,
} from 'naive-ui'
import {
  IconStar,
  IconTrendingUp,
  IconInfoCircle,
  IconChartBar,
  IconUser,
  IconBriefcase,
  IconCoin,
  IconChevronDown,
} from '@tabler/icons-vue'
import { favoriteApi, fundApi } from '../api/fund'
import { useAuthStore } from '../stores/auth'
import FundTrendChart from '../components/FundTrendChart.vue'
import type { FundDetailVO, FundHoldingVO, FundManagerVO, FundNavHistoryVO } from '../types'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const fund = ref<FundDetailVO>()
const navHistory = ref<FundNavHistoryVO[]>([])
const intradayData = ref<{ fundCode: string; prevNav: number; isEtf: boolean; trends: Array<{ time: string; price: number; changeRatio: number }> } | null>(null)
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
  intradayData.value = null
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

const growthBgClass = (value?: number) => {
  if (value == null) return ''
  return value >= 0 ? 'perf-block--up' : 'perf-block--down'
}

const shouldCollapse = (text?: string, limit: number = 120) => {
  return !!text && text.length > limit
}

const loadFundDetail = async () => {
  loading.value = true
  try {
    fund.value = await fundApi.getDetail(fundCode.value)
    await Promise.all([loadNavHistory('month'), loadIntradayData()])
    await checkFavorite()
  } catch (e: any) {
    message.error(e.message || '加载基金详情失败')
    fund.value = undefined
  } finally {
    loading.value = false
  }
}

const loadNavHistory = async (period: string) => {
  if (period === 'intraday') {
    await loadIntradayData()
    return
  }
  try {
    navHistory.value = (await fundApi.getNavHistory(fundCode.value, period)) || []
  } catch {
    navHistory.value = []
  }
}

const loadIntradayData = async () => {
  try {
    intradayData.value = await fundApi.getFundTrends(fundCode.value)
  } catch {
    intradayData.value = null
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

/* 卡片公共样式 */
.info-card {
  padding: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 650;
  color: var(--text-color);
}

.card-subtitle {
  margin-left: auto;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-secondary);
}

/* 概览 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-item .label {
  color: var(--text-secondary);
  font-size: 12px;
}

.overview-item .value {
  font-size: 16px;
  font-weight: 600;
}

/* 业绩表现 */
.performance-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.perf-block {
  text-align: center;
  padding: 14px 8px;
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}

.perf-block--up {
  background: rgba(207, 19, 34, 0.06);
  border-color: rgba(207, 19, 34, 0.15);
}

.perf-block--down {
  background: rgba(0, 128, 0, 0.06);
  border-color: rgba(0, 128, 0, 0.15);
}

.perf-label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.perf-value {
  font-size: 20px;
  font-weight: 700;
  font-family: var(--font-number);
}

/* 基金经理 */
.manager-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.manager-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px;
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

/* 重仓持仓 - 表格 */
.holdings-table {
  width: 100%;
}

.holdings-table-header {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
}

.holding-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
  font-size: 14px;
}

.holding-row:last-child {
  border-bottom: none;
}

.col-rank {
  width: 50px;
  flex-shrink: 0;
}

.col-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.col-code {
  width: 80px;
  flex-shrink: 0;
  color: var(--text-secondary);
  font-size: 12px;
  font-family: var(--font-number);
}

.col-ratio {
  width: 180px;
  flex-shrink: 0;
}

.col-growth {
  width: 90px;
  flex-shrink: 0;
  text-align: right;
  font-weight: 600;
  font-family: var(--font-number);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
}

.rank-badge--top3 {
  background: var(--primary-color);
  color: #fff;
  border-color: var(--primary-color);
}

.ratio-bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ratio-bar {
  height: 6px;
  border-radius: 3px;
  background: var(--primary-color);
  min-width: 4px;
  flex: 1;
  max-width: 100px;
  opacity: 0.6;
}

.ratio-text {
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font-number);
  white-space: nowrap;
}

.holdings-expand {
  text-align: center;
  padding-top: 8px;
}

/* 费率 */
.fee-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.fee-item {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.fee-item .label {
  color: var(--text-secondary);
  font-size: 12px;
}

.fee-item .value {
  font-size: 16px;
  font-weight: 600;
}

/* 涨跌颜色 */
.growth-positive {
  color: var(--up-color);
}

.growth-negative {
  color: var(--down-color);
}

/* 响应式 */
@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .performance-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

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

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .performance-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .fee-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .holdings-table-header,
  .holding-row {
    font-size: 13px;
  }

  .col-ratio {
    width: 120px;
  }

  .col-code {
    display: none;
  }

  .col-growth {
    width: 70px;
  }
}
</style>
