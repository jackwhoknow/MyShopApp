import http from './http'
import type { ProductQuery } from './product'
import type { PageResult, ProductListItem, ShopDetail } from '@/types'

/** 店铺公开接口：游客可用 */
export const shopApi = {
  detail: (shopId: number) => http.get<ShopDetail>(`/api/shops/${shopId}`),
  products: (shopId: number, params: ProductQuery) =>
    http.get<PageResult<ProductListItem>>(`/api/shops/${shopId}/products`, { params })
}
