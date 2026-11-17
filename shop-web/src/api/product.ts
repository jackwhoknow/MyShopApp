import http from './http'
import type { PageResult, ProductDetail, ProductListItem } from '@/types'

export interface ProductQuery {
  keyword?: string
  categoryId?: number
  /** 价格区间筛选，最低价与最高价均可单独传 */
  minPrice?: number
  maxPrice?: number
  /** default / sales / priceAsc / priceDesc / newest */
  sort?: string
  page?: number
  size?: number
}

export const productApi = {
  list: (params: ProductQuery) => http.get<PageResult<ProductListItem>>('/api/products', { params }),
  detail: (id: number) => http.get<ProductDetail>(`/api/products/${id}`),
  /** 相关商品：同分类热销 */
  related: (id: number, limit = 8) =>
    http.get<ProductListItem[]>(`/api/products/${id}/related`, { params: { limit } }),
  /** 首页推荐：全站热销 */
  recommended: (limit = 8) =>
    http.get<ProductListItem[]>('/api/products/recommended', { params: { limit } })
}
