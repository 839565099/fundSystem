<template>
  <div class="login-page">
    <div class="login-card card">
      <h1 class="login-title">登录</h1>
      <n-form ref="formRef" :model="form" :rules="rules">
        <n-form-item path="username" label="用户名">
          <n-input v-model:value="form.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input 
            v-model:value="form.password" 
            type="password" 
            placeholder="请输入密码"
            show-password-on="click"
          />
        </n-form-item>
        <n-button type="primary" block :loading="loading" @click="handleLogin">
          登录
        </n-button>
      </n-form>
      <div class="login-footer">
        还没有账号？
        <n-button text type="primary" @click="router.push('/register')">立即注册</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, type FormInst, type FormRules, createDiscreteApi } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInst>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  
  loading.value = true
  try {
    await authStore.login(form)
    message.success('登录成功')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (e: any) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 32px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  color: var(--text-secondary);
}
</style>
