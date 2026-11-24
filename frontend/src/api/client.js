/**
 * 全局 Axios HTTP 请求客户端封装。
 * 作用：统一收敛前端所有与 Spring Boot 后端 `/api/v1` 的通信细节，避免每个页面重复写鉴权和报错逻辑。
 * 核心职责：
 * 1. 请求拦截：自动从 localStorage 读取 JWT 令牌并注入 Authorization 请求头；
 * 2. 响应拦截：放行二进制文件流（如 Excel 导出），并对普通 JSON 统一校验 code 并解包 data；
 * 3. 401 熔断重定向：当后端返回 401 未认证/登录过期时，自动清理本地登录态并跳转至登录页。
 */
import axios from 'axios'

// 创建统一的请求实例：timeout 设为 60 秒，给大模型 Text-to-SQL 生成和复杂数仓查询留出足够时间
const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：发请求前统一带上 Bearer JWT 令牌，供后端 Spring Security / 鉴权过滤器验签
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('datapulse_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一处理业务解包、文件流直通与 401 登录失效跳回
apiClient.interceptors.response.use(
  response => {
    // 支持 Blob 二进制文件流下载（如后端 Excel 导出）：需要保留完整 response 以便外层读取响应头里的文件名
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response
    }
    const res = response.data
    // 后端约定统一返回 { code, message, data }，非 200 视作业务失败并转成 Promise 拒绝交给调用方弹窗提示
    if (res.code && res.code !== 200) {
      const err = new Error(res.message || '系统异常')
      err.code = res.code
      return Promise.reject(err)
    }
    // 校验通过后直接剥壳返回业务数据本身，调用方拿到的就是纯粹的 data
    return res.data
  },
  error => {
    // 捕获 HTTP 401（Token 过期、被篡改或未登录）：清空本地残留凭证并强制回到登录页（避免在登录页死循环刷新）
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('datapulse_token')
      localStorage.removeItem('datapulse_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    // 优先展示后端返回的可读错误说明，其次降级为网络层错误信息
    const message = error.response?.data?.message || error.message || '网络连接失败'
    return Promise.reject(new Error(message))
  }
)

export default apiClient
