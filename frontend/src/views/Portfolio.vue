<template>
  <div class="portfolio-page page-container">
    <div class="page-header">
      <h1 class="page-title">
        <n-icon size="28"><WalletOutline /></n-icon>
        投资组合
      </h1>
      <n-button type="primary" @click="showCreateModal = true">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        创建组合
      </n-button>
    </div>

    <n-spin :show="loading">
      <div v-if="portfolios.length" class="portfolio-grid">
        <n-card
          v-for="portfolio in portfolios"
          :key="portfolio.id"
          class="portfolio-card"
          :class="{ 'is-default': portfolio.isDefault }"
          @click="viewPortfolio(portfolio.id)"
        >
          <template #header>
            <div class="card-header">
              <span class="portfolio-name">{{ portfolio.name }}</span>
              <n-tag v-if="portfolio.isDefault" type="primary" size="small">默认</n-tag>
            </div>
          </template>
          <div class="portfolio-stats">
            <div class="stat">
              <div class="label">总资产</div>
              <div class="value">{{ formatMoney(portfolio.currentValue) }}</div>
            </div>
            <div class="stat">
              <div class="label">总收益</div>
              <div class="value" :class="portfolio.totalReturn >= 0 ? 'positive' : 'negative'">
                {{ portfolio.totalReturn >= 0 ? '+' : '' }}{{ portfolio.totalProfit?.toFixed(2) }}
              </div>
            </div>
            <div class="stat">
              <div class="label">收益率</div>
              <div class="value" :class="portfolio.totalReturn >= 0 ? 'positive' : 'negative'">
                {{ portfolio.totalReturn >= 0 ? '+' : '' }}{{ portfolio.totalReturn?.toFixed(2) }}%
              </div>
            </div>
            <div class="stat">
              <div class="label">基金数</div>
              <div class="value">{{ portfolio.fundCount }}</div>
            </div>
          </div>
          <div class="portfolio-actions" @click.stop>
            <n-button text @click="editPortfolio(portfolio)">编辑</n-button>
            <n-button text @click="setDefault(portfolio.id)" v-if="!portfolio.isDefault">
              设为默认
            </n-button>
            <n-button text type="error" @click="deletePortfolio(portfolio.id)">
              删除
            </n-button>
          </div>
        </n-card>
      </div>
      <n-empty v-else description="暂无投资组合">
        <template #extra>
          <n-button type="primary" @click="showCreateModal = true">
            创建第一个组合
          </n-button>
        </template>
      </n-empty>
    </n-spin>

    <!-- 创建/编辑组合弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="dialog" title="创建投资组合">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入组合名称" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" placeholder="组合描述（选填）" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showCreateModal = false">取消</n-button>
        <n-button type="primary" @click="submitForm" :loading="submitting">确定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NButton, NIcon, NTag, NSpin, NEmpty, NModal, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { WalletOutline, AddOutline } from '@vicons/ionicons5'
import { fundApi } from '@/api/fund'
import type { PortfolioVO } from '@/types'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const portfolios = ref<PortfolioVO[]>([])
const showCreateModal = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formData = ref({ name: '', description: '' })

const loadPortfolios = async () => {
  loading.value = true
  try {
    portfolios.value = await fundApi.getPortfolios()
  } finally {
    loading.value = false
  }
}

const viewPortfolio = (id: number) => {
  router.push(`/portfolio/${id}`)
}

const editPortfolio = (portfolio: PortfolioVO) => {
  editingId.value = portfolio.id
  formData.value = { name: portfolio.name, description: portfolio.description || '' }
  showCreateModal.value = true
}

const setDefault = async (id: number) => {
  await fundApi.setDefaultPortfolio(id)
  message.success('设置成功')
  loadPortfolios()
}

const deletePortfolio = async (id: number) => {
  await fundApi.deletePortfolio(id)
  message.success('删除成功')
  loadPortfolios()
}

const submitForm = async () => {
  submitting.value = true
  try {
    if (editingId.value) {
      await fundApi.updatePortfolio(editingId.value, formData.value)
      message.success('更新成功')
    } else {
      await fundApi.createPortfolio(formData.value)
      message.success('创建成功')
    }
    showCreateModal.value = false
    editingId.value = null
    formData.value = { name: '', description: '' }
    loadPortfolios()
  } finally {
    submitting.value = false
  }
}

const formatMoney = (value: number) => {
  return '¥' + (value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(loadPortfolios)
</script>

<style scoped>
.portfolio-page {
  padding: 24px;
}

.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.portfolio-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.portfolio-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.portfolio-card.is-default {
  border: 2px solid var(--primary-color);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.portfolio-name {
  font-weight: 600;
  font-size: 16px;
}

.portfolio-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.portfolio-stats .stat .label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.portfolio-stats .stat .value {
  font-size: 18px;
  font-weight: 600;
}

.portfolio-actions {
  display: flex;
  gap: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.positive { color: var(--up-color); }
.negative { color: var(--down-color); }
</style>
