<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="brand-section">
      <canvas ref="particleCanvas" class="particle-canvas"></canvas>
      <div class="glow-orb glow-orb-1"></div>
      <div class="glow-orb glow-orb-2"></div>
      <div class="glow-orb glow-orb-3"></div>

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
    </div>

    <!-- 右侧表单区域 -->
    <div class="login-section">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">邮箱登录</h2>
          <p class="login-desc">使用邮箱验证码快速登录</p>
        </div>

        <!-- 步骤1：输入邮箱 -->
        <template v-if="step === 1">
          <n-form ref="emailFormRef" :model="emailForm" :rules="emailRules" size="large" label-placement="left" label-width="70">
            <n-form-item path="email" label="邮箱">
              <n-input
                v-model:value="emailForm.email"
                placeholder="请输入邮箱地址"
                @keyup.enter="handleSendCode"
              />
            </n-form-item>

            <n-button
              type="primary"
              block
              size="large"
              :loading="sending"
              :disabled="sending"
              class="login-btn"
              @click="handleSendCode"
            >
              {{ sending ? '发送中...' : '发送验证码' }}
            </n-button>
          </n-form>
        </template>

        <!-- 步骤2：输入验证码 -->
        <template v-if="step === 2">
          <div class="email-display">
            <span class="email-label">验证码已发送至</span>
            <span class="email-value">{{ emailForm.email }}</span>
          </div>

          <n-form ref="codeFormRef" :model="codeForm" :rules="codeRules" size="large">
            <n-form-item path="code" label="验证码">
              <n-input
                v-model:value="codeForm.code"
                placeholder="请输入6位验证码"
                :maxlength="6"
                @keyup.enter="handleLogin"
              />
            </n-form-item>

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

          <div class="resend-row">
            <n-button
              text
              :disabled="cooldown > 0"
              @click="handleResend"
            >
              {{ cooldown > 0 ? `${cooldown}秒后可重新发送` : '重新发送验证码' }}
            </n-button>
          </div>
        </template>

        <div class="divider">
          <span>或</span>
        </div>

        <n-button block size="large" quaternary class="back-btn" @click="router.push('/login')">
          返回账号密码登录
        </n-button>

        <div class="login-footer">
          登录即表示您同意 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
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
import { authApi } from '../api/auth'

const message = useMessage()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const emailFormRef = ref<FormInst>()
const codeFormRef = ref<FormInst>()
const step = ref(1)
const sending = ref(false)
const loading = ref(false)
const cooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const emailForm = reactive({ email: '' })
const codeForm = reactive({ code: '' })

// 粒子动画
const particleCanvas = ref<HTMLCanvasElement>()
let animationId: number | null = null

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  radius: number
  alpha: number
  alphaSpeed: number
}

const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const parent = canvas.parentElement!
  let w = parent.clientWidth
  let h = parent.clientHeight
  canvas.width = w
  canvas.height = h

  const particleCount = 60
  const particles: Particle[] = []

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      radius: Math.random() * 2 + 0.5,
      alpha: Math.random() * 0.5 + 0.2,
      alphaSpeed: (Math.random() - 0.5) * 0.005,
    })
  }

  const maxDist = 120

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < maxDist) {
          const lineAlpha = (1 - dist / maxDist) * 0.15
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(201, 169, 110, ${lineAlpha})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      p.alpha += p.alphaSpeed
      if (p.alpha <= 0.1 || p.alpha >= 0.7) p.alphaSpeed *= -1
      p.alpha = Math.max(0.1, Math.min(0.7, p.alpha))
      if (p.x < 0) p.x = w
      if (p.x > w) p.x = 0
      if (p.y < 0) p.y = h
      if (p.y > h) p.y = 0
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(201, 169, 110, ${p.alpha})`
      ctx.fill()
    }
    animationId = requestAnimationFrame(draw)
  }

  draw()

  const handleResize = () => {
    w = parent.clientWidth
    h = parent.clientHeight
    canvas.width = w
    canvas.height = h
  }
  window.addEventListener('resize', handleResize)

  return () => {
    window.removeEventListener('resize', handleResize)
  }
}

const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

const codeRules: FormRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
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
  try {
    await emailFormRef.value?.validate()
  } catch {
    return
  }

  sending.value = true
  try {
    await authApi.sendEmailLoginCode(emailForm.email)
    message.success('验证码已发送，请查收邮箱')
    step.value = 2
    startCooldown()
  } catch (e: any) {
    message.error(e.message || '发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

const handleResend = async () => {
  try {
    await authApi.sendEmailLoginCode(emailForm.email)
    message.success('验证码已重新发送')
    startCooldown()
  } catch (e: any) {
    message.error(e.message || '发送失败，请稍后重试')
  }
}

const handleLogin = async () => {
  try {
    await codeFormRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.emailLogin(emailForm.email, codeForm.code)
    message.success('登录成功')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (e: any) {
    message.error(e.message || '登录失败，请检查验证码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initParticles()
})

onUnmounted(() => {
  if (animationId !== null) {
    cancelAnimationFrame(animationId)
  }
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: var(--bg-secondary);
}

.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #0C0C0E 0%, #18181B 40%, #1C1C1F 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  z-index: 0;
  pointer-events: none;
}

.glow-orb-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #C9A96E 0%, transparent 70%);
  top: -80px;
  right: -60px;
  animation: orbFloat1 8s ease-in-out infinite;
}

.glow-orb-2 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, #B08D4E 0%, transparent 70%);
  bottom: -60px;
  left: -40px;
  animation: orbFloat2 10s ease-in-out infinite;
}

.glow-orb-3 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, #D4BA82 0%, transparent 70%);
  top: 50%;
  left: 55%;
  transform: translate(-50%, -50%);
  animation: orbFloat3 12s ease-in-out infinite;
}

@keyframes orbFloat1 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(-30px, 40px); }
  66% { transform: translate(20px, -20px); }
}

@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(40px, -30px); }
  66% { transform: translate(-20px, 20px); }
}

@keyframes orbFloat3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.3); }
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

.login-section {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--card-bg);
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

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.login-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
}

.login-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: var(--gradient-accent) !important;
  box-shadow: 0 4px 12px rgba(201, 169, 110, 0.3);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(201, 169, 110, 0.4);
}

.email-display {
  text-align: center;
  margin-bottom: 24px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 8px;
}

.email-label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.email-value {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.resend-row {
  text-align: center;
  margin-top: 16px;
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

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.login-footer a {
  color: var(--accent-color);
  text-decoration: none;
}

.login-footer a:hover {
  text-decoration: underline;
}

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

.login-card :deep(.n-form-item) {
  width: 100% !important;
}

.login-card :deep(.n-form-item-blank) {
  width: 100% !important;
}

.login-card :deep(.n-form-item-label) {
  color: var(--text-secondary);
  font-weight: 500;
}

.login-card :deep(.n-input) {
  background-color: var(--bg-secondary) !important;
  border: 1.5px solid var(--border-color) !important;
  border-radius: 8px !important;
}

.login-card :deep(.n-input .n-input-wrapper) {
  background-color: transparent !important;
}

.login-card :deep(.n-input:hover) {
  border-color: var(--border-primary) !important;
  background-color: var(--card-bg) !important;
}

.login-card :deep(.n-input:focus-within) {
  background-color: var(--card-bg) !important;
  border-color: var(--accent-color) !important;
  box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.1) !important;
}

.login-card :deep(.n-input .n-input__input-el),
.login-card :deep(.n-input .n-input__textarea-el) {
  color: var(--text-primary) !important;
  background: transparent !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  caret-color: var(--accent-color) !important;
}

.login-card :deep(.n-input .n-input__input-el::placeholder),
.login-card :deep(.n-input .n-input__textarea-el::placeholder) {
  color: var(--text-disabled) !important;
  opacity: 1 !important;
}

.login-card :deep(.n-input input:-webkit-autofill),
.login-card :deep(.n-input input:-webkit-autofill:hover),
.login-card :deep(.n-input input:-webkit-autofill:focus),
.login-card :deep(.n-input input:-webkit-autofill:active) {
  -webkit-box-shadow: 0 0 0 100px var(--bg-secondary) inset !important;
  -webkit-text-fill-color: var(--text-primary) !important;
  transition: background-color 5000s ease-in-out 0s !important;
}
</style>
