<template>
  <div class="recommend-page page-container">
    <div class="page-header">
      <h1 class="page-title">
        <n-icon size="28"><SparklesOutline /></n-icon>
        智能推荐
      </h1>
    </div>

    <!-- 风险偏好选择 -->
    <n-card class="preference-card">
      <template #header>
        <span class="card-title">您的投资偏好</span>
      </template>
      <div class="preference-options">
        <div
          v-for="pref in preferences"
          :key="pref.value"
          class="preference-item"
          :class="{ active: selectedPreference === pref.value }"
          @click="selectedPreference = pref.value"
        >
          <n-icon :component="pref.icon" size="24" />
          <div class="preference-info">
            <div class="preference-name">{{ pref.label }}</div>
            <div class="preference-desc">{{ pref.desc }}</div>
          </div>
        </div>
      </div>
    </n-card>

    <!-- 掙跌幅排行 -->
    <n-card class="ranking-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">
            <n-icon><TrendingUpOutline /></n-icon>
            涨幅排行 TOP 10
          </span>
          <n-button text @click="showMoreGrowth">
            查看更多
            <n-icon><ChevronForwardOutline /></n-icon>
          </n-button>
        </div>
      </template>
      <n-data-table
        :columns="rankingColumns"
        :data="growthRanking"
        :loading="loading"
        :bordered="false"
        :row-key="row => row.fundCode"
        @click-row="handleRowClick"
      />
    </n-card>

    <div class="recommend-grid">
      <!-- 热门推荐 -->
      <n-card class="recommend-section">
        <template #header>
          <div class="section-header">
            <span class="section-title">
              <n-icon><FlameOutline /></n-icon>
              热门推荐
            </span>
          </div>
        </template>
        <n-spin :show="hotLoading">
          <div class="fund-grid">
            <fund-recommend-card
              v-for="fund in hotFunds"
              :key="fund.fundCode"
              :fund="fund"
              source="热门"
              @click="goToFund(fund.fundCode)"
            />
          </div>
        </n-spin>
      </n-card>

      <!-- 个性化推荐 -->
      <n-card class="recommend-section">
        <template #header>
          <div class="section-header">
            <span class="section-title">
              <n-icon><PersonOutline /></n-icon>
              为您推荐
            </span>
            <span class="recommend-tip">基于您的投资偏好</span>
          </div>
        </template>
        <n-spin :show="personalLoading">
          <div v-if="personalFunds.length" class="fund-grid">
            <fund-recommend-card
              v-for="fund in personalFunds"
              :key="fund.fundCode"
              :fund="fund"
              :source="fund.source"
              :reason="fund.reason"
              @click="goToFund(fund.fundCode)"
            />
          </div>
          <n-empty v-else description="登录后获取个性化推荐">
            <template #extra>
              <n-button type="primary" @click="router.push('/login')">
                立即登录
              </n-button>
            </template>
          </n-empty>
        </n-spin>
      </n-card>
    </div>

    <!-- 按类型推荐 -->
    <n-card class="type-recommend-card">
      <template #header>
        <span class="card-title">按类型查看</span>
      </template>
      <div class="type-tabs">
        <n-tag
          v-for="type in fundTypes"
          :key="type"
          :checked="selectedType === type"
          checkable
          @click="selectType = type"
        >
          {{ type }}
        </n-tag>
      </div>
      <n-spin :show="typeLoading">
        <div class="fund-grid">
          <fund-recommend-card
            v-for="fund in typeFunds"
            :key="fund.fundCode"
            :fund="fund"
            @click="goToFund(fund.fundCode)"
          />
        </div>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, h } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard, NIcon, NDataTable, NSpin, NEmpty, NButton, NTag, useMessage
} from 'naive-ui'
import {
  SparklesOutline, TrendingUpOutline, ChevronForwardOutline, FlameOutline,
  PersonOutline, ShieldCheckmarkOutline, WarningOutline, FlashOutline
} from '@vicons/ionicons5'
import { useAuthStore } from '@/stores/auth'
import { fundApi } from '@/api/fund'
import type { Fund } from '@/types'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

const loading = ref(false)
const hotLoading = ref(false)
const personalLoading = ref(false)
const typeLoading = ref(false)

const selectedPreference = ref(2) // 默认稳健型
const selectedType = ref('全部')
const growthRanking = ref<any[]>([])
const hotFunds = ref<any[]>([])
const personalFunds = ref<any[]>([])
const typeFunds = ref<any[]>([])

const fundTypes = ['全部', '股票型', '混合型', '债券型', '指数型', '货币型', 'QDII']

const preferences = [
  { value: 1, label: '保守型', desc: '追求稳定收益，风险承受能力低', icon: ShieldCheckmarkOutline },
  { value: 2, label: '稳健型', desc: '平衡收益与风险，追求长期增值', icon: WarningOutline },
  { value: 3, label: '激进型', desc: '追求高收益，接受较大波动', icon: FlashOutline }
]

const rankingColumns = [
  { title: '排名', key: 'rank', width: 60, render: (_: any, i: number) => i + 1 },
  { title: '基金代码', key: 'fundCode', width: 90 },
  { title: '基金名称', key: 'fundName', ellipsis: { tooltip: true } },
  { title: '最新净值', key: 'nav', width: 100, render: (row: any) => row.nav?.toFixed(4) },
  { title: '日涨跌', key: 'dayGrowth', width: 100, render: (row: any) => {
    const val = row.dayGrowth
    const cls = val >= 0 ? 'positive' : 'negative'
    return { text: `${val >= 0 ? '+' : ''}${val?.toFixed(2)}%`, class: cls }
  }},
  { title: '近一年', key: 'yearGrowth', width: 100, render: (row: any) => {
    const val = row.yearGrowth
    const cls = val >= 0 ? 'positive' : 'negative'
    return { text: `${val >= 0 ? '+' : ''}${val?.toFixed(2)}%`, class: cls }
  }}
]

// 加载数据
const loadRanking = async () => {
  loading.value = true
  try {
    const res = await fundApi.getRanking('dayGrowth', 'desc', 1, 10)
    growthRanking.value = res.list || []
  } finally {
    loading.value = false
  }
}

const loadHotFunds = async () => {
  hotLoading.value = true
  try {
    const res = await fundApi.getHotRecommend(8)
    hotFunds.value = res || []
  } finally {
    hotLoading.value = false
  }
}

const loadPersonalFunds = async () => {
  if (!authStore.isLoggedIn) return
  personalLoading.value = true
  try {
    const res = await fundApi.getPersonalizedRecommend(8)
    personalFunds.value = res || []
  } finally {
    personalLoading.value = false
  }
}

const loadTypeFunds = async (type: string) => {
  typeLoading.value = true
  try {
    if (type === '全部') {
      const res = await fundApi.getHotRecommend(8)
      typeFunds.value = res || []
    } else {
      const res = await fundApi.search({ fundType: type, pageNum: 1, pageSize: 8 })
      typeFunds.value = res.list || []
    }
  } finally {
    typeLoading.value = false
  }
}

const goToFund = (fundCode: string) => {
  router.push(`/fund/${fundCode}`)
  // 记录用户行为
  if (authStore.isLoggedIn) {
    fundApi.recordBehavior(fundCode, 'click', 0)
  }
}

const handleRowClick = (row: any) => {
  goToFund(row.fundCode)
}

const showMoreGrowth = () => {
  router.push('/ranking')
}

// 监听偏好变化
watch(selectedPreference, async (val) => {
  // 更新推荐
  loadPersonalFunds()
})

// 监听类型变化
watch(selectedType, (val) => {
  loadTypeFunds(val)
})

onMounted(() => {
  loadRanking()
  loadHotFunds()
  loadPersonalFunds()
  loadTypeFunds('全部')
})

// 推荐卡片组件
const FundRecommendCard = defineComponent({
  name: 'FundRecommendCard',
  props: {
    fund: { type: Object, required: true },
    source: { type: String, default: '' },
    reason: { type: String, default: '' }
  },
  emits: ['click'],
  setup(props) {
    return () => h('div', {
      class: 'fund-recommend-card',
      onClick: () => emit('click'),
    }, [
      h('div', { class: 'card-header' }, [
        h('div', { class: 'fund-name' }, props.fund.fundName),
        h('div', { class: 'fund-code' }, props.fund.fundCode)
      ]),
      h('div', { class: 'card-body' }, [
        h('div', { class: 'nav-value' }, `净值: ${props.fund.nav?.toFixed(4)}`),
        h('div', {
          class: ['growth-value', props.fund.yearGrowth >= 0 ? 'positive' : 'negative']
        }, [
          h('span', null, props.fund.yearGrowth >= 0 ? '+' : ''),
          props.fund.yearGrowth?.toFixed(2),
          '%'
        ])
      ]),
      props.source && h('n-tag', {
        size: 'small',
        type: 'info',
        class: 'source-tag'
      }, props.source),
      props.reason && h('div', { class: 'recommend-reason' }, props.reason)
    ])
  }
})
</script>

<style scoped>
.recommend-page {
  padding: 24px;
}

.preference-card {
  margin-bottom: 20px;
}

.preference-options {
  display: flex;
  gap: 16px;
}

.preference-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-radius: 12px;
  border: 2px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
}

.preference-item:hover {
  border-color: var(--primary-color);
  background: rgba(59, 130, 246, 0.05);
}

.preference-item.active {
  border-color: var(--primary-color);
  background: rgba(59, 130, 246, 0.1);
}

.preference-info {
  flex: 1;
}

.preference-name {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 4px;
}

.preference-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

.ranking-card {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.recommend-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}

.recommend-section {
  min-height: 400px;
}

.recommend-tip {
  font-size: 12px;
  color: var(--text-tertiary);
}

.fund-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .fund-grid {
    grid-template-columns: 1fr;
  }
}

.type-recommend-card {
  margin-top: 20px;
}

.type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.fund-recommend-card {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
}

.fund-recommend-card:hover {
  border-color: var(--primary-color);
  background: var(--card-bg-hover);
  transform: translateY(-2px);
}

.fund-recommend-card .card-header {
  margin-bottom: 8px;
}

.fund-recommend-card .fund-name {
  font-weight: 600;
  font-size: 14px;
}

.fund-recommend-card .fund-code {
  font-size: 12px;
  color: var(--text-secondary);
}

.fund-recommend-card .card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fund-recommend-card .nav-value {
  color: var(--text-secondary);
  font-size: 13px;
}

.fund-recommend-card .growth-value {
  font-weight: 600;
  font-size: 16px;
}

.fund-recommend-card .source-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.fund-recommend-card .recommend-reason {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
}

.positive {
  color: var(--up-color);
}

.negative {
  color: var(--down-color);
}
</style>
