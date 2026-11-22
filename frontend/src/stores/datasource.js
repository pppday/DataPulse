import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '../api/client'

export const useDatasourceStore = defineStore('datasource', () => {
  const saved = localStorage.getItem('datapulse_active_ds')
  const currentDatasourceId = ref(saved ? Number(saved) : 1)
  const datasources = ref([])
  const loading = ref(false)

  const currentDatasource = computed(() => {
    return datasources.value.find(d => d.id === currentDatasourceId.value) || null
  })

  const setDatasourceId = (id) => {
    const val = Number(id)
    if (!val) return
    currentDatasourceId.value = val
    localStorage.setItem('datapulse_active_ds', String(val))
  }

  const fetchDatasources = async (force = false) => {
    if (datasources.value.length > 0 && !force) {
      return datasources.value
    }
    loading.value = true
    try {
      const list = await apiClient.get('/datasource/list')
      datasources.value = list || []
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
