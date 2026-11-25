import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layouts/MainLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import UserLayout from '@/layouts/UserLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, guestOnly: true, title: '登录' }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true, guestOnly: true, title: '注册' }
    },
    {
      path: '/forgot',
      name: 'forgot',
      component: () => import('@/views/ForgotView.vue'),
      meta: { public: true, guestOnly: true, title: '找回凭据' }
    },
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'market',
          component: () => import('@/views/MarketView.vue'),
          meta: { requiresVerified: true, title: '校园集市' }
        },
        {
          path: 'items/:id',
          name: 'item-detail',
          component: () => import('@/views/ItemDetailView.vue'),
          meta: { requiresVerified: true, title: '商品详情' }
        },
        {
          path: 'publish',
          name: 'publish',
          component: () => import('@/views/PublishView.vue'),
          meta: { requiresVerified: true, title: '发布闲置' }
        },
        {
          path: 'items/:id/edit',
          name: 'item-edit',
          component: () => import('@/views/PublishView.vue'),
          meta: { requiresVerified: true, title: '编辑商品' }
        },
        {
          path: 'messages',
          name: 'messages',
          component: () => import('@/views/MessagesView.vue'),
          meta: { requiresVerified: true, title: '我的消息' }
        },
        {
          path: 'verify',
          name: 'verify',
          component: () => import('@/views/VerifyView.vue'),
          meta: { title: '校园身份认证' }
        },
        {
          path: 'me',
          component: UserLayout,
          children: [
            { path: '', redirect: { name: 'me-profile' } },
            {
              path: 'profile',
              name: 'me-profile',
              component: () => import('@/views/me/ProfileView.vue'),
              meta: { title: '个人资料' }
            },
            {
              path: 'items',
              name: 'me-items',
              component: () => import('@/views/me/MyItemsView.vue'),
              meta: { requiresVerified: true, title: '我的商品' }
            },
            {
              path: 'favorites',
              name: 'me-favorites',
              component: () => import('@/views/me/FavoritesView.vue'),
              meta: { requiresVerified: true, title: '我的收藏' }
            },
            {
              path: 'orders',
              name: 'me-orders',
              component: () => import('@/views/me/OrdersView.vue'),
              meta: { requiresVerified: true, title: '我的订单' }
            },
            {
              path: 'orders/:id',
              name: 'order-detail',
              component: () => import('@/views/OrderDetailView.vue'),
              meta: { requiresVerified: true, title: '订单详情' }
            },
            {
              path: 'reports',
              name: 'me-reports',
              component: () => import('@/views/me/ReportsView.vue'),
              meta: { requiresVerified: true, title: '我的举报' }
            },
            {
              path: 'notifications',
              name: 'me-notifications',
              component: () => import('@/views/me/NotificationsView.vue'),
              meta: { title: '通知中心' }
            }
          ]
        }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAdmin: true },
      children: [
        { path: '', redirect: { name: 'admin-dashboard' } },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { requiresAdmin: true, title: '运营统计' }
        },
        {
          path: 'items',
          name: 'admin-items',
          component: () => import('@/views/admin/ItemAuditView.vue'),
          meta: { requiresAdmin: true, title: '商品审核' }
        },
        {
          path: 'reports',
          name: 'admin-reports',
          component: () => import('@/views/admin/ReportManageView.vue'),
          meta: { requiresAdmin: true, title: '举报处理' }
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/views/admin/DisputeOrdersView.vue'),
          meta: { requiresAdmin: true, title: '争议订单' }
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserManageView.vue'),
          meta: { requiresAdmin: true, title: '用户管理' }
        },
        {
          path: 'audit-logs',
          name: 'admin-audit-logs',
          component: () => import('@/views/admin/AuditLogView.vue'),
          meta: { requiresAdmin: true, title: '审计日志' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { public: true, title: '页面不存在' }
    }
  ]
})

router.beforeEach(async (to) => {
  const store = useUserStore()

  if (to.meta.public) {
    if (to.meta.guestOnly && store.isLoggedIn) {
      try {
        await store.fetchMe()
        if (store.user) {
          return { path: store.isAdmin ? '/admin' : '/' }
        }
      } catch {
        /* token 失效时继续访问登录页 */
      }
    }
    return true
  }

  if (!store.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!store.user) {
    try {
      await store.fetchMe()
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.requiresAdmin && !store.isAdmin) {
    return { name: 'market' }
  }

  if (to.meta.requiresVerified && !store.isVerified) {
    return { name: 'verify', query: { redirect: to.fullPath } }
  }

  return true
})

router.afterEach((to) => {
  const base = '校园集市'
  document.title = to.meta.title ? `${to.meta.title} · ${base}` : base
})

export default router
