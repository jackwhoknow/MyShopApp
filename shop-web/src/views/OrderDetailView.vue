<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'
import { afterSaleApi } from '@/api/afterSale'
import { reviewApi } from '@/api/review'
import { invoiceApi } from '@/api/invoice'
import { uploadApi } from '@/api/upload'
import { useAuthStore } from '@/stores/auth'
import LogisticsTimeline from '@/components/LogisticsTimeline.vue'
import PaymentDialog from '@/components/PaymentDialog.vue'
import type { AfterSale, Invoice, Order, OrderItem, ReorderResult, Review, SubOrder, SubOrderLogistics } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const order = ref<Order>()
const loading = ref(true)

const afterSales = ref<AfterSale[]>([])
const refundDialogVisible = ref(false)
const refundSubmitting = ref(false)
const refundTarget = ref<SubOrder>()
const refundReason = ref('')
/** 售后凭证图片，最多 6 张 */
const refundImages = ref<string[]>([])
const refundUploading = ref(false)
const refundInputRef = ref<HTMLInputElement>()

const returnDialogVisible = ref(false)
const returnSubmitting = ref(false)
const returnTarget = ref<AfterSale>()
const returnForm = ref({ returnCompany: '', returnTrackingNo: '' })

const reviews = ref<Review[]>([])
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref<OrderItem>()
const reviewForm = ref<{ rating: number; content: string; images: string[] }>({
  rating: 5,
  content: '',
  images: []
})

/** 追评：每笔评价只能追评一次 */
const appendDialogVisible = ref(false)
const appendSubmitting = ref(false)
const appendTarget = ref<Review>()
const appendForm = ref<{ content: string; images: string[] }>({ content: '', images: [] })
const appendUploading = ref(false)
const appendInputRef = ref<HTMLInputElement>()

/** 评价图片最多 6 张，上传走通用图片接口 */
const reviewUploading = ref(false)
const reviewInputRef = ref<HTMLInputElement>()

function pickReviewImages() {
  if (reviewForm.value.images.length >= 6) {
    ElMessage.warning('最多上传 6 张图片')
    return
  }
  reviewInputRef.value?.click()
}

async function onReviewImagesChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) return
  reviewUploading.value = true
  try {
    for (const file of files) {
      if (reviewForm.value.images.length >= 6) {
        ElMessage.warning('最多上传 6 张图片')
        break
      }
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 5MB，已跳过`)
        continue
      }
      if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
        ElMessage.warning(`${file.name} 格式不支持，已跳过`)
        continue
      }
      const result = await uploadApi.image(file, 'content')
      reviewForm.value.images.push(result.url)
    }
  } finally {
    reviewUploading.value = false
  }
}

function removeReviewImage(index: number) {
  reviewForm.value.images.splice(index, 1)
}

/** 售后凭证上传：与评价图片共用上传接口，最多 6 张 */
function pickRefundImages() {
  if (refundImages.value.length >= 6) {
    ElMessage.warning('最多上传 6 张凭证')
    return
  }
  refundInputRef.value?.click()
}

async function onRefundImagesChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) return
  refundUploading.value = true
  try {
    for (const file of files) {
      if (refundImages.value.length >= 6) {
        ElMessage.warning('最多上传 6 张凭证')
        break
      }
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 5MB，已跳过`)
        continue
      }
      if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
        ElMessage.warning(`${file.name} 格式不支持，已跳过`)
        continue
      }
      const result = await uploadApi.image(file, 'content')
      refundImages.value.push(result.url)
    }
  } finally {
    refundUploading.value = false
  }
}

function removeRefundImage(index: number) {
  refundImages.value.splice(index, 1)
}

/** 追评图片上传 */
function openAppend(review: Review) {
  appendTarget.value = review
  appendForm.value = { content: '', images: [] }
  appendDialogVisible.value = true
}

function pickAppendImages() {
  if (appendForm.value.images.length >= 6) {
    ElMessage.warning('最多上传 6 张图片')
    return
  }
  appendInputRef.value?.click()
}

async function onAppendImagesChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (!files.length) return
  appendUploading.value = true
  try {
    for (const file of files) {
      if (appendForm.value.images.length >= 6) break
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 5MB，已跳过`)
        continue
      }
      if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
        ElMessage.warning(`${file.name} 格式不支持，已跳过`)
        continue
      }
      const result = await uploadApi.image(file, 'content')
      appendForm.value.images.push(result.url)
    }
  } finally {
    appendUploading.value = false
  }
}

function removeAppendImage(index: number) {
  appendForm.value.images.splice(index, 1)
}

async function submitAppend() {
  if (!appendTarget.value) return
  if (!appendForm.value.content.trim() && !appendForm.value.images.length) {
    ElMessage.warning('请填写追评内容或上传图片')
    return
  }
  appendSubmitting.value = true
  try {
    const updated = await reviewApi.append(appendTarget.value.id, {
      content: appendForm.value.content.trim() || undefined,
      images: appendForm.value.images.length ? appendForm.value.images : undefined
    })
    reviews.value = reviews.value.map((item) => (item.id === updated.id ? updated : item))
    ElMessage.success('追评已提交')
    appendDialogVisible.value = false
  } finally {
    appendSubmitting.value = false
  }
}

/** 发票：已支付订单可申请，一个订单只开一张 */
const invoice = ref<Invoice | null>(null)
const invoiceDialogVisible = ref(false)
const invoiceSubmitting = ref(false)
const invoiceForm = ref<{ type: 'PERSONAL' | 'COMPANY'; title: string; taxNo: string; email: string }>({
  type: 'PERSONAL',
  title: '',
  taxNo: '',
  email: ''
})

const invoiceStatusLabels: Record<string, string> = {
  PENDING: '开票中',
  ISSUED: '已开票'
}

const logisticsDialogVisible = ref(false)
const logisticsLoading = ref(false)
const logisticsData = ref<SubOrderLogistics>()

const reordering = ref(false)
const reorderDialogVisible = ref(false)
const reorderResult = ref<ReorderResult>()

/** 查看某个子订单的物流轨迹（模拟数据） */
async function openLogisticsTrace(sub: SubOrder) {
  if (!order.value) return
  logisticsDialogVisible.value = true
  logisticsLoading.value = true
  logisticsData.value = undefined
  try {
    logisticsData.value = await orderApi.logistics(order.value.id, sub.id)
  } finally {
    logisticsLoading.value = false
  }
}

/** 某个订单商品的评价（一件商品只能评价一次） */
function reviewOf(orderItemId: number) {
  return reviews.value.find((item) => item.orderItemId === orderItemId)
}

function openReview(item: OrderItem) {
  reviewTarget.value = item
  reviewForm.value = { rating: 5, content: '', images: [] }
  reviewDialogVisible.value = true
}

async function submitReview() {
  if (!reviewTarget.value) return
  if (!reviewForm.value.rating) {
    ElMessage.warning('请先选择评分')
    return
  }
  reviewSubmitting.value = true
  try {
    await reviewApi.create({
      orderItemId: reviewTarget.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim() || undefined,
      images: reviewForm.value.images.length ? reviewForm.value.images : undefined
    })
    ElMessage.success('评价成功，感谢你的反馈')
    reviewDialogVisible.value = false
    await load()
  } finally {
    reviewSubmitting.value = false
  }
}

function openInvoiceApply() {
  if (!order.value) return
  invoiceForm.value = {
    type: 'PERSONAL',
    title: auth.displayName || '个人',
    taxNo: '',
    email: ''
  }
  invoiceDialogVisible.value = true
}

async function submitInvoice() {
  if (!order.value) return
  if (!invoiceForm.value.title.trim()) {
    ElMessage.warning('请填写发票抬头')
    return
  }
  if (invoiceForm.value.type === 'COMPANY' && !invoiceForm.value.taxNo.trim()) {
    ElMessage.warning('单位发票必须填写税号')
    return
  }
  invoiceSubmitting.value = true
  try {
    invoice.value = await invoiceApi.apply(order.value.id, {
      type: invoiceForm.value.type,
      title: invoiceForm.value.title.trim(),
      taxNo: invoiceForm.value.taxNo.trim() || undefined,
      email: invoiceForm.value.email.trim() || undefined
    })
    ElMessage.success('发票申请已提交')
    invoiceDialogVisible.value = false
  } finally {
    invoiceSubmitting.value = false
  }
}

const statusLabels: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '待发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const subStatusLabels: Record<string, string> = {
  WAIT_SHIP: '待发货',
  REFUNDING: '退款中',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

/** 某个子订单最新一条售后记录（列表按 id 倒序，第一条即最新） */
function latestAfterSale(subOrderId: number) {
  return afterSales.value.find((item) => item.subOrderId === subOrderId)
}

/** 未发货可申请仅退款，已发货/已完成可申请退货退款 */
function canApplyRefund(sub: SubOrder) {
  if (!['WAIT_SHIP', 'SHIPPED', 'COMPLETED'].includes(sub.status)) return false
  const latest = latestAfterSale(sub.id)
  return !latest || latest.status === 'REJECTED'
}

/** 按子订单状态决定按钮文案 */
function applyActionLabel(sub: SubOrder) {
  const isReturn = sub.status !== 'WAIT_SHIP'
  const again = latestAfterSale(sub.id)?.status === 'REJECTED' ? '重新' : ''
  return `${again}申请${isReturn ? '退货退款' : '退款'}`
}

function afterSaleTagType(status: string): 'warning' | 'primary' | 'success' | 'danger' | 'info' {
  if (status === 'PENDING') return 'warning'
  if (status === 'RETURNING') return 'primary'
  if (status === 'REFUNDED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

const afterSaleStatusLabels: Record<string, string> = {
  PENDING: '待商家处理',
  RETURNING: '退货中',
  REFUNDED: '已退款',
  REJECTED: '已驳回',
  CLOSED: '平台已关闭'
}

const afterSaleTypeLabels: Record<string, string> = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款'
}

function statusLabelsOfAfterSale(status?: string) {
  return status ? afterSaleStatusLabels[status] || status : ''
}

/** 已支付且所有子订单都已发货，等待买家确认收货 */
const allShipped = computed(
  () =>
    order.value?.status === 'PAID' &&
    order.value.subOrders.length > 0 &&
    order.value.subOrders.every((sub) => sub.status === 'SHIPPED')
)

/** 所有子订单都有发货记录（售后中的子订单状态变为退款中，但货已发出） */
const allDispatched = computed(() => {
  const subs = order.value?.subOrders ?? []
  return subs.length > 0 && subs.every((sub) => !!sub.shippedAt)
})

/** 主订单展示状态：有售后中子订单显示退款中；已发货显示待收货，否则待发货 */
const displayStatus = computed(() => {
  if (!order.value) return ''
  if (order.value.status === 'PAID') {
    if (order.value.subOrders.some((sub) => sub.status === 'REFUNDING')) return '退款中'
    return allDispatched.value ? '待收货' : '待发货'
  }
  return statusLabels[order.value.status] || order.value.status
})

/** 0 提交订单 / 1 支付成功 / 2 商家发货 / 3 交易完成 */
const stepActive = computed(() => {
  if (!order.value) return 0
  if (order.value.status === 'COMPLETED') return 4
  if (order.value.status === 'PAID') return allDispatched.value ? 3 : 2
  return 1
})

async function load() {
  loading.value = true
  try {
    const orderId = Number(route.params.id)
    const [detail, mine, myReviews, myInvoice] = await Promise.all([
      orderApi.detail(orderId),
      afterSaleApi.listMine({ page: 0, size: 100 }).catch(() => ({ list: [] as AfterSale[] })),
      reviewApi.mine({ page: 0, size: 200 }).catch(() => ({ list: [] as Review[] })),
      invoiceApi.findByOrder(orderId).catch(() => null)
    ])
    order.value = detail
    afterSales.value = mine.list
    reviews.value = myReviews.list
    invoice.value = myInvoice
  } finally {
    loading.value = false
  }
}

function openRefund(sub: SubOrder) {
  refundTarget.value = sub
  refundReason.value = ''
  refundImages.value = []
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!refundTarget.value) return
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  const isReturn = refundTarget.value.status !== 'WAIT_SHIP'
  refundSubmitting.value = true
  try {
    await afterSaleApi.apply({
      subOrderId: refundTarget.value.id,
      reason: refundReason.value.trim(),
      evidenceImages: refundImages.value.length ? refundImages.value : undefined
    })
    ElMessage.success(isReturn ? '退货退款申请已提交，等待商家处理' : '退款申请已提交，等待商家处理')
    refundDialogVisible.value = false
    await load()
  } finally {
    refundSubmitting.value = false
  }
}

function openReturnShipment(afterSale: AfterSale) {
  returnTarget.value = afterSale
  returnForm.value = { returnCompany: '', returnTrackingNo: '' }
  returnDialogVisible.value = true
}

/** 退货中且尚未填写退货物流 */
function needReturnShipment(subOrderId: number) {
  const latest = latestAfterSale(subOrderId)
  return latest?.status === 'RETURNING' && !latest.returnTrackingNo
}

function openReturnShipmentBySub(subOrderId: number) {
  const latest = latestAfterSale(subOrderId)
  if (latest) openReturnShipment(latest)
}

async function submitReturnShipment() {
  if (!returnTarget.value) return
  if (!returnForm.value.returnCompany.trim() || !returnForm.value.returnTrackingNo.trim()) {
    ElMessage.warning('请填写退货物流公司和运单号')
    return
  }
  returnSubmitting.value = true
  try {
    await afterSaleApi.returnShipment(returnTarget.value.id, {
      returnCompany: returnForm.value.returnCompany.trim(),
      returnTrackingNo: returnForm.value.returnTrackingNo.trim()
    })
    ElMessage.success('退货物流已提交，等待商家确认收货')
    returnDialogVisible.value = false
    await load()
  } finally {
    returnSubmitting.value = false
  }
}

const payVisible = ref(false)

function pay() {
  if (!order.value) return
  payVisible.value = true
}

async function handlePaid() {
  await load()
}

async function cancel() {
  if (!order.value) return
  await ElMessageBox.confirm('取消后库存会回补，确定取消该订单吗？', '取消订单', {
    type: 'warning',
    confirmButtonText: '确定取消',
    cancelButtonText: '再想想'
  })
  order.value = await orderApi.cancel(order.value.id)
  ElMessage.success('订单已取消')
}

async function confirmReceipt() {
  if (!order.value) return
  await ElMessageBox.confirm('确认已收到全部商品吗？确认后订单将完成。', '确认收货', {
    type: 'info',
    confirmButtonText: '确认收货',
    cancelButtonText: '取消'
  })
  order.value = await orderApi.confirm(order.value.id)
  ElMessage.success('确认收货成功，订单已完成')
}

/** 一键复购：全部成功直接进购物车，有商品被跳过时弹窗说明 */
async function reorder() {
  if (!order.value) return
  reordering.value = true
  try {
    const result = await orderApi.reorder(order.value.id)
    if (result.addedCount === 0 || result.skippedCount > 0) {
      reorderResult.value = result
      reorderDialogVisible.value = true
      return
    }
    ElMessage.success(`已将 ${result.addedCount} 款商品加入购物车`)
    router.push('/cart')
  } finally {
    reordering.value = false
  }
}

function goCart() {
  reorderDialogVisible.value = false
  router.push('/cart')
}

onMounted(load)

const itemCount = computed(() =>
  order.value?.subOrders.reduce(
    (sum, sub) => sum + sub.items.reduce((count, item) => count + item.quantity, 0),
    0
  ) ?? 0
)

/** 全部发货完成的时间，用于步骤条展示 */
const shippedAt = computed(() => {
  const times = order.value?.subOrders.map((sub) => sub.shippedAt).filter(Boolean) ?? []
  return times.length ? times[times.length - 1]?.replace('T', ' ') : ''
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2 class="section-title">订单详情</h2>
      <el-button link @click="router.push('/orders')">返回订单列表</el-button>
    </div>

    <template v-if="order">
      <div class="page-card block">
        <div class="status-head">
          <div>
            <div class="status-text">{{ displayStatus }}</div>
            <div class="muted">订单号：{{ order.orderNo }}</div>
          </div>
          <el-tag
            :type="order.status === 'CANCELLED' ? 'info' : order.status === 'COMPLETED' ? 'success' : 'danger'"
            effect="light"
          >
            {{ displayStatus }}
          </el-tag>
        </div>

        <el-alert
          v-if="order.status === 'CANCELLED'"
          title="订单已取消，商品库存已回补"
          type="info"
          :closable="false"
          show-icon
        />
        <el-steps v-else :active="stepActive" align-center finish-status="success" class="steps">
          <el-step title="提交订单" :description="order.createdAt?.replace('T', ' ') ?? ''" />
          <el-step title="支付成功" :description="order.paidAt?.replace('T', ' ') ?? '待支付'" />
          <el-step title="商家发货" :description="allDispatched ? shippedAt : '待商家发货'" />
          <el-step title="交易完成" :description="order.status === 'COMPLETED' ? '已确认收货' : '待确认收货'" />
        </el-steps>

        <div v-if="order.paidAt" class="invoice-bar">
          <template v-if="invoice">
            <span class="muted">
              发票：{{ invoice.title }}（{{ invoice.type === 'COMPANY' ? '单位' : '个人' }}）
              ¥{{ Number(invoice.amount).toFixed(2) }}
            </span>
            <el-tag
              size="small"
              effect="light"
              :type="invoice.status === 'ISSUED' ? 'success' : 'warning'"
            >
              {{ invoiceStatusLabels[invoice.status] || invoice.status }}
            </el-tag>
          </template>
          <el-button v-else size="small" plain @click="openInvoiceApply">申请发票</el-button>
        </div>
      </div>

      <div class="page-card block">
        <div class="block-title">收货信息</div>
        <div class="receiver">
          {{ order.receiverName }}
          <span class="muted">{{ order.receiverPhone }}</span>
        </div>
        <div class="muted address">{{ order.receiverAddress }}</div>
        <div v-if="order.remark" class="remark">备注：{{ order.remark }}</div>
      </div>

      <div v-for="sub in order.subOrders" :key="sub.id" class="page-card block">
        <div class="sub-head">
          <span class="shop-name">{{ sub.shopName }}</span>
          <span class="muted">子订单号：{{ sub.subOrderNo }}</span>
          <el-tag size="small" :type="sub.status === 'REFUNDING' ? 'warning' : 'primary'" effect="plain">
            {{ subStatusLabels[sub.status] || sub.status }}
          </el-tag>
        </div>

        <div v-if="sub.shipCompany || sub.trackingNo" class="logistics">
          <span>物流公司：{{ sub.shipCompany }}</span>
          <span>运单号：{{ sub.trackingNo }}</span>
          <span class="muted">发货时间：{{ sub.shippedAt?.replace('T', ' ') }}</span>
          <el-button link type="primary" size="small" @click="openLogisticsTrace(sub)">
            查看物流轨迹
          </el-button>
        </div>

        <div v-if="latestAfterSale(sub.id)" class="after-sale">
          <div class="after-sale-head">
            <span class="after-sale-label">售后单</span>
            <span>{{ latestAfterSale(sub.id)?.afterSaleNo }}</span>
            <el-tag size="small" effect="plain">
              {{ afterSaleTypeLabels[latestAfterSale(sub.id)?.type || ''] || latestAfterSale(sub.id)?.type }}
            </el-tag>
            <el-tag
              size="small"
              :type="afterSaleTagType(latestAfterSale(sub.id)?.status || '')"
              effect="light"
            >
              {{ statusLabelsOfAfterSale(latestAfterSale(sub.id)?.status) }}
            </el-tag>
          </div>
          <div class="after-sale-line">退款原因：{{ latestAfterSale(sub.id)?.reason }}</div>
          <div
            v-if="latestAfterSale(sub.id)?.evidenceImages?.length"
            class="after-sale-line evidence-line"
          >
            <span>凭证：</span>
            <el-image
              v-for="image in latestAfterSale(sub.id)?.evidenceImages"
              :key="image"
              :src="image"
              :preview-src-list="latestAfterSale(sub.id)?.evidenceImages"
              preview-teleported
              fit="cover"
              class="evidence-image"
            />
          </div>

          <template v-if="latestAfterSale(sub.id)?.type === 'RETURN_REFUND'">
            <div v-if="latestAfterSale(sub.id)?.merchantRemark" class="after-sale-line">
              退货地址/说明：{{ latestAfterSale(sub.id)?.merchantRemark }}
            </div>
            <div v-if="latestAfterSale(sub.id)?.returnTrackingNo" class="after-sale-line">
              退货物流：{{ latestAfterSale(sub.id)?.returnCompany }}
              {{ latestAfterSale(sub.id)?.returnTrackingNo }}
              <span class="muted">（{{ latestAfterSale(sub.id)?.returnedAt?.replace('T', ' ') }}）</span>
            </div>
            <div
              v-else-if="latestAfterSale(sub.id)?.status === 'RETURNING'"
              class="after-sale-line muted"
            >
              请填写退货物流，商家确认收货后退款
            </div>
            <div v-if="latestAfterSale(sub.id)?.receivedAt" class="after-sale-line muted">
              商家确认收货：{{ latestAfterSale(sub.id)?.receivedAt?.replace('T', ' ') }}
            </div>
          </template>
          <div v-else-if="latestAfterSale(sub.id)?.merchantRemark" class="after-sale-line muted">
            商家处理：{{ latestAfterSale(sub.id)?.merchantRemark }}
          </div>
          <div v-if="latestAfterSale(sub.id)?.adminRemark" class="after-sale-line admin-remark">
            平台介入：{{ latestAfterSale(sub.id)?.adminRemark }}
          </div>
          <div v-if="latestAfterSale(sub.id)?.handledAt" class="after-sale-line muted">
            处理时间：{{ latestAfterSale(sub.id)?.handledAt?.replace('T', ' ') }}
          </div>
        </div>

        <div v-for="item in sub.items" :key="item.id" class="order-item">
          <el-image :src="item.image" fit="cover" class="thumb">
            <template #error>
              <div class="img-fallback">无图</div>
            </template>
          </el-image>
          <div class="item-info">
            <div class="item-name">{{ item.productName }}</div>
            <div class="muted">{{ item.skuDesc }}</div>
            <div v-if="reviewOf(item.id)" class="item-review">
              <el-rate :model-value="reviewOf(item.id)?.rating" disabled size="small" />
              <span v-if="reviewOf(item.id)?.content" class="muted">{{ reviewOf(item.id)?.content }}</span>
              <div v-if="reviewOf(item.id)?.images?.length" class="review-images">
                <el-image
                  v-for="img in reviewOf(item.id)?.images ?? []"
                  :key="img"
                  :src="img"
                  :preview-src-list="reviewOf(item.id)?.images ?? []"
                  fit="cover"
                  class="review-image"
                />
              </div>
              <div v-if="reviewOf(item.id)?.reply" class="review-reply">
                商家回复：{{ reviewOf(item.id)?.reply }}
              </div>
              <div v-if="reviewOf(item.id)?.appendContent || reviewOf(item.id)?.appendImages?.length" class="review-append">
                <div class="append-label">我的追评</div>
                <div v-if="reviewOf(item.id)?.appendContent" class="muted">
                  {{ reviewOf(item.id)?.appendContent }}
                </div>
                <div v-if="reviewOf(item.id)?.appendImages?.length" class="review-images">
                  <el-image
                    v-for="img in reviewOf(item.id)?.appendImages ?? []"
                    :key="img"
                    :src="img"
                    :preview-src-list="reviewOf(item.id)?.appendImages ?? []"
                    fit="cover"
                    class="review-image"
                  />
                </div>
              </div>
              <el-button
                v-else
                link
                type="primary"
                size="small"
                class="append-btn"
                @click="openAppend(reviewOf(item.id)!)"
              >
                追评
              </el-button>
            </div>
            <div v-else-if="sub.status === 'COMPLETED'" class="item-review">
              <el-button link type="primary" size="small" @click="openReview(item)">评价商品</el-button>
            </div>
          </div>
          <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
          <div class="price item-total">¥{{ item.totalPrice.toFixed(2) }}</div>
        </div>

        <div class="sub-total">
          <span v-if="sub.discountAmount > 0" class="discount">
            优惠券「{{ sub.couponTitle || '店铺优惠' }}」-¥{{ sub.discountAmount.toFixed(2) }}
          </span>
          小计：<span class="price">¥{{ sub.totalAmount.toFixed(2) }}</span>
        </div>

        <div v-if="canApplyRefund(sub) || needReturnShipment(sub.id)" class="sub-actions">
          <el-button
            v-if="needReturnShipment(sub.id)"
            size="small"
            type="primary"
            plain
            @click="openReturnShipmentBySub(sub.id)"
          >
            填写退货物流
          </el-button>
          <el-button v-if="canApplyRefund(sub)" size="small" type="warning" plain @click="openRefund(sub)">
            {{ applyActionLabel(sub) }}
          </el-button>
        </div>
      </div>

      <div class="page-card submit-bar">
        <div class="summary">
          共 {{ itemCount }} 件商品，订单总额：
          <span class="price total">¥{{ order.totalAmount.toFixed(2) }}</span>
        </div>
        <div class="actions">
          <el-button v-if="order.status === 'PENDING_PAYMENT'" @click="cancel">取消订单</el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" @click="pay">去支付</el-button>
          <el-button v-if="allShipped" type="danger" @click="confirmReceipt">确认收货</el-button>
          <el-button :loading="reordering" @click="reorder">再来一单</el-button>
          <el-button v-if="order.status !== 'PENDING_PAYMENT'" @click="router.push('/')">继续购物</el-button>
        </div>
      </div>
    </template>

    <el-dialog
      v-model="logisticsDialogVisible"
      title="物流轨迹"
      width="520px"
    >
      <div v-loading="logisticsLoading" class="logistics-dialog">
        <LogisticsTimeline v-if="logisticsData" :logistics="logisticsData" />
      </div>
    </el-dialog>

    <el-dialog
      v-model="refundDialogVisible"
      :title="refundTarget && refundTarget.status !== 'WAIT_SHIP' ? '申请退货退款' : '申请退款'"
      width="480px"
    >
      <div v-if="refundTarget" class="refund-summary">
        子订单 {{ refundTarget.subOrderNo }}，退款金额
        <span class="price">¥{{ refundTarget.totalAmount.toFixed(2) }}</span>
      </div>
      <el-alert
        v-if="refundTarget && refundTarget.status !== 'WAIT_SHIP'"
        title="提交后需等待商家同意退货，再填写退货物流寄回，商家确认收货后退款"
        type="info"
        :closable="false"
        show-icon
        class="refund-tip"
      />
      <el-form label-width="80px">
        <el-form-item label="退款原因" required>
          <el-input
            v-model="refundReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明退款原因，方便商家处理"
          />
        </el-form-item>
        <el-form-item label="凭证图片">
          <div class="review-upload">
            <div v-for="(img, index) in refundImages" :key="img" class="review-upload-item">
              <el-image :src="img" fit="cover" class="review-upload-img" />
              <span class="review-upload-remove" @click="removeRefundImage(index)">×</span>
            </div>
            <div
              v-if="refundImages.length < 6"
              class="review-upload-add"
              :class="{ 'is-loading': refundUploading }"
              @click="pickRefundImages"
            >
              <span v-if="refundUploading">上传中</span>
              <span v-else>+ 上传</span>
            </div>
          </div>
          <div class="muted upload-tip">选填，最多 6 张，单张不超过 5MB</div>
          <input
            ref="refundInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            class="hidden-input"
            @change="onRefundImagesChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialogVisible" title="填写退货物流" width="460px">
      <div v-if="returnTarget" class="refund-summary">
        售后单 {{ returnTarget.afterSaleNo }}，退款金额 ¥{{ returnTarget.amount.toFixed(2) }}
        <div v-if="returnTarget.merchantRemark" class="muted return-address">
          退货地址：{{ returnTarget.merchantRemark }}
        </div>
      </div>
      <el-form label-width="90px">
        <el-form-item label="物流公司" required>
          <el-input v-model="returnForm.returnCompany" maxlength="50" placeholder="如：顺丰速运" />
        </el-form-item>
        <el-form-item label="退货运单号" required>
          <el-input v-model="returnForm.returnTrackingNo" maxlength="50" placeholder="请填写退货运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returnSubmitting" @click="submitReturnShipment">
          提交物流
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reorderDialogVisible" title="复购结果" width="520px">
      <div v-if="reorderResult">
        <div class="reorder-summary">
          已加入购物车 <span class="price">{{ reorderResult.addedCount }}</span> 款商品
          <template v-if="reorderResult.skippedCount > 0">
            ，<span class="reorder-skip">{{ reorderResult.skippedCount }}</span> 款不可购买
          </template>
        </div>
        <div v-if="reorderResult.skippedItems.length" class="reorder-skip-list">
          <div v-for="(item, index) in reorderResult.skippedItems" :key="index" class="reorder-skip-item">
            <div class="reorder-name">{{ item.productName }}</div>
            <div class="muted">{{ item.skuDesc }}</div>
            <div class="reorder-reason">{{ item.reason }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="reorderDialogVisible = false">继续逛逛</el-button>
        <el-button v-if="reorderResult?.addedCount" type="primary" @click="goCart">去购物车</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="评价商品" width="460px">
      <div v-if="reviewTarget" class="refund-summary">
        {{ reviewTarget.productName }}
        <span class="muted">{{ reviewTarget.skuDesc }}</span>
      </div>
      <el-form label-width="70px">
        <el-form-item label="评分" required>
          <el-rate v-model="reviewForm.rating" show-text :texts="['很差', '较差', '一般', '满意', '超赞']" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说商品的使用感受（可不填）"
          />
        </el-form-item>
        <el-form-item label="图片">
          <div class="review-upload">
            <div v-for="(img, index) in reviewForm.images" :key="img" class="review-upload-item">
              <el-image :src="img" fit="cover" class="review-upload-img" />
              <span class="review-upload-remove" @click="removeReviewImage(index)">×</span>
            </div>
            <div
              v-if="reviewForm.images.length < 6"
              class="review-upload-add"
              :class="{ 'is-loading': reviewUploading }"
              @click="pickReviewImages"
            >
              <span v-if="reviewUploading">上传中</span>
              <span v-else>+ 上传</span>
            </div>
          </div>
          <div class="muted upload-tip">最多 6 张，单张不超过 5MB</div>
          <input
            ref="reviewInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            class="hidden-input"
            @change="onReviewImagesChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
          提交评价
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appendDialogVisible" title="追加评价" width="460px">
      <el-form label-width="70px">
        <el-form-item label="追评">
          <el-input
            v-model="appendForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="使用一段时间后的感受（与图片至少填写一项）"
          />
        </el-form-item>
        <el-form-item label="图片">
          <div class="review-upload">
            <div v-for="(img, index) in appendForm.images" :key="img" class="review-upload-item">
              <el-image :src="img" fit="cover" class="review-upload-img" />
              <span class="review-upload-remove" @click="removeAppendImage(index)">×</span>
            </div>
            <div
              v-if="appendForm.images.length < 6"
              class="review-upload-add"
              :class="{ 'is-loading': appendUploading }"
              @click="pickAppendImages"
            >
              <span v-if="appendUploading">上传中</span>
              <span v-else>+ 上传</span>
            </div>
          </div>
          <input
            ref="appendInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            class="hidden-input"
            @change="onAppendImagesChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appendDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="appendSubmitting" @click="submitAppend">提交追评</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invoiceDialogVisible" title="申请发票" width="460px">
      <el-form label-width="80px">
        <el-form-item label="发票类型">
          <el-radio-group v-model="invoiceForm.type">
            <el-radio value="PERSONAL">个人</el-radio>
            <el-radio value="COMPANY">单位</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发票抬头" required>
          <el-input v-model="invoiceForm.title" maxlength="100" placeholder="个人姓名或单位名称" />
        </el-form-item>
        <el-form-item v-if="invoiceForm.type === 'COMPANY'" label="税号" required>
          <el-input v-model="invoiceForm.taxNo" maxlength="50" placeholder="单位纳税人识别号" />
        </el-form-item>
        <el-form-item label="接收邮箱">
          <el-input v-model="invoiceForm.email" maxlength="100" placeholder="电子发票接收邮箱（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="invoiceSubmitting" @click="submitInvoice">提交申请</el-button>
      </template>
    </el-dialog>

    <PaymentDialog
      v-model="payVisible"
      :order-id="order?.id ?? 0"
      :amount="order?.totalAmount ?? 0"
      @paid="handlePaid"
    />
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.block {
  margin-bottom: 16px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.status-text {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
}

.steps {
  margin-top: 8px;
}

.receiver {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.address {
  margin-top: 6px;
}

.remark {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.sub-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f2f5;
}

.shop-name {
  font-weight: 600;
}

.logistics {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  margin-top: 12px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.logistics-dialog {
  min-height: 120px;
}

.after-sale {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #f3d19e;
  background: #fdf6ec;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.after-sale-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.after-sale-label {
  font-weight: 600;
  color: #b88230;
}

.after-sale-line {
  line-height: 1.8;
}

.evidence-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.evidence-image {
  width: 52px;
  height: 52px;
  border-radius: 4px;
}

.admin-remark {
  color: #c45656;
}

.sub-actions {
  margin-top: 12px;
  text-align: right;
}

.refund-summary {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
}

.reorder-summary {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.reorder-skip {
  color: #e6a23c;
  font-weight: 600;
}

.reorder-skip-list {
  max-height: 260px;
  overflow-y: auto;
}

.reorder-skip-item {
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 13px;
}

.reorder-name {
  font-weight: 600;
  color: #303133;
}

.reorder-reason {
  margin-top: 2px;
  color: #f56c6c;
}

.refund-tip {
  margin-bottom: 16px;
}

.return-address {
  margin-top: 6px;
  font-size: 13px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}

.thumb {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-weight: 600;
  font-size: 14px;
}

.item-review {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
}

.review-reply {
  width: 100%;
  padding: 6px 10px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
}

.review-append {
  width: 100%;
  padding: 6px 10px;
  border-left: 3px solid #c81623;
  border-radius: 4px;
  background: #fffafa;
}

.append-label {
  font-size: 12px;
  color: #c81623;
  margin-bottom: 2px;
}

.append-btn {
  align-self: flex-start;
}

.item-price {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.item-total {
  width: 110px;
  text-align: right;
}

.sub-total {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.sub-total .discount {
  margin-right: 12px;
  color: #c81623;
  font-size: 13px;
}

.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.total {
  font-size: 24px;
}

.invoice-bar {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.review-image {
  width: 56px;
  height: 56px;
  border-radius: 4px;
}

.review-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-upload-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 6px;
  overflow: hidden;
}

.review-upload-img {
  width: 100%;
  height: 100%;
  display: block;
}

.review-upload-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  line-height: 16px;
  text-align: center;
  font-size: 14px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-bottom-left-radius: 6px;
  cursor: pointer;
}

.review-upload-add {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  background: #fafafa;
}

.review-upload-add.is-loading {
  cursor: wait;
}

.upload-tip {
  width: 100%;
  margin-top: 6px;
  font-size: 12px;
}

.hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .submit-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}
</style>
