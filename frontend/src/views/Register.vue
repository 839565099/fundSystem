<template>
  <div class="register-page">
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

    <!-- 右侧注册区域 -->
    <div class="register-section">
      <div class="register-card">
        <div class="register-header">
          <h2 class="register-title">创建账户</h2>
          <p class="register-desc">注册新账户开始使用</p>
        </div>

        <n-form ref="formRef" :model="form" :rules="rules" size="large">
          <n-form-item path="username">
            <n-input
              v-model:value="form.username"
              placeholder="请输入用户名"
              :input-props="{ autocomplete: 'username', style: 'color: var(--text-primary)' }"
            />
          </n-form-item>

          <n-form-item path="password">
            <n-input
              v-model:value="form.password"
              type="password"
              placeholder="请输入密码"
              show-password-on="click"
              :input-props="{ autocomplete: 'new-password', style: 'color: var(--text-primary)' }"
            />
          </n-form-item>

          <n-form-item path="confirmPassword">
            <n-input
              v-model:value="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password-on="click"
              :input-props="{ autocomplete: 'new-password', style: 'color: var(--text-primary)' }"
            />
          </n-form-item>

          <n-form-item path="nickname">
            <n-input
              v-model:value="form.nickname"
              placeholder="请输入昵称（可选）"
              :input-props="{ style: 'color: var(--text-primary)' }"
            />
          </n-form-item>

          <n-form-item path="email">
            <n-input
              v-model:value="form.email"
              placeholder="请输入邮箱（可选）"
              :input-props="{ autocomplete: 'email', style: 'color: var(--text-primary)' }"
            />
          </n-form-item>

          <n-button
            type="primary"
            block
            size="large"
            :loading="loading"
            :disabled="loading"
            class="register-btn"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </n-button>
        </n-form>

        <div class="divider">
          <span>或</span>
        </div>

        <n-button block size="large" quaternary class="login-btn" @click="router.push('/login')">
          已有账户？立即登录
        </n-button>

        <div class="register-footer">
          注册即表示您同意 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NForm,
  NFormItem,
  NInput,
  NButton,
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

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_rule, value) => value === form.password, message: '两次密码不一致', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
}

const handleRegister = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined,
    })
    message.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    message.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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

/* 右侧注册区域 */
.register-section {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--card-bg);
  overflow-y: auto;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.register-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
}

.register-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: var(--gradient-accent) !important;
  box-shadow: 0 4px 12px rgba(201, 169, 110, 0.3);
  margin-top: 8px;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(201, 169, 110, 0.4);
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

.login-btn {
  height: 48px;
  font-size: 15px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
}

.login-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.register-footer a {
  color: var(--accent-color);
  text-decoration: none;
}

.register-footer a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 900px) {
  .brand-section {
    display: none;
  }

  .register-section {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .register-section {
    padding: 24px 16px;
  }

  .register-title {
    font-size: 24px;
  }
}

/* 修复输入框样式 */
.register-card :deep(.n-form-item) {
  width: 100% !important;
}

.register-card :deep(.n-form-item-blank) {
  width: 100% !important;
}

.register-card :deep(.n-input) {
  background-color: var(--bg-secondary) !important;
  border: 1.5px solid var(--border-color) !important;
  border-radius: 8px !important;
}

.register-card :deep(.n-input .n-input-wrapper) {
  background-color: transparent !important;
}

.register-card :deep(.n-input:hover) {
  border-color: var(--border-primary) !important;
  background-color: var(--card-bg) !important;
}

.register-card :deep(.n-input:focus-within) {
  background-color: var(--card-bg) !important;
  border-color: var(--accent-color) !important;
  box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.1) !important;
}

/* 输入框文字颜色 */
.register-card :deep(.n-input .n-input__input-el),
.register-card :deep(.n-input .n-input__textarea-el) {
  color: var(--text-primary) !important;
  background: transparent !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  caret-color: var(--accent-color) !important;
}

/* Placeholder 颜色 */
.register-card :deep(.n-input .n-input__input-el::placeholder),
.register-card :deep(.n-input .n-input__textarea-el::placeholder) {
  color: var(--text-tertiary) !important;
  opacity: 1 !important;
}

/* 浏览器自动填充样式覆盖 */
.register-card :deep(.n-input input:-webkit-autofill),
.register-card :deep(.n-input input:-webkit-autofill:hover),
.register-card :deep(.n-input input:-webkit-autofill:focus),
.register-card :deep(.n-input input:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 100px var(--bg-secondary) inset !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  transition: background-color 5000s ease-in-out 0s !important;
}

/* 密码框特殊处理 */
.register-card :deep(.n-input--password .n-input__input-el) {
  color: var(--text-primary) !important;
  -webkit-text-fill-color: var(--text-primary) !important;
}
</style>
