<template>
  <div class="register-page">
    <div class="register-card card">
      <h1 class="register-title">注册</h1>
      <n-form ref="formRef" :model="form" :rules="rules">
        <n-form-item path="username" label="用户名">
          <n-input v-model:value="form.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码">
          <n-input v-model:value="form.password" type="password" placeholder="请输入密码" show-password-on="click" />
        </n-form-item>
        <n-form-item path="confirmPassword" label="确认密码">
          <n-input v-model:value="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password-on="click" />
        </n-form-item>
        <n-form-item path="nickname" label="昵称">
          <n-input v-model:value="form.nickname" placeholder="请输入昵称（可选）" />
        </n-form-item>
        <n-form-item path="email" label="邮箱">
          <n-input v-model:value="form.email" placeholder="请输入邮箱（可选）" />
        </n-form-item>
        <n-button type="primary" block :loading="loading" @click="handleRegister">注册</n-button>
      </n-form>
      <div class="register-footer">
        已有账号？
        <n-button text type="primary" @click="router.push('/login')">立即登录</n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, type FormInst, type FormRules, createDiscreteApi } from 'naive-ui'
import { useAuthStore } from '../stores/auth'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
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
    message.success('注册成功，正在自动登录...')
    // 注册成功后自动登录
    try {
      await authStore.login({ username: form.username, password: form.password })
      const redirect = (router.currentRoute.value.query.redirect as string) || '/'
      router.push(redirect)
    } catch {
      // 自动登录失败，跳转到登录页
      router.push('/login')
    }
  } catch (e: any) {
    message.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 32px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  color: var(--text-secondary);
}
</style>
