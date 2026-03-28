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
            <circle cx="24" cy="24" r="4" fill="#3b82f6"/>
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
        <div class="forgot-header">
          <h2 class="forgot-title">找回密码</h2>
          <p class="forgot-desc">请输入您的注册邮箱</p>
        </div>

        <!-- 成功提示 -->
        <div v-if="sent" class="success-message">
          <div class="success-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="#18a058" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M9 12l2 2 4-4"/>
            </svg>
          </div>
          <p class="success-text">邮件已发送</p>
          <p class="success-email">{{ form.email }}</p>
          <p class="success-hint">请查收邮件并按照提示重置密码，邮件有效期30分钟</p>
        </div>

        <!-- 表单 -->
        <n-form v-show="!sent" ref="formRef" :model="form" :rules="rules" size="large">
          <n-form-item path="email">
            <n-input
              v-model:value="form.email"
              placeholder="请输入注册时使用的邮箱"
              :input-props="{ autocomplete: 'email', style: 'color: #1e293b' }"
            />
          </n-form-item>

          <n-form-item path="type">
            <n-radio-group v-model:value="form.type" class="reset-type-group">
              <n-radio-button value="link">发送重置链接</n-radio-button>
              <n-radio-button value="code">发送验证码</n-radio-button>
            </n-radio-group>
          </n-form-item>

          <n-button
            type="primary"
            block
            size="large"
            :loading="loading"
            :disabled="sent"
            class="submit-btn"
            @click="handleSend"
          >
            {{ sent ? '已发送' : '发送邮件' }}
          </n-button>
        </n-form>

        <div class="divider">
          <span>或</span>
        </div>

        <n-button block size="large" quaternary class="back-btn" @click="router.push('/login')">
          返回登录
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  NForm,
  NFormItem,
  NInput,
  NButton,
  NRadioGroup,
  NRadioButton,
  type FormInst,
  type FormRules,
  useMessage,
} from 'naive-ui'
import { authApi } from '../api/auth'

const message = useMessage()
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
.forgot-page {
  min-height: 100vh;
  display: flex;
  background: #f1f5f9;
}

/* 左侧品牌区域 */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);
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
  background: #ffffff;
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
  color: #1e293b;
  margin: 0 0 8px;
}

.forgot-desc {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.reset-type-group {
  width: 100%;
}

.reset-type-group :deep(.n-radio-button) {
  flex: 1;
}

.reset-type-group :deep(.n-radio-button__inner) {
  width: 100%;
  text-align: center;
}

.submit-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  margin-top: 8px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
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
  color: #1e293b;
  margin: 0 0 8px;
}

.success-email {
  font-size: 15px;
  color: #3b82f6;
  font-weight: 500;
  margin: 0 0 16px;
}

.success-hint {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}

.divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
  color: #94a3b8;
  font-size: 14px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.divider span {
  padding: 0 16px;
}

.back-btn {
  height: 48px;
  font-size: 15px;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  color: #475569;
}

.back-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
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
  background-color: #f8fafc !important;
  border: 1.5px solid #e2e8f0 !important;
  border-radius: 8px !important;
}

.forgot-card :deep(.n-input .n-input-wrapper) {
  background-color: transparent !important;
}

.forgot-card :deep(.n-input:hover) {
  border-color: #cbd5e1 !important;
  background-color: #ffffff !important;
}

.forgot-card :deep(.n-input:focus-within) {
  background-color: #ffffff !important;
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

.forgot-card :deep(.n-input .n-input__input-el),
.forgot-card :deep(.n-input .n-input__textarea-el) {
  color: #0f172a !important;
  background: transparent !important;
  -webkit-text-fill-color: #0f172a !important;
  caret-color: #3b82f6 !important;
}

.forgot-card :deep(.n-input .n-input__input-el::placeholder),
.forgot-card :deep(.n-input .n-input__textarea-el::placeholder) {
  color: #94a3b8 !important;
  opacity: 1 !important;
}

.forgot-card :deep(.n-input input:-webkit-autofill),
.forgot-card :deep(.n-input input:-webkit-autofill:hover),
.forgot-card :deep(.n-input input:-webkit-autofill:focus),
.forgot-card :deep(.n-input input:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 100px #f8fafc inset !important;
  -webkit-text-fill-color: #0f172a !important;
  transition: background-color 5000s ease-in-out 0s !important;
}
</style>
