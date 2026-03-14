<template>
  <div class="analytics-page page-container">
    <div class="page-header">
      <h1 class="page-title">
        <n-icon size="28"><AnalyticsOutline /></n-icon>
        高级分析
      </h1>
    </div>

    <n-tabs v-model:value="activeTab" type="line" animated>
      <!-- 风险分析 -->
      <n-tab-pane name="risk" tab="风险分析">
        <n-card title="基金风险分析" class="analysis-card">
          <div class="analysis-form">
            <n-input-group>
              <n-input-group-label>基金代码</n-input-group-label>
              <n-auto-complete
                v-model:value="riskForm.fundCode"
                :options="getRiskOptions()"
                :input-props="{ autocomplete: 'disabled' }"
                placeholder="输入代码/名称搜索"
                style="width: 200px"
                @update:value="handleRiskInput"
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
          <div class="analysis-form">
            <n-input-group>
              <n-input-group-label>添加基金</n-input-group-label>
              <n-auto-complete
                v-model:value="selectedCorrelationFund"
                :options="getCorrelationOptions()"
                :input-props="{ autocomplete: 'disabled' }"
                placeholder="输入代码/名称搜索"
                style="width: 200px"
                @update:value="handleCorrelationInput"
                clearable
              />
              <n-input
                v-model:value="correlationCodes"
                placeholder="已选基金代码（逗号分隔）"
                style="width: 300px"
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
                    <th v-for="code in correlationResult.fundCodes" :key="code" class="header-cell">
                      {{ code }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in correlationResult.correlationMatrix" :key="i">
                    <td class="header-cell">{{ correlationResult.fundCodes[i] }}</td>
                    <td
                      v-for="(val, j) in row"
                      :key="j"
                      class="data-cell"
                      :style="{ backgroundColor: getCorrelationColor(val) }"
                    >
                      {{ val?.toFixed(2) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="correlation-legend">
              <span class="legend-title">图例：</span>
              <div class="legend-bar">
                <span class="legend-item" style="background: rgba(239, 68, 68, 0.6)">正相关</span>
                <span class="legend-item" style="background: rgba(156, 163, 175, 0.3)">无相关</span>
                <span class="legend-item" style="background: rgba(34, 197, 94, 0.6)">负相关</span>
              </div>
              <span class="legend-hint">数值范围：-1 到 1，绝对值越大相关性越强</span>
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
                  <n-auto-complete
                    v-model:value="dipForm.fundCode"
                    :options="getDipOptions()"
                    :input-props="{ autocomplete: 'disabled' }"
                    placeholder="输入代码/名称搜索"
                    @update:value="handleDipInput"
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
              <n-auto-complete
                v-model:value="reportFundCode"
                :options="getReportOptions()"
                :input-props="{ autocomplete: 'disabled' }"
                placeholder="输入代码/名称搜索"
                style="width: 200px"
                @update:value="handleReportInput"
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
  NDataTable, NAlert, NFormItem, NDatePicker, NIcon, NAutoComplete, NTag, useMessage
} from 'naive-ui'
import { AnalyticsOutline, StarOutline } from '@vicons/ionicons5'
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
const favoriteOptions = ref<Array<{label: string, value: string, type: 'favorite'}>>([])

// 加载收藏的基金
const loadFavorites = async () => {
  try {
    const list = await favoriteApi.getList()
    favoriteFunds.value = list || []
    favoriteOptions.value = list.map((f: any) => ({
      label: `${f.fundCode} - ${f.fundName}`,
      value: `${f.fundCode} - ${f.fundName}`,
      code: f.fundCode,
      type: 'favorite' as const
    }))
  } catch {
    // 忽略错误
  }
}

onMounted(() => {
  loadFavorites()
})

// 基金搜索防抖定时器
let searchTimer: ReturnType<typeof setTimeout> | null = null

// 搜索结果选项
const searchOptions = ref<Array<{label: string, value: string, code: string, type: 'search'}>>([])

// 执行基金搜索
const searchFunds = async (keyword: string) => {
  if (!keyword || keyword.length < 1) {
    searchOptions.value = []
    return
  }
  try {
    const results = await fundApi.searchByKeyword(keyword, 10)
    searchOptions.value = (results || []).map((f: any) => ({
      label: `${f.fundCode} - ${f.fundName}`,
      value: `${f.fundCode} - ${f.fundName}`,
      code: f.fundCode,
      type: 'search' as const
    }))
  } catch {
    searchOptions.value = []
  }
}

// 从显示值中提取基金代码
const extractFundCode = (value: string): string => {
  if (!value) return ''
  // 移除星标前缀（如果有）
  const cleanValue = value.replace(/^⭐\s*/, '')
  // 格式为 "000001 - 基金名称"，提取代码部分
  const match = cleanValue.match(/^(\d+)/)
  return match ? match[1] : cleanValue.trim()
}

// 生成自动完成的选项（收藏 + 搜索结果）
const getFundOptions = (searchResults: Array<{label: string, value: string, code: string, type: string}>) => {
  const options: any[] = []
  const favoriteCodes = new Set(favoriteOptions.value.map(f => f.code))

  // 添加收藏的基金（带星标前缀）
  favoriteOptions.value.forEach(f => {
    options.push({
      label: `⭐ ${f.label}`,
      value: f.value
    })
  })

  // 添加搜索结果（排除已在收藏中的）
  searchResults.filter(f => !favoriteCodes.has(f.code)).forEach(f => {
    options.push({
      label: f.label,
      value: f.value
    })
  })

  return options
}

// 处理输入变化（带防抖）
const handleFundInput = (value: string, callback: (results: any[]) => void) => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(async () => {
    await searchFunds(value)
    callback(searchOptions.value)
  }, 300)
}

// 风险分析
const riskForm = ref({ fundCode: '', period: 'year' })
const riskLoading = ref(false)
const riskResult = ref<any>(null)
const riskSearchOptions = ref<any[]>([])

const handleRiskInput = (value: string) => {
  handleFundInput(value, (results) => {
    riskSearchOptions.value = results
  })
}

const getRiskOptions = () => {
  return getFundOptions(riskSearchOptions.value)
}

const analyzeRisk = async () => {
  const fundCode = extractFundCode(riskForm.value.fundCode)
  if (!fundCode) {
    message.warning('请输入基金代码')
    return
  }
  riskLoading.value = true
  try {
    const [sharpe, drawdown, volatility] = await Promise.all([
      fundApi.getSharpeRatio(fundCode, riskForm.value.period),
      fundApi.getMaxDrawdown(fundCode, riskForm.value.period),
      fundApi.getVolatility(fundCode, riskForm.value.period)
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
const correlationSearchOptions = ref<any[]>([])
const selectedCorrelationFund = ref('')

const handleCorrelationInput = (value: string) => {
  // 检查是否选择了某个选项（值匹配选项的完整格式 "000001 - 基金名称"）
  if (value && value.includes(' - ')) {
    const fundCode = extractFundCode(value)
    if (fundCode) {
      const codes = correlationCodes.value.split(',').map(c => c.trim()).filter(Boolean)
      if (!codes.includes(fundCode)) {
        codes.push(fundCode)
        correlationCodes.value = codes.join(',')
      }
      // 延迟清空，避免立即触发搜索
      setTimeout(() => {
        selectedCorrelationFund.value = ''
        correlationSearchOptions.value = []
      }, 50)
      return
    }
  }

  // 执行搜索
  if (value && value.length > 0) {
    handleFundInput(value, (results) => {
      correlationSearchOptions.value = results
    })
  } else {
    correlationSearchOptions.value = []
  }
}

const getCorrelationOptions = () => {
  const options: any[] = []
  const favoriteCodes = new Set(favoriteOptions.value.map(f => f.code))
  const selectedCodes = new Set(correlationCodes.value.split(',').map(c => c.trim()).filter(Boolean))

  // 添加收藏的基金（排除已选的）
  favoriteOptions.value.filter(f => !selectedCodes.has(f.code)).forEach(f => {
    options.push({
      label: `⭐ ${f.label}`,
      value: f.value
    })
  })

  // 添加搜索结果（排除已收藏和已选的）
  correlationSearchOptions.value
    .filter(f => !favoriteCodes.has(f.code) && !selectedCodes.has(f.code))
    .forEach(f => {
      options.push({
        label: f.label,
        value: f.value
      })
    })

  return options
}

// 从相关性分析列表移除基金
const removeCorrelationFund = (fundCode: string) => {
  const codes = correlationCodes.value.split(',').map(c => c.trim()).filter(c => c && c !== fundCode)
  correlationCodes.value = codes.join(',')
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

const getCorrelationColor = (val: number) => {
  if (val === 1) return 'rgba(59, 130, 246, 0.8)'
  const intensity = Math.abs(val)
  if (val > 0) return `rgba(239, 68, 68, ${intensity * 0.6})`
  if (val < 0) return `rgba(34, 197, 94, ${intensity * 0.6})`
  return 'transparent'
}

// 定投模拟
const dipForm = ref({ fundCode: '', amount: 1000, frequency: 'monthly' })
const dipStartTime = ref(Date.now() - 365 * 24 * 60 * 60 * 1000)
const dipLoading = ref(false)
const dipResult = ref<any>(null)
const dipSearchOptions = ref<any[]>([])

const handleDipInput = (value: string) => {
  handleFundInput(value, (results) => {
    dipSearchOptions.value = results
  })
}

const getDipOptions = () => {
  return getFundOptions(dipSearchOptions.value)
}

const dipColumns = [
  { title: '月份', key: 'month' },
  { title: '投入金额', key: 'invested', render: (r: any) => `¥${r.invested}` },
  { title: '买入份额', key: 'shares', render: (r: any) => r.shares?.toFixed(4) },
  { title: '买入净值', key: 'nav', render: (r: any) => r.nav?.toFixed(4) }
]

const simulateDIP = async () => {
  const fundCode = extractFundCode(dipForm.value.fundCode)
  if (!fundCode) {
    message.warning('请输入基金代码')
    return
  }
  dipLoading.value = true
  try {
    dipResult.value = await fundApi.simulateDIP({
      fundCode: fundCode,
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
const reportSearchOptions = ref<any[]>([])

const handleReportInput = (value: string) => {
  handleFundInput(value, (results) => {
    reportSearchOptions.value = results
  })
}

const getReportOptions = () => {
  return getFundOptions(reportSearchOptions.value)
}

const getReport = async () => {
  const fundCode = extractFundCode(reportFundCode.value)
  if (!fundCode) {
    message.warning('请输入基金代码')
    return
  }
  reportLoading.value = true
  try {
    reportResult.value = await fundApi.getAnalyticsReport(fundCode)
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
  border-radius: 8px;
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
  font-size: 13px;
}

.correlation-table th,
.correlation-table td {
  min-width: 60px;
  height: 40px;
  text-align: center;
  border: 1px solid var(--border-color);
  padding: 8px;
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
  font-weight: 500;
}

.correlation-legend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.legend-title {
  font-size: 14px;
  color: var(--text-secondary);
}

.legend-bar {
  display: flex;
  gap: 8px;
}

.legend-item {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
}

.legend-hint {
  font-size: 12px;
  color: var(--text-secondary);
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
  border-radius: 8px;
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
