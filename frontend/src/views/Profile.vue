<template>
  <div class="page-container">
    <div class="profile-header glass-card">
      <div class="avatar-section">
        <n-avatar round :size="80">{{ user?.nickname?.charAt(0) || user?.username?.charAt(0) || 'U' }}</n-avatar>
        <div class="user-info">
          <h2>{{ user?.nickname || user?.username }}</h2>
          <p>{{ user?.email || '暂无邮箱' }}</p>
        </div>
      </div>
    </div>

    <div class="profile-content">
      <n-tabs type="line" animated>
        <n-tab-pane name="info" tab="个人信息">
          <div class="info-form card">
            <n-form :model="profileForm" label-placement="left" label-width="80">
              <n-form-item label="用户名">
                <n-input v-model:value="profileForm.username" disabled />
              </n-form-item>
              <n-form-item label="昵称">
                <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
              </n-form-item>
              <n-form-item label="邮箱">
                <n-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
              </n-form-item>
              <n-form-item label="手机">
                <n-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
              </n-form-item>
              <n-form-item>
                <n-button type="primary" :loading="saveLoading" @click="handleSaveProfile">保存修改</n-button>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>

        <n-tab-pane name="password" tab="修改密码">
          <div class="password-form card">
            <n-form ref="pwdFormRef" :model="passwordForm" :rules="pwdRules" label-placement="left" label-width="100">
              <n-form-item path="oldPassword" label="原密码">
                <n-input v-model:value="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password-on="click" />
              </n-form-item>
              <n-form-item path="newPassword" label="新密码">
                <n-input v-model:value="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password-on="click" />
              </n-form-item>
              <n-form-item path="confirmPassword" label="确认密码">
                <n-input v-model:value="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password-on="click" />
              </n-form-item>
              <n-form-item>
                <n-button type="primary" :loading="pwdLoading" @click="handleChangePassword">修改密码</n-button>
              </n-form-item>
            </n-form>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NAvatar, NTabs, NTabPane, NForm, NFormItem, NInput, NButton, type FormInst, type FormRules, createDiscreteApi } from 'naive-ui'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/auth'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const profileForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const saveLoading = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInst>()

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_rule, value) => value === passwordForm.newPassword, message: '两次密码不一致', trigger: 'blur' },
  ],
}

const initForm = () => {
  if (user.value) {
    profileForm.username = user.value.username
    profileForm.nickname = user.value.nickname || ''
    profileForm.email = user.value.email || ''
    profileForm.phone = user.value.phone || ''
  }
}

const handleSaveProfile = async () => {
  saveLoading.value = true
  try {
    await authApi.updateProfile({
      nickname: profileForm.nickname || undefined,
      email: profileForm.email || undefined,
      phone: profileForm.phone || undefined,
    })
    await authStore.fetchProfile()
    message.success('保存成功')
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleChangePassword = async () => {
  try {
    await pwdFormRef.value?.validate()
  } catch {
    return
  }
  
  pwdLoading.value = true
  try {
    await authApi.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    message.success('密码修改成功，请重新登录')
    authStore.logout()
    router.push('/login')
  } catch (e: any) {
    message.error(e.message || '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  initForm()
})
</script>

<style scoped>
.profile-header {
  margin-bottom: 24px;
  padding: 32px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-info h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.user-info p {
  color: var(--text-secondary);
}

.profile-content {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--shadow);
}

.info-form, .password-form {
  padding: 20px 0;
}
</style>
