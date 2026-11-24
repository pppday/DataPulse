/**
 * DataPulse（企业级 Text-to-SQL 自然语言取数与数据分析平台）前端应用入口文件。
 * 负责初始化 Vue 3 应用实例，依次挂载 Pinia 全局状态管理、Vue Router 路由系统以及全局样式。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'

// 创建 Vue 根实例并依次注入状态管理与路由插件，最后挂载到 index.html 的 #app 节点
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
