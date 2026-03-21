<template>
  <div class="page-container">
    <div class="detail-header card">
      <n-spin :show="loading">
        <div v-if="fund" class="fund-info">
          <div class="fund-title">
            <h1>{{ fund.fundName }}</h1>
            <n-tag type="info" size="small">{{ fund.fundType }}</n-tag>
          </div>
          <div class="fund-code">{{ fund.fundCode }}</div>
          
          <div class="fund-stats">
            <div class="stat-card">
              <div class="stat-label">最新净值</div>
              <div class="stat-value primary">{{ fund.nav?.toFixed(4) }}</div>
              <div class="stat-date">{{ fund.navDate }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">今日涨跌</div>
              <div class="stat-value" :class="fund.dayGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund.dayGrowth! >= 0 ? '+' : '' }}{{ fund.dayGrowth?.toFixed(2) }}%
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">近一周</div>
              <div class="stat-value" :class="fund.weekGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund.weekGrowth! >= 0 ? '+' : '' }}{{ fund.weekGrowth?.toFixed(2) }}%
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">近一年</div>
              <div class="stat-value" :class="fund.yearGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund.yearGrowth! >= 0 ? '+' : '' }}{{ fund.yearGrowth?.toFixed(2) }}%
              </div>
            </div>
          </div>
          
          <div class="fund-actions">
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
        </div>
      </n-spin>
    </div>

    <div class="chart-section">
      <h2 class="section-title">
        <n-icon size="24"><TrendingUpOutline /></n-icon>
        净值走势
      </h2>
      <FundTrendChart 
        :data="navHistory" 
        :fund-name="fund?.fundName"
        @period-change="loadNavHistory"
      />
    </div>

    <div class="info-section">
      <n-tabs type="line" animated>
        <n-tab-pane name="performance" tab="业绩表现">
          <div class="performance-grid">
            <div class="perf-item">
              <span class="label">近一月</span>
              <span :class="fund?.monthGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund?.monthGrowth! >= 0 ? '+' : '' }}{{ fund?.monthGrowth?.toFixed(2) || '--' }}%
              </span>
            </div>
            <div class="perf-item">
              <span class="label">近三月</span>
              <span :class="fund?.threeMonthGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund?.threeMonthGrowth! >= 0 ? '+' : '' }}{{ fund?.threeMonthGrowth?.toFixed(2) || '--' }}%
              </span>
            </div>
            <div class="perf-item">
              <span class="label">近六月</span>
              <span :class="fund?.sixMonthGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund?.sixMonthGrowth! >= 0 ? '+' : '' }}{{ fund?.sixMonthGrowth?.toFixed(2) || '--' }}%
              </span>
            </div>
            <div class="perf-item">
              <span class="label">成立以来</span>
              <span :class="fund?.totalGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund?.totalGrowth! >= 0 ? '+' : '' }}{{ fund?.totalGrowth?.toFixed(2) || '--' }}%
              </span>
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
                  <div class="manager-name">{{ m.managerName }}</div>
                  <div v-if="m.company" class="manager-company">{{ m.company }}</div>
                  <div class="manager-meta">
                    <span v-if="m.startDate">任职: {{ m.startDate }}</span>
                    <span v-if="m.workYears != null"> | 从业{{ m.workYears }}年</span>
                    <span v-if="m.totalAsset != null"> | 管理规模{{ m.totalAsset }}亿</span>
                    <span v-if="m.fundCount != null"> | 管理{{ m.fundCount }}只基金</span>
                  </div>
                  <div v-if="m.bestReturn != null" class="manager-detail">
                    任职回报: <span :class="m.bestReturn >= 0 ? 'growth-positive' : 'growth-negative'">{{ m.bestReturn >= 0 ? '+' : '' }}{{ m.bestReturn.toFixed(2) }}%</span>
                  </div>
                  <div v-if="m.investmentIdea" class="manager-idea">
                    <div
                      class="idea-content"
                      :class="{ 'idea-collapsed': !expandedIdeas[m.managerId] }"
                    >
                      <span class="idea-label">投资理念：</span>{{ m.investmentIdea }}
                    </div>
                    <span
                      v-if="m.investmentIdea.length > 60"
                      class="idea-toggle"
                      @click="expandedIdeas[m.managerId] = !expandedIdeas[m.managerId]"
                    >
                      {{ expandedIdeas[m.managerId] ? '收起' : '展开' }}
                    </span>
                  </div>
                  <div v-if="m.resume" class="manager-resume">
                    <div
                      class="resume-content"
                      :class="{ 'resume-collapsed': !expandedResumes[m.managerId] }"
                    >
                      <span class="resume-label">简历：</span>{{ m.resume }}
                    </div>
                    <span
                      class="resume-toggle"
                      @click="expandedResumes[m.managerId] = !expandedResumes[m.managerId]"
                    >
                      {{ expandedResumes[m.managerId] ? '收起' : '展开' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无基金经理信息" />
          </n-spin>
        </n-tab-pane>
        
        <n-tab-pane name="holdings" tab="重仓股票">
          <n-spin :show="holdingsLoading">
            <div v-if="holdings.length > 0" class="holdings-section">
              <div class="holdings-header">
                <span class="report-date">报告日期: {{ holdings[0]?.reportDate }}</span>
              </div>
              <div class="holdings-list">
                <div v-for="(h, index) in holdings" :key="h.id" class="holding-item">
                  <div class="holding-rank">{{ index + 1 }}</div>
                  <div class="holding-info">
                    <div class="stock-name">{{ h.stockName }}</div>
                    <div class="stock-code">{{ h.stockCode }}</div>
                  </div>
                  <div class="holding-ratio">
                    <div class="ratio-label">持仓比例</div>
                    <div class="ratio-value">{{ h.holdingRatio?.toFixed(2) }}%</div>
                  </div>
                  <div class="stock-growth">
                    <div class="growth-label">今日涨跌</div>
                    <div class="growth-value" :class="h.dayGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                      {{ h.dayGrowth !== undefined && h.dayGrowth !== null ? (h.dayGrowth >= 0 ? '+' : '') + h.dayGrowth.toFixed(2) + '%' : '--' }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无重仓股票信息" />
          </n-spin>
        </n-tab-pane>
        
        <n-tab-pane name="fee" tab="费率信息">
          <div class="fee-grid">
            <div class="fee-item">
              <span class="label">管理费</span>
              <span>{{ fund?.managementRate || '--' }}%</span>
            </div>
            <div class="fee-item">
              <span class="label">托管费</span>
              <span>{{ fund?.custodyRate || '--' }}%</span>
            </div>
            <div class="fee-item">
              <span class="label">申购费</span>
              <span>{{ fund?.purchaseRate || '--' }}%</span>
            </div>
            <div class="fee-item">
              <span class="label">赎回费</span>
              <span>{{ fund?.redemptionRate || '--' }}%</span>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NTag, NTabs, NTabPane, NSpin, NEmpty, NAvatar, createDiscreteApi } from 'naive-ui'
import { StarOutline, TrendingUpOutline } from '@vicons/ionicons5'
import { fundApi, favoriteApi } from '../api/fund'
import { useAuthStore } from '../stores/auth'
import FundTrendChart from '../components/FundTrendChart.vue'
import type { FundDetailVO, FundNavHistoryVO, FundManagerVO, FundHoldingVO } from '../types'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const fund = ref<FundDetailVO>()
const navHistory = ref<FundNavHistoryVO[]>([])
const managers = ref<FundManagerVO[]>([])
const holdings = ref<FundHoldingVO[]>([])
const loading = ref(true)
const managerLoading = ref(false)
const holdingsLoading = ref(false)
const favoriteLoading = ref(false)
const isFavorite = ref(false)
const expandedIdeas = reactive<Record<string, boolean>>({})
const expandedResumes = reactive<Record<string, boolean>>({})

const fundCode = computed(() => route.params.fundCode as string)

const loadFundDetail = async () => {
  loading.value = true
  try {
    fund.value = await fundApi.getDetail(fundCode.value)
    await loadNavHistory('month')
    checkFavorite()
  } catch (e: any) {
    message.error(e.message || '加载基金详情失败')
    // 保留在详情页显示错误，不跳转
  } finally {
    loading.value = false
  }
}

const loadNavHistory = async (period: string) => {
  try {
    navHistory.value = await fundApi.getNavHistory(fundCode.value, period) || []
  } catch {
    navHistory.value = []
  }
}

const loadManagers = async () => {
  managerLoading.value = true
  try {
    managers.value = await fundApi.getManager(fundCode.value) || []
  } catch {
    managers.value = []
  } finally {
    managerLoading.value = false
  }
}

const loadHoldings = async () => {
  holdingsLoading.value = true
  try {
    holdings.value = await fundApi.getHoldings(fundCode.value, 10) || []
  } catch {
    holdings.value = []
  } finally {
    holdingsLoading.value = false
  }
}

const checkFavorite = async () => {
  if (!authStore.isLoggedIn) return
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

onMounted(() => {
  loadFundDetail()
  loadManagers()
  loadHoldings()
})
</script>

<style scoped>
.detail-header {
  margin-bottom: 24px;
  padding: 24px;
}

.fund-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.fund-title h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}

.fund-code {
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.fund-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 800px) {
  .fund-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-value.primary {
  color: var(--primary-color);
}

.stat-date {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.fund-actions {
  display: flex;
  gap: 12px;
}

.chart-section {
  margin-bottom: 24px;
}

.info-section {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.performance-grid, .fee-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 16px 0;
}

@media (max-width: 800px) {
  .performance-grid, .fee-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.perf-item, .fee-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.perf-item .label, .fee-item .label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.perf-item span:last-child, .fee-item span:last-child {
  font-size: 20px;
  font-weight: 600;
}

.manager-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.manager-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.manager-name {
  font-weight: 600;
  margin-bottom: 2px;
}

.manager-company {
  font-size: 13px;
  color: var(--primary-color);
  margin-bottom: 4px;
}

.manager-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.manager-detail {
  font-size: 13px;
  color: var(--text-secondary);
}

.manager-idea,
.manager-resume {
  margin-top: 8px;
}

.idea-content,
.resume-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.idea-collapsed,
.resume-collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.idea-label,
.resume-label {
  color: var(--text-color);
  font-weight: 500;
}

.idea-toggle,
.resume-toggle {
  font-size: 12px;
  color: var(--primary-color);
  cursor: pointer;
  user-select: none;
  margin-top: 2px;
  display: inline-block;
}

.idea-toggle:hover,
.resume-toggle:hover {
  opacity: 0.8;
}

.holdings-section {
  padding: 16px 0;
}

.holdings-header {
  margin-bottom: 16px;
  padding: 0 8px;
}

.report-date {
  font-size: 13px;
  color: var(--text-secondary);
}

.holdings-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.holding-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}

.holding-rank {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-color);
  color: white;
  border-radius: 50%;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.holding-info {
  flex: 1;
  min-width: 0;
}

.stock-name {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stock-code {
  font-size: 12px;
  color: var(--text-secondary);
}

.holding-ratio {
  text-align: center;
  min-width: 80px;
}

.ratio-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.ratio-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.stock-growth {
  text-align: center;
  min-width: 80px;
}

.growth-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.growth-value {
  font-size: 16px;
  font-weight: 600;
}

.growth-positive {
  color: #f5222d;
}

.growth-negative {
  color: #18a058;
}</style>
