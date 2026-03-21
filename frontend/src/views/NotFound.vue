<template>
  <div class="not-found-page">
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-message">页面未找到</div>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除。</p>
      <n-button type="primary" size="large" @click="router.push('/')">
        返回首页
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'

const router = useRouter()
</script>

<style scoped>
.not-found-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
}

.error-message {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}
</style>
