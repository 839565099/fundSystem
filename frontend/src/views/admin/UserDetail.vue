<template>
  <div class="user-detail">
    <n-card>
      <template #header>
        <div class="card-header">
          <n-button text @click="router.back()">
            <template #icon>
              <n-icon><IconArrowLeft /></n-icon>
            </template>
            返回
          </n-button>
          <span class="title">用户详情</span>
        </div>
      </template>

      <n-spin :show="loading">
        <div v-if="user" class="user-info">
          <!-- 基本信息 -->
          <div class="info-section">
            <h3 class="section-title">基本信息</h3>
            <n-descriptions :column="2" label-placement="left" bordered>
              <n-descriptions-item label="用户ID">
                {{ user.id }}
              </n-descriptions-item>
              <n-descriptions-item label="用户名">
                {{ user.username }}
              </n-descriptions-item>
              <n-descriptions-item label="昵称">
                {{ user.nickname || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="邮箱">
                {{ user.email || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="手机">
                {{ user.phone || '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="头像">
                <n-avatar v-if="user.avatar" :src="user.avatar" size="small" />
                <span v-else>-</span>
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <!-- 状态信息 -->
          <div class="info-section">
            <h3 class="section-title">状态信息</h3>
            <n-descriptions :column="2" label-placement="left" bordered>
              <n-descriptions-item label="角色">
                <n-tag :type="user.role === 'ADMIN' ? 'warning' : 'default'" size="small">
                  {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="状态">
                <n-tag :type="user.status === 1 ? 'success' : 'error'" size="small">
                  {{ user.status === 1 ? '正常' : '禁用' }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="注册时间">
                {{ user.createTime ? new Date(user.createTime).toLocaleString('zh-CN') : '-' }}
              </n-descriptions-item>
              <n-descriptions-item label="最后登录">
                {{ user.lastLoginTime ? new Date(user.lastLoginTime).toLocaleString('zh-CN') : '-' }}
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <n-space>
              <n-button
                :type="user.status === 1 ? 'warning' : 'success'"
                @click="handleToggleStatus"
                :loading="statusLoading"
              >
                {{ user.status === 1 ? '禁用用户' : '启用用户' }}
              </n-button>
              <n-button
                :type="user.role === 'ADMIN' ? 'default' : 'warning'"
                @click="handleToggleRole"
                :loading="roleLoading"
              >
                {{ user.role === 'ADMIN' ? '降为普通用户' : '升为管理员' }}
              </n-button>
            </n-space>
          </div>
        </div>

        <n-empty v-else-if="!loading" description="用户不存在" />
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NCard,
  NSpin,
  NButton,
  NIcon,
  NTag,
  NSpace,
  NDescriptions,
  NDescriptionsItem,
  NAvatar,
  NEmpty,
  useMessage,
  useDialog,
} from 'naive-ui'
import { IconArrowLeft } from '@tabler/icons-vue'
import { adminApi } from '@/api/admin'
import type { UserVO } from '@/types'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const statusLoading = ref(false)
const roleLoading = ref(false)
const user = ref<UserVO | null>(null)

const userId = computed(() => Number(route.params.id))

const loadUser = async () => {
  loading.value = true
  try {
    user.value = await adminApi.getUser(userId.value)
  } catch (e) {
    message.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

const handleToggleStatus = () => {
  if (!user.value) return

  const newStatus = user.value.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'

  dialog.warning({
    title: '确认操作',
    content: `确定要${action}用户 "${user.value.username}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      statusLoading.value = true
      try {
        await adminApi.updateUserStatus(user.value!.id, newStatus)
        message.success(`用户已${action}`)
        user.value!.status = newStatus
      } catch (e: any) {
        message.error(e.response?.data?.message || `${action}失败`)
      } finally {
        statusLoading.value = false
      }
    },
  })
}

const handleToggleRole = () => {
  if (!user.value) return

  const newRole = user.value.role === 'ADMIN' ? 'USER' : 'ADMIN'
  const action = newRole === 'ADMIN' ? '提升为管理员' : '降为普通用户'

  dialog.warning({
    title: '确认操作',
    content: `确定要将用户 "${user.value.username}" ${action}吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      roleLoading.value = true
      try {
        await adminApi.updateUserRole(user.value!.id, newRole)
        message.success('角色更新成功')
        user.value!.role = newRole
      } catch (e: any) {
        message.error(e.response?.data?.message || '角色更新失败')
      } finally {
        roleLoading.value = false
      }
    },
  })
}

onMounted(() => {
  loadUser()
})
</script>

<script lang="ts">
import { computed, defineComponent } from 'vue'
export default defineComponent({
  name: 'AdminUserDetail'
})
</script>

<style scoped>
.user-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.action-section {
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}
</style>
