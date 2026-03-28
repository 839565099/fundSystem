<template>
  <div class="reset-password-page">
    <div class="reset-card card">
      <template v-if="loading">
        <div class="loading-state">
          <n-spin size="large" />
          <p>验证中...</p>
        </div>
      </template>
      <template v-else-if="!validToken && !usingCode">
        <div class="error-state">
          <n-icon size="64" color="#d03050"><IconCircleX /></n-icon>
          <h2>链接无效或已过期</h2>
          <p>请重新获取密码重置链接</p>
          <n-button type="primary" @click="router.push('/forgot-password')">重新获取</n-button>
        </div>
      </template>
      <template v-else>
        <h1 class="reset-title">重置密码</h1>
        <n-form ref="formRef" :model="form" :rules="rules">
          <n-form-item path="newPassword" label="新密码">
            <n-input
              v-model:value="form.newPassword"
              type="password"
              placeholder="请输入新密码（6-20位）"
              show-password-on="click"
            />
          </n-form-item>
          <n-form-item path="confirmPassword" label="确认密码">
            <n-input
              v-model:value="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password-on="click"
            />
          </n-form-item>
          <n-button type="primary" block :loading="submitting" @click="handleReset">
            重置密码
          </n-button>
        </n-form>
        <div class="reset-footer">
          <n-button text type="primary" @click="router.push('/login')">返回登录</n-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NSpin, NIcon, type FormInst, type FormRules, createDiscreteApi } from 'naive-ui'
import { IconCircleX } from '@tabler/icons-vue'
import { authApi } from '../api/auth'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInst>()
const loading = ref(true)
const submitting = ref(false)
const validToken = ref(false)
const usingCode = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const validatePasswordSame = (rule: any, value: string) => {
  return value === form.newPassword
}

const rules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validatePasswordSame, message: '两次输入的密码不一致', trigger: 'blur' }
  ],
}

onMounted(async () => {
  const token = route.query.token as string
  const email = route.query.email as string
  const code = route.query.code as string

  if (token) {
    try {
      validToken.value = await authApi.validateResetToken(token)
    } catch {
      validToken.value = false
    }
  } else if (email && code) {
    try {
      const valid = await authApi.validateResetCode(email, code)
      if (valid) {
        usingCode.value = true
        validToken.value = true
      }
    } catch {
      validToken.value = false
    }
  }

  loading.value = false
})

const handleReset = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const token = route.query.token as string
    const email = route.query.email as string
    const code = route.query.code as string

    if (token) {
      await authApi.resetPassword({ token, newPassword: form.newPassword })
    } else if (email && code) {
      await authApi.resetPassword({ email, code, newPassword: form.newPassword })
    }

    message.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (e: any) {
    message.error(e.message || '重置失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.reset-password-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.reset-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
}

.reset-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 32px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 40px 20px;
}

.loading-state p,
.error-state p {
  margin-top: 16px;
  color: var(--text-secondary);
}

.error-state h2 {
  margin-top: 16px;
  color: var(--text-primary);
}

.error-state .n-button {
  margin-top: 24px;
}

.reset-footer {
  text-align: center;
  margin-top: 24px;
}
</style>
