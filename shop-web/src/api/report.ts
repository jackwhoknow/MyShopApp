const baseURL = import.meta.env.VITE_API_BASE ?? ''

/** 带登录态下载 CSV：后端返回附件，前端转 Blob 触发浏览器下载 */
export async function downloadCsv(path: string, filename: string, params?: Record<string, string>) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : ''
  const token = localStorage.getItem('token')
  const response = await fetch(`${baseURL}${path}${query}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    throw new Error('导出失败，请稍后重试')
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export const reportApi = {
  exportOrders: (startDate?: string, endDate?: string) =>
    downloadCsv('/api/merchant/reports/orders.csv', `orders-${Date.now()}.csv`,
      buildParams(startDate, endDate)),
  exportSettlements: (startDate?: string, endDate?: string) =>
    downloadCsv('/api/merchant/reports/settlements.csv', `settlements-${Date.now()}.csv`,
      buildParams(startDate, endDate))
}

function buildParams(startDate?: string, endDate?: string) {
  const params: Record<string, string> = {}
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  return params
}
