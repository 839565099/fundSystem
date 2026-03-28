import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页', requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '投资概览', requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/Search.vue'),
    meta: { title: '基金搜索' }
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('../views/Ranking.vue'),
    meta: { title: '基金排行' }
  },
  {
    path: '/sector',
    name: 'SectorRanking',
    component: () => import('../views/SectorRanking.vue'),
    meta: { title: '板块排行' }
  },
  {
    path: '/sector/:code',
    name: 'SectorDetail',
    component: () => import('../views/SectorDetail.vue'),
    meta: { title: '板块详情' }
  },
  {
    path: '/fund/:fundCode',
    name: 'FundDetail',
    component: () => import('../views/FundDetail.vue'),
    meta: { title: '基金详情' }
  },
  {
    path: '/market/:marketCode',
    name: 'MarketDetail',
    component: () => import('../views/MarketDetail.vue'),
    meta: { title: '大盘详情' }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('../views/Favorites.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/compare',
    name: 'Compare',
    component: () => import('../views/Compare.vue'),
    meta: { title: '基金对比' }
  },
  {
    path: '/portfolio',
    name: 'Portfolio',
    component: () => import('../views/Portfolio.vue'),
    meta: { title: '投资组合', requiresAuth: true }
  },
  {
    path: '/portfolio/:id',
    name: 'PortfolioDetail',
    component: () => import('../views/PortfolioDetail.vue'),
    meta: { title: '组合详情', requiresAuth: true }
  },
  {
    path: '/alerts',
    name: 'Alerts',
    component: () => import('../views/Alerts.vue'),
    meta: { title: '预警管理', requiresAuth: true }
  },
  {
    path: '/ai-assistant',
    name: 'AIAssistant',
    component: () => import('../views/AIAssistant.vue'),
    meta: { title: 'AI 助手' }
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: () => import('../views/Analytics.vue'),
    meta: { title: '高级分析' }
  },
  {
    path: '/recommend',
    name: 'Recommend',
    component: () => import('../views/Recommend.vue'),
    meta: { title: '智能推荐' }
  },
  {
    path: '/news',
    name: 'News',
    component: () => import('../views/News.vue'),
    meta: { title: '资讯中心' }
  },
  {
    path: '/news/:id',
    name: 'NewsDetail',
    component: () => import('../views/NewsDetail.vue'),
    meta: { title: '资讯详情' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue'),
    meta: { title: '找回密码' }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('../views/ResetPassword.vue'),
    meta: { title: '重置密码' }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue'),
    meta: { title: '消息中心', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  // 管理员路由
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '管理概览', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('../views/admin/OperationLog.vue'),
        meta: { title: '操作日志', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'users/:id',
        name: 'AdminUserDetail',
        component: () => import('../views/admin/UserDetail.vue'),
        meta: { title: '用户详情', requiresAuth: true, requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面未找到' }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  document.title = `${to.meta.title || '基金系统'} - Fund System`

  const token = localStorage.getItem('token')
  const isLoggedIn = !!token
  const authStore = useAuthStore()

  // 需要认证但未登录 -> 跳转登录页
  if (to.meta.requiresAuth && !isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  // 需要管理员权限但不是管理员 -> 跳转首页
  else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Home' })
  }
  // 已登录访问登录/注册页 -> 跳转首页
  else if ((to.name === 'Login' || to.name === 'Register' || to.name === 'ForgotPassword' || to.name === 'ResetPassword') && isLoggedIn) {
    next({ name: 'Home' })
  }
  else {
    next()
  }
})

export default router
