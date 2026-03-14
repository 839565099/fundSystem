<template>
  <div class="page-container">
    <div class="detail-header glass-card">
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
                <n-avatar round size="large">{{ m.managerName?.charAt(0) }}</n-avatar>
                <div class="manager-info">
                  <div class="manager-name">{{ m.managerName }}</div>
                  <div class="manager-detail">任职: {{ m.startDate }} | 任职回报: {{ m.bestReturn?.toFixed(2) }}%</div>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无基金经理信息" />
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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NTag, NTabs, NTabPane, NSpin, NEmpty, NAvatar, createDiscreteApi } from 'naive-ui'
import { StarOutline, TrendingUpOutline } from '@vicons/ionicons5'
import { fundApi, favoriteApi } from '../api/fund'
import { useAuthStore } from '../stores/auth'
import FundTrendChart from '../components/FundTrendChart.vue'
import type { FundDetailVO, FundNavHistoryVO, FundManagerVO } from '../types'

const { message } = createDiscreteApi(['message'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const fund = ref<FundDetailVO>()
const navHistory = ref<FundNavHistoryVO[]>([])
const managers = ref<FundManagerVO[]>([])
const loading = ref(true)
const managerLoading = ref(false)
const favoriteLoading = ref(false)
const isFavorite = ref(false)

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
  background: var(--bg-color);
  border-radius: 12px;
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
  color: #3b82f6;
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
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--shadow);
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
  background: var(--bg-color);
  border-radius: 12px;
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
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--bg-color);
  border-radius: 12px;
}

.manager-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.manager-detail {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
