import http from './http'
import type { PageResult, ProductDetail, ProductListItem } from '@/types'

/** 后端 ProductStatus 枚举 */
export type MerchantProductStatus = 'DRAFT' | 'PENDING_REVIEW' | 'ON_SHELF' | 'OFF_SHELF'

export interface MerchantProductQuery {
  keyword?: string
  page?: number
  size?: number
}

export interface ProductSpecPayload {
  name: string
  values: string[]
}

export interface ProductSkuPayload {
  price: number
  stock: number
  image?: string
  /** key = 规格名，value = 规格值 */
  specValues: Record<string, string>
}

/** 对应后端 ProductCreateRequest */
export interface ProductCreatePayload {
  categoryId: number
  name: string
  subtitle?: string
  description?: string
  mainImage: string
  specs: ProductSpecPayload[]
  skus: ProductSkuPayload[]
}

/** 编辑时已有的 SKU 需要带 id，新生成的 SKU 不传 id */
export interface ProductUpdateSkuPayload extends ProductSkuPayload {
  id?: number
}

/**
 * 对应后端 ProductUpdateRequest：
 * specs 与 skus 都不传时只改基础信息，都传时全量替换规格与 SKU。
 */
export interface ProductUpdatePayload {
  categoryId?: number
  name: string
  subtitle?: string
  description?: string
  mainImage?: string
  specs?: ProductSpecPayload[]
  skus?: ProductUpdateSkuPayload[]
}

/** 对应后端 SkuStockUpdateRequest.Item */
export interface ProductSkuStockPayload {
  skuId: number
  price: number
  stock: number
}

export interface BatchProductStatusFailure {
  productId: number
  productName?: string
  reason: string
}

/** 对应后端 BatchProductStatusResultVO */
export interface BatchProductStatusResult {
  total: number
  updated: number
  failures: BatchProductStatusFailure[]
}

export const merchantProductApi = {
  list: (params: MerchantProductQuery) =>
    http.get<PageResult<ProductListItem>>('/api/merchant/products', { params }),
  detail: (id: number) => http.get<ProductDetail>(`/api/merchant/products/${id}`),
  create: (data: ProductCreatePayload) =>
    http.post<ProductDetail>('/api/merchant/products', data),
  update: (id: number, data: ProductUpdatePayload) =>
    http.put<ProductDetail>(`/api/merchant/products/${id}`, data),
  changeStatus: (id: number, status: MerchantProductStatus) =>
    http.put<ProductDetail>(`/api/merchant/products/${id}/status`, { status }),
  updateSkus: (id: number, skus: ProductSkuStockPayload[]) =>
    http.put<ProductDetail>(`/api/merchant/products/${id}/skus`, { skus }),
  batchChangeStatus: (productIds: number[], status: MerchantProductStatus) =>
    http.put<BatchProductStatusResult>('/api/merchant/products/batch-status', {
      productIds,
      status
    }),
  remove: (id: number) => http.delete<void>(`/api/merchant/products/${id}`)
}
