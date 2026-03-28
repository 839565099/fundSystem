<template>
  <div class="analytics-page page-container">
    <PageHeader title="高级分析" icon="📊" />

    <n-tabs v-model:value="activeTab" type="line" animated>
      <!-- 风险分析 -->
      <n-tab-pane name="risk" tab="风险分析">
        <n-card title="基金风险分析" class="analysis-card">
          <div class="analysis-form">
            <n-input-group>
              <n-input-group-label>基金代码</n-input-group-label>
              <n-select
                v-model:value="riskForm.fundCode"
                :options="riskOptions"
                placeholder="选择或搜索基金"
                style="width: 240px"
                filterable
                remote
                :loading="riskSearchLoading"
                @search="handleRiskSearch"
                @update:value="onRiskSelect"
                clearable
              />
              <n-select v-model:value="riskForm.period" :options="periodOptions" style="width: 120px" />
              <n-button type="primary" @click="analyzeRisk" :loading="riskLoading">
                分析
              </n-button>
            </n-input-group>
          </div>

          <div v-if="riskResult" class="analysis-result">
            <n-grid :cols="3" :x-gap="16">
              <n-gi>
                <n-statistic label="夏普比率" :value="riskResult.sharpeRatio?.toFixed(4) || '-'" />
              </n-gi>
              <n-gi>
                <n-statistic label="最大回撤" :value="riskResult.maxDrawdown?.toFixed(2) || '-'" suffix="%" />
              </n-gi>
              <n-gi>
                <n-statistic label="年化波动率" :value="riskResult.volatility?.toFixed(2) || '-'" suffix="%" />
              </n-gi>
            </n-grid>

            <n-divider />

            <n-descriptions label-placement="left" :column="2" bordered>
              <n-descriptions-item label="基金名称">{{ riskResult.fundName }}</n-descriptions-item>
              <n-descriptions-item label="分析周期">{{ getPeriodText(riskResult.period) }}</n-descriptions-item>
              <n-descriptions-item label="平均收益">{{ riskResult.avgReturn?.toFixed(4) }}%</n-descriptions-item>
              <n-descriptions-item label="无风险利率">{{ riskResult.riskFreeRate }}%</n-descriptions-item>
            </n-descriptions>
          </div>
        </n-card>
      </n-tab-pane>

      <!-- 相关性分析 -->
      <n-tab-pane name="correlation" tab="相关性分析">
        <n-card title="基金相关性矩阵" class="analysis-card">
          <!-- 功能说明 -->
          <n-alert type="info" style="margin-bottom: 16px;">
            <template #header>什么是相关性分析？</template>
            相关性分析用于衡量多只基金走势的相似程度。
            <ul style="margin: 8px 0 0 0; padding-left: 20px;">
              <li><b>正相关（红色）</b>：两只基金同涨同跌，数值越接近1越相似</li>
              <li><b>负相关（绿色）</b>：两只基金走势相反，一只涨另一只跌</li>
              <li><b>无相关</b>：两只基金走势独立，互不影响</li>
            </ul>
            <div style="margin-top: 8px;"><b>投资建议</b>：选择相关性较低的基金组合，可以分散风险、平滑波动。</div>
          </n-alert>

          <div class="analysis-form">
            <n-input-group>
              <n-input-group-label>添加基金</n-input-group-label>
              <n-select
                v-model:value="selectedCorrelationFund"
                :options="correlationOptions"
                placeholder="选择或搜索基金"
                style="width: 240px"
                filterable
                remote
                :loading="correlationSearchLoading"
                @search="handleCorrelationSearch"
                @update:value="onCorrelationSelect"
                clearable
              />
              <n-button size="small" @click="selectAllCorrelationFunds" :disabled="favoriteOptions.length === 0">
                全选收藏
              </n-button>
              <n-button size="small" @click="clearCorrelationFunds" :disabled="!correlationCodes">
                清空
              </n-button>
              <n-input
                v-model:value="correlationCodes"
                placeholder="已选基金代码（逗号分隔）"
                style="width: 200px"
                readonly
              />
              <n-select v-model:value="correlationPeriod" :options="periodOptions" style="width: 120px" />
              <n-button type="primary" @click="analyzeCorrelation" :loading="correlationLoading">
                分析
              </n-button>
            </n-input-group>
            <div class="selected-funds" v-if="correlationCodes">
              <span class="label">已选基金：</span>
              <n-tag
                v-for="code in correlationCodes.split(',').filter(Boolean)"
                :key="code"
                closable
                @close="removeCorrelationFund(code)"
                style="margin-right: 8px"
              >
                {{ code }}
              </n-tag>
            </div>
          </div>

          <!-- 空状态引导 -->
          <div v-if="!correlationResult" class="correlation-empty">
            <n-empty description="请至少选择2只基金进行相关性分析">
              <template #icon>
                <n-icon size="48" :depth="3"><GitCompareOutline /></n-icon>
              </template>
            </n-empty>
          </div>

          <!-- 分析结果 -->
          <div v-if="correlationResult" class="correlation-result">
            <n-divider>已选基金</n-divider>
            <div class="fund-list">
              <div v-for="(code, i) in correlationResult.fundCodes" :key="code" class="fund-item">
                <span class="fund-code">{{ code }}</span>
                <span class="fund-name">{{ correlationResult.fundNames[i] }}</span>
              </div>
            </div>

            <n-divider>相关性矩阵</n-divider>
            <div class="correlation-matrix-wrapper">
              <table class="correlation-table">
                <thead>
                  <tr>
                    <th class="corner-cell"></th>
                    <th v-for="(name, i) in correlationResult.fundNames" :key="i" class="header-cell">
                      {{ name?.substring(0, 6) }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in correlationResult.correlationMatrix" :key="i">
                    <td class="header-cell">{{ correlationResult.fundNames[i]?.substring(0, 6) }}</td>
                    <td
                      v-for="(val, j) in row"
                      :key="j"
                      class="data-cell"
                      :class="{ 'diagonal-cell': i === j }"
                      :style="{ backgroundColor: getCorrelationColor(val, i === j) }"
                      :title="getCorrelationTooltip(val, i, j)"
                    >
                      <span v-if="i === j">1.00</span>
                      <span v-else class="cell-content">
                        {{ val?.toFixed(2) }}
                        <small class="correlation-label">{{ getCorrelationLabel(val) }}</small>
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- 改进的图例 -->
            <div class="correlation-legend">
              <div class="legend-gradient">
                <div class="gradient-bar">
                  <div class="gradient-scale"></div>
                  <div class="gradient-labels">
                    <span>-1 完全负相关</span>
                    <span>0 无相关</span>
                    <span>+1 完全正相关</span>
                  </div>
                </div>
              </div>
              <div class="legend-tips">
                <n-tag size="small" type="success">绿色 = 走势相反（分散风险）</n-tag>
                <n-tag size="small" type="error">红色 = 走势相同（集中风险）</n-tag>
              </div>
            </div>
          </div>
        </n-card>
      </n-tab-pane>

      <!-- 定投模拟 -->
      <n-tab-pane name="dip" tab="定投模拟">
        <n-card title="定投收益模拟" class="analysis-card">
          <div class="dip-form">
            <n-grid :cols="4" :x-gap="16">
              <n-gi>
                <n-form-item label="基金代码">
                  <n-select
                    v-model:value="dipForm.fundCode"
                    :options="dipOptions"
                    placeholder="选择或搜索基金"
                    filterable
                    remote
                    :loading="dipSearchLoading"
                    @search="handleDipSearch"
                    @update:value="onDipSelect"
                    clearable
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="每期金额">
                  <n-input-number v-model:value="dipForm.amount" :min="100" :step="100">
                    <template #prefix>¥</template>
                  </n-input-number>
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="投资频率">
                  <n-select v-model:value="dipForm.frequency" :options="frequencyOptions" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="开始日期">
                  <n-date-picker v-model:value="dipStartTime" type="date" />
                </n-form-item>
              </n-gi>
            </n-grid>
            <n-button type="primary" block @click="simulateDIP" :loading="dipLoading">
              开始模拟
            </n-button>
          </div>

          <div v-if="dipResult" class="dip-result">
            <n-grid :cols="4" :x-gap="16">
              <n-gi>
                <n-statistic label="累计投入" :value="dipResult.totalInvested?.toFixed(2)" prefix="¥" />
              </n-gi>
              <n-gi>
                <n-statistic label="当前市值" :value="dipResult.currentValue?.toFixed(2)" prefix="¥" />
              </n-gi>
              <n-gi>
                <n-statistic label="累计收益" :value="dipResult.totalProfit?.toFixed(2)" prefix="¥" />
              </n-gi>
              <n-gi>
                <n-statistic label="收益率" :value="dipResult.totalReturn?.toFixed(2)" suffix="%" />
              </n-gi>
            </n-grid>

            <n-divider>定投记录</n-divider>

            <n-data-table
              :columns="dipColumns"
              :data="dipResult.monthlyRecords"
              :bordered="false"
              size="small"
              max-height="400"
            />
          </div>
        </n-card>
      </n-tab-pane>

      <!-- 综合报告 -->
      <n-tab-pane name="report" tab="综合报告">
        <n-card title="基金综合分析报告" class="analysis-card">
          <div class="analysis-form">
            <n-input-group>
              <n-input-group-label>基金代码</n-input-group-label>
              <n-select
                v-model:value="reportFundCode"
                :options="reportOptions"
                placeholder="选择或搜索基金"
                style="width: 240px"
                filterable
                remote
                :loading="reportSearchLoading"
                @search="handleReportSearch"
                @update:value="onReportSelect"
                clearable
              />
              <n-button type="primary" @click="getReport" :loading="reportLoading">
                生成报告
              </n-button>
            </n-input-group>
          </div>

          <div v-if="reportResult" class="report-result">
            <n-descriptions label-placement="left" :column="2" bordered>
              <n-descriptions-item label="基金代码">{{ reportResult.fundCode }}</n-descriptions-item>
              <n-descriptions-item label="基金名称">{{ reportResult.fundName }}</n-descriptions-item>
              <n-descriptions-item label="基金类型">{{ reportResult.fundType }}</n-descriptions-item>
              <n-descriptions-item label="风险等级">{{ reportResult.riskLevel }}级</n-descriptions-item>
            </n-descriptions>

            <n-divider>收益指标</n-divider>

            <n-grid :cols="4" :x-gap="16">
              <n-gi>
                <n-statistic label="最新净值" :value="reportResult.currentNav?.toFixed(4)" />
              </n-gi>
              <n-gi>
                <n-statistic label="日涨跌" :value="reportResult.dayGrowth?.toFixed(2)" suffix="%" />
              </n-gi>
              <n-gi>
                <n-statistic label="近一月" :value="reportResult.monthGrowth?.toFixed(2)" suffix="%" />
              </n-gi>
              <n-gi>
                <n-statistic label="近一年" :value="reportResult.yearGrowth?.toFixed(2)" suffix="%" />
              </n-gi>
            </n-grid>

            <n-divider>风险指标</n-divider>

            <n-grid :cols="3" :x-gap="16">
              <n-gi>
                <n-statistic label="夏普比率" :value="reportResult.sharpeRatio?.toFixed(4)" />
              </n-gi>
              <n-gi>
                <n-statistic label="最大回撤" :value="reportResult.maxDrawdown?.toFixed(2)" suffix="%" />
              </n-gi>
              <n-gi>
                <n-statistic label="波动率" :value="reportResult.volatility?.toFixed(2)" suffix="%" />
              </n-gi>
            </n-grid>

            <n-divider>评级与建议</n-divider>

            <n-grid :cols="3" :x-gap="16">
              <n-gi>
                <div class="rating-item">
                  <div class="rating-label">收益评级</div>
                  <div class="rating-value">{{ reportResult.performanceRating }}</div>
                </div>
              </n-gi>
              <n-gi>
                <div class="rating-item">
                  <div class="rating-label">风险评级</div>
                  <div class="rating-value">{{ reportResult.riskRating }}</div>
                </div>
              </n-gi>
              <n-gi>
                <div class="rating-item">
                  <div class="rating-label">综合评级</div>
                  <div class="rating-value overall">{{ reportResult.overallRating }}</div>
                </div>
              </n-gi>
            </n-grid>

            <n-alert type="info" title="投资建议" style="margin-top: 16px">
              {{ reportResult.investmentAdvice }}
            </n-alert>
          </div>
        </n-card>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import {
  NCard, NTabs, NTabPane, NInputGroup, NInputGroupLabel, NInput, NInputNumber,
  NSelect, NButton, NGrid, NGi, NStatistic, NDivider, NDescriptions, NDescriptionsItem,
  NDataTable, NAlert, NFormItem, NDatePicker, NIcon, NTag, useMessage
} from 'naive-ui'
import { IconGitCompare as GitCompareOutline } from '@tabler/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import { fundApi, favoriteApi } from '@/api/fund'

const message = useMessage()
const activeTab = ref('risk')

// 周期选项
const periodOptions = [
  { label: '近一周', value: 'week' },
  { label: '近一月', value: 'month' },
  { label: '近三月', value: 'threeMonth' },
  { label: '近六月', value: 'sixMonth' },
  { label: '近一年', value: 'year' },
  { label: '近三年', value: 'threeYear' }
]

const frequencyOptions = [
  { label: '每周', value: 'weekly' },
  { label: '每两周', value: 'biweekly' },
  { label: '每月', value: 'monthly' }
]

// 收藏的基金列表
const favoriteFunds = ref<any[]>([])
const favoriteOptions = ref<Array<{label: string, value: string}>>([])

// 加载收藏的基金
const loadFavorites = async () => {
  try {
    const list = await favoriteApi.getList()
    favoriteFunds.value = list || []
    favoriteOptions.value = (list || []).map((f: any) => ({
      label: `⭐ ${f.fundCode} - ${f.fundName}`,
      value: f.fundCode
    }))
    // 初始化各选项列表
    riskOptions.value = [...favoriteOptions.value]
    correlationOptions.value = [...favoriteOptions.value]
    dipOptions.value = [...favoriteOptions.value]
    reportOptions.value = [...favoriteOptions.value]
  } catch {
    // 忽略错误
  }
}

onMounted(() => {
  loadFavorites()
})

// 基金搜索防抖定时器
let searchTimer: ReturnType<typeof setTimeout> | null = null

// 执行基金搜索（返回选项数组）
const searchFunds = async (keyword: string): Promise<Array<{label: string, value: string}>> => {
  if (!keyword || keyword.length < 1) {
    return []
  }
  try {
    const results = await fundApi.searchByKeyword(keyword, 10)
    const favoriteCodes = new Set(favoriteOptions.value.map(f => f.value))
    const searchResults = (results || [])
      .filter((f: any) => !favoriteCodes.has(f.fundCode))
      .map((f: any) => ({
        label: `${f.fundCode} - ${f.fundName}`,
        value: f.fundCode
      }))
    return searchResults
  } catch {
    return []
  }
}

// 合并收藏和搜索结果的选项
const mergeOptions = (searchResults: Array<{label: string, value: string}>) => {
  return [...favoriteOptions.value, ...searchResults]
}

// 风险分析
const riskForm = ref({ fundCode: '', period: 'year' })
const riskLoading = ref(false)
const riskResult = ref<any>(null)
const riskOptions = ref<Array<{label: string, value: string}>>([])
const riskSearchLoading = ref(false)

const handleRiskSearch = (query: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!query) {
    riskOptions.value = [...favoriteOptions.value]
    return
  }
  riskSearchLoading.value = true
  searchTimer = setTimeout(async () => {
    const results = await searchFunds(query)
    riskOptions.value = mergeOptions(results)
    riskSearchLoading.value = false
  }, 300)
}

const onRiskSelect = (value: string) => {
  riskForm.value.fundCode = value
}

const analyzeRisk = async () => {
  if (!riskForm.value.fundCode) {
    message.warning('请选择基金')
    return
  }
  riskLoading.value = true
  try {
    const [sharpe, drawdown, volatility] = await Promise.all([
      fundApi.getSharpeRatio(riskForm.value.fundCode, riskForm.value.period),
      fundApi.getMaxDrawdown(riskForm.value.fundCode, riskForm.value.period),
      fundApi.getVolatility(riskForm.value.fundCode, riskForm.value.period)
    ])
    riskResult.value = { ...sharpe, ...drawdown, ...volatility }
  } catch (e: any) {
    message.error(e.message || '分析失败')
  } finally {
    riskLoading.value = false
  }
}

// 相关性分析
const correlationCodes = ref('')
const correlationPeriod = ref('year')
const correlationLoading = ref(false)
const correlationResult = ref<any>(null)
const selectedCorrelationFund = ref('')
const correlationOptions = ref<Array<{label: string, value: string}>>([])
const correlationSearchLoading = ref(false)

const handleCorrelationSearch = (query: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  const selectedCodes = new Set(correlationCodes.value.split(',').filter(Boolean))
  const availableFavorites = favoriteOptions.value.filter(f => !selectedCodes.has(f.value))

  if (!query) {
    correlationOptions.value = availableFavorites
    return
  }
  correlationSearchLoading.value = true
  searchTimer = setTimeout(async () => {
    const results = await searchFunds(query)
    const filteredResults = results.filter(r => !selectedCodes.has(r.value))
    correlationOptions.value = [...availableFavorites, ...filteredResults]
    correlationSearchLoading.value = false
  }, 300)
}

const onCorrelationSelect = (value: string) => {
  if (!value) return
  const codes = correlationCodes.value.split(',').map(c => c.trim()).filter(Boolean)
  if (!codes.includes(value)) {
    codes.push(value)
    correlationCodes.value = codes.join(',')
  }
  // 清空选择，准备添加下一个
  setTimeout(() => {
    selectedCorrelationFund.value = ''
    correlationOptions.value = favoriteOptions.value.filter(f => !codes.includes(f.value))
  }, 50)
}

// 从相关性分析列表移除基金
const removeCorrelationFund = (fundCode: string) => {
  const codes = correlationCodes.value.split(',').map(c => c.trim()).filter(c => c && c !== fundCode)
  correlationCodes.value = codes.join(',')
}

// 全选所有收藏的基金
const selectAllCorrelationFunds = () => {
  const allCodes = favoriteOptions.value.map(f => f.value)
  correlationCodes.value = allCodes.join(',')
  // 更新下拉选项（排除已选的）
  correlationOptions.value = []
}

// 清空已选基金
const clearCorrelationFunds = () => {
  correlationCodes.value = ''
  correlationOptions.value = [...favoriteOptions.value]
}

const analyzeCorrelation = async () => {
  const codes = correlationCodes.value.split(',').map(c => c.trim()).filter(Boolean)
  if (codes.length < 2) {
    message.warning('请输入至少2个基金代码')
    return
  }
  correlationLoading.value = true
  try {
    correlationResult.value = await fundApi.getCorrelation({ fundCodes: codes, period: correlationPeriod.value })
  } catch (e: any) {
    message.error(e.message || '分析失败')
  } finally {
    correlationLoading.value = false
  }
}

// 获取相关性颜色（热力图）
const getCorrelationColor = (val: number, isDiagonal: boolean = false) => {
  if (isDiagonal) return 'var(--primary-color)'
  if (val === null || val === undefined) return 'var(--bg-secondary)'

  const intensity = Math.abs(val)
  if (val > 0) {
    // 正相关 - 红色系
    return `rgba(239, 68, 68, ${0.15 + intensity * 0.65})`
  } else if (val < 0) {
    // 负相关 - 绿色系
    return `rgba(34, 197, 94, ${0.15 + intensity * 0.65})`
  }
  return 'var(--bg-secondary)'
}

// 获取相关性等级标签
const getCorrelationLabel = (val: number): string => {
  if (val >= 0.8) return '强正'
  if (val >= 0.5) return '中正'
  if (val >= 0.2) return '弱正'
  if (val > -0.2) return '无'
  if (val > -0.5) return '弱负'
  if (val > -0.8) return '中负'
  return '强负'
}

// 获取鼠标悬停提示
const getCorrelationTooltip = (val: number, i: number, j: number): string => {
  if (i === j) return `${correlationResult.value?.fundNames[i]} 自身相关性为1`
  const label = getCorrelationLabel(val)
  const nameA = correlationResult.value?.fundNames[i] || ''
  const nameB = correlationResult.value?.fundNames[j] || ''
  return `${nameA} vs ${nameB}\n相关性: ${val?.toFixed(4) || '-'} (${label})`
}

// 定投模拟
const dipForm = ref({ fundCode: '', amount: 1000, frequency: 'monthly' })
const dipStartTime = ref(Date.now() - 365 * 24 * 60 * 60 * 1000)
const dipLoading = ref(false)
const dipResult = ref<any>(null)
const dipOptions = ref<Array<{label: string, value: string}>>([])
const dipSearchLoading = ref(false)

const handleDipSearch = (query: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!query) {
    dipOptions.value = [...favoriteOptions.value]
    return
  }
  dipSearchLoading.value = true
  searchTimer = setTimeout(async () => {
    const results = await searchFunds(query)
    dipOptions.value = mergeOptions(results)
    dipSearchLoading.value = false
  }, 300)
}

const onDipSelect = (value: string) => {
  dipForm.value.fundCode = value
}

const dipColumns = [
  { title: '月份', key: 'month' },
  { title: '投入金额', key: 'invested', render: (r: any) => `¥${r.invested}` },
  { title: '买入份额', key: 'shares', render: (r: any) => r.shares?.toFixed(4) },
  { title: '买入净值', key: 'nav', render: (r: any) => r.nav?.toFixed(4) }
]

const simulateDIP = async () => {
  if (!dipForm.value.fundCode) {
    message.warning('请选择基金')
    return
  }
  dipLoading.value = true
  try {
    dipResult.value = await fundApi.simulateDIP({
      fundCode: dipForm.value.fundCode,
      amount: dipForm.value.amount,
      frequency: dipForm.value.frequency,
      startDate: new Date(dipStartTime.value).toISOString().split('T')[0]
    })
  } catch (e: any) {
    message.error(e.message || '模拟失败')
  } finally {
    dipLoading.value = false
  }
}

// 综合报告
const reportFundCode = ref('')
const reportLoading = ref(false)
const reportResult = ref<any>(null)
const reportOptions = ref<Array<{label: string, value: string}>>([])
const reportSearchLoading = ref(false)

const handleReportSearch = (query: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!query) {
    reportOptions.value = [...favoriteOptions.value]
    return
  }
  reportSearchLoading.value = true
  searchTimer = setTimeout(async () => {
    const results = await searchFunds(query)
    reportOptions.value = mergeOptions(results)
    reportSearchLoading.value = false
  }, 300)
}

const onReportSelect = (value: string) => {
  reportFundCode.value = value
}

const getReport = async () => {
  if (!reportFundCode.value) {
    message.warning('请选择基金')
    return
  }
  reportLoading.value = true
  try {
    reportResult.value = await fundApi.getAnalyticsReport(reportFundCode.value)
  } catch (e: any) {
    message.error(e.message || '获取报告失败')
  } finally {
    reportLoading.value = false
  }
}

const getPeriodText = (period: string) => {
  return periodOptions.find(p => p.value === period)?.label || period
}
</script>

<style scoped>
.analytics-page {
  padding: 24px;
}

.analysis-card {
  margin-bottom: 20px;
}

.analysis-form {
  margin-bottom: 24px;
}

.analysis-result {
  padding-top: 16px;
}

.correlation-matrix {
  margin: 20px auto;
}

.correlation-result {
  padding-top: 16px;
}

.correlation-empty {
  padding: 40px 0;
}

.fund-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.fund-item {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.fund-code {
  font-weight: 600;
  font-size: 14px;
  color: var(--primary-color);
}

.fund-name {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.correlation-matrix-wrapper {
  overflow-x: auto;
  margin: 16px 0;
}

.correlation-table {
  border-collapse: collapse;
  margin: 0 auto;
  font-size: 14px;
}

.correlation-table th,
.correlation-table td {
  min-width: 80px;
  height: 50px;
  text-align: center;
  border: 1px solid var(--border-color);
  padding: 8px;
  transition: all 0.2s;
}

.correlation-table td:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 1;
  position: relative;
}

.corner-cell {
  background: transparent;
  border: none !important;
}

.header-cell {
  font-weight: 600;
  background: var(--bg-secondary);
  color: var(--primary-color);
}

.data-cell {
  font-weight: 600;
  cursor: pointer;
}

.diagonal-cell {
  color: white;
  font-weight: 700;
}

.cell-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.correlation-label {
  display: block;
  font-size: 10px;
  font-weight: 400;
  opacity: 0.8;
  margin-top: 2px;
}

.correlation-legend {
  margin-top: 24px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.legend-gradient {
  margin-bottom: 12px;
}

.gradient-bar {
  max-width: 400px;
  margin: 0 auto;
}

.gradient-scale {
  height: 20px;
  border-radius: var(--radius-sm);
  background: linear-gradient(to right, var(--down-color), var(--bg-secondary) 50%, var(--up-color));
}

.gradient-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.legend-tips {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.dip-form {
  margin-bottom: 24px;
}

.dip-result {
  padding-top: 16px;
}

.report-result {
  padding-top: 16px;
}

.rating-item {
  text-align: center;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.rating-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.rating-value {
  font-size: 24px;
  font-weight: 600;
}

.rating-value.overall {
  color: var(--primary-color);
}

.selected-funds {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.selected-funds .label {
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
