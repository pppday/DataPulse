import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])
  let nextId = 1

  const add = ({ type = 'info', title = '', message = '', duration = 3500 }) => {
    const id = nextId++
    const toast = { id, type, title, message }
    toasts.value.push(toast)

    if (duration > 0) {
      setTimeout(() => {
        remove(id)
      }, duration)
    }
    return id
  }

  const success = (message, title = '操作成功') => add({ type: 'success', title, message })
  const error = (message, title = '操作失败') => add({ type: 'error', title, message: formatErrorMessage(message) })
  const warning = (message, title = '注意提示') => add({ type: 'warning', title, message })
  const info = (message, title = '系统通知') => add({ type: 'info', title, message })

  const remove = (id) => {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
      toasts.value.splice(index, 1)
    }
  }

  const clear = () => {
    toasts.value = []
  }

  /**
   * 将底层生硬的数据库/网络异常转换为专业友好的企业文案
   */
  function formatErrorMessage(msg) {
    if (!msg) return '未知异常，请稍后重试'
    if (typeof msg !== 'string') return String(msg)
    if (msg.includes('Communications link failure') || msg.includes('Failed to initialize pool')) {
      return '目标数据库连接失败：网络中断或数仓端口未启动，请检查数仓连通性。'
    }
    if (msg.includes('Access denied for user')) {
      return '数据库认证失败：用户名或密码错误，请核对数仓账号权限。'
    }
    if (msg.includes('Unknown database')) {
      return '目标数据库不存在，请核对数据库名称。'
    }
    if (msg.includes('Connection refused')) {
      return '目标主机拒绝连接，请确认数仓 IP 与端口服务已开启。'
    }
    return msg
  }

  return {
    toasts,
    add,
    success,
    error,
    warning,
    info,
    remove,
    clear
  }
})
