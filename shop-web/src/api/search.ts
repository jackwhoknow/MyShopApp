import http from './http'

/** 搜索增强：热搜、联想词与个人搜索历史 */
export const searchApi = {
  hot: (limit = 10) => http.get<string[]>('/api/search/hot', { params: { limit } }),
  suggest: (keyword: string, limit = 10) =>
    http.get<string[]>('/api/search/suggest', { params: { keyword, limit } }),
  history: () => http.get<string[]>('/api/search/history'),
  clearHistory: () => http.delete<void>('/api/search/history')
}
