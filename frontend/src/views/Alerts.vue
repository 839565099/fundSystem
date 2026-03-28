<template>
  <div class="alerts-page page-container">
    <PageHeader title="预警管理" icon="🔔">
      <template #actions>
        <n-button type="primary" @click="openCreateModal">
          <template #icon>
            <n-icon><IconPlus /></n-icon>
          </template>
          创建预警
        </n-button>
      </template>
    </PageHeader>

    <n-tabs v-model:value="activeTab">
      <n-tab-pane name="history" tab="预警通知">
        <div class="tab-header">
          <n-space>
            <n-button
              :disabled="selectedIds.length === 0"
              @click="markSelectedAsRead"
            >
              标记已读
            </n-button>
            <n-button @click="markAllAsRead">全部已读</n-button>
          </n-space>
        </div>
        <n-spin :show="loading">
          <div v-if="alerts.length" class="alert-list">
            <div
              v-for="alert in alerts"
              :key="alert.id"
              class="alert-item"
              :class="{ unread: !alert.isRead }"
              @click="viewAlert(alert)"
            >
              <n-checkbox
                v-model:checked="alert._selected"
                @click.stop
                class="alert-checkbox"
              />
              <div class="alert-icon" :class="getAlertType(alert.alertType)">
                <n-icon size="20">
                  <IconTrendingUp v-if="alert.alertType === 'GROWTH'" />
                  <IconNews v-else-if="alert.alertType === 'NEWS'" />
                  <IconBell v-else />
                </n-icon>
              </div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.alertTitle }}</div>
                <div class="alert-message">{{ alert.alertMessage }}</div>
                <div class="alert-meta">
                  <span v-if="alert.fundCode">{{ alert.fundCode }}</span>
                  <span>{{ formatTime(alert.triggeredTime) }}</span>
                </div>
              </div>
              <div class="alert-value" :class="getValueClass(alert)">
                {{ alert.alertValue >= 0 ? '+' : '' }}{{ alert.alertValue?.toFixed(2) }}%
              </div>
            </div>
          </div>
          <n-empty v-else description="暂无预警通知" />
        </n-spin>
      </n-tab-pane>
      <n-tab-pane name="rules" tab="预警规则">
        <n-spin :show="rulesLoading">
          <div v-if="rules.length" class="rule-list">
            <div
              v-for="rule in rules"
              :key="rule.id"
              class="rule-card"
              :class="{ 'rule-disabled': rule.status !== 1 }"
            >
              <div class="rule-status-bar"></div>
              <div class="rule-content">
                <div class="rule-header">
                  <div class="rule-title-row">
                    <div class="rule-icon" :class="getAlertTypeClass(rule.alertType)">
                      <n-icon size="18">
                        <IconTrendingUp v-if="rule.alertType === 'GROWTH'" />
                        <IconNews v-else-if="rule.alertType === 'NEWS'" />
                        <IconChartBar v-else />
                      </n-icon>
                    </div>
                    <span class="rule-name">{{ rule.alertName }}</span>
                    <n-tag size="small" :type="getAlertTagType(rule.alertType)" :bordered="false">
                      {{ getAlertTypeText(rule.alertType) }}
                    </n-tag>
                  </div>
                  <n-switch
                    :value="rule.status === 1"
                    size="small"
                    @update:value="toggleRule(rule)"
                  />
                </div>

                <div class="rule-body">
                  <div class="rule-condition">
                    <span class="condition-label">触发条件</span>
                    <span class="condition-value">
                      <template v-if="rule.alertCondition === 'GT'">涨幅 &gt;</template>
                      <template v-else-if="rule.alertCondition === 'LT'">跌幅 &lt;</template>
                      <template v-else-if="rule.alertCondition === 'GE'">涨幅 ≥</template>
                      <template v-else-if="rule.alertCondition === 'LE'">跌幅 ≤</template>
                      <span class="threshold">{{ rule.threshold }}%</span>
                    </span>
                  </div>
                  <div class="rule-fund" v-if="rule.fundCode">
                    <span class="fund-label">监控基金</span>
                    <span class="fund-value">{{ rule.fundCode }}<span v-if="rule.fundName"> - {{ rule.fundName }}</span></span>
                  </div>
                  <div class="rule-fund" v-else>
                    <span class="fund-label">监控范围</span>
                    <span class="fund-value">全部基金（大盘预警）</span>
                  </div>
                </div>

                <div class="rule-footer">
                  <div class="rule-stats">
                    <span class="stat-item">
                      <n-icon size="14"><IconGauge /></n-icon>
                      触发 {{ rule.triggerCount || 0 }} 次
                    </span>
                    <span class="stat-divider">|</span>
                    <span class="stat-item" v-if="rule.lastTriggeredTime">
                      最后触发: {{ formatTime(rule.lastTriggeredTime) }}
                    </span>
                    <span class="stat-item" v-else>
                      暂未触发
                    </span>
                  </div>
                  <div class="rule-actions">
                    <n-button size="small" quaternary @click="editRule(rule)">
                      <template #icon><n-icon><IconPencil /></n-icon></template>
                      编辑
                    </n-button>
                    <n-button size="small" quaternary type="error" @click="deleteRule(rule.id)">
                      <template #icon><n-icon><IconTrash /></n-icon></template>
                      删除
                    </n-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <n-empty v-else description="暂无预警规则">
            <template #extra>
              <n-button type="primary" @click="openCreateModal">
                创建第一个预警
              </n-button>
            </template>
          </n-empty>
        </n-spin>
      </n-tab-pane>
    </n-tabs>

    <!-- 创建预警弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="dialog" :title="editingId ? '编辑预警规则' : '创建预警规则'">
      <n-form ref="formRef" :model="ruleForm" label-placement="left" label-width="80">
        <n-form-item label="预警名称" path="alertName">
          <n-input v-model:value="ruleForm.alertName" placeholder="如：涨幅超过5%" />
        </n-form-item>
        <n-form-item label="预警类型" path="alertType">
          <n-select v-model:value="ruleForm.alertType" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="基金代码" path="fundCode">
          <n-select
            v-model:value="ruleForm.fundCode"
            filterable
            remote
            clearable
            placeholder="搜索或选择基金（留空表示大盘预警）"
            :loading="fundSearchLoading"
            :options="fundOptions"
            @search="searchFunds"
          />
        </n-form-item>
        <n-form-item label="条件" path="alertCondition">
          <n-select v-model:value="ruleForm.alertCondition" :options="conditionOptions" />
        </n-form-item>
        <n-form-item label="阈值" path="threshold">
          <n-input-number v-model:value="ruleForm.threshold" :min="-100" :max="100" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showCreateModal = false">取消</n-button>
        <n-button type="primary" @click="submitRule" :loading="submitting">确定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  NButton, NIcon, NTag, NSpin, NEmpty, NModal, NForm, NFormItem,
  NInput, NInputNumber, NSelect, NSwitch, NCheckbox, NTabs, NTabPane, NSpace,
  useMessage
} from 'naive-ui'
import { IconBell, IconPlus, IconTrendingUp, IconNews, IconGauge, IconChartBar, IconPencil, IconTrash } from '@tabler/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import { fundApi } from '@/api/fund'
import type { AlertHistoryVO, AlertRuleVO } from '@/types'

const message = useMessage()
const activeTab = ref('history')
const loading = ref(false)
const rulesLoading = ref(false)
const alerts = ref<(AlertHistoryVO & { _selected?: boolean })[]>([])
const rules = ref<AlertRuleVO[]>([])
const showCreateModal = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const ruleForm = ref({
  alertName: '',
  alertType: 'GROWTH',
  fundCode: '' as string | null,
  alertCondition: 'GT',
  threshold: 5
})

// 基金搜索相关
const fundSearchLoading = ref(false)
const fundOptions = ref<{ label: string; value: string }[]>([])

// 搜索基金
const searchFunds = async (keyword: string) => {
  if (!keyword) {
    await loadFavoriteFunds()
    return
  }
  fundSearchLoading.value = true
  try {
    const funds = await fundApi.searchByKeyword(keyword, 20)
    fundOptions.value = funds.map((f: any) => ({
      label: `${f.fundCode} - ${f.fundName}`,
      value: f.fundCode
    }))
  } finally {
    fundSearchLoading.value = false
  }
}

// 加载收藏的基金作为默认选项
const loadFavoriteFunds = async () => {
  try {
    const favorites = await fundApi.getFavorites()
    fundOptions.value = favorites.map((f: any) => ({
      label: `${f.fundCode} - ${f.fundName || '未知'}`,
      value: f.fundCode
    }))
  } catch {
    fundOptions.value = []
  }
}

const typeOptions = [
  { label: '涨跌幅预警', value: 'GROWTH' },
  { label: '资讯预警', value: 'NEWS' },
  { label: '大盘预警', value: 'MARKET' }
]

const conditionOptions = [
  { label: '大于', value: 'GT' },
  { label: '小于', value: 'LT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于等于', value: 'LE' }
]

const selectedIds = computed(() => alerts.value.filter(a => a._selected).map(a => a.id))

const loadAlerts = async () => {
  loading.value = true
  try {
    alerts.value = await fundApi.getAlertHistory()
  } finally {
    loading.value = false
  }
}

const loadRules = async () => {
  rulesLoading.value = true
  try {
    rules.value = await fundApi.getAlertRules()
  } finally {
    rulesLoading.value = false
  }
}

const viewAlert = (alert: AlertHistoryVO) => {
  if (!alert.isRead) {
    fundApi.markAlertsAsRead([alert.id])
    alert.isRead = 1
  }
  if (alert.fundCode) {
    window.open(`/fund/${alert.fundCode}`, '_blank')
  }
}

const markSelectedAsRead = async () => {
  await fundApi.markAlertsAsRead(selectedIds.value)
  alerts.value.forEach(a => {
    if (a._selected) a.isRead = 1
  })
  message.success('已标记')
}

const markAllAsRead = async () => {
  await fundApi.markAllAlertsAsRead()
  alerts.value.forEach(a => a.isRead = 1)
  message.success('已标记')
}

const toggleRule = async (rule: AlertRuleVO) => {
  const newStatus = rule.status === 1 ? 0 : 1
  await fundApi.toggleAlertRule(rule.id, newStatus)
  rule.status = newStatus
  message.success('已更新')
}

const openCreateModal = () => {
  editingId.value = null
  ruleForm.value = { alertName: '', alertType: 'GROWTH', fundCode: '', alertCondition: 'GT', threshold: 5 }
  showCreateModal.value = true
}

const editRule = (rule: AlertRuleVO) => {
  editingId.value = rule.id
  ruleForm.value = { ...rule, threshold: Number(rule.threshold) } as any
  showCreateModal.value = true
}

const deleteRule = async (id: number) => {
  await fundApi.deleteAlertRule(id)
  message.success('删除成功')
  loadRules()
}

const submitRule = async () => {
  submitting.value = true
  try {
    const data = {
      ...ruleForm.value,
      fundCode: ruleForm.value.fundCode || ''
    }
    if (editingId.value) {
      await fundApi.updateAlertRule(editingId.value, data)
      message.success('更新成功')
    } else {
      await fundApi.createAlertRule(data)
      message.success('创建成功')
    }
    showCreateModal.value = false
    editingId.value = null
    ruleForm.value = { alertName: '', alertType: 'GROWTH', fundCode: '', alertCondition: 'GT', threshold: 5 }
    loadRules()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const getAlertType = (type: string) => {
  switch (type) {
    case 'GROWTH': return 'danger'
    case 'NEWS': return 'warning'
    default: return 'info'
  }
}

const getAlertTypeText = (type: string) => {
  const map: Record<string, string> = { GROWTH: '涨跌幅', NEWS: '资讯', MARKET: '大盘' }
  return map[type] || type
}

const getAlertTypeClass = (type: string) => {
  const map: Record<string, string> = { GROWTH: 'growth', NEWS: 'news', MARKET: 'market' }
  return map[type] || 'default'
}

const getAlertTagType = (type: string): 'default' | 'success' | 'warning' | 'error' | 'info' | 'primary' => {
  const map: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info' | 'primary'> = {
    GROWTH: 'error',
    NEWS: 'warning',
    MARKET: 'info'
  }
  return map[type] || 'default'
}

const getValueClass = (alert: AlertHistoryVO) => alert.alertValue >= 0 ? 'positive' : 'negative'

const formatTime = (time: string) => {
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return `${Math.floor(hours / 24)}天前`
}

onMounted(() => {
  loadAlerts()
  loadRules()
  loadFavoriteFunds()
})

// 弹窗打开时加载收藏基金
watch(showCreateModal, (val) => {
  if (val) {
    loadFavoriteFunds()
  }
})
</script>

<style scoped>
.alerts-page {
  padding: 24px;
}

.tab-header {
  margin-bottom: 16px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
}

.alert-item:hover {
  background: var(--card-bg-hover);
}

.alert-item.unread {
  border-left: 3px solid var(--primary-color);
  background: rgba(79, 70, 229, 0.05);
}

.alert-checkbox {
  flex-shrink: 0;
}

.alert-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alert-icon.danger {
  background: rgba(220, 38, 38, 0.1);
  color: var(--danger-color);
}

.alert-icon.warning {
  background: rgba(217, 119, 6, 0.1);
  color: var(--warning-color);
}

.alert-icon.info {
  background: rgba(79, 70, 229, 0.1);
  color: var(--primary-color);
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.alert-message {
  font-size: 14px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-meta {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
  display: flex;
  gap: 12px;
}

.alert-value {
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rule-card {
  display: flex;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
  transition: all 0.3s ease;
}

.rule-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rule-card.rule-disabled {
  opacity: 0.7;
}

.rule-card.rule-disabled .rule-status-bar {
  background: var(--text-tertiary);
}

.rule-status-bar {
  width: 4px;
  background: var(--success-color);
  flex-shrink: 0;
}

.rule-content {
  flex: 1;
  padding: 16px 20px;
  min-width: 0;
}

.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rule-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.rule-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rule-icon.growth {
  background: rgba(220, 38, 38, 0.1);
  color: var(--danger-color);
}

.rule-icon.news {
  background: rgba(217, 119, 6, 0.1);
  color: var(--warning-color);
}

.rule-icon.market {
  background: rgba(79, 70, 229, 0.1);
  color: var(--primary-color);
}

.rule-name {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rule-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.rule-condition {
  display: flex;
  align-items: center;
  gap: 8px;
}

.condition-label,
.fund-label {
  font-size: 13px;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.condition-value,
.fund-value {
  font-size: 14px;
  color: var(--text-primary);
}

.condition-value .threshold {
  font-weight: 600;
  color: var(--danger-color);
  margin-left: 2px;
}

.rule-fund {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fund-value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rule-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.rule-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-divider {
  color: var(--border-color);
}

.rule-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.positive { color: var(--up-color); }
.negative { color: var(--down-color); }
</style>
