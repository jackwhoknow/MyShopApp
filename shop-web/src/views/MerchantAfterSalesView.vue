<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantAfterSaleApi } from '@/api/afterSale'
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
  CLOSED: '平台已关闭',
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

const activeStatus = ref('PENDING')
const afterSales = ref<AfterSale[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(5)
const loading = ref(false)

const isEmpty = computed(() => !loading.value && afterSales.value.length === 0)

const rejectDialogVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectTarget = ref<AfterSale>()
const rejectForm = reactive({ merchantRemark: '' })

const approveDialogVisible = ref(false)
const approveSubmitting = ref(false)
const approveTarget = ref<AfterSale>()
const approveForm = reactive({ merchantRemark: '' })

async function load() {
  loading.value = true
  try {
    const result = await merchantAfterSaleApi.list({
      status: activeStatus.value || undefined,
      page: page.value,
      size: size.value
    })
    afterSales.value = result.list
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

function itemCount(item: AfterSale) {
  return item.items.reduce((sum, orderItem) => sum + orderItem.quantity, 0)
}

/** 售后备注的语义随状态变化 */
function remarkLabel(item: AfterSale) {
  if (item.status === 'CLOSED') return '平台关闭原因'
  if (item.type === 'RETURN_REFUND' && item.status === 'RETURNING') return '退货地址'
  if (item.status === 'REJECTED') return '驳回原因'
  return '处理备注'
}

/** 仅退款：二次确认后直接退款 */
async function approveRefund(item: AfterSale) {
  await ElMessageBox.confirm(
    `同意退款后将原路退回 ¥${item.amount.toFixed(2)}，商品库存回补。确定同意吗？`,
    '同意退款',
    { type: 'warning', confirmButtonText: '同意退款', cancelButtonText: '取消' }
  )
  await merchantAfterSaleApi.approve(item.id, { merchantRemark: '商家同意退款' })
  ElMessage.success('已同意退款')
  load()
}

/** 退货退款：商家同意后进入退货中，需要填写退货地址 */
function openApproveReturn(item: AfterSale) {
  approveTarget.value = item
  approveForm.merchantRemark = ''
  approveDialogVisible.value = true
}

async function submitApproveReturn() {
  if (!approveTarget.value) return
  if (!approveForm.merchantRemark.trim()) {
    ElMessage.warning('请填写退货地址或退货说明')
    return
  }
  approveSubmitting.value = true
  try {
    await merchantAfterSaleApi.approve(approveTarget.value.id, {
      merchantRemark: approveForm.merchantRemark.trim()
    })
    ElMessage.success('已同意退货，等待买家寄回')
    approveDialogVisible.value = false
    load()
  } finally {
    approveSubmitting.value = false
  }
}

/** 退货中：确认收到退货后退款 */
async function confirmReceipt(item: AfterSale) {
  if (!item.returnTrackingNo) {
    ElMessage.warning('买家还未填写退货物流，暂不能确认收货')
    return
  }
  await ElMessageBox.confirm(
    `确认已收到买家退货吗？确认后退款 ¥${item.amount.toFixed(2)} 并回补库存。`,
    '确认收货并退款',
    { type: 'warning', confirmButtonText: '确认收货', cancelButtonText: '取消' }
  )
  await merchantAfterSaleApi.confirmReceipt(item.id)
  ElMessage.success('已确认收货，退款完成')
  load()
}

function openReject(item: AfterSale) {
  rejectTarget.value = item
  rejectForm.merchantRemark = ''
  rejectDialogVisible.value = true
}

async function submitReject() {
  if (!rejectTarget.value) return
  if (!rejectForm.merchantRemark.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  rejectSubmitting.value = true
  try {
    await merchantAfterSaleApi.reject(rejectTarget.value.id, {
      merchantRemark: rejectForm.merchantRemark.trim()
    })
    ElMessage.success('已驳回售后申请')
    rejectDialogVisible.value = false
    load()
  } finally {
    rejectSubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">商家售后</h2>

    <el-tabs
      :model-value="activeStatus"
      class="status-tabs"
      @tab-change="(name: string) => { activeStatus = name; handleTabChange() }"
    >
      <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div v-loading="loading">
      <div v-for="item in afterSales" :key="item.id" class="page-card after-sale-card">
        <div class="card-head">
          <div class="meta">
            <span class="strong">售后单号：{{ item.afterSaleNo }}</span>
            <span class="muted">子订单号：{{ item.subOrderNo }}</span>
            <span class="muted">主订单号：{{ item.orderNo }}</span>
            <span class="muted">申请时间：{{ item.createdAt?.replace('T', ' ') }}</span>
          </div>
          <div class="tags">
            <el-tag v-if="item.adminRemark" type="danger" effect="plain" size="small">平台介入</el-tag>
            <el-tag size="small" effect="plain">{{ typeLabels[item.type] || item.type }}</el-tag>
            <el-tag :type="statusTypes[item.status] || 'info'" effect="light">
              {{ statusLabels[item.status] || item.status }}
            </el-tag>
          </div>
        </div>

        <div class="reason">
          <div class="reason-line">退款原因：{{ item.reason }}</div>
          <div v-if="item.evidenceImages?.length" class="reason-line evidence-line">
            <span>买家凭证：</span>
            <el-image
              v-for="image in item.evidenceImages"
              :key="image"
              :src="image"
              :preview-src-list="item.evidenceImages"
              preview-teleported
              fit="cover"
              class="evidence-image"
            />
          </div>
          <div v-if="item.merchantRemark" class="reason-line">
            {{ remarkLabel(item) }}：{{ item.merchantRemark }}
          </div>
          <div v-if="item.returnTrackingNo" class="reason-line">
            买家退货物流：{{ item.returnCompany }} {{ item.returnTrackingNo }}
            <span class="muted">（寄回时间：{{ item.returnedAt?.replace('T', ' ') }}）</span>
          </div>
          <div v-else-if="item.status === 'RETURNING'" class="reason-line muted">
            等待买家填写退货物流
          </div>
          <div v-if="item.adminRemark" class="reason-line admin-line">
            平台介入说明：{{ item.adminRemark }}
          </div>
          <div v-if="item.receivedAt" class="reason-line muted">
            确认收货时间：{{ item.receivedAt.replace('T', ' ') }}
          </div>
          <div v-else-if="item.handledAt" class="reason-line muted">
            处理时间：{{ item.handledAt.replace('T', ' ') }}
          </div>
        </div>

        <div v-for="orderItem in item.items" :key="`${orderItem.productId}-${orderItem.skuId}`" class="after-sale-item">
          <el-image :src="orderItem.image" fit="cover" class="thumb">
            <template #error>
              <div class="img-fallback">无图</div>
            </template>
          </el-image>
          <div class="item-info">
            <div class="item-name">{{ orderItem.productName }}</div>
            <div class="muted">{{ orderItem.skuDesc }}</div>
          </div>
          <div class="item-price">¥{{ orderItem.price.toFixed(2) }} × {{ orderItem.quantity }}</div>
          <div class="price item-total">¥{{ orderItem.totalPrice.toFixed(2) }}</div>
        </div>

        <div class="card-foot">
          <span class="muted">共 {{ itemCount(item) }} 件商品</span>
          <div class="foot-right">
            <span class="amount">退款金额：<span class="price">¥{{ item.amount.toFixed(2) }}</span></span>
            <template v-if="item.status === 'PENDING'">
              <el-button size="small" @click="openReject(item)">驳回</el-button>
              <el-button
                v-if="item.type === 'RETURN_REFUND'"
                size="small"
                type="primary"
                @click="openApproveReturn(item)"
              >
                同意退货
              </el-button>
              <el-button v-else size="small" type="danger" @click="approveRefund(item)">
                同意退款
              </el-button>
            </template>
            <el-button
              v-if="item.status === 'RETURNING'"
              size="small"
              type="danger"
              @click="confirmReceipt(item)"
            >
              确认收货并退款
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="isEmpty" class="page-card empty-block">
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

    <el-dialog v-model="approveDialogVisible" title="同意退货" width="460px">
      <div v-if="approveTarget" class="dialog-summary">
        售后单 {{ approveTarget.afterSaleNo }}，退款金额 ¥{{ approveTarget.amount.toFixed(2) }}
      </div>
      <el-form label-width="90px">
        <el-form-item label="退货地址" required>
          <el-input
            v-model="approveForm.merchantRemark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写退货地址及收件人信息，买家会在此页看到"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="approveSubmitting" @click="submitApproveReturn">
          同意退货
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="驳回售后申请" width="460px">
      <div v-if="rejectTarget" class="dialog-summary">
        售后单 {{ rejectTarget.afterSaleNo }}，退款金额 ¥{{ rejectTarget.amount.toFixed(2) }}
      </div>
      <el-form label-width="80px">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.merchantRemark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明驳回原因，买家可在订单详情看到"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectSubmitting" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.status-tabs {
  margin-bottom: 8px;
}

.after-sale-card {
  margin-bottom: 16px;
  padding: 18px 22px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
}

.tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.strong {
  font-weight: 600;
  color: #303133;
}

.reason {
  margin: 12px 0;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.reason-line {
  line-height: 1.8;
}

.evidence-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.evidence-image {
  width: 56px;
  height: 56px;
  border-radius: 4px;
}

.admin-line {
  color: #c45656;
}

.after-sale-item {
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

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  font-size: 13px;
}

.foot-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.amount {
  font-size: 14px;
  color: #606266;
}

.amount .price {
  font-size: 18px;
}

.dialog-summary {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .card-foot {
    flex-direction: column;
    align-items: flex-end;
  }
}
</style>
