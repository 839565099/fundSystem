<template>
  <div class="login-page">
    <!-- Left: Brand showcase -->
    <div class="brand-panel">
      <canvas ref="particleCanvas" class="particle-canvas"></canvas>
      <div class="brand-grain"></div>

      <!-- Ambient gold orbs -->
      <div class="orb orb--1"></div>
      <div class="orb orb--2"></div>
      <div class="orb orb--3"></div>

      <div class="brand-content">
        <div class="brand-emblem">
          <div class="emblem-ring"></div>
          <div class="emblem-core">
            <span class="emblem-text">FT</span>
          </div>
        </div>

        <h1 class="brand-title">
          <span class="brand-title__line">Fund</span>
          <span class="brand-title__line brand-title__line--gold">Terminal</span>
        </h1>
        <p class="brand-subtitle">专业基金投资分析平台</p>

        <div class="brand-features">
          <div class="feature" v-for="(feat, i) in features" :key="i" :style="{ animationDelay: `${i * 120}ms` }">
            <div class="feature__icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path :d="feat.icon" />
              </svg>
            </div>
            <div class="feature__text">
              <span class="feature__name">{{ feat.name }}</span>
              <span class="feature__desc">{{ feat.desc }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Login form -->
    <div class="login-panel">
      <div class="login-card">
        <!-- Header -->
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-desc">登录您的账户继续</p>
        </div>

        <!-- Form -->
        <n-form ref="formRef" :model="form" :rules="rules" size="large" label-placement="left" label-width="0">
          <div class="form-field">
            <label class="form-label">用户名</label>
            <n-form-item path="username" :show-label="false">
              <n-input
                v-model:value="form.username"
                placeholder="请输入用户名"
                :input-props="{ autocomplete: 'username' }"
                @keyup.enter="handleLogin"
              />
            </n-form-item>
          </div>

          <div class="form-field">
            <label class="form-label">密码</label>
            <n-form-item path="password" :show-label="false">
              <n-input
                v-model:value="form.password"
                type="password"
                placeholder="请输入密码"
                show-password-on="click"
                :input-props="{ autocomplete: 'current-password' }"
                @keyup.enter="handleLogin"
              />
            </n-form-item>
          </div>

          <div class="form-row">
            <n-checkbox v-model:checked="rememberMe">记住我</n-checkbox>
            <n-button text type="primary" @click="router.push('/forgot-password')">忘记密码？</n-button>
          </div>

          <n-button
            type="primary"
            block
            size="large"
            :loading="loading"
            :disabled="loading"
            class="login-btn btn-gold"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </n-button>
        </n-form>

        <!-- Dividers & alt logins -->
        <div class="divider"><span>其他登录方式</span></div>

        <div class="alt-logins">
          <button class="alt-btn" @click="handleGoogleLogin">
            <svg class="alt-btn__icon" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
            <span>Google 登录</span>
          </button>
          <button class="alt-btn" @click="router.push('/email-login')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="4" width="20" height="16" rx="2"/>
              <path d="M22 7l-10 7L2 7"/>
            </svg>
            <span>邮箱验证码</span>
          </button>
        </div>

        <div class="login-footer">
          还没有账户？ <n-button text type="primary" @click="router.push('/register')">创建账户</n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, NCheckbox, type FormInst, type FormRules, useMessage } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const message = useMessage()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInst>()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({ username: '', password: '' })

const features = [
  { name: '实时行情', desc: '毫秒级数据更新', icon: 'M22 7l-10 14-4-6' },
  { name: '智能分析', desc: 'AI驱动投资决策', icon: 'M3 3v18h18M7 16l4-4 4 4 5-6' },
  { name: '安全可靠', desc: '多重安全保障', icon: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z' },
]

const particleCanvas = ref<HTMLCanvasElement>()
let animationId: number | null = null

interface Particle {
  x: number; y: number; vx: number; vy: number;
  radius: number; alpha: number; alphaSpeed: number;
}

const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const parent = canvas.parentElement!
  let w = parent.clientWidth, h = parent.clientHeight
  canvas.width = w; canvas.height = h

  const particles: Particle[] = Array.from({ length: 50 }, () => ({
    x: Math.random() * w, y: Math.random() * h,
    vx: (Math.random() - 0.5) * 0.3, vy: (Math.random() - 0.5) * 0.3,
    radius: Math.random() * 1.5 + 0.5,
    alpha: Math.random() * 0.4 + 0.1,
    alphaSpeed: (Math.random() - 0.5) * 0.004,
  }))

  const maxDist = 100

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < maxDist) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(201, 169, 110, ${(1 - dist / maxDist) * 0.1})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    for (const p of particles) {
      p.x += p.vx; p.y += p.vy
      p.alpha += p.alphaSpeed
      if (p.alpha <= 0.1 || p.alpha >= 0.5) p.alphaSpeed *= -1
      p.alpha = Math.max(0.1, Math.min(0.5, p.alpha))
      if (p.x < 0) p.x = w; if (p.x > w) p.x = 0
      if (p.y < 0) p.y = h; if (p.y > h) p.y = 0
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(201, 169, 110, ${p.alpha})`
      ctx.fill()
    }
    animationId = requestAnimationFrame(draw)
  }
  draw()

  const onResize = () => { w = parent.clientWidth; h = parent.clientHeight; canvas.width = w; canvas.height = h }
  window.addEventListener('resize', onResize)
  return () => window.removeEventListener('resize', onResize)
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不少于6位', trigger: 'blur' },
  ],
}

onMounted(() => {
  const savedUsername = localStorage.getItem('rememberedUsername')
  if (savedUsername) { form.username = savedUsername; rememberMe.value = true }
  const reason = route.query.reason as string
  if (reason === 'session_expired') message.warning('会话已过期，请重新登录')
  else if (reason === 'admin_kicked') message.warning('您已被管理员强制下线')
  initParticles()
})

onUnmounted(() => { if (animationId !== null) cancelAnimationFrame(animationId) })

const handleLogin = async () => {
  try { await formRef.value?.validate() } catch { return }
  loading.value = true
  try {
    await authStore.login(form)
    if (rememberMe.value) localStorage.setItem('rememberedUsername', form.username)
    else localStorage.removeItem('rememberedUsername')
    message.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } catch (e: any) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleGoogleLogin = () => { window.location.href = '/api/auth/google' }
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: var(--bg-primary);
}

/* ═══ LEFT BRAND PANEL ═══ */
.brand-panel {
  flex: 1;
  background: #050507;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  width: 100%; height: 100%;
  z-index: 1;
  pointer-events: none;
}

.brand-grain {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  opacity: 0.4;
  background: var(--gradient-noise);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  z-index: 0;
}

.orb--1 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, rgba(201, 169, 110, 0.2) 0%, transparent 70%);
  top: -60px; right: 5%;
  animation: orbFloat1 8s ease-in-out infinite;
}

.orb--2 {
  width: 220px; height: 220px;
  background: radial-gradient(circle, rgba(176, 141, 78, 0.15) 0%, transparent 70%);
  bottom: -40px; left: 5%;
  animation: orbFloat2 10s ease-in-out infinite;
}

.orb--3 {
  width: 140px; height: 140px;
  background: radial-gradient(circle, rgba(212, 186, 130, 0.12) 0%, transparent 70%);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  animation: orbFloat3 12s ease-in-out infinite;
}

@keyframes orbFloat1 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(-20px, 30px); }
  66% { transform: translate(15px, -15px); }
}

@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(30px, -20px); }
  66% { transform: translate(-15px, 15px); }
}

@keyframes orbFloat3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.3); }
}

.brand-content {
  position: relative;
  z-index: 3;
  text-align: center;
  color: white;
  padding: 40px;
  max-width: 400px;
}

/* Emblem */
.brand-emblem {
  width: 80px; height: 80px;
  margin: 0 auto 32px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emblem-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(201, 169, 110, 0.3);
  animation: emblemSpin 20s linear infinite;
}

.emblem-ring::before {
  content: '';
  position: absolute;
  top: -1px; left: 50%;
  width: 8px; height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  transform: translateX(-50%);
  box-shadow: 0 0 12px rgba(201, 169, 110, 0.6);
}

@keyframes emblemSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.emblem-core {
  width: 56px; height: 56px;
  border-radius: 50%;
  background: linear-gradient(155deg, rgba(201, 169, 110, 0.15), rgba(201, 169, 110, 0.05));
  border: 1px solid rgba(201, 169, 110, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
}

.emblem-text {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-color);
  letter-spacing: 0.05em;
}

/* Title */
.brand-title {
  margin: 0 0 8px;
  line-height: 1.1;
}

.brand-title__line {
  display: block;
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.brand-title__line--gold {
  background: var(--gradient-gold);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 48px;
  letter-spacing: 0.1em;
  font-weight: 400;
}

/* Features */
.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  text-align: left;
}

.feature {
  display: flex;
  align-items: center;
  gap: 14px;
  animation: fadeIn 0.6s ease-out both;
}

.feature__icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  background: rgba(201, 169, 110, 0.1);
  border: 1px solid rgba(201, 169, 110, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature__icon svg { width: 18px; height: 18px; color: var(--primary-color); }

.feature__text { display: flex; flex-direction: column; }
.feature__name { font-size: 14px; font-weight: 500; color: rgba(255, 255, 255, 0.9); }
.feature__desc { font-size: 12px; color: rgba(255, 255, 255, 0.4); }

/* ═══ RIGHT LOGIN PANEL ═══ */
.login-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--card-bg);
  border-left: 1px solid var(--border-color);
}

.login-card { width: 100%; max-width: 380px; }

.login-header { text-align: center; margin-bottom: 36px; }
.login-title { font-size: 28px; font-weight: 700; color: var(--text-primary); margin: 0 0 8px; letter-spacing: -0.02em; }
.login-desc { font-size: 14px; color: var(--text-secondary); margin: 0; }

/* Form */
.form-field { margin-bottom: 20px; }
.form-label { display: block; font-size: 13px; font-weight: 500; color: var(--text-secondary); margin-bottom: 6px; letter-spacing: 0.02em; }

.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-btn {
  height: 48px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 10px !important;
  letter-spacing: 0.05em;
}

.divider {
  display: flex;
  align-items: center;
  margin: 24px 0;
  color: var(--text-disabled);
  font-size: 12px;
}

.divider::before, .divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.divider span { padding: 0 12px; letter-spacing: 0.05em; }

.alt-logins {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.alt-btn {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.alt-btn:hover {
  border-color: rgba(201, 169, 110, 0.3);
  color: var(--text-primary);
  background: var(--card-bg-hover);
}

.alt-btn__icon { width: 18px; height: 18px; flex-shrink: 0; }
.alt-btn svg { width: 18px; height: 18px; flex-shrink: 0; }

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-tertiary);
}

/* ═══ INPUT OVERRIDES ═══ */
.login-card :deep(.n-form-item) { width: 100% !important; }
.login-card :deep(.n-form-item-blank) { width: 100% !important; }
.login-card :deep(.n-form-item-feedback-wrapper) { min-height: 0; }

.login-card :deep(.n-input) {
  background-color: var(--bg-tertiary) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 10px !important;
  height: 44px !important;
}

.login-card :deep(.n-input .n-input-wrapper) { background-color: transparent !important; }

.login-card :deep(.n-input:hover) {
  border-color: rgba(201, 169, 110, 0.3) !important;
}

.login-card :deep(.n-input:focus-within) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 3px rgba(201, 169, 110, 0.08) !important;
}

.login-card :deep(.n-input .n-input__input-el) {
  color: var(--text-primary) !important;
  caret-color: var(--primary-color) !important;
}

.login-card :deep(.n-input .n-input__input-el::placeholder) {
  color: var(--text-disabled) !important;
}

.login-card :deep(.n-input input:-webkit-autofill) {
  -webkit-box-shadow: 0 0 0 100px var(--bg-tertiary) inset !important;
  -webkit-text-fill-color: var(--text-primary) !important;
}

.login-card :deep(.n-checkbox__label) { color: var(--text-secondary); font-size: 13px; }

/* ═══ RESPONSIVE ═══ */
@media (max-width: 960px) {
  .brand-panel { display: none; }
  .login-panel { width: 100%; border-left: none; }
}

@media (max-width: 480px) {
  .login-panel { padding: 24px 20px; }
  .login-title { font-size: 24px; }
  .alt-logins { grid-template-columns: 1fr; }
}
</style>
