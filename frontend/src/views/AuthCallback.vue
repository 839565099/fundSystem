<template>
  <div class="callback-page">
    <div class="callback-card">
      <template v-if="loading">
        <n-spin size="large" />
        <p class="callback-text">{{ message }}</p>
      </template>
      <template v-else-if="hasError">
        <div class="error-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
        </div>
        <p class="callback-text error-text">{{ message }}</p>
        <n-button type="primary" @click="router.push('/login')">返回登录</n-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NSpin, NButton, useMessage } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const authStore = useAuthStore()

const loading = ref(true)
const hasError = ref(false)
const messageText = ref('正在处理登录...')

const errorMessages: Record<string, string> = {
  access_denied: '您拒绝了 Google 授权',
  invalid_state: '授权验证失败，请重试',
  google_api_error: 'Google 服务异常，请稍后重试',
  account_disabled: '该账号已被禁用',
  not_configured: 'Google 登录暂未配置',
}

onMounted(async () => {
  const token = route.query.token as string
  const errorCode = route.query.error as string

  if (errorCode) {
    hasError.value = true
    loading.value = false
    messageText.value = errorMessages[errorCode] || '登录失败，请重试'
    return
  }

  if (!token) {
    hasError.value = true
    loading.value = false
    messageText.value = '授权回调参数异常'
    return
  }

  try {
    localStorage.setItem('token', token)
    authStore.token = token

    messageText.value = '正在获取用户信息...'
    await authStore.fetchProfile()

    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e: any) {
    hasError.value = true
    loading.value = false
    messageText.value = '登录失败：' + (e.message || '未知错误')
  }
})
</script>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
}

.callback-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.callback-text {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
}

.error-text {
  color: #e74c3c;
}

.error-icon {
  width: 48px;
  height: 48px;
  color: #e74c3c;
}

.error-icon svg {
  width: 100%;
  height: 100%;
}
</style>
