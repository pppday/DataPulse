import * as XLSX from 'xlsx'
import apiClient from '../api/client'

/**
 * 【后端主导】通过 Spring Boot 后端 (/api/v1/agent/export/excel) 生成企业级多工作表 Excel 报表
 * 包含：专业商务 Navy/Slate 调色板、斑马纹行交替、数值千分位格式化、自动列宽、Sheet1明细 + Sheet2商业决策洞察
 * @param {Object} payload - 导出请求参数
 */
export async function exportFromBackend(payload) {
  if (!payload) {
    throw new Error('导出请求参数不能为空')
  }

  // 发起二进制 Blob 请求至 Java 后端
  const response = await apiClient.post('/agent/export/excel', payload, {
    responseType: 'blob'
  })

  // 1. 从 HTTP 响应头解析文件名 (RFC 5987 UTF-8 或标准 filename)
  let downloadFileName = payload.fileName || 'DataPulse_分析报表.xlsx'
  const disposition = response.headers?.['content-disposition'] || response.headers?.['Content-Disposition']
  if (disposition) {
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const normalMatch = disposition.match(/filename="?([^;"]+)"?/i)
    if (utf8Match && utf8Match[1]) {
      try {
        downloadFileName = decodeURIComponent(utf8Match[1])
      } catch (e) {
        downloadFileName = utf8Match[1]
      }
    } else if (normalMatch && normalMatch[1]) {
      try {
        downloadFileName = decodeURIComponent(normalMatch[1])
      } catch (e) {
        downloadFileName = normalMatch[1]
      }
    }
  }

  if (!downloadFileName.toLowerCase().endsWith('.xlsx')) {
    downloadFileName += '.xlsx'
  }

  // 2. 将后端字节流转为 Blob 并触发浏览器标准下载
  const blobData = response.data instanceof Blob ? response.data : new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const blobUrl = window.URL.createObjectURL(blobData)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = downloadFileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)

  return downloadFileName
}

/**
 * 【前端兜底】在离线或极端弱网环境下的一键本地快速导出
 * @param {Array<string>} columns - 表头列名
 * @param {Array<Object>} rows - 行数据对象列表
 * @param {string} fileName - 导出的文件名
 * @param {string} [sheetName='数据明细'] - Excel Sheet 工作表名
 */
export function exportToExcel(columns, rows, fileName = 'DataPulse_报表', sheetName = '数据明细') {
  if (!columns || columns.length === 0 || !rows || rows.length === 0) {
    throw new Error('当前没有可导出的数据行')
  }

  const sheetData = []
  sheetData.push([...columns])

  rows.forEach((row) => {
    const rowValues = columns.map((col) => {
      const val = row[col]
      if (val === null || val === undefined) return ''
      return val
    })
    sheetData.push(rowValues)
  })

  const ws = XLSX.utils.aoa_to_sheet(sheetData)

  const colWidths = columns.map((col) => {
    let maxLen = getVisualLength(col)
    for (let i = 0; i < rows.length; i++) {
      const val = rows[i][col]
      if (val !== null && val !== undefined) {
        const len = getVisualLength(String(val))
        if (len > maxLen) {
          maxLen = len
        }
      }
    }
    return { wch: Math.min(Math.max(maxLen + 3, 12), 60) }
  })
  ws['!cols'] = colWidths

  const wb = XLSX.utils.book_new()
  const safeSheetName = (sheetName || 'Sheet1')
    .replace(/[\\/*?[\]]/g, '')
    .slice(0, 31) || 'Sheet1'
  XLSX.utils.book_append_sheet(wb, ws, safeSheetName)

  const dateStr = formatDate(new Date())
  let cleanName = (fileName || `DataPulse_导出报表_${dateStr}`).trim()
  if (!cleanName.toLowerCase().endsWith('.xlsx')) {
    cleanName = `${cleanName}.xlsx`
  }
  cleanName = cleanName.replace(/[\\/:*?"<>|]/g, '_')

  try {
    XLSX.writeFile(wb, cleanName)
  } catch (err) {
    const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([wbout], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = cleanName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  return cleanName
}

function getVisualLength(str) {
  if (!str) return 0
  let len = 0
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i)
    if (code >= 0x4e00 && code <= 0x9fa5) {
      len += 2
    } else if (code > 127) {
      len += 1.5
    } else {
      len += 1
    }
  }
  return Math.ceil(len)
}

function formatDate(d) {
  const pad = (n) => String(n).padStart(2, '0')
  const YYYY = d.getFullYear()
  const MM = pad(d.getMonth() + 1)
  const DD = pad(d.getDate())
  const HH = pad(d.getHours())
  const mm = pad(d.getMinutes())
  const ss = pad(d.getSeconds())
  return `${YYYY}${MM}${DD}_${HH}${mm}${ss}`
}
