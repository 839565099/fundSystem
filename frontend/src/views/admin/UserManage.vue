<template>
  <div class="user-manage">
    <n-card>
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-input
          v-model:value="searchForm.keyword"
          placeholder="搜索用户名/邮箱/昵称"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <n-icon><IconSearch /></n-icon>
          </template>
        </n-input>
        <n-select
          v-model:value="searchForm.role"
          placeholder="角色"
          clearable
          :options="roleOptions"
          style="width: 120px"
        />
        <n-select
          v-model:value="searchForm.status"
          placeholder="状态"
          clearable
          :options="statusOptions"
          style="width: 120px"
        />
        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <n-icon><IconSearch /></n-icon>
          </template>
          搜索
        </n-button>
        <n-button @click="handleReset">重置</n-button>
      </div>

      <!-- 用户列表 -->
      <n-data-table
        :columns="columns"
        :data="users"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: UserVO) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <!-- 编辑用户弹窗 -->
    <n-modal
      v-model:show="showEditModal"
      preset="dialog"
      title="编辑用户信息"
      positive-text="保存"
      negative-text="取消"
      :loading="editLoading"
      @positive-click="handleSaveEdit"
    >
      <n-form ref="editFormRef" :model="editForm" :rules="editRules" label-placement="left" label-width="60">
        <n-form-item label="用户名">
          <n-input :value="editForm.username" disabled />
        </n-form-item>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="editForm.nickname" placeholder="请输入昵称" maxlength="20" />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="editForm.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item label="手机号" path="phone">
          <n-input v-model:value="editForm.phone" placeholder="请输入手机号" maxlength="11" />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NButton,
  NIcon,
  NTag,
  NSpace,
  NModal,
  NForm,
  NFormItem,
  type DataTableColumns,
  type PaginationProps,
  useDialog,
  useMessage,
} from 'naive-ui'
import {
  IconSearch,
  IconEye,
  IconEdit,
} from '@tabler/icons-vue'
import { adminApi } from '@/api/admin'
import type { UserVO } from '@/types'

const message = useMessage()
const dialog = useDialog()
const router = useRouter()

const loading = ref(false)
const users = ref<UserVO[]>([])

const searchForm = reactive({
  keyword: '',
  role: null as string | null,
  status: null as number | null,
})

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '普通用户', value: 'USER' },
]

const statusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

const showEditModal = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: 0,
  username: '',
  nickname: '',
  email: '',
  phone: '',
})

const editFormRef = ref()

const editRules = {
  email: {
    trigger: 'blur',
    validator: (_rule: any, value: string) => {
      if (!value) return true
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        return new Error('邮箱格式不正确')
      }
      return true
    }
  },
  phone: {
    trigger: 'blur',
    validator: (_rule: any, value: string) => {
      if (!value) return true
      if (!/^1[3-9]\d{9}$/.test(value)) {
        return new Error('手机号格式不正确')
      }
      return true
    }
  },
}

const columns: DataTableColumns<UserVO> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'username' },
  { title: '昵称', key: 'nickname' },
  { title: '邮箱', key: 'email' },
  {
    title: '角色',
    key: 'role',
    render: (row) => h(NTag, {
      type: row.role === 'ADMIN' ? 'warning' : 'default',
      size: 'small',
    }, { default: () => row.role === 'ADMIN' ? '管理员' : '用户' })
  },
  {
    title: '状态',
    key: 'status',
    render: (row) => h(NTag, {
      type: row.status === 1 ? 'success' : 'error',
      size: 'small',
    }, { default: () => row.status === 1 ? '正常' : '禁用' })
  },
  {
    title: '注册时间',
    key: 'createTime',
    width: 180,
    render: (row) => row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render: (row) => h(NSpace, null, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => handleEdit(row),
        }, { icon: () => h(NIcon, null, { default: () => h(IconEdit) }) }),
        h(NButton, {
          size: 'small',
          onClick: () => router.push(`/admin/users/${row.id}`),
        }, { icon: () => h(NIcon, null, { default: () => h(IconEye) }) }),
        h(NButton, {
          size: 'small',
          type: row.status === 1 ? 'warning' : 'success',
          onClick: () => handleToggleStatus(row),
        }, { default: () => row.status === 1 ? '禁用' : '启用' }),
        h(NButton, {
          size: 'small',
          type: row.role === 'ADMIN' ? 'default' : 'warning',
          onClick: () => handleToggleRole(row),
        }, { default: () => row.role === 'ADMIN' ? '降为用户' : '升为管理员' }),
      ]
    })
  },
]

onMounted(() => {
  loadUsers()
})

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUsers({
      page: pagination.page!,
      pageSize: pagination.pageSize!,
      keyword: searchForm.keyword || undefined,
      role: searchForm.role || undefined,
      status: searchForm.status ?? undefined,
    })
    users.value = res.list || []
    pagination.itemCount = res.total || 0
  } catch (e) {
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadUsers()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.role = null
  searchForm.status = null
  pagination.page = 1
  loadUsers()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadUsers()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadUsers()
}

const handleToggleStatus = (user: UserVO) => {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'

  dialog.warning({
    title: '确认操作',
    content: `确定要${action}用户 "${user.username}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await adminApi.updateUserStatus(user.id, newStatus)
        message.success(`用户已${action}`)
        loadUsers()
      } catch (e) {
        message.error(`${action}失败`)
      }
    },
  })
}

const handleToggleRole = (user: UserVO) => {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  const action = newRole === 'ADMIN' ? '提升为管理员' : '降为普通用户'

  dialog.warning({
    title: '确认操作',
    content: `确定要将用户 "${user.username}" ${action}吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await adminApi.updateUserRole(user.id, newRole)
        message.success('角色更新成功')
        loadUsers()
      } catch (e) {
        message.error('角色更新失败')
      }
    },
  })
}

const handleEdit = (user: UserVO) => {
  editForm.id = user.id
  editForm.username = user.username
  editForm.nickname = user.nickname || ''
  editForm.email = user.email || ''
  editForm.phone = user.phone || ''
  showEditModal.value = true
}

const handleSaveEdit = () => {
  return new Promise((resolve) => {
    editFormRef.value?.validate((errors: any) => {
      if (errors) {
        resolve(false)
        return
      }
      adminApi.updateUserInfo(editForm.id, {
        nickname: editForm.nickname,
        email: editForm.email,
        phone: editForm.phone,
      }).then(() => {
        message.success('用户信息更新成功')
        loadUsers()
        resolve(true)
      }).catch((err) => {
        const msg = err?.response?.data?.message || '更新失败'
        message.error(msg)
        resolve(false)
      })
    })
  })
}
</script>

<style scoped>
.user-manage {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-form {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-form .n-input {
  width: 240px;
}
</style>
