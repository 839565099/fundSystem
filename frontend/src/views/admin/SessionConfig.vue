<template>
  <div class="session-config">
    <n-card>
      <template #header>
        <div class="page-header">
          <n-icon size="24"><IconSettings /></n-icon>
          <span>会话时长配置</span>
        </div>
      </template>

      <n-spin :show="loading">
        <div class="config-cards">
          <n-card
            v-for="config in configs"
            :key="config.roleName"
            class="config-card"
            :bordered="true"
            hoverable
          >
            <template #header>
              <div class="card-header">
                <n-tag
                  :type="config.roleName === 'ADMIN' ? 'warning' : 'info'"
                  size="medium"
                  round
                >
                  {{ config.roleName === 'ADMIN' ? '管理员' : '普通用户' }}
                </n-tag>
                <n-switch
                  v-model:value="editData[config.roleName].isEnabled"
                  size="small"
                >
                  <template #checked>已启用</template>
                  <template #unchecked>已禁用</template>
                </n-switch>
              </div>
            </template>

            <n-form label-placement="left" label-width="140px">
              <n-form-item label="最大在线时长（分钟）">
                <n-input-number
                  v-model:value="editData[config.roleName].maxDurationMinutes"
                  :min="1"
                  :max="1440"
                  :step="10"
                  clearable
                  style="width: 200px"
                >
                  <template #suffix>分钟</template>
                </n-input-number>
              </n-form-item>
              <n-form-item label="到期提醒时间（分钟）">
                <n-input-number
                  v-model:value="editData[config.roleName].warningMinutes"
                  :min="1"
                  :max="editData[config.roleName].maxDurationMinutes"
                  :step="5"
                  clearable
                  style="width: 200px"
                >
                  <template #suffix>分钟</template>
                </n-input-number>
              </n-form-item>
            </n-form>

            <template #action>
              <div class="card-footer">
                <n-button
                  type="primary"
                  :loading="saving[config.roleName]"
                  @click="handleSave(config.roleName)"
                >
                  <template #icon>
                    <n-icon><IconDeviceFloppy /></n-icon>
                  </template>
                  保存配置
                </n-button>
              </div>
            </template>
          </n-card>
        </div>

        <n-empty v-if="!loading && configs.length === 0" description="暂无会话配置数据" />
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  NCard,
  NForm,
  NFormItem,
  NInputNumber,
  NButton,
  NIcon,
  NTag,
  NSwitch,
  NSpin,
  NEmpty,
  useMessage,
} from 'naive-ui'
import {
  IconSettings,
  IconClock,
  IconDeviceFloppy,
} from '@tabler/icons-vue'
import { sessionApi, type SessionConfig, type SessionConfigUpdateDTO } from '@/api/session'

const message = useMessage()

const loading = ref(false)
const configs = ref<SessionConfig[]>([])
const saving = reactive<Record<string, boolean>>({})

// 每个角色的可编辑数据
const editData = reactive<Record<string, SessionConfigUpdateDTO>>({})

onMounted(() => {
  loadConfigs()
})

const loadConfigs = async () => {
  loading.value = true
  try {
    configs.value = await sessionApi.getSessionConfigs()
    // 初始化每个角色的编辑数据
    for (const config of configs.value) {
      editData[config.roleName] = {
        maxDurationMinutes: config.maxDurationMinutes,
        warningMinutes: config.warningMinutes,
        isEnabled: config.isEnabled,
      }
      saving[config.roleName] = false
    }
  } catch (e) {
    message.error('加载会话配置失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async (roleName: string) => {
  const data = editData[roleName]
  if (!data) return

  // 校验
  if (!data.maxDurationMinutes || data.maxDurationMinutes < 1) {
    message.warning('最大在线时长不能小于 1 分钟')
    return
  }
  if (!data.warningMinutes || data.warningMinutes < 1) {
    message.warning('到期提醒时间不能小于 1 分钟')
    return
  }
  if (data.warningMinutes > data.maxDurationMinutes) {
    message.warning('到期提醒时间不能大于最大在线时长')
    return
  }

  saving[roleName] = true
  try {
    await sessionApi.updateSessionConfig(roleName, data)
    message.success(`${roleName === 'ADMIN' ? '管理员' : '普通用户'}会话配置已保存`)
  } catch (e) {
    message.error('保存配置失败')
  } finally {
    saving[roleName] = false
  }
}
</script>

<style scoped>
.session-config {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(460px, 1fr));
  gap: 20px;
}

.config-card {
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
