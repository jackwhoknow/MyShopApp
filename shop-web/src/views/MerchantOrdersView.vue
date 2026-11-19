<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantOrderApi } from '@/api/merchantOrder'
import LogisticsTimeline from '@/components/LogisticsTimeline.vue'
import type { MerchantSubOrder, SubOrderLogistics } from '@/types'

const statusTabs = [
  { label: '待发货', value: 'WAIT_SHIP' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '全部', value: '' }
]

const statusLabels: Record<string, string> = {
  WAIT_SHIP: '待发货',
  REFUNDING: '退款中',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const statusTypes: Record<string, 'warning' | 'primary' | 'success' | 'info'> = {
  WAIT_SHIP: 'warning',
  REFUNDING: 'warning',
  SHIPPED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

const activeStatus = ref('WAIT_SHIP')
const orders = ref<MerchantSubOrder[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(5)
const loading = ref(false)

const searchForm = reactive<{ keyword: string; dateRange: string[] | null }>({
  keyword: '',
  dateRange: null
})

const isEmpty = computed(() => !loading.value && orders.value.length === 0)

const shipDialogVisible = ref(false)
const shipSubmitting = ref(false)
const shipTarget = ref<MerchantSubOrder>()
const shipMode = ref<'ship' | 'logistics'>('ship')
const shipForm = reactive({ shipCompany: '', trackingNo: '' })

const selectedIds = ref<number[]>([])
const batchDialogVisible = ref(false)
const batchSubmitting = ref(false)
const batchForm = reactive({ shipCompany: '', trackingNo: '' })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MerchantSubOrder>()
const remarkDraft = ref('')
const remarkSaving = ref(false)

const traceVisible = ref(false)
const traceLoading = ref(false)
const traceData = ref<SubOrderLogistics>()

/** 查看已发货子订单的物流轨迹（模拟数据） */
async function openTrace(subOrderId?: number) {
  const id = subOrderId ?? detail.value?.id
  if (!id) return
  traceVisible.value = true
  traceLoading.value = true
  traceData.value = undefined
  try {
    traceData.value = await merchantOrderApi.logistics(id)
  } finally {
    traceLoading.value = false
  }
}

const detailSteps = computed(() => {
  const order = detail.value
  if (!order) return []
  return [
    { title: '提交订单', time: order.createdAt },
    { title: '买家付款', time: order.paidAt },
    { title: '商家发货', time: order.shippedAt },
    { title: '交易完成', time: order.completedAt }
  ]
})

/** 待发货/退款中停在发货环节，已发货进到完成环节，已完成全部点亮 */
const detailActiveStep = computed(() => {
  switch (detail.value?.status) {
    case 'SHIPPED':
      return 3
    case 'COMPLETED':
      return 4
    default:
      return 2
  }
})

/** 只有待发货的订单可以勾选批量发货 */
const selectableOrders = computed(() => orders.value.filter((order) => order.status === 'WAIT_SHIP'))
const allSelected = computed(
  () => selectableOrders.value.length > 0 && selectableOrders.value.every((order) => selectedIds.value.includes(order.id))
)
const someSelected = computed(
  () => !allSelected.value && selectableOrders.value.some((order) => selectedIds.value.includes(order.id))
)

function clearSelection() {
  selectedIds.value = []
}

function toggleOrder(id: number, checked: boolean) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
  }
}

function toggleAll(checked: boolean) {
  selectedIds.value = checked ? selectableOrders.value.map((order) => order.id) : []
}

function openBatch() {
  if (!selectedIds.value.length) return
  batchForm.shipCompany = ''
  batchForm.trackingNo = ''
  batchDialogVisible.value = true
}

async function submitBatch() {
  if (!selectedIds.value.length) return
  if (!batchForm.shipCompany.trim() || !batchForm.trackingNo.trim()) {
    ElMessage.warning('请填写真实的物流公司与运单号')
    return
  }
  batchSubmitting.value = true
  try {
    const result = await merchantOrderApi.batchShip({
      subOrderIds: selectedIds.value,
      shipCompany: batchForm.shipCompany.trim(),
      trackingNo: batchForm.trackingNo.trim()
    })
    batchDialogVisible.value = false
    clearSelection()
    await load()
    if (result.failures.length) {
      const detail = result.failures
        .map((item) => `${item.subOrderNo || item.subOrderId}：${item.reason}`)
        .join('<br/>')
      await ElMessageBox.alert(detail, `发货成功 ${result.shipped} 单，失败 ${result.failures.length} 单`, {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '知道了'
      })
    } else {
      ElMessage.success(`批量发货成功 ${result.shipped} 单`)
    }
  } finally {
    batchSubmitting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const result = await merchantOrderApi.list({
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
  clearSelection()
  load()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.dateRange = null
  handleSearch()
}

function handleTabChange() {
  page.value = 0
  clearSelection()
  load()
}

function handlePage(current: number) {
  page.value = current
  clearSelection()
  load()
}

function itemCount(order: MerchantSubOrder) {
  return order.items.reduce((sum, item) => sum + item.quantity, 0)
}

function openShip(order: MerchantSubOrder) {
  shipTarget.value = order
  shipMode.value = 'ship'
  shipForm.shipCompany = ''
  shipForm.trackingNo = ''
  shipDialogVisible.value = true
}

function openLogistics(order: MerchantSubOrder) {
  shipTarget.value = order
  shipMode.value = 'logistics'
  shipForm.shipCompany = order.shipCompany || ''
  shipForm.trackingNo = order.trackingNo || ''
  shipDialogVisible.value = true
}

function openLogisticsFromDetail() {
  if (detail.value) openLogistics(detail.value)
}

async function submitShip() {
  if (!shipTarget.value) return
  if (shipMode.value === 'logistics' && !shipForm.shipCompany.trim() && !shipForm.trackingNo.trim()) {
    ElMessage.warning('请至少填写物流公司或运单号')
    return
  }
  if (shipMode.value === 'ship' && (!shipForm.shipCompany.trim() || !shipForm.trackingNo.trim())) {
    ElMessage.warning('请填写真实的物流公司与运单号')
    return
  }
  shipSubmitting.value = true
  try {
    if (shipMode.value === 'logistics') {
      const updated = await merchantOrderApi.updateLogistics(shipTarget.value.id, {
        shipCompany: shipForm.shipCompany || undefined,
        trackingNo: shipForm.trackingNo || undefined
      })
      syncListItem(updated)
      if (detail.value?.id === updated.id) detail.value = updated
      ElMessage.success('物流信息已更新')
    } else {
      await merchantOrderApi.ship(shipTarget.value.id, {
        shipCompany: shipForm.shipCompany.trim(),
        trackingNo: shipForm.trackingNo.trim()
      })
      ElMessage.success('发货成功')
    }
    shipDialogVisible.value = false
    load()
  } finally {
    shipSubmitting.value = false
  }
}

function syncListItem(updated: MerchantSubOrder) {
  const target = orders.value.find((order) => order.id === updated.id)
  if (target) target.merchantRemark = updated.merchantRemark
  if (target) {
    target.shipCompany = updated.shipCompany
    target.trackingNo = updated.trackingNo
  }
}

async function openDetail(order: MerchantSubOrder) {
  detail.value = order
  remarkDraft.value = order.merchantRemark || ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    const fresh = await merchantOrderApi.detail(order.id)
    detail.value = fresh
    remarkDraft.value = fresh.merchantRemark || ''
  } finally {
    detailLoading.value = false
  }
}

async function saveRemark() {
  if (!detail.value) return
  remarkSaving.value = true
  try {
    const updated = await merchantOrderApi.updateRemark(detail.value.id, remarkDraft.value)
    detail.value = updated
    remarkDraft.value = updated.merchantRemark || ''
    syncListItem(updated)
    ElMessage.success(updated.merchantRemark ? '备注已保存' : '备注已清空')
  } finally {
    remarkSaving.value = false
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

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">商家订单履约</h2>

    <div class="page-card search-bar">
      <el-input
        v-model="searchForm.keyword"
        class="search-keyword"
        clearable
        placeholder="子订单号 / 主订单号 / 收货人 / 手机号 / 商品名"
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
    </div>

    <el-tabs
      :model-value="activeStatus"
      class="status-tabs"
      @tab-change="(name: string) => { activeStatus = name; handleTabChange() }"
    >
      <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div v-loading="loading">
      <div v-if="activeStatus === 'WAIT_SHIP' && selectableOrders.length" class="page-card batch-bar">
        <el-checkbox :model-value="allSelected" :indeterminate="someSelected" @change="(v: string | number | boolean) => toggleAll(!!v)">
          全选
        </el-checkbox>
        <span class="muted">已选 {{ selectedIds.length }} 单</span>
        <el-button type="danger" size="small" :disabled="!selectedIds.length" @click="openBatch">
          批量发货
        </el-button>
      </div>

      <div v-for="order in orders" :key="order.id" class="page-card order-card">
        <div class="order-head">
          <div class="order-meta">
            <el-checkbox
              v-if="order.status === 'WAIT_SHIP'"
              class="card-check"
              :model-value="selectedIds.includes(order.id)"
              @change="(v: string | number | boolean) => toggleOrder(order.id, !!v)"
            />
            <span class="order-no">子订单号：{{ order.subOrderNo }}</span>
            <span class="muted">主订单号：{{ order.orderNo }}</span>
            <span class="muted">下单时间：{{ order.createdAt?.replace('T', ' ') }}</span>
          </div>
          <el-tag :type="statusTypes[order.status] || 'info'" effect="light">
            {{ statusLabels[order.status] || order.status }}
          </el-tag>
        </div>

        <div class="receiver">
          <span class="receiver-name">{{ order.receiverName }}</span>
          <span class="muted">{{ order.receiverPhone }}</span>
          <span class="muted">{{ order.receiverAddress }}</span>
        </div>
        <div v-if="order.remark" class="remark">买家备注：{{ order.remark }}</div>
        <div v-if="order.merchantRemark" class="remark merchant-remark">商家备注：{{ order.merchantRemark }}</div>

        <div v-for="item in order.items" :key="`${item.productId}-${item.skuId}`" class="order-item">
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

        <div class="order-foot">
          <div class="ship-info">
            <template v-if="order.shipCompany || order.trackingNo">
              <span>{{ order.shipCompany }}</span>
              <span class="muted">{{ order.trackingNo }}</span>
              <span class="muted">{{ order.shippedAt?.replace('T', ' ') }}</span>
            </template>
            <span v-else class="muted">共 {{ itemCount(order) }} 件商品</span>
          </div>
          <div class="foot-right">
            <div class="amount">
              <div v-if="order.discountAmount > 0" class="muted">
                优惠券 -¥{{ order.discountAmount.toFixed(2) }}
              </div>
              小计：<span class="price">¥{{ order.totalAmount.toFixed(2) }}</span>
            </div>
            <el-button size="small" @click="openDetail(order)">详情</el-button>
            <el-button v-if="order.status === 'SHIPPED'" size="small" type="primary" plain @click="openLogistics(order)">
              修改物流
            </el-button>
            <el-button v-if="order.status === 'WAIT_SHIP'" type="danger" size="small" @click="openShip(order)">
              发货
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="isEmpty" class="page-card empty-block">
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

    <el-dialog v-model="shipDialogVisible" :title="shipMode === 'logistics' ? '修改物流信息' : '订单发货'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="物流公司">
          <el-input
            v-model="shipForm.shipCompany"
            maxlength="50"
            :placeholder="shipMode === 'logistics' ? '不填则保留原物流公司' : '如：顺丰速运'"
          />
        </el-form-item>
        <el-form-item label="运单号">
          <el-input
            v-model="shipForm.trackingNo"
            maxlength="50"
            :placeholder="shipMode === 'logistics' ? '不填则保留原运单号' : '填写快递公司提供的真实运单号'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="submitShip">
          {{ shipMode === 'logistics' ? '保存修改' : '确认发货' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="订单详情" width="760px" top="6vh">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <div>
              子订单号：<span class="order-no">{{ detail.subOrderNo }}</span>
              <el-button link type="primary" size="small" @click="copyText(detail.subOrderNo)">复制</el-button>
            </div>
            <el-tag :type="statusTypes[detail.status] || 'info'" effect="light">
              {{ statusLabels[detail.status] || detail.status }}
            </el-tag>
          </div>
          <div class="detail-meta">
            <span>主订单号：{{ detail.orderNo }}</span>
            <span>店铺：{{ detail.shopName }}</span>
            <span>下单时间：{{ detail.createdAt?.replace('T', ' ') }}</span>
          </div>

          <el-steps
            :active="detailActiveStep"
            :process-status="detail.status === 'CANCELLED' ? 'error' : 'process'"
            simple
            class="detail-steps"
          >
            <el-step
              v-for="step in detailSteps"
              :key="step.title"
              :title="step.title"
              :description="step.time ? step.time.replace('T', ' ') : ''"
            />
          </el-steps>

          <div class="detail-section">
            <div class="section-label">收货信息</div>
            <div class="detail-receiver">
              <span class="receiver-name">{{ detail.receiverName }}</span>
              <span>{{ detail.receiverPhone }}</span>
              <span>{{ detail.receiverAddress }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-label">商品明细</div>
            <div v-for="item in detail.items" :key="`${item.productId}-${item.skuId}`" class="order-item">
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
            <div class="detail-total">
              共 {{ itemCount(detail) }} 件商品，合计：
              <span v-if="detail.discountAmount > 0" class="muted">
                （已用「{{ detail.couponTitle || '优惠券' }}」抵扣 ¥{{ detail.discountAmount.toFixed(2) }}）
              </span>
              <span class="price">¥{{ detail.totalAmount.toFixed(2) }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="section-label">物流信息</div>
            <div v-if="detail.shipCompany || detail.trackingNo" class="detail-logistics">
              <span>{{ detail.shipCompany || '未填写' }}</span>
              <span>运单号：{{ detail.trackingNo || '-' }}</span>
              <el-button v-if="detail.trackingNo" link type="primary" size="small" @click="copyText(detail.trackingNo)">
                复制单号
              </el-button>
              <span v-if="detail.shippedAt" class="muted">发货时间：{{ detail.shippedAt.replace('T', ' ') }}</span>
              <el-button link type="primary" size="small" @click="openTrace()">物流轨迹</el-button>
            </div>
            <span v-else class="muted">暂未发货</span>
          </div>

          <div v-if="detail.remark" class="detail-section">
            <div class="section-label">买家备注</div>
            <div class="remark">{{ detail.remark }}</div>
          </div>

          <div class="detail-section">
            <div class="section-label">商家备注（仅商家可见）</div>
            <el-input
              v-model="remarkDraft"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="例如：已电话确认收货时间"
            />
            <div class="remark-actions">
              <el-button size="small" type="primary" :loading="remarkSaving" @click="saveRemark">保存备注</el-button>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button v-if="detail?.status === 'SHIPPED'" type="primary" plain @click="openLogisticsFromDetail">
          修改物流
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="traceVisible" title="物流轨迹" width="520px">
      <div v-loading="traceLoading" class="trace-body">
        <LogisticsTimeline v-if="traceData" :logistics="traceData" />
      </div>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量发货" width="460px">
      <div class="dialog-summary">
        已选 {{ selectedIds.length }} 个子订单，将使用同一包裹的真实运单号发货
      </div>
      <el-form label-width="80px">
        <el-form-item label="物流公司">
          <el-input v-model="batchForm.shipCompany" maxlength="50" placeholder="如：顺丰速运" />
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="batchForm.trackingNo" maxlength="50" placeholder="填写快递公司提供的真实运单号" />
        </el-form-item>
      </el-form>
      <div class="batch-tip">同一包裹内的多个子订单共用一个运单号，系统不会改写单号</div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="submitBatch">
          确认发货
        </el-button>
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
  flex: 1 1 280px;
  min-width: 220px;
}

.search-date {
  width: 280px;
}

.status-tabs {
  margin-bottom: 8px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 22px;
}

.card-check {
  margin-right: 4px;
}

.dialog-summary {
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.batch-tip {
  font-size: 12px;
  color: #909399;
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

.receiver {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  font-size: 14px;
}

.receiver-name {
  font-weight: 600;
}

.remark {
  margin-bottom: 8px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.merchant-remark {
  background: #fdf6ec;
  color: #b88230;
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

.item-price {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.item-total {
  width: 110px;
  text-align: right;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
}

.ship-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.foot-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.amount {
  font-size: 14px;
  color: #606266;
}

.amount .price {
  font-size: 18px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
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

.detail-steps {
  margin: 16px 0;
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

.detail-logistics {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.detail-total {
  padding-top: 12px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.remark-actions {
  margin-top: 8px;
  text-align: right;
}

@media (max-width: 768px) {
  .order-foot {
    flex-direction: column;
    align-items: flex-end;
  }

  .search-date {
    width: 100%;
  }
}
</style>
