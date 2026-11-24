import { createRouter, createWebHistory } from 'vue-router'
import { isJwtExpired } from '../stores/auth'
import MainLayout from '../layouts/MainLayout.vue'
import LoginView from '../views/LoginView.vue'
import AgentQueryView from '../views/AgentQueryView.vue'
import SchemaExplorerView from '../views/SchemaExplorerView.vue'
import AuditLogView from '../views/AuditLogView.vue'
import DashboardView from '../views/DashboardView.vue'

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
    redirect: '/agent',
    children: [
      {
        path: 'agent',
        name: 'AgentQuery',
        component: AgentQueryView,
        meta: { title: '智能分析工作台' }
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
  {
    path: '/:pathMatch(.*)*',
    redirect: '/agent'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

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
