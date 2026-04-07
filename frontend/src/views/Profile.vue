<template>
  <div class="page-container">
    <PageHeader title="个人中心" icon="👤">
      <template #actions>
        <n-button type="error" ghost size="small" @click="showLogoutConfirm = true">
          退出登录
        </n-button>
      </template>
    </PageHeader>

    <!-- 顶部 Banner 卡片 -->
    <div class="profile-banner">
      <div class="banner-bg">
        <div class="banner-pattern"></div>
      </div>
      <div class="banner-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-ring" :class="{ 'avatar-uploading': avatarUploading }">
            <div class="avatar-wrapper" @click="triggerFileInput">
              <n-avatar
                v-if="user?.avatar"
                round
                :size="110"
                :src="user.avatar"
                object-fit="cover"
              />
              <n-avatar
                v-else
                round
                :size="110"
                :style="{ backgroundColor: avatarBgColor, fontSize: '40px', fontWeight: '600' }"
              >
                {{ avatarLetter }}
              </n-avatar>
              <div class="avatar-overlay">
                <div class="avatar-overlay-content">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                    <circle cx="12" cy="13" r="4"/>
                  </svg>
                  <span>更换头像</span>
                </div>
              </div>
            </div>
            <div v-if="avatarUploading" class="avatar-spinner">
              <n-spin size="small" />
            </div>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleFileChange"
          />
        </div>

        <!-- 用户信息 -->
        <div class="user-info">
          <h1 class="user-name">{{ user?.nickname || user?.username || '用户' }}</h1>
          <div class="user-meta">
            <span class="meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              {{ user?.email || '暂无邮箱' }}
              <span v-if="user?.email && user.emailVerified === 1" class="verified-badge">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
                已验证
              </span>
            </span>
            <span class="meta-divider"></span>
            <span class="meta-item" v-if="user?.phone">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
              {{ user.phone }}
            </span>
            <span class="meta-divider" v-if="user?.phone"></span>
            <span class="meta-item" v-if="user?.createTime">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              {{ formatDate(user.createTime) }} 加入
            </span>
          </div>
          <div class="user-badge" v-if="stats">
            <span class="badge">已陪伴 {{ stats.registerDays }} 天</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="mini-stat-card" @click="router.push('/favorites')">
        <div class="mini-stat-icon" style="background: linear-gradient(135deg, #fbbf24, #f59e0b);">
          <span>&#11088;</span>
        </div>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats?.favoriteCount ?? 0 }}</span>
          <span class="mini-stat-label">我的收藏</span>
        </div>
        <svg class="mini-stat-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </div>

      <div class="mini-stat-card" @click="router.push('/portfolio')">
        <div class="mini-stat-icon" style="background: linear-gradient(135deg, #60a5fa, #3b82f6);">
          <span>&#128188;</span>
        </div>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats?.portfolioCount ?? 0 }}</span>
          <span class="mini-stat-label">投资组合</span>
        </div>
        <svg class="mini-stat-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </div>

      <div class="mini-stat-card" @click="router.push('/alerts')">
        <div class="mini-stat-icon" style="background: linear-gradient(135deg, #fb923c, #f97316);">
          <span>&#128276;</span>
        </div>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats?.alertCount ?? 0 }}</span>
          <span class="mini-stat-label">预警规则</span>
        </div>
        <svg class="mini-stat-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </div>

      <div class="mini-stat-card mini-stat-card--static">
        <div class="mini-stat-icon" style="background: linear-gradient(135deg, #4ade80, #22c55e);">
          <span>&#128197;</span>
        </div>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats?.registerDays ?? 0 }}<small>天</small></span>
          <span class="mini-stat-label">加入时间</span>
        </div>
      </div>
    </div>

    <!-- 设置区域 -->
    <div class="settings-section">
      <n-tabs type="segment" animated>
        <n-tab-pane name="info" tab="个人信息">
          <div class="form-container">
            <div class="form-header">
              <h3>基本信息</h3>
              <p>管理你的个人资料信息</p>
            </div>
            <n-form :model="profileForm" label-placement="top">
              <div class="form-grid">
                <n-form-item label="用户名">
                  <div class="username-field">
                    <n-input v-model:value="profileForm.username" placeholder="设置用户名" maxlength="20" show-count>
                      <template #prefix>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                      </template>
                    </n-input>
                    <n-button type="primary" size="small" :loading="usernameLoading" @click="handleUpdateUsername" :disabled="profileForm.username === user?.username">
                      保存
                    </n-button>
                  </div>
                </n-form-item>
                <n-form-item label="昵称">
                  <n-input v-model:value="profileForm.nickname" placeholder="设置你的昵称" maxlength="20" show-count>
                    <template #prefix>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                    </template>
                  </n-input>
                </n-form-item>
                <n-form-item label="邮箱">
                  <n-input v-model:value="profileForm.email" placeholder="your@email.com">
                    <template #prefix>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                    </template>
                  </n-input>
                </n-form-item>
                <n-form-item label="手机号">
                  <n-input v-model:value="profileForm.phone" placeholder="请输入手机号">
                    <template #prefix>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
                    </template>
                  </n-input>
                </n-form-item>
              </div>
              <div class="form-actions">
                <n-button type="primary" :loading="saveLoading" @click="handleSaveProfile" size="large">
                  保存修改
                </n-button>
              </div>
            </n-form>
          </div>
        </n-tab-pane>

        <n-tab-pane name="password" tab="安全设置">
          <div class="form-container">
            <!-- 未设置密码 -->
            <template v-if="!userHasPassword">
              <div class="form-header">
                <h3>设置密码</h3>
                <p>设置密码后可使用用户名+密码登录</p>
              </div>
              <n-form ref="setPwdFormRef" :model="setPasswordForm" :rules="setPwdRules" label-placement="top">
                <div class="form-grid form-grid--single">
                  <n-form-item path="password" label="新密码">
                    <n-input v-model:value="setPasswordForm.password" type="password" placeholder="至少6个字符" show-password-on="click">
                      <template #prefix>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                      </template>
                    </n-input>
                  </n-form-item>
                  <n-form-item path="confirmPassword" label="确认密码">
                    <n-input v-model:value="setPasswordForm.confirmPassword" type="password" placeholder="再次输入密码" show-password-on="click">
                      <template #prefix>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                      </template>
                    </n-input>
                  </n-form-item>
                </div>
                <div class="form-actions">
                  <n-button type="primary" :loading="setPwdLoading" @click="handleSetPassword" size="large">
                    设置密码
                  </n-button>
                </div>
              </n-form>
            </template>

            <!-- 已有密码 -->
            <template v-else>
              <div class="form-header">
                <h3>修改密码</h3>
                <p>定期修改密码可以提高账号安全性</p>
              </div>
              <n-form ref="pwdFormRef" :model="passwordForm" :rules="pwdRules" label-placement="top">
                <div class="form-grid form-grid--single">
                  <n-form-item path="oldPassword" label="当前密码">
                    <n-input v-model:value="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password-on="click">
                      <template #prefix>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                      </template>
                    </n-input>
                  </n-form-item>
                  <n-form-item path="newPassword" label="新密码">
                    <n-input v-model:value="passwordForm.newPassword" type="password" placeholder="至少6个字符" show-password-on="click">
                      <template #prefix>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg>
                      </template>
                    </n-input>
                  </n-form-item>
                  <n-form-item path="confirmPassword" label="确认新密码">
                    <n-input v-model:value="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password-on="click">
                      <template #prefix>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                      </template>
                    </n-input>
                  </n-form-item>
                </div>
                <div class="form-actions">
                  <n-button type="primary" :loading="pwdLoading" @click="handleChangePassword" size="large">
                    修改密码
                  </n-button>
                </div>
              </n-form>
            </template>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>

    <!-- 退出登录确认对话框 -->
    <n-modal
      v-model:show="showLogoutConfirm"
      preset="dialog"
      title="确认退出"
      content="确定要退出登录吗？退出后需要重新登录才能使用完整功能。"
      positive-text="确认退出"
      negative-text="取消"
      type="warning"
      @positive-click="handleLogout"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NAvatar, NTabs, NTabPane, NForm, NFormItem, NInput, NButton, NModal, NSpin,
  type FormInst, type FormRules, createDiscreteApi
} from 'naive-ui'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/auth'
import PageHeader from '../components/PageHeader.vue'
import type { UserStats } from '../types'

const { message } = createDiscreteApi(['message'])

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const stats = ref<UserStats | null>(null)
const statsLoading = ref(false)
const showLogoutConfirm = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const avatarUploading = ref(false)

const avatarBgColor = computed(() => {
  const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316', '#0ea5e9', '#14b8a6']
  const name = user.value?.username || 'U'
  const index = name.charCodeAt(0) % colors.length
  return colors[index]
})

const avatarLetter = computed(() => {
  return user.value?.nickname?.charAt(0) || user.value?.username?.charAt(0) || 'U'
})

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

const setPasswordForm = reactive({
  password: '',
  confirmPassword: '',
})

const saveLoading = ref(false)
const pwdLoading = ref(false)
const usernameLoading = ref(false)
const setPwdLoading = ref(false)
const pwdFormRef = ref<FormInst>()
const setPwdFormRef = ref<FormInst>()
const userHasPassword = ref(true)

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_rule: any, value: string) => value === passwordForm.newPassword, message: '两次密码不一致', trigger: 'blur' },
  ],
}

const setPwdRules: FormRules = {
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_rule: any, value: string) => value === setPasswordForm.password, message: '两次密码不一致', trigger: 'blur' },
  ],
}

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

const initForm = () => {
  if (user.value) {
    profileForm.username = user.value.username
    profileForm.nickname = user.value.nickname || ''
    profileForm.email = user.value.email || ''
    profileForm.phone = user.value.phone || ''
  }
}

const fetchStats = async () => {
  statsLoading.value = true
  try {
    stats.value = await authApi.getUserStats()
    console.log('获取用户统计信息成功:', stats.value)
  } catch (e: any) {
    console.error('获取用户统计信息失败:', e.message)
    // 设置默认值，避免显示空白
    stats.value = {
      favoriteCount: 0,
      portfolioCount: 0,
      alertCount: 0,
      registerDays: 0
    }
  } finally {
    statsLoading.value = false
  }
}

const triggerFileInput = () => {
  if (avatarUploading.value) return
  fileInputRef.value?.click()
}

const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    message.error('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error('图片大小不能超过 2MB')
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const size = 200
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d')!

      // 居中裁剪为正方形
      const minSide = Math.min(img.width, img.height)
      const sx = (img.width - minSide) / 2
      const sy = (img.height - minSide) / 2
      ctx.drawImage(img, sx, sy, minSide, minSide, 0, 0, size, size)

      const base64 = canvas.toDataURL('image/jpeg', 0.8)
      uploadAvatar(base64)
    }
    img.src = reader.result as string
  }
  reader.readAsDataURL(file)

  input.value = ''
}

const uploadAvatar = async (base64: string) => {
  avatarUploading.value = true
  try {
    await authApi.updateProfile({ avatar: base64 })
    await authStore.fetchProfile()
    message.success('头像更新成功')
  } catch (e: any) {
    message.error(e.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
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

const handleUpdateUsername = async () => {
  const newUsername = profileForm.username.trim()
  if (!newUsername) {
    message.error('用户名不能为空')
    return
  }
  usernameLoading.value = true
  try {
    await authApi.updateUsername(newUsername)
    await authStore.fetchProfile()
    message.success('用户名修改成功')
  } catch (e: any) {
    message.error(e.message || '修改失败')
  } finally {
    usernameLoading.value = false
  }
}

const handleSetPassword = async () => {
  try {
    await setPwdFormRef.value?.validate()
  } catch {
    return
  }
  setPwdLoading.value = true
  try {
    await authApi.setPassword(setPasswordForm.password)
    userHasPassword.value = true
    message.success('密码设置成功，现在可以使用用户名+密码登录')
  } catch (e: any) {
    message.error(e.message || '设置失败')
  } finally {
    setPwdLoading.value = false
  }
}

const checkHasPassword = async () => {
  try {
    const res = await authApi.hasPassword()
    userHasPassword.value = res.hasPassword
  } catch {
    userHasPassword.value = true
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

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  initForm()
  fetchStats()
  checkHasPassword()
})
</script>

<style scoped>
/* ============ Banner 区域 ============ */
.profile-banner {
  position: relative;
  border-radius: var(--radius-xl);
  overflow: hidden;
  margin-bottom: 20px;
}

.banner-bg {
  position: absolute;
  inset: 0;
  background: var(--gradient-brand);
  z-index: 0;
}

.banner-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(255,255,255,0.12) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 40%),
    radial-gradient(circle at 60% 80%, rgba(255,255,255,0.06) 0%, transparent 30%);
}

.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 40px 44px;
}

/* ============ 头像 ============ */
.avatar-section {
  flex-shrink: 0;
}

.avatar-ring {
  position: relative;
  padding: 4px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(4px);
  transition: var(--transition-base);
}

.avatar-ring:hover {
  background: rgba(255,255,255,0.4);
  transform: scale(1.03);
}

.avatar-ring.avatar-uploading {
  opacity: 0.7;
  pointer-events: none;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: white;
  font-size: 12px;
  font-weight: 500;
}

.avatar-spinner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.3);
  border-radius: 50%;
}

/* ============ 用户信息 ============ */
.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin: 0 0 10px;
  line-height: 1.2;
  text-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.user-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: rgba(255,255,255,0.85);
  font-size: 14px;
  margin-bottom: 14px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.meta-item svg {
  opacity: 0.8;
  flex-shrink: 0;
}

.verified-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #22c55e;
  font-weight: 500;
}

.meta-divider {
  width: 1px;
  height: 14px;
  background: rgba(255,255,255,0.3);
}

.user-badge {
  display: flex;
  gap: 8px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  border-radius: var(--radius-full);
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(4px);
  color: white;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid rgba(255,255,255,0.15);
}

/* ============ 统计卡片 ============ */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.mini-stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
}

.mini-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-300);
}

.mini-stat-card:active {
  transform: translateY(0);
}

.mini-stat-card--static {
  cursor: default;
}

.mini-stat-card--static:hover {
  transform: none;
  box-shadow: none;
  border-color: var(--border-color);
}

.mini-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
  box-shadow: var(--shadow-sm);
}

.mini-stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.mini-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.mini-stat-value small {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.mini-stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.mini-stat-arrow {
  color: var(--text-tertiary);
  flex-shrink: 0;
  opacity: 0;
  transition: var(--transition-base);
}

.mini-stat-card:not(.mini-stat-card--static):hover .mini-stat-arrow {
  opacity: 1;
  color: var(--primary-color);
}

/* ============ 设置区域 ============ */
.settings-section {
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-color);
  padding: 28px;
}

.form-container {
  padding: 8px 0;
}

.form-header {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-secondary);
}

.form-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.form-header p {
  font-size: 14px;
  color: var(--text-tertiary);
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 24px;
  max-width: 720px;
}

.form-grid--single {
  grid-template-columns: 1fr;
  max-width: 420px;
}

.form-actions {
  padding-top: 12px;
}

.form-actions .n-button {
  min-width: 140px;
}

.username-field {
  display: flex;
  gap: 8px;
  width: 100%;
}

.username-field .n-input {
  flex: 1;
}

/* ============ 响应式 ============ */
@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .banner-content {
    flex-direction: column;
    text-align: center;
    padding: 32px 24px;
    gap: 20px;
  }

  .user-meta {
    justify-content: center;
  }

  .user-badge {
    justify-content: center;
  }

  .user-name {
    font-size: 24px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .settings-section {
    padding: 20px 16px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .mini-stat-card {
    padding: 14px 16px;
  }
}
</style>
