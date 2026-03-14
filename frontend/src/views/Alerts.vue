<template>
  <div class="alerts-page page-container">
    <div class="page-header">
      <h1 class="page-title">
        <n-icon size="28"><NotificationsOutline /></n-icon>
        预警管理
      </h1>
      <n-button type="primary" @click="showCreateModal = true">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        创建预警
      </n-button>
    </div>

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
                  <TrendingUpOutline v-if="alert.alertType === 'GROWTH'" />
                  <NewspaperOutline v-else-if="alert.alertType === 'NEWS'" />
                  <NotificationsOutline v-else />
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
            <n-card v-for="rule in rules" :key="rule.id" class="rule-card">
              <div class="rule-info">
                <div class="rule-name">{{ rule.alertName }}</div>
                <div class="rule-detail">
                  <n-tag size="small">{{ getAlertTypeText(rule.alertType) }}</n-tag>
                  <span>{{ getConditionText(rule.alertCondition) }} {{ rule.threshold }}%</span>
                  <span v-if="rule.fundCode">| 基金: {{ rule.fundCode }}</span>
                </div>
              </div>
              <div class="rule-actions">
                <n-switch
                  :value="rule.status === 1"
                  @update:value="toggleRule(rule)"
                />
                <n-button text @click="editRule(rule)">编辑</n-button>
                <n-button text type="error" @click="deleteRule(rule.id)">删除</n-button>
              </div>
            </n-card>
          </div>
          <n-empty v-else description="暂无预警规则">
            <template #extra>
              <n-button type="primary" @click="showCreateModal = true">
                创建第一个预警
              </n-button>
            </template>
          </n-empty>
        </n-spin>
      </n-tab-pane>
    </n-tabs>

    <!-- 创建预警弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="dialog" title="创建预警规则">
      <n-form ref="formRef" :model="ruleForm" label-placement="left" label-width="80">
        <n-form-item label="预警名称" path="alertName">
          <n-input v-model:value="ruleForm.alertName" placeholder="如：涨幅超过5%" />
        </n-form-item>
        <n-form-item label="预警类型" path="alertType">
          <n-select v-model:value="ruleForm.alertType" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="基金代码" path="fundCode">
          <n-input v-model:value="ruleForm.fundCode" placeholder="留空表示大盘预警" />
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
import { ref, computed, onMounted } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NSpin, NEmpty, NModal, NForm, NFormItem,
  NInput, NInputNumber, NSelect, NSwitch, NCheckbox, NTabs, NTabPane, NSpace,
  useMessage
} from 'naive-ui'
import { NotificationsOutline, AddOutline, TrendingUpOutline, NewspaperOutline } from '@vicons/ionicons5'
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
const ruleForm = ref({
  alertName: '',
  alertType: 'GROWTH',
  fundCode: '',
  alertCondition: 'GT',
  threshold: 5
})

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

const editRule = (rule: AlertRuleVO) => {
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
    await fundApi.createAlertRule(ruleForm.value)
    message.success('创建成功')
    showCreateModal.value = false
    ruleForm.value = { alertName: '', alertType: 'GROWTH', fundCode: '', alertCondition: 'GT', threshold: 5 }
    loadRules()
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

const getConditionText = (cond: string) => {
  const map: Record<string, string> = { GT: '大于', LT: '小于', GE: '≥', LE: '≤' }
  return map[cond] || cond
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
  border-radius: 12px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
}

.alert-item:hover {
  background: var(--card-bg-hover);
}

.alert-item.unread {
  border-left: 3px solid var(--primary-color);
  background: rgba(59, 130, 246, 0.05);
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
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

.alert-icon.warning {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

.alert-icon.info {
  background: rgba(59, 130, 246, 0.1);
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
  gap: 12px;
}

.rule-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rule-name {
  font-weight: 600;
  margin-bottom: 4px;
}

.rule-detail {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.rule-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.positive { color: var(--up-color); }
.negative { color: var(--down-color); }
</style>
