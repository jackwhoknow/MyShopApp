<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminOrderApi, type AdminOrderStatus } from '@/api/adminOrder'
import type { AdminOrder, AdminOrderDetail, AdminOrderSubOrderDetail } from '@/types'

const orderStatusLabels: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const orderStatusTypes: Record<string, 'warning' | 'primary' | 'success' | 'info'> = {
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

const subStatusLabels: Record<string, string> = {
  WAIT_SHIP: '待发货',
  REFUNDING: '退款中',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const subStatusTypes: Record<string, 'warning' | 'primary' | 'success' | 'info'> = {
  WAIT_SHIP: 'warning',
  REFUNDING: 'warning',
  SHIPPED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

const afterSaleStatusLabels: Record<string, string> = {
  PENDING: '待处理',
  RETURNING: '待退货',
  REFUNDED: '已退款',
  REJECTED: '已驳回',
  CLOSED: '平台已关闭'
}

const afterSaleTypeLabels: Record<string, string> = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款'
}

const statusTabs: { label: string; value: AdminOrderStatus | '' }[] = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '已支付', value: 'PAID' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const activeStatus = ref<AdminOrderStatus | ''>('')
const orders = ref<AdminOrder[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const searchForm = reactive<{ keyword: string; dateRange: string[] | null }>({
  keyword: '',
  dateRange: null
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminOrderDetail>()

const isEmpty = computed(() => !loading.value && orders.value.length === 0)

async function load() {
  loading.value = true
  try {
    const result = await adminOrderApi.list({
      status: activeStatus.value || undefined,
      keyword: searchForm.keyword.trim() || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
      page: page.value,
      size: size.value
    })
    orders.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 0
  load()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.dateRange = null
  handleSearch()
}

function handleTabChange() {
  page.value = 0
  load()
}

function handlePage(current: number) {
  page.value = current
  load()
}

/** 导出当前筛选条件下的订单（后端最多 10000 行） */
const exporting = ref(false)

async function exportCsv() {
  exporting.value = true
  try {
    const blob = await adminOrderApi.exportCsv({
      status: activeStatus.value || undefined,
      keyword: searchForm.keyword.trim() || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `orders-${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('订单已导出')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

function shopNames(order: AdminOrder) {
  return Array.from(new Set(order.subOrders.map((sub) => sub.shopName)))
}

function itemCount(order: AdminOrder) {
  return order.subOrders.reduce((sum, sub) => sum + sub.itemCount, 0)
}

/** 任意子订单有售后时展示最新售后状态 */
function afterSaleStatus(order: AdminOrder) {
  return order.subOrders.find((sub) => sub.afterSaleStatus)?.afterSaleStatus || ''
}

async function openDetail(order: AdminOrder) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  try {
    detail.value = await adminOrderApi.detail(order.id)
  } finally {
    detailLoading.value = false
  }
}

async function copyText(value?: string) {
  if (!value) return
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

function subItemCount(sub: AdminOrderSubOrderDetail) {
  return sub.items.reduce((sum, item) => sum + item.quantity, 0)
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">订单管理</h2>

    <div class="page-card search-bar">
      <el-input
        v-model="searchForm.keyword"
        class="search-keyword"
        clearable
        placeholder="订单号 / 子订单号 / 收货人 / 手机号 / 买家 / 店铺 / 商品名"
        @keyup.enter="handleSearch"
      />
      <el-date-picker
        v-model="searchForm.dateRange"
        class="search-date"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="下单开始日期"
        end-placeholder="下单结束日期"
        range-separator="至"
        unlink-panels
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button :loading="exporting" @click="exportCsv">导出 CSV</el-button>
    </div>

    <el-tabs
      :model-value="activeStatus"
      class="status-tabs"
      @tab-change="(name: string) => { activeStatus = name as AdminOrderStatus | ''; handleTabChange() }"
    >
      <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div class="page-card table-card" v-loading="loading">
      <el-table :data="orders" empty-text="暂无订单">
        <el-table-column label="订单信息" min-width="230">
          <template #default="{ row }">
            <div class="order-no">{{ row.orderNo }}</div>
            <div class="muted">{{ row.createdAt?.replace('T', ' ') }}</div>
            <div class="muted">
              买家：{{ row.buyerUsername }}
              <span v-if="row.buyerNickname">（{{ row.buyerNickname }}）</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="店铺 / 商品" min-width="200">
          <template #default="{ row }">
            <div class="shop-tags">
              <el-tag v-for="name in shopNames(row)" :key="name" size="small" effect="plain">
                {{ name }}
              </el-tag>
            </div>
            <div class="muted">共 {{ itemCount(row) }} 件商品</div>
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="170">
          <template #default="{ row }">
            <div>{{ row.receiverName }}</div>
            <div class="muted">{{ row.receiverPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ row.totalAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="orderStatusTypes[row.status] || 'info'" effect="light">
              {{ orderStatusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售后" width="110">
          <template #default="{ row }">
            <el-tag v-if="afterSaleStatus(row)" type="warning" effect="plain" size="small">
              {{ afterSaleStatusLabels[afterSaleStatus(row)] || afterSaleStatus(row) }}
            </el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isEmpty" class="empty-block">
        <el-empty description="暂无订单" />
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

    <el-dialog v-model="detailVisible" title="订单详情" width="820px" top="5vh">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              订单号：<span class="order-no">{{ detail.orderNo }}</span>
              <el-button link type="primary" size="small" @click="copyText(detail.orderNo)">复制</el-button>
            </div>
            <el-tag :type="orderStatusTypes[detail.status] || 'info'" effect="light">
              {{ orderStatusLabels[detail.status] || detail.status }}
            </el-tag>
          </div>

          <div class="detail-meta">
            <span>
              买家：{{ detail.buyerUsername }}
              <span v-if="detail.buyerNickname">（{{ detail.buyerNickname }}）</span>
            </span>
            <span>下单时间：{{ detail.createdAt?.replace('T', ' ') }}</span>
            <span>支付时间：{{ detail.paidAt?.replace('T', ' ') || '未支付' }}</span>
          </div>

          <div class="detail-section">
            <div class="section-label">收货信息</div>
            <div class="detail-receiver">
              <span class="receiver-name">{{ detail.receiverName }}</span>
              <span>{{ detail.receiverPhone }}</span>
              <span>{{ detail.receiverAddress }}</span>
            </div>
          </div>

          <div v-if="detail.remark" class="detail-section">
            <div class="section-label">买家备注</div>
            <div class="remark">{{ detail.remark }}</div>
          </div>

          <div class="detail-section">
            <div class="section-label">子订单（{{ detail.subOrders.length }}）</div>

            <div v-for="sub in detail.subOrders" :key="sub.id" class="sub-card">
              <div class="sub-head">
                <div class="sub-title">
                  <span class="order-no">{{ sub.subOrderNo }}</span>
                  <el-tag size="small" effect="plain">{{ sub.shopName }}</el-tag>
                  <el-tag :type="subStatusTypes[sub.status] || 'info'" effect="light" size="small">
                    {{ subStatusLabels[sub.status] || sub.status }}
                  </el-tag>
                  <el-tag v-if="sub.afterSale" type="warning" effect="plain" size="small">
                    售后：{{ afterSaleStatusLabels[sub.afterSale.status] || sub.afterSale.status }}
                  </el-tag>
                </div>
                <div class="muted">下单时间：{{ sub.createdAt?.replace('T', ' ') }}</div>
              </div>

              <div class="sub-logistics">
                <template v-if="sub.shipCompany || sub.trackingNo">
                  <span>{{ sub.shipCompany || '未填写物流公司' }}</span>
                  <span class="muted">运单号：{{ sub.trackingNo || '-' }}</span>
                  <span v-if="sub.shippedAt" class="muted">
                    发货时间：{{ sub.shippedAt.replace('T', ' ') }}
                  </span>
                </template>
                <span v-else class="muted">暂未发货</span>
              </div>

              <div v-for="item in sub.items" :key="`${sub.id}-${item.id}`" class="order-item">
                <el-image :src="item.image" fit="cover" class="thumb">
                  <template #error>
                    <div class="img-fallback">无图</div>
                  </template>
                </el-image>
                <div class="item-info">
                  <div class="item-name">{{ item.productName }}</div>
                  <div class="muted">{{ item.skuDesc }}</div>
                </div>
                <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
                <div class="price item-total">¥{{ item.totalPrice.toFixed(2) }}</div>
              </div>

              <div class="sub-total">
                共 {{ subItemCount(sub) }} 件商品
                <span v-if="sub.discountAmount > 0" class="muted">
                  ，已用「{{ sub.couponTitle || '优惠券' }}」抵扣 ¥{{ sub.discountAmount.toFixed(2) }}
                </span>
                ，小计：<span class="price">¥{{ sub.totalAmount.toFixed(2) }}</span>
              </div>

              <div v-if="sub.afterSale" class="after-sale">
                <div class="after-sale-head">
                  售后单 {{ sub.afterSale.afterSaleNo }}
                  <el-tag type="warning" effect="plain" size="small">
                    {{ afterSaleTypeLabels[sub.afterSale.type] || sub.afterSale.type }}
                  </el-tag>
                  <el-tag type="warning" effect="light" size="small">
                    {{ afterSaleStatusLabels[sub.afterSale.status] || sub.afterSale.status }}
                  </el-tag>
                </div>
                <div class="after-sale-body">
                  <span>退款金额：¥{{ sub.afterSale.amount.toFixed(2) }}</span>
                  <span>原因：{{ sub.afterSale.reason }}</span>
                  <span class="muted">申请时间：{{ sub.afterSale.createdAt?.replace('T', ' ') }}</span>
                  <span v-if="sub.afterSale.handledAt" class="muted">
                    处理时间：{{ sub.afterSale.handledAt.replace('T', ' ') }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 22px;
}

.search-keyword {
  flex: 1 1 300px;
  min-width: 240px;
}

.search-date {
  width: 280px;
}

.status-tabs {
  margin-bottom: 8px;
}

.table-card {
  margin-top: 4px;
}

.order-no {
  font-weight: 600;
  color: #303133;
}

.shop-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}

.empty-block {
  padding: 24px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-body {
  min-height: 120px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.detail-section {
  padding: 12px 0;
  border-top: 1px solid #f0f2f5;
}

.section-label {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.detail-receiver {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 14px;
}

.receiver-name {
  font-weight: 600;
}

.remark {
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.sub-card {
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.sub-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}

.sub-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.sub-logistics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 0;
  font-size: 13px;
  color: #606266;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.thumb {
  width: 52px;
  height: 52px;
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

.item-price {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.item-total {
  width: 100px;
  text-align: right;
}

.sub-total {
  padding-top: 10px;
  text-align: right;
  font-size: 13px;
  color: #606266;
}

.after-sale {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 6px;
  font-size: 13px;
}

.after-sale-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-weight: 600;
  color: #b88230;
}

.after-sale-body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #606266;
}

@media (max-width: 768px) {
  .search-date {
    width: 100%;
  }
}
</style>
