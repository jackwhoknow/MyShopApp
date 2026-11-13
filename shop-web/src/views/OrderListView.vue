<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'
import PaymentDialog from '@/components/PaymentDialog.vue'
import type { Order } from '@/types'

const router = useRouter()

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待发货', value: 'PAID' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const statusLabels: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '待发货',
  REFUNDING: '退款中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const statusTypes: Record<string, 'warning' | 'primary' | 'success' | 'info'> = {
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  REFUNDING: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

const activeStatus = ref('')
const orders = ref<Order[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(5)
const loading = ref(false)

const isEmpty = computed(() => !loading.value && orders.value.length === 0)

async function load() {
  loading.value = true
  try {
    const result = await orderApi.list({
      status: activeStatus.value || undefined,
      page: page.value,
      size: size.value
    })
    orders.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 0
  load()
}

function handlePage(current: number) {
  page.value = current
  load()
}

function itemCount(order: Order) {
  return order.subOrders.reduce(
    (sum, sub) => sum + sub.items.reduce((count, item) => count + item.quantity, 0),
    0
  )
}

/** 已支付且所有子订单都已发货时显示“待收货”，有退款中显示“退款中” */
function orderStatusLabel(order: Order) {
  if (order.status === 'PAID' && order.subOrders.some((sub) => sub.status === 'REFUNDING')) {
    return '退款中'
  }
  if (
    order.status === 'PAID' &&
    order.subOrders.length > 0 &&
    order.subOrders.every((sub) => sub.status === 'SHIPPED')
  ) {
    return '待收货'
  }
  return statusLabels[order.status] || order.status
}

function orderStatusType(order: Order): 'warning' | 'primary' | 'success' | 'info' {
  if (orderStatusLabel(order) === '退款中') return 'warning'
  return statusTypes[order.status] || 'info'
}

/** 打开收银台：渠道选择与支付结果轮询统一在 PaymentDialog 内完成 */
const payTarget = ref<Order>()
const payVisible = ref(false)

function pay(order: Order) {
  payTarget.value = order
  payVisible.value = true
}

async function handlePaid() {
  await load()
}

async function cancel(order: Order) {
  await ElMessageBox.confirm('取消后库存会回补，确定取消该订单吗？', '取消订单', {
    type: 'warning',
    confirmButtonText: '确定取消',
    cancelButtonText: '再想想'
  })
  await orderApi.cancel(order.id)
  ElMessage.success('订单已取消')
  load()
}

/** 一键复购：按当前库存把订单商品加入购物车 */
const reorderingId = ref<number>()

async function reorder(order: Order) {
  reorderingId.value = order.id
  try {
    const result = await orderApi.reorder(order.id)
    if (result.addedCount === 0) {
      ElMessage.warning(result.skippedItems[0]?.reason || '商品暂不可购买')
      return
    }
    if (result.skippedCount > 0) {
      ElMessage.warning(
        `已加入 ${result.addedCount} 款商品，${result.skippedCount} 款已下架或库存不足被跳过`
      )
    } else {
      ElMessage.success(`已将 ${result.addedCount} 款商品加入购物车`)
    }
    router.push('/cart')
  } finally {
    reorderingId.value = undefined
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">我的订单</h2>

    <el-tabs :model-value="activeStatus" class="status-tabs" @tab-change="(name: string) => { activeStatus = name; handleTabChange() }">
      <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div v-loading="loading">
      <div v-for="order in orders" :key="order.id" class="page-card order-card">
        <div class="order-head">
          <div class="order-meta">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="muted">{{ order.createdAt?.replace('T', ' ') }}</span>
          </div>
          <el-tag :type="orderStatusType(order)" effect="light">
            {{ orderStatusLabel(order) }}
          </el-tag>
        </div>

        <div class="order-body" @click="router.push(`/orders/${order.id}`)">
          <div class="thumbs">
            <el-image
              v-for="item in order.subOrders.flatMap((sub) => sub.items).slice(0, 4)"
              :key="`${item.productId}-${item.skuId}`"
              :src="item.image"
              fit="cover"
              class="thumb"
            >
              <template #error>
                <div class="img-fallback">无图</div>
              </template>
            </el-image>
          </div>
          <div class="order-desc">
            <div class="shop-names muted">
              {{ order.subOrders.map((sub) => sub.shopName).join('、') }}
            </div>
            <div class="item-titles">
              {{ order.subOrders.flatMap((sub) => sub.items).map((item) => item.productName).join(' / ') }}
            </div>
            <div class="muted">共 {{ itemCount(order) }} 件</div>
          </div>
        </div>

        <div class="order-foot">
          <div class="amount">
            实付：<span class="price">¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="actions">
            <el-button size="small" @click="router.push(`/orders/${order.id}`)">查看详情</el-button>
            <el-button size="small" :loading="reorderingId === order.id" @click="reorder(order)">
              再来一单
            </el-button>
            <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" size="small" @click="pay(order)">
              去支付
            </el-button>
            <el-button v-if="order.status === 'PENDING_PAYMENT'" size="small" @click="cancel(order)">
              取消订单
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="isEmpty" class="page-card empty-block">
        <el-empty description="还没有订单">
          <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
        </el-empty>
      </div>

      <div v-if="total > size" class="pagination">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="(current: number) => handlePage(current - 1)"
        />
      </div>
    </div>

    <PaymentDialog
      v-model="payVisible"
      :order-id="payTarget?.id ?? 0"
      :amount="payTarget?.totalAmount ?? 0"
      @paid="handlePaid"
    />
  </div>
</template>

<style scoped>
.status-tabs {
  margin-bottom: 8px;
}

.order-card {
  margin-bottom: 16px;
  padding: 18px 22px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
}

.order-no {
  font-weight: 600;
  color: #303133;
}

.order-body {
  display: flex;
  gap: 18px;
  padding: 16px 0;
  cursor: pointer;
}

.thumbs {
  display: flex;
  gap: 8px;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
}

.order-desc {
  flex: 1;
  min-width: 0;
  font-size: 13px;
}

.item-titles {
  margin: 6px 0;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.amount {
  font-size: 14px;
  color: #606266;
}

.amount .price {
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .thumbs {
    display: none;
  }

  .order-foot {
    flex-direction: column;
    align-items: flex-end;
  }
}
</style>
