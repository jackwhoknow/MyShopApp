<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { merchantSettlementApi } from '@/api/merchantSettlement'
import { reportApi } from '@/api/report'
import type { SettlementBill, SettlementSummary, WithdrawalRequest } from '@/types'

const loading = ref(false)
const summary = ref<SettlementSummary>()
const activeTab = ref<'bills' | 'withdrawals'>('bills')

const billStatus = ref('')
const billKeyword = ref('')
const bills = ref<SettlementBill[]>([])
const billTotal = ref(0)
const billPage = ref(0)
const billSize = ref(10)

const withdrawals = ref<WithdrawalRequest[]>([])
const withdrawalTotal = ref(0)
const withdrawalPage = ref(0)
const withdrawalSize = ref(10)

const withdrawVisible = ref(false)
const withdrawing = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  amount: 0,
  accountName: '',
  accountNo: '',
  bankName: ''
})
const rules: FormRules = {
  amount: [{ required: true, message: '请填写提现金额', trigger: 'blur' }],
  accountName: [{ required: true, message: '请填写收款户名', trigger: 'blur' }],
  accountNo: [{ required: true, message: '请填写收款账号', trigger: 'blur' }],
  bankName: [{ required: true, message: '请填写开户银行', trigger: 'blur' }]
}

const billStatusLabels: Record<string, string> = {
  WAIT_SHIP: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  REFUNDING: '退款中',
  CANCELLED: '已退款/取消'
}

const withdrawalStatusLabels: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '待打款',
  REJECTED: '已驳回',
  PAID: '已打款'
}

const withdrawalStatusTypes: Record<string, 'warning' | 'primary' | 'danger' | 'success'> = {
  PENDING: 'warning',
  APPROVED: 'primary',
  REJECTED: 'danger',
  PAID: 'success'
}

const available = computed(() => Number(summary.value?.availableAmount ?? 0))

function formatMoney(value?: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

async function loadSummary() {
  summary.value = await merchantSettlementApi.summary()
}

async function loadBills() {
  const result = await merchantSettlementApi.bills({
    status: billStatus.value || undefined,
    keyword: billKeyword.value || undefined,
    page: billPage.value,
    size: billSize.value
  })
  bills.value = result.list
  billTotal.value = result.total
}

async function loadWithdrawals() {
  const result = await merchantSettlementApi.withdrawals({
    page: withdrawalPage.value,
    size: withdrawalSize.value
  })
  withdrawals.value = result.list
  withdrawalTotal.value = result.total
}

async function load() {
  loading.value = true
  try {
    await Promise.all([loadSummary(), loadBills(), loadWithdrawals()])
  } catch {
    // 没有店铺时后端返回 404，页面保持空态
  } finally {
    loading.value = false
  }
}

function searchBills() {
  billPage.value = 0
  loadBills()
}

function resetBills() {
  billStatus.value = ''
  billKeyword.value = ''
  searchBills()
}

function openWithdraw() {
  if (available.value <= 0) {
    ElMessage.warning('当前没有可提现余额')
    return
  }
  form.amount = available.value
  form.accountName = ''
  form.accountNo = ''
  form.bankName = ''
  withdrawVisible.value = true
}

async function submitWithdraw() {
  if (!formRef.value) return
  await formRef.value.validate()
  if (form.amount <= 0) {
    ElMessage.warning('提现金额需大于 0')
    return
  }
  if (form.amount > available.value) {
    ElMessage.warning('提现金额不能超过可提现余额')
    return
  }
  withdrawing.value = true
  try {
    await merchantSettlementApi.createWithdrawal({
      amount: form.amount,
      accountName: form.accountName,
      accountNo: form.accountNo,
      bankName: form.bankName
    })
    ElMessage.success('提现申请已提交，等待平台审核')
    withdrawVisible.value = false
    activeTab.value = 'withdrawals'
    withdrawalPage.value = 0
    await Promise.all([loadSummary(), loadWithdrawals()])
  } finally {
    withdrawing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="board-head">
      <h2 class="section-title">商家结算</h2>
      <div class="board-actions">
        <el-button size="small" :disabled="available <= 0" type="primary" @click="openWithdraw">
          申请提现
        </el-button>
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        <el-button size="small" @click="reportApi.exportSettlements()">导出账单</el-button>
      </div>
    </div>

    <template v-if="summary">
      <div class="metric-grid">
        <div class="page-card metric-card highlight">
          <div class="metric-label">可提现余额</div>
          <div class="metric-value price">{{ formatMoney(summary.availableAmount) }}</div>
          <div class="muted">冻结中 {{ formatMoney(summary.frozenAmount) }}</div>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">可结算收入</div>
          <div class="metric-value">{{ formatMoney(summary.totalIncome) }}</div>
          <div class="muted">已发货 + 已完成子订单</div>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">已提现</div>
          <div class="metric-value">{{ formatMoney(summary.withdrawnAmount) }}</div>
          <div class="muted">平台已打款</div>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">已退款</div>
          <div class="metric-value">{{ formatMoney(summary.refundedAmount) }}</div>
          <div class="muted">售后退款成功金额</div>
        </div>
      </div>

      <div class="page-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="账单明细" name="bills">
            <div class="toolbar">
              <el-select v-model="billStatus" placeholder="全部状态" class="filter-select" @change="searchBills">
                <el-option label="全部状态" value="" />
                <el-option label="待发货" value="WAIT_SHIP" />
                <el-option label="已发货" value="SHIPPED" />
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="退款中" value="REFUNDING" />
                <el-option label="已退款/取消" value="CANCELLED" />
              </el-select>
              <el-input
                v-model="billKeyword"
                placeholder="子订单号 / 主订单号"
                class="filter-input"
                clearable
                @keyup.enter="searchBills"
                @clear="searchBills"
              />
              <el-button type="primary" plain @click="searchBills">查询</el-button>
              <el-button @click="resetBills">重置</el-button>
            </div>

            <el-table :data="bills" empty-text="还没有结算账单">
              <el-table-column prop="subOrderNo" label="子订单号" min-width="180" show-overflow-tooltip />
              <el-table-column prop="orderNo" label="主订单号" min-width="180" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="{ row }">
                  <span class="price">{{ formatMoney(row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="订单状态" width="120">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">{{ billStatusLabels[row.status] || row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="支付时间" width="180">
                <template #default="{ row }">{{ formatTime(row.paidAt) }}</template>
              </el-table-column>
              <el-table-column label="完成时间" width="180">
                <template #default="{ row }">{{ formatTime(row.completedAt) }}</template>
              </el-table-column>
              <el-table-column label="计入结算" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.settleable ? 'success' : 'info'" size="small" effect="light">
                    {{ row.settleable ? '已计入' : '未计入' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>

            <div v-if="billTotal > billSize" class="pagination">
              <el-pagination
                background
                layout="prev, pager, next, total"
                :total="billTotal"
                :page-size="billSize"
                :current-page="billPage + 1"
                @current-change="(current: number) => { billPage = current - 1; loadBills() }"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="提现记录" name="withdrawals">
            <el-table :data="withdrawals" empty-text="还没有提现记录">
              <el-table-column prop="withdrawalNo" label="提现单号" min-width="200" show-overflow-tooltip />
              <el-table-column label="金额" width="120" align="right">
                <template #default="{ row }">
                  <span class="price">{{ formatMoney(row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="收款信息" min-width="200">
                <template #default="{ row }">
                  {{ row.accountName }} · {{ row.accountNo }} · {{ row.bankName }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="withdrawalStatusTypes[row.status]" size="small" effect="light">
                    {{ withdrawalStatusLabels[row.status] || row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="申请时间" width="180">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="打款时间" width="180">
                <template #default="{ row }">{{ formatTime(row.paidAt) }}</template>
              </el-table-column>
              <el-table-column label="备注" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ row.adminRemark || '—' }}</template>
              </el-table-column>
            </el-table>

            <div v-if="withdrawalTotal > withdrawalSize" class="pagination">
              <el-pagination
                background
                layout="prev, pager, next, total"
                :total="withdrawalTotal"
                :page-size="withdrawalSize"
                :current-page="withdrawalPage + 1"
                @current-change="(current: number) => { withdrawalPage = current - 1; loadWithdrawals() }"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <div v-else-if="!loading" class="page-card">
      <el-empty description="还没有店铺或暂无结算数据" :image-size="90" />
    </div>

    <el-dialog v-model="withdrawVisible" title="申请提现" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="可提现">
          <span class="price">{{ formatMoney(available) }}</span>
        </el-form-item>
        <el-form-item label="提现金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :max="available" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="收款户名" prop="accountName">
          <el-input v-model="form.accountName" maxlength="50" placeholder="请输入收款户名" />
        </el-form-item>
        <el-form-item label="收款账号" prop="accountNo">
          <el-input v-model="form.accountNo" maxlength="64" placeholder="请输入银行卡号或收款账号" />
        </el-form-item>
        <el-form-item label="开户银行" prop="bankName">
          <el-input v-model="form.bankName" maxlength="100" placeholder="请输入开户银行" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawVisible = false">取消</el-button>
        <el-button type="primary" :loading="withdrawing" @click="submitWithdraw">提交申请</el-button>
      </template>
    </el-dialog>
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

.board-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.metric-card.highlight {
  border: 1px solid #f3c4c8;
  background: linear-gradient(180deg, #fff6f6 0%, #ffffff 70%);
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.filter-select {
  width: 160px;
}

.filter-input {
  width: 240px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .board-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-input {
    width: 100%;
  }
}
</style>
