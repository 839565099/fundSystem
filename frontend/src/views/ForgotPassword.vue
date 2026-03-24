<template>
  <div class="forgot-password-page">
    <div class="forgot-card card">
      <h1 class="forgot-title">找回密码</h1>
      <n-form ref="formRef" :model="form" :rules="rules">
        <n-form-item path="email" label="邮箱">
          <n-input v-model:value="form.email" placeholder="请输入注册时使用的邮箱" />
        </n-form-item>
        <n-form-item path="type" label="重置方式">
          <n-radio-group v-model:value="form.type">
            <n-radio-button value="link">发送重置链接</n-radio-button>
            <n-radio-button value="code">发送验证码</n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-button type="primary" block :loading="loading" :disabled="sent" @click="handleSend">
          {{ sent ? '已发送' : '发送' }}
        </n-button>
      </n-form>
      <div v-if="sent" class="success-message">
        <n-icon size="48" color="#18a058"><CheckmarkCircleOutline /></n-icon>
        <p>邮件已发送到 <strong>{{ form.email }}</strong></p>
        <p class="hint">请查收邮件并按照提示重置密码，邮件有效期30分钟</p>
      </div>
      <div class="forgot-footer">
        <n-button text type="primary" @click="router.push('/login')">返回登录</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NRadioGroup, NRadioButton, NIcon, type FormInst, type FormRules, createDiscreteApi } from 'naive-ui'
import { CheckmarkCircleOutline } from '@vicons/ionicons5'
import { authApi } from '../api/auth'

const { message } = createDiscreteApi(['message'])

const router = useRouter()

const formRef = ref<FormInst>()
const loading = ref(false)
const sent = ref(false)

const form = reactive({
  email: '',
  type: 'link' as 'link' | 'code',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
}

const handleSend = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authApi.forgotPassword(form.email, form.type)
    sent.value = true
    message.success('邮件发送成功')
  } catch (e: any) {
    message.error(e.message || '发送失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-password-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.forgot-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
}

.forgot-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 32px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.success-message {
  text-align: center;
  margin-top: 32px;
  padding: 24px;
  background: var(--bg-color-secondary);
  border-radius: 8px;
}

.success-message p {
  margin: 12px 0 0;
  color: var(--text-primary);
}

.success-message .hint {
  font-size: 14px;
  color: var(--text-secondary);
}

.forgot-footer {
  text-align: center;
  margin-top: 24px;
}
</style>
