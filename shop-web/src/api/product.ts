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
  detail: (id: number) => http.get<ProductDetail>(`/api/products/${id}`)
}
