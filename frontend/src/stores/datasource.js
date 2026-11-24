/**
 * 全局目标数据源状态仓库（Pinia）。
 * 作用：统一保存当前全站挂载的数仓列表以及用户选中的“当前分析目标数据源 ID”，
 * 让顶栏下拉框、自然语言取数工作台、数仓字典和数据源管理页始终保持同步，并借助 localStorage 记住上次选择。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '../api/client'

export const useDatasourceStore = defineStore('datasource', () => {
  // 优先恢复上次选中的数据源 ID，首次访问默认指向 1 号主数仓
  const saved = localStorage.getItem('datapulse_active_ds')
  const currentDatasourceId = ref(saved ? Number(saved) : 1)
  const datasources = ref([])
  const loading = ref(false)

  // 根据当前选中的 ID，实时计算对应的完整数据源配置对象（名称、库名、主机端口等）
  const currentDatasource = computed(() => {
    return datasources.value.find(d => d.id === currentDatasourceId.value) || null
  })

  // 切换当前激活的数据源，并同步写入 localStorage 以便刷新后保持
  const setDatasourceId = (id) => {
    const val = Number(id)
    if (!val) return
    currentDatasourceId.value = val
    localStorage.setItem('datapulse_active_ds', String(val))
  }

  /**
   * 从后端拉取已配置的数据源列表。
   * @param {boolean} force - 为 false 时若内存里已有列表则直接复用缓存；数据源增删改后需传 true 强制刷新。
   */
  const fetchDatasources = async (force = false) => {
    if (datasources.value.length > 0 && !force) {
      return datasources.value
    }
    loading.value = true
    try {
      const list = await apiClient.get('/datasource/list')
      datasources.value = list || []
      // 容错回退：如果浏览器里缓存的旧数据源 ID 已经被管理员删掉了，自动切换到默认数据源或列表第一项
      if (datasources.value.length > 0) {
        const exists = datasources.value.some(d => d.id === currentDatasourceId.value)
        if (!exists) {
          const defaultDs = datasources.value.find(d => d.isDefault === 1) || datasources.value[0]
          setDatasourceId(defaultDs.id)
        }
      }
      return datasources.value
    } catch (err) {
      console.error('获取数据源列表失败', err)
      return []
    } finally {
      loading.value = false
    }
  }

  return {
    currentDatasourceId,
    currentDatasource,
    datasources,
    loading,
    setDatasourceId,
    fetchDatasources
  }
})
