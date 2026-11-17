<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminSettlementApi } from '@/api/adminSettlement'
import type { AdminSettlementSummary, WithdrawalRequest } from '@/types'

const status = ref('')
const keyword = ref('')
const list = ref<WithdrawalRequest[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const summary = ref<AdminSettlementSummary>()

const statusLabels: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '待打款',
  REJECTED: '已驳回',
  PAID: '已打款'
}

const statusTypes: Record<string, 'warning' | 'primary' | 'danger' | 'success'> = {
  PENDING: 'warning',
  APPROVED: 'primary',
  REJECTED: 'danger',
  PAID: 'success'
}

function formatMoney(value?: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

async function load() {
  loading.value = true
  try {
    const [summaryData, result] = await Promise.all([
      adminSettlementApi.summary(),
      adminSettlementApi.withdrawals({
        status: status.value || undefined,
        keyword: keyword.value || undefined,
        page: page.value,
        size: size.value
      })
    ])
    summary.value = summaryData
    list.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 0
  load()
}

function reset() {
  status.value = ''
  keyword.value = ''
  search()
}

async function approve(row: WithdrawalRequest) {
  await ElMessageBox.confirm(
    `确定通过店铺「${row.shopName}」的提现申请 ${formatMoney(row.amount)} 吗？`,
    '审核通过',
    { type: 'info', confirmButtonText: '通过', cancelButtonText: '取消' }
  )
  await adminSettlementApi.approve(row.id)
  ElMessage.success('已审核通过，等待打款')
  load()
}

async function reject(row: WithdrawalRequest) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回提现申请', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPlaceholder: '驳回原因（必填）',
    inputValidator: (input: string) => (input && input.trim() ? true : '请填写驳回原因')
  })
  await adminSettlementApi.reject(row.id, value.trim())
  ElMessage.success('已驳回该提现申请')
  load()
}

async function pay(row: WithdrawalRequest) {
  await ElMessageBox.confirm(
    `确认已向「${row.accountName}」打款 ${formatMoney(row.amount)} 吗？`,
    '确认打款',
    { type: 'warning', confirmButtonText: '已打款', cancelButtonText: '取消' }
  )
  await adminSettlementApi.pay(row.id)
  ElMessage.success('已标记为打款完成')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="board-head">
      <h2 class="section-title">结算审核</h2>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-if="summary" class="metric-grid">
      <div class="page-card metric-card">
        <div class="metric-label">待审核</div>
        <div class="metric-value warning">{{ summary.pendingCount }}</div>
        <div class="muted">{{ formatMoney(summary.pendingAmount) }}</div>
      </div>
      <div class="page-card metric-card">
        <div class="metric-label">待打款</div>
        <div class="metric-value">{{ summary.approvedCount }}</div>
        <div class="muted">{{ formatMoney(summary.approvedAmount) }}</div>
      </div>
      <div class="page-card metric-card">
        <div class="metric-label">已打款</div>
        <div class="metric-value">{{ summary.paidCount }}</div>
        <div class="muted">{{ formatMoney(summary.paidAmount) }}</div>
      </div>
    </div>

    <div class="page-card toolbar">
      <el-select v-model="status" placeholder="全部状态" class="filter-select" @change="search">
        <el-option label="全部状态" value="" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="待打款" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已打款" value="PAID" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="提现单号 / 店铺 / 户名 / 账号"
        class="filter-input"
        clearable
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" plain @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="page-card" v-loading="loading">
      <el-table :data="list" empty-text="没有提现申请">
        <el-table-column prop="withdrawalNo" label="提现单号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="shopName" label="店铺" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="price">{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收款信息" min-width="200">
          <template #default="{ row }">
            <div>{{ row.accountName }} · {{ row.bankName }}</div>
            <div class="muted">{{ row.accountNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status]" size="small" effect="light">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.adminRemark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="primary" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <template v-else-if="row.status === 'APPROVED'">
              <el-button link type="primary" @click="pay(row)">打款</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <span v-else class="muted">已完成</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > size" class="pagination">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="(current: number) => { page = current - 1; load() }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.board-head .section-title {
  margin: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  padding: 18px 20px;
}

.metric-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 8px;
}

.metric-value.warning {
  color: #e6a23c;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 18px;
}

.filter-select {
  width: 160px;
}

.filter-input {
  width: 260px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .filter-input {
    width: 100%;
  }
}
</style>
