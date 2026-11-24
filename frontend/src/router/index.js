/**
 * DataPulse 前端路由配置与全局权限守卫。
 * 负责维护登录页与主工作台下 5 大业务模块（自然语言分析、数仓字典、数据源管理、SQL 安全审计、运营看板）的路由映射；
 * 每次页面切换前都会做本地 JWT 过期预检，避免用户带着过期令牌进页面后再吃一堆 401 报错。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { isJwtExpired } from '../stores/auth'
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import AgentQueryView from '../views/AgentQueryView.vue'
import SchemaExplorerView from '../views/SchemaExplorerView.vue'
import AuditLogView from '../views/AuditLogView.vue'
import DashboardView from '../views/DashboardView.vue'

// 系统路由表：除 /login 标记为 public 公开可访问外，其余页面均嵌套在 MainLayout 导航壳中并受登录守卫保护
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/query',
    children: [
      {
        path: 'query',
        name: 'AgentQuery',
        component: AgentQueryView,
        meta: { title: '智能分析工作台' }
      },
      // 兼容旧版 /agent 路径书签，自动重定向至当前工作台 /query
      {
        path: 'agent',
        redirect: '/query'
      },
      {
        path: 'schema',
        name: 'SchemaExplorer',
        component: SchemaExplorerView,
        meta: { title: '数仓字典全景' }
      },
      {
        path: 'datasource',
        name: 'Datasource',
        // 数据源管理页采用按需动态懒加载，减小首屏 JS 体积
        component: () => import('../views/DatasourceView.vue'),
        meta: { title: '数据源管理' }
      },
      {
        path: 'audit',
        name: 'AuditLog',
        component: AuditLogView,
        meta: { title: 'SQL安全审计日志' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: DashboardView,
        meta: { title: '数仓运营看板' }
      }
    ]
  },
  // 兜底通配路由：用户手输未知路径时统一拉回智能分析工作台（未登录会再被守卫拦去登录页）
  {
    path: '/:pathMatch(.*)*',
    redirect: '/query'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置路由守卫（JWT 本地过期预检 + 登录访问控制）：
 * 1. 先检查 localStorage 里的 Token 是否已过期，过期就当场清理，不等接口报错；
 * 2. 访问受保护页面但没有有效 Token -> 拦截并跳转登录页；
 * 3. 已登录用户手动访问登录页 -> 直接送回分析工作台，免得重复登录。
 */
router.beforeEach((to, from, next) => {
  let token = localStorage.getItem('datapulse_token')
  if (token && isJwtExpired(token)) {
    localStorage.removeItem('datapulse_token')
    localStorage.removeItem('datapulse_user')
    token = null
  }
  if (!to.meta.public && !token) {
    next({ name: 'Login' })
  } else if (to.name === 'Login' && token) {
    next({ name: 'AgentQuery' })
  } else {
    next()
  }
})

export default router
