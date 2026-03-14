<template>
  <div class="page-container">
    <div class="ranking-header card">
      <div class="filter-group">
        <span class="filter-label">排行类型:</span>
        <n-radio-group v-model:value="rankingType" @update:value="loadRanking">
          <n-radio-button value="dayGrowth">日涨幅</n-radio-button>
          <n-radio-button value="weekGrowth">周涨幅</n-radio-button>
          <n-radio-button value="monthGrowth">月涨幅</n-radio-button>
          <n-radio-button value="yearGrowth">年涨幅</n-radio-button>
        </n-radio-group>
      </div>
      <div class="filter-group">
        <span class="filter-label">时间周期:</span>
        <n-radio-group v-model:value="period" @update:value="loadRanking">
          <n-radio-button value="day">今日</n-radio-button>
          <n-radio-button value="week">近一周</n-radio-button>
          <n-radio-button value="month">近一月</n-radio-button>
          <n-radio-button value="year">近一年</n-radio-button>
        </n-radio-group>
      </div>
    </div>

    <n-spin :show="loading">
      <div class="ranking-list">
        <div 
          v-for="(fund, index) in funds" 
          :key="fund.fundCode" 
          class="ranking-item card"
          @click="router.push(`/fund/${fund.fundCode}`)"
        >
          <div class="rank-badge" :class="getRankClass(index)">
            {{ index + 1 }}
          </div>
          <div class="fund-info">
            <div class="fund-name">{{ fund.fundName }}</div>
            <div class="fund-meta">
              <span>{{ fund.fundCode }}</span>
              <n-tag v-if="fund.fundType" size="small" type="info">{{ fund.fundType }}</n-tag>
            </div>
          </div>
          <div class="fund-stats">
            <div class="stat">
              <span class="label">净值</span>
              <span class="value">{{ fund.nav?.toFixed(4) }}</span>
            </div>
            <div class="stat">
              <span class="label">涨跌幅</span>
              <span class="value" :class="fund.dayGrowth! >= 0 ? 'growth-positive' : 'growth-negative'">
                {{ fund.dayGrowth! >= 0 ? '+' : '' }}{{ fund.dayGrowth?.toFixed(2) }}%
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="pagination">
        <n-pagination
          v-model:page="pageNum"
          :page-count="pageCount"
          @update:page="loadRanking"
        />
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NRadioGroup, NRadioButton, NSpin, NTag, NPagination, createDiscreteApi } from 'naive-ui'
import { fundApi } from '../api/fund'
import type { Fund } from '../types'

const { message } = createDiscreteApi(['message'])

const router = useRouter()

const loading = ref(false)
const rankingType = ref('dayGrowth')
const period = ref('day')
const funds = ref<Fund[]>([])
const pageNum = ref(1)
const pageSize = 20
const pageCount = ref(1)

const getRankClass = (index: number) => {
  if (index === 0) return 'gold'
  if (index === 1) return 'silver'
  if (index === 2) return 'bronze'
  return ''
}

const loadRanking = async () => {
  loading.value = true
  try {
    const data = await fundApi.getRanking(rankingType.value, period.value, pageNum.value, pageSize)
    funds.value = data.records || []
    pageCount.value = data.pages || 1
  } catch {
    message.error('加载排行榜失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadRanking)
</script>

<style scoped>
.ranking-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: var(--text-secondary);
  min-width: 80px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  cursor: pointer;
}

.rank-badge {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  background: var(--bg-color);
  color: var(--text-secondary);
}

.rank-badge.gold {
  background: linear-gradient(135deg, #ffd700 0%, #ffec8b 100%);
  color: #8b6914;
}

.rank-badge.silver {
  background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%);
  color: #5a5a5a;
}

.rank-badge.bronze {
  background: linear-gradient(135deg, #cd7f32 0%, #daa06d 100%);
  color: #5c3d1e;
}

.fund-info {
  flex: 1;
}

.fund-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.fund-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.fund-stats {
  display: flex;
  gap: 24px;
}

.stat {
  text-align: right;
}

.stat .label {
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}

.stat .value {
  font-size: 16px;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
