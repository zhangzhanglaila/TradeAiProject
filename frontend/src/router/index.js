import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页看板', requiresAuth: true, roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'trade',
        name: 'Trade',
        component: () => import('@/views/trade/Trade.vue'),
        meta: { title: '贸易数据管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'news',
        name: 'News',
        component: () => import('@/views/news/News.vue'),
        meta: { title: '新闻数据管理', requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/Stats.vue'),
        meta: { title: '统计分析', requiresAuth: true, roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'ai',
        name: 'Ai',
        component: () => import('@/views/ai/Ai.vue'),
        meta: { title: 'AI智能问答', requiresAuth: true, roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'knowledge-graph/create',
        name: 'KnowledgeGraphCreate',
        component: () => import('@/views/knowledge-graph/KnowledgeGraphCreate.vue'),
        meta: { title: '知识图谱-创建', requiresAuth: true, roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'knowledge-graph/view/:id',
        name: 'KnowledgeGraphView',
        component: () => import('@/views/knowledge-graph/KnowledgeGraphView.vue'),
        meta: { title: '知识图谱-查看', requiresAuth: true, roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'knowledge-graph/history',
        name: 'KnowledgeGraphHistory',
        component: () => import('@/views/knowledge-graph/KnowledgeGraphHistory.vue'),
        meta: { title: '知识图谱-历史', requiresAuth: true, roles: ['ADMIN', 'USER'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (userStore.token) {
    if (to.path === '/login') {
      next('/dashboard')
    } else {
      const roles = to.meta.roles
      if (roles) {
        const role = userStore.userInfo?.role

        // role 丢失时，避免误判为无权限并反复跳转
        if (!role) {
          ElMessage.warning('登录信息不完整，请重新登录')
          userStore.logout()
          next('/login')
          return
        }

        if (!roles.includes(role)) {
          ElMessage.warning('暂无权限访问该页面')
          next('/dashboard')
          return
        }
      }

      next()
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
