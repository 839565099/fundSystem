<template>
  <div class="login-page">
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

    <!-- 右侧登录区域 -->
    <div class="login-section">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-desc">请登录您的账户以继续</p>
        </div>

        <n-form ref="formRef" :model="form" :rules="rules" size="large" label-placement="left" label-width="70">
          <n-form-item path="username" label="用户名">
            <n-input
              v-model:value="form.username"
              placeholder="请输入用户名"
              :input-props="{ autocomplete: 'username', style: 'color: #1e293b' }"
              @keyup.enter="handleLogin"
            />
          </n-form-item>

          <n-form-item path="password" label="密码">
            <n-input
              v-model:value="form.password"
              type="password"
              placeholder="请输入密码"
              show-password-on="click"
              :input-props="{ autocomplete: 'current-password', style: 'color: #1e293b' }"
              @keyup.enter="handleLogin"
            />
          </n-form-item>

          <div class="login-options">
            <n-checkbox v-model:checked="rememberMe">
              记住我
            </n-checkbox>
            <n-button text type="primary" @click="router.push('/forgot-password')">
              忘记密码？
            </n-button>
          </div>

          <n-button
            type="primary"
            block
            size="large"
            :loading="loading"
            :disabled="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </n-button>
        </n-form>

        <div class="divider">
          <span>或</span>
        </div>

        <n-button block size="large" quaternary class="register-btn" @click="router.push('/register')">
          创建新账户
        </n-button>

        <div class="login-footer">
          登录即表示您同意 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NForm,
  NFormItem,
  NInput,
  NButton,
  NCheckbox,
  type FormInst,
  type FormRules,
  useMessage,
} from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const message = useMessage()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInst>()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
}

onMounted(() => {
  const savedUsername = localStorage.getItem('rememberedUsername')
  if (savedUsername) {
    form.username = savedUsername
    rememberMe.value = true
  }
})

const handleLogin = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login(form)

    if (rememberMe.value) {
      localStorage.setItem('rememberedUsername', form.username)
    } else {
      localStorage.removeItem('rememberedUsername')
    }

    message.success('登录成功')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (e: any) {
    message.error(e.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
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

/* 右侧登录区域 */
.login-section {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #ffffff;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

/* 确保所有表单项宽度一致 */
.login-card :deep(.n-form) {
  width: 100%;
}

.login-card :deep(.n-space) {
  width: 100%;
}

.login-card :deep(.n-space .n-space-item) {
  width: 100%;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
}

.login-desc {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
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

.register-btn {
  height: 48px;
  font-size: 15px;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  color: #475569;
}

.register-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: #94a3b8;
}

.login-footer a {
  color: #3b82f6;
  text-decoration: none;
}

.login-footer a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 900px) {
  .brand-section {
    display: none;
  }

  .login-section {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .login-section {
    padding: 24px 16px;
  }

  .login-title {
    font-size: 24px;
  }
}

/* 修复输入框样式 - 使用 !important 覆盖 Naive UI 默认样式 */
.login-card :deep(.n-form-item) {
  width: 100% !important;
}

.login-card :deep(.n-form-item-blank) {
  width: 100% !important;
}

.login-card :deep(.n-form-item-label) {
  color: #334155;
  font-weight: 500;
}

.login-card :deep(.n-input) {
  background-color: #f8fafc !important;
  border: 1.5px solid #e2e8f0 !important;
  border-radius: 8px !important;
}

.login-card :deep(.n-input .n-input-wrapper) {
  background-color: transparent !important;
}

.login-card :deep(.n-input:hover) {
  border-color: #cbd5e1 !important;
  background-color: #ffffff !important;
}

.login-card :deep(.n-input:focus-within) {
  background-color: #ffffff !important;
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

/* 输入框文字颜色 */
.login-card :deep(.n-input .n-input__input-el),
.login-card :deep(.n-input .n-input__textarea-el) {
  color: #0f172a !important;
  background: transparent !important;
  -webkit-text-fill-color: #0f172a !important;
  caret-color: #3b82f6 !important;
}

/* Placeholder 颜色 */
.login-card :deep(.n-input .n-input__input-el::placeholder),
.login-card :deep(.n-input .n-input__textarea-el::placeholder) {
  color: #94a3b8 !important;
  opacity: 1 !important;
}

/* 浏览器自动填充样式覆盖 */
.login-card :deep(.n-input input:-webkit-autofill),
.login-card :deep(.n-input input:-webkit-autofill:hover),
.login-card :deep(.n-input input:-webkit-autofill:focus),
.login-card :deep(.n-input input:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 100px #f8fafc inset !important;
  -webkit-text-fill-color: #0f172a !important;
  transition: background-color 5000s ease-in-out 0s !important;
}

/* 密码框特殊处理 */
.login-card :deep(.n-input--password .n-input__input-el) {
  color: #0f172a !important;
  -webkit-text-fill-color: #0f172a !important;
}

/* 复选框样式 */
.login-card :deep(.n-checkbox__label) {
  color: #64748b;
}
</style>
