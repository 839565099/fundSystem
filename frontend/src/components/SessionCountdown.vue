<template>
  <div v-if="visible" class="session-countdown" :class="{ 'session-warning': isWarning }">
    <n-icon size="16"><IconClock /></n-icon>
    <span class="countdown-text">{{ formattedTime }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { NIcon } from 'naive-ui'
import { IconClock } from '@tabler/icons-vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const visible = computed(() => authStore.isLoggedIn && !!authStore.sessionInfo?.expireTime)

const expireTime = computed(() => {
  if (!authStore.sessionInfo?.expireTime) return null
  return new Date(authStore.sessionInfo.expireTime).getTime()
})

const remainingMs = ref(0)
let timer: number | undefined

const isWarning = computed(() => remainingMs.value > 0 && remainingMs.value <= 5 * 60 * 1000)

const formattedTime = computed(() => {
  const ms = remainingMs.value
  if (ms <= 0) return '00:00:00'
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const updateCountdown = () => {
  if (expireTime.value) {
    remainingMs.value = expireTime.value - Date.now()
    if (remainingMs.value <= 0) {
      remainingMs.value = 0
      // 倒计时归零，执行强制下线
      authStore.forceLogout()
      window.location.href = '/login?reason=session_expired'
    }
  }
}

watch(() => authStore.sessionInfo, (newVal) => {
  if (newVal) {
    updateCountdown()
  }
}, { immediate: true })

onMounted(() => {
  updateCountdown()
  timer = window.setInterval(updateCountdown, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.session-countdown {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 16px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-color);
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.3s ease;
}

.session-countdown.session-warning {
  background: #fff3e0;
  color: #d93025;
  border-bottom-color: #ffd59a;
  animation: pulse-warning 2s infinite;
}

.countdown-text {
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
  font-weight: 600;
  letter-spacing: 0.5px;
}

@keyframes pulse-warning {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
</style>
