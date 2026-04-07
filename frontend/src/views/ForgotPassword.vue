<template>
  <div class="forgot-page">
    <!-- 左侧品牌区域 -->
    <div class="brand-section">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" fill="none">
            <defs>
              <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#ffffff;stop-opacity:1" />
                <stop offset="100%" style="stop-color:#e0f2fe;stop-opacity:1" />
              </linearGradient>
            </defs>
            <path d="M24 4L42 14V34L24 44L6 34V14L24 4Z" fill="url(#logoGradient)" opacity="0.9"/>
            <path d="M24 14L34 20V32L24 38L14 32V20L24 14Z" fill="#ffffff"/>
            <circle cx="24" cy="24" r="4" fill="#C9A96E"/>
          </svg>
        </div>

        <h1 class="brand-title">Fund System</h1>
        <p class="brand-subtitle">智能基金投资分析平台</p>

        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="22,7 13.5,15.5 8.5,10.5 2,17"/>
                <polyline points="16,7 22,7 22,13"/>
              </svg>
            </div>
            <span>实时行情追踪</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M3 9h18M9 21V9"/>
              </svg>
            </div>
            <span>智能数据分析</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </div>
            <span>安全可靠</span>
          </div>
        </div>
      </div>

      <div class="brand-decoration">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
      </div>
    </div>

    <!-- 右侧找回密码区域 -->
    <div class="forgot-section">
      <div class="forgot-card">
        <!-- 步骤1: 输入邮箱 -->
        <template v-if="step === 1">
          <div class="forgot-header">
            <h2 class="forgot-title">找回密码</h2>
            <p class="forgot-desc">请输入您的注册邮箱，我们将发送验证码</p>
          </div>

          <n-form ref="emailFormRef" :model="emailForm" :rules="emailRules" size="large">
            <n-form-item path="email">
              <n-input
                v-model:value="emailForm.email"
                placeholder="请输入注册时使用的邮箱"
                :input-props="{ autocomplete: 'email', style: 'color: var(--text-primary)' }"
              />
            </n-form-item>

            <n-button
              type="primary"
              block
              size="large"
              :loading="sendLoading"
              class="submit-btn"
              @click="handleSendCode"
            >
              发送验证码
            </n-button>
          </n-form>
        </template>

        <!-- 步骤2: 输入验证码 + 设置新密码 -->
        <template v-if="step === 2">
          <div class="forgot-header">
            <h2 class="forgot-title">重置密码</h2>
            <p class="forgot-desc">验证码已发送至 <span class="accent-email">{{ emailForm.email }}</span></p>
          </div>

          <n-form ref="resetFormRef" :model="resetForm" :rules="resetRules" size="large">
            <n-form-item path="code" label="验证码">
              <div class="code-input-wrapper">
                <n-input
                  v-model:value="resetForm.code"
                  placeholder="请输入6位验证码"
                  :maxlength="6"
                  :input-props="{ autocomplete: 'one-time-code', style: 'color: var(--text-primary)' }"
                />
                <n-button
                  :disabled="cooldown > 0"
                  :loading="sendLoading"
                  class="resend-btn"
                  @click="handleSendCode"
                >
                  {{ cooldown > 0 ? `${cooldown}s` : '重新发送' }}
                </n-button>
              </div>
            </n-form-item>

            <n-form-item path="newPassword" label="新密码">
              <n-input
                v-model:value="resetForm.newPassword"
                type="password"
                show-password-on="click"
                placeholder="请输入新密码（6-20位）"
                :input-props="{ autocomplete: 'new-password', style: 'color: var(--text-primary)' }"
              />
            </n-form-item>

            <n-form-item path="confirmPassword" label="确认密码">
              <n-input
                v-model:value="resetForm.confirmPassword"
                type="password"
                show-password-on="click"
                placeholder="请再次输入新密码"
                :input-props="{ autocomplete: 'new-password', style: 'color: var(--text-primary)' }"
              />
            </n-form-item>

            <n-button
              type="primary"
              block
              size="large"
              :loading="resetLoading"
              class="submit-btn"
              @click="handleReset"
            >
              重置密码
            </n-button>
          </n-form>
        </template>

        <!-- 步骤3: 重置成功 -->
        <template v-if="step === 3">
          <div class="success-message">
            <div class="success-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="var(--success-color)" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9 12l2 2 4-4"/>
              </svg>
            </div>
            <p class="success-text">密码重置成功</p>
            <p class="success-hint">请使用新密码登录</p>
            <n-button
              type="primary"
              block
              size="large"
              class="submit-btn"
              style="margin-top: 24px"
              @click="router.push('/login')"
            >
              返回登录
            </n-button>
          </div>
        </template>

        <div v-if="step !== 3" class="divider">
          <span>或</span>
        </div>

        <n-button
          v-if="step !== 3"
          block
          size="large"
          quaternary
          class="back-btn"
          @click="step === 2 ? (step = 1) : router.push('/login')"
        >
          {{ step === 2 ? '返回上一步' : '返回登录' }}
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NForm,
  NFormItem,
  NInput,
  NButton,
  type FormInst,
  type FormRules,
  useMessage,
} from 'naive-ui'
import { authApi } from '../api/auth'

const message = useMessage()
const router = useRouter()

const emailFormRef = ref<FormInst>()
const resetFormRef = ref<FormInst>()
const sendLoading = ref(false)
const resetLoading = ref(false)
const step = ref(1)
const cooldown = ref(0)

let cooldownTimer: ReturnType<typeof setInterval> | null = null

const emailForm = reactive({
  email: '',
})

const resetForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const resetRules: FormRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value) => {
        if (value && value !== resetForm.newPassword) {
          return new Error('两次密码输入不一致')
        }
        return true
      },
      trigger: 'blur',
    },
  ],
}

const startCooldown = () => {
  cooldown.value = 60
  cooldownTimer = setInterval(() => {
    cooldown.value--
    if (cooldown.value <= 0 && cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (step.value === 1) {
    try {
      await emailFormRef.value?.validate()
    } catch {
      return
    }
  }

  sendLoading.value = true
  try {
    await authApi.forgotPassword(emailForm.email, 'code')
    message.success('验证码已发送')
    step.value = 2
    startCooldown()
  } catch (e: any) {
    message.error(e.message || '发送失败')
  } finally {
    sendLoading.value = false
  }
}

const handleReset = async () => {
  try {
    await resetFormRef.value?.validate()
  } catch {
    return
  }

  resetLoading.value = true
  try {
    await authApi.resetPassword({
      email: emailForm.email,
      code: resetForm.code,
      newPassword: resetForm.newPassword,
    })
    message.success('密码重置成功')
    step.value = 3
  } catch (e: any) {
    message.error(e.message || '重置失败')
  } finally {
    resetLoading.value = false
  }
}

onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})
</script>

<style scoped>
.forgot-page {
  min-height: 100vh;
  display: flex;
  background: var(--bg-secondary);
}

/* 左侧品牌区域 */
.brand-section {
  flex: 1;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
  padding: 40px;
}

.brand-logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
}

.brand-logo svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.15));
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 48px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: flex-start;
  max-width: 280px;
  margin: 0 auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 15px;
  font-weight: 500;
}

.feature-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-icon svg {
  width: 20px;
  height: 20px;
  color: white;
}

.brand-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  left: -50px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 60%;
}

/* 右侧找回密码区域 */
.forgot-section {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--card-bg);
}

.forgot-card {
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
}

.forgot-header {
  text-align: center;
  margin-bottom: 32px;
}

.forgot-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.forgot-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
}

.accent-email {
  color: var(--accent-color);
  font-weight: 500;
}

/* 验证码输入行 */
.code-input-wrapper {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-input-wrapper .n-input {
  flex: 1;
}

.resend-btn {
  flex-shrink: 0;
  min-width: 100px;
  border-radius: 8px !important;
}

.submit-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: var(--gradient-accent) !important;
  box-shadow: 0 4px 12px rgba(201, 169, 110, 0.3);
  margin-top: 8px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(201, 169, 110, 0.4);
}

/* 成功提示 */
.success-message {
  text-align: center;
  padding: 24px;
}

.success-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
}

.success-icon svg {
  width: 100%;
  height: 100%;
}

.success-text {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.success-hint {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

.divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
  color: var(--text-tertiary);
  font-size: 14px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.divider span {
  padding: 0 16px;
}

.back-btn {
  height: 48px;
  font-size: 15px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
}

.back-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

/* 响应式 */
@media (max-width: 900px) {
  .brand-section {
    display: none;
  }

  .forgot-section {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .forgot-section {
    padding: 24px 16px;
  }

  .forgot-title {
    font-size: 24px;
  }
}

/* 修复输入框样式 */
.forgot-card :deep(.n-form-item) {
  width: 100% !important;
}

.forgot-card :deep(.n-form-item-blank) {
  width: 100% !important;
}

.forgot-card :deep(.n-input) {
  background-color: var(--bg-secondary) !important;
  border: 1.5px solid var(--border-color) !important;
  border-radius: 8px !important;
}

.forgot-card :deep(.n-input .n-input-wrapper) {
  background-color: transparent !important;
}

.forgot-card :deep(.n-input:hover) {
  border-color: var(--border-primary) !important;
  background-color: var(--card-bg) !important;
}

.forgot-card :deep(.n-input:focus-within) {
  background-color: var(--card-bg) !important;
  border-color: var(--accent-color) !important;
  box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.1) !important;
}

.forgot-card :deep(.n-input .n-input__input-el),
.forgot-card :deep(.n-input .n-input__textarea-el) {
  color: var(--text-primary) !important;
  background: transparent !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  caret-color: var(--accent-color) !important;
}

.forgot-card :deep(.n-input .n-input__input-el::placeholder),
.forgot-card :deep(.n-input .n-input__textarea-el::placeholder) {
  color: var(--text-tertiary) !important;
  opacity: 1 !important;
}

.forgot-card :deep(.n-input input:-webkit-autofill),
.forgot-card :deep(.n-input input:-webkit-autofill:hover),
.forgot-card :deep(.n-input input:-webkit-autofill:focus),
.forgot-card :deep(.n-input input:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 100px var(--bg-secondary) inset !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  transition: background-color 5000s ease-in-out 0s !important;
}
</style>
