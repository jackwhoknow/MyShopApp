<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAfterSaleApi } from '@/api/adminAfterSale'
import type { AfterSale } from '@/types'

const statusTabs = [
  { label: '待处理', value: 'PENDING' },
  { label: '退货中', value: 'RETURNING' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '全部', value: '' }
]

const statusLabels: Record<string, string> = {
  PENDING: '待处理',
  RETURNING: '退货中',
  REFUNDED: '已退款',
  CLOSED: '已关闭',
  REJECTED: '已驳回'
}

const statusTypes: Record<string, 'warning' | 'primary' | 'success' | 'danger' | 'info'> = {
  PENDING: 'warning',
  RETURNING: 'primary',
  REFUNDED: 'success',
  CLOSED: 'info',
  REJECTED: 'danger'
}

const typeLabels: Record<string, string> = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款'
}

const activeStatus = ref('')
const afterSales = ref<AfterSale[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const searchForm = reactive<{ keyword: string; dateRange: string[] | null }>({
  keyword: '',
  dateRange: null
})

const detailVisible = ref(false)
const detail = ref<AfterSale>()

const actionVisible = ref(false)
const actionSubmitting = ref(false)
const actionType = ref<'refund' | 'close'>('refund')
const actionTarget = ref<AfterSale>()
const actionForm = reactive({ remark: '' })

const isEmpty = computed(() => !loading.value && afterSales.value.length === 0)

async function load() {
  loading.value = true
  try {
    const result = await adminAfterSaleApi.list({
      status: activeStatus.value || undefined,
      keyword: searchForm.keyword.trim() || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
      page: page.value,
      size: size.value
    })
    afterSales.value = result.list
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

function itemCount(item: AfterSale) {
  return item.items.reduce((sum, orderItem) => sum + orderItem.quantity, 0)
}

function canIntervene(item: AfterSale) {
  return item.status === 'PENDING' || item.status === 'RETURNING'
}

function openDetail(item: AfterSale) {
  detail.value = item
  detailVisible.value = true
}

async function openAction(item: AfterSale, type: 'refund' | 'close') {
  actionType.value = type
  actionTarget.value = item
  actionForm.remark = ''
  if (type === 'refund') {
    try {
      await ElMessageBox.confirm(
        `平台介入退款将直接退回 ¥${item.amount.toFixed(2)} 并回补库存，确定继续吗？`,
        '平台介入退款',
        { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  actionVisible.value = true
}

async function submitAction() {
  if (!actionTarget.value) return
  const remark = actionForm.remark.trim()
  if (!remark) {
    ElMessage.warning('请填写平台介入说明')
    return
  }
  actionSubmitting.value = true
  try {
    if (actionType.value === 'refund') {
      await adminAfterSaleApi.refund(actionTarget.value.id, { remark })
      ElMessage.success('平台已介入并完成退款')
    } else {
      await adminAfterSaleApi.close(actionTarget.value.id, { remark })
      ElMessage.success('平台已关闭该售后')
    }
    actionVisible.value = false
    load()
  } finally {
    actionSubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">售后监管</h2>

    <div class="page-card search-bar">
      <el-input
        v-model="searchForm.keyword"
        class="search-keyword"
        clearable
        placeholder="售后单号 / 子订单号 / 主订单号 / 店铺 / 买家 / 退款原因"
        @keyup.enter="handleSearch"
      />
      <el-date-picker
        v-model="searchForm.dateRange"
        class="search-date"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="申请开始日期"
        end-placeholder="申请结束日期"
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

    <div class="page-card table-card" v-loading="loading">
      <el-table :data="afterSales" empty-text="暂无售后单">
        <el-table-column label="售后单号" min-width="210">
          <template #default="{ row }">
            <div class="strong">{{ row.afterSaleNo }}</div>
            <div class="muted">申请时间：{{ row.createdAt?.replace('T', ' ') }}</div>
            <div class="muted">共 {{ itemCount(row) }} 件商品</div>
          </template>
        </el-table-column>
        <el-table-column label="子订单 / 主订单" min-width="200">
          <template #default="{ row }">
            <div>{{ row.subOrderNo }}</div>
            <div class="muted">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="店铺 / 买家" min-width="150">
          <template #default="{ row }">
            <div>{{ row.shopName }}</div>
            <div class="muted">{{ row.buyerUsername }}</div>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ row.amount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型 / 状态" width="150">
          <template #default="{ row }">
            <div class="tag-line">
              <el-tag size="small" effect="plain">{{ typeLabels[row.type] || row.type }}</el-tag>
              <el-tag :type="statusTypes[row.status] || 'info'" effect="light" size="small">
                {{ statusLabels[row.status] || row.status }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="平台介入" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.adminRemark" type="danger" effect="plain" size="small">平台已介入</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="canIntervene(row)">
              <el-button link type="danger" @click="openAction(row, 'refund')">介入退款</el-button>
              <el-button link type="warning" @click="openAction(row, 'close')">关闭售后</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isEmpty" class="empty-block">
        <el-empty description="暂无售后单" />
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

    <el-dialog v-model="detailVisible" title="售后详情" width="760px" top="5vh">
      <div v-if="detail" class="detail-body">
        <div class="detail-head">
          <div>
            <span class="strong">{{ detail.afterSaleNo }}</span>
            <el-tag size="small" effect="plain" class="gap-left">
              {{ typeLabels[detail.type] || detail.type }}
            </el-tag>
            <el-tag :type="statusTypes[detail.status] || 'info'" effect="light" size="small" class="gap-left">
              {{ statusLabels[detail.status] || detail.status }}
            </el-tag>
            <el-tag v-if="detail.adminRemark" type="danger" effect="plain" size="small" class="gap-left">
              平台已介入
            </el-tag>
          </div>
          <span class="price">¥{{ detail.amount.toFixed(2) }}</span>
        </div>

        <div class="detail-meta">
          <span>子订单：{{ detail.subOrderNo }}</span>
          <span>主订单：{{ detail.orderNo }}</span>
          <span>店铺：{{ detail.shopName }}</span>
          <span>买家：{{ detail.buyerUsername }}</span>
        </div>

        <div class="detail-section">
          <div class="section-label">售后信息</div>
          <div class="detail-line">退款原因：{{ detail.reason }}</div>
          <div class="detail-line">申请时间：{{ detail.createdAt?.replace('T', ' ') }}</div>
          <div v-if="detail.merchantRemark" class="detail-line">商家备注：{{ detail.merchantRemark }}</div>
          <div v-if="detail.returnTrackingNo" class="detail-line">
            买家退货物流：{{ detail.returnCompany }} {{ detail.returnTrackingNo }}
            <span class="muted">（寄回时间：{{ detail.returnedAt?.replace('T', ' ') }}）</span>
          </div>
          <div v-if="detail.receivedAt" class="detail-line muted">
            商家确认收货：{{ detail.receivedAt.replace('T', ' ') }}
          </div>
          <div v-if="detail.handledAt" class="detail-line muted">
            处理时间：{{ detail.handledAt.replace('T', ' ') }}
          </div>
          <div v-if="detail.adminRemark" class="detail-line admin-line">平台介入说明：{{ detail.adminRemark }}</div>
        </div>

        <div class="detail-section">
          <div class="section-label">售后商品</div>
          <div v-for="item in detail.items" :key="`${item.productId}-${item.skuId}`" class="detail-item">
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
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="actionVisible"
      :title="actionType === 'refund' ? '平台介入退款' : '平台介入关闭售后'"
      width="480px"
    >
      <div v-if="actionTarget" class="dialog-summary">
        售后单 {{ actionTarget.afterSaleNo }}，退款金额 ¥{{ actionTarget.amount.toFixed(2) }}
        <div class="muted">
          {{ actionType === 'refund' ? '退款后将回补商品库存。' : '关闭后不退款，子订单恢复原状态。' }}
        </div>
      </div>
      <el-form label-width="90px">
        <el-form-item :label="actionType === 'refund' ? '退款说明' : '关闭原因'" required>
          <el-input
            v-model="actionForm.remark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写平台介入的处理依据，买卖双方都会收到通知"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button
          :type="actionType === 'refund' ? 'danger' : 'primary'"
          :loading="actionSubmitting"
          @click="submitAction"
        >
          {{ actionType === 'refund' ? '确认退款' : '确认关闭' }}
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

.strong {
  font-weight: 600;
  color: #303133;
}

.tag-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.gap-left {
  margin-left: 6px;
}

.empty-block {
  padding: 24px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
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

.detail-line {
  line-height: 1.9;
  font-size: 13px;
  color: #606266;
}

.admin-line {
  margin-top: 6px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 6px;
  color: #c45656;
}

.detail-item {
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

.dialog-summary {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
}

@media (max-width: 768px) {
  .search-date {
    width: 100%;
  }
}
</style>
