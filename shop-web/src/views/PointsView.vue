<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pointApi } from '@/api/points'
import type { CheckinStatus, PointAccount, PointCoupon, PointTransaction } from '@/types'

const account = ref<PointAccount>({
  balance: 0,
  totalEarned: 0,
  totalSpent: 0,
  level: 'BRONZE',
  levelName: '青铜会员'
})
const records = ref<PointTransaction[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const checkin = ref<CheckinStatus>({
  checkedInToday: false,
  consecutiveDays: 0,
  todayPoints: 0,
  nextPoints: 5
})
const coupons = ref<PointCoupon[]>([])
const checkinLoading = ref(false)
const exchangingId = ref<number | null>(null)

const typeMeta: Record<string, { label: string; type: 'success' | 'danger' | 'warning' | 'info' }> = {
  ORDER_EARN: { label: '支付赠送', type: 'success' },
  ORDER_DEDUCT: { label: '下单抵扣', type: 'danger' },
  ORDER_CANCEL_RETURN: { label: '取消退回', type: 'warning' },
  ORDER_REFUND: { label: '整单退款', type: 'info' },
  CHECKIN: { label: '签到奖励', type: 'success' },
  POINT_EXCHANGE: { label: '积分兑换', type: 'danger' }
}

const levelProgress = computed(() => {
  if (!account.value.pointsToNext) return 100
  const current = account.value.totalEarned
  const need = current + account.value.pointsToNext
  return need <= 0 ? 100 : Math.min(100, Math.round((current / need) * 100))
})

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadAccount() {
  account.value = await pointApi.account()
}

async function loadRecords() {
  const result = await pointApi.transactions({ page: page.value, size: size.value })
  records.value = result.list
  total.value = result.total
}

async function loadCheckin() {
  checkin.value = await pointApi.checkinStatus()
}

async function loadCoupons() {
  coupons.value = await pointApi.exchangeableCoupons()
}

async function load() {
  loading.value = true
  try {
    await Promise.all([loadAccount(), loadRecords(), loadCheckin(), loadCoupons()])
  } finally {
    loading.value = false
  }
}

function handlePage(current: number) {
  page.value = current
  loadRecords()
}

async function handleCheckin() {
  checkinLoading.value = true
  try {
    checkin.value = await pointApi.checkin()
    ElMessage.success(`签到成功，获得 ${checkin.value.todayPoints} 积分`)
    await Promise.all([loadAccount(), loadRecords()])
  } finally {
    checkinLoading.value = false
  }
}

async function handleExchange(coupon: PointCoupon) {
  exchangingId.value = coupon.id
  try {
    await pointApi.exchangeCoupon(coupon.id)
    ElMessage.success(`兑换成功，「${coupon.title}」已放入我的优惠券`)
    await Promise.all([loadAccount(), loadCoupons()])
  } finally {
    exchangingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2 class="section-title">我的积分</h2>
      <span class="muted">100 积分抵 1 元，单笔最多抵扣应付金额的 50%；支付成功按实付金额返还积分</span>
    </div>

    <div class="page-card balance-card">
      <div class="balance-main">
        <div class="balance-label">当前可用积分</div>
        <div class="balance-value">{{ account.balance }}</div>
        <div class="muted">约可抵扣 ¥{{ (account.balance / 100).toFixed(2) }}</div>
      </div>
      <div class="balance-side">
        <div class="stat">
          <span class="muted">会员等级</span>
          <el-tag type="warning" effect="plain">{{ account.levelName }}</el-tag>
        </div>
        <div class="stat">
          <span class="muted">累计获得</span>
          <span class="stat-value">{{ account.totalEarned }}</span>
        </div>
        <div class="stat">
          <span class="muted">累计使用</span>
          <span class="stat-value">{{ account.totalSpent }}</span>
        </div>
      </div>
    </div>

    <div class="page-card level-card" v-if="account.nextLevelName">
      <div class="level-row">
        <span>距离「{{ account.nextLevelName }}」还差 <b>{{ account.pointsToNext }}</b> 积分</span>
        <span class="muted">等级按累计获得积分计算，只升不降</span>
      </div>
      <el-progress :percentage="levelProgress" :show-text="false" color="#c81623" />
    </div>

    <div class="grid-two">
      <div class="page-card checkin-card">
        <div class="block-head">
          <span class="block-title">每日签到</span>
          <span class="muted">连续 {{ checkin.consecutiveDays }} 天</span>
        </div>
        <p class="muted">连续签到奖励递增，单日最高 15 积分</p>
        <el-button
          type="primary"
          :loading="checkinLoading"
          :disabled="checkin.checkedInToday"
          @click="handleCheckin"
        >
          {{ checkin.checkedInToday ? '今天已签到' : `签到领 ${checkin.nextPoints} 积分` }}
        </el-button>
      </div>

      <div class="page-card">
        <div class="block-head">
          <span class="block-title">积分兑换优惠券</span>
          <span class="muted">每人每张券限兑一次</span>
        </div>
        <div v-if="coupons.length" class="coupon-grid">
          <div v-for="coupon in coupons" :key="coupon.id" class="coupon-card">
            <div class="coupon-title">{{ coupon.title }}</div>
            <div class="coupon-rule">满 {{ coupon.thresholdAmount }} 减 {{ coupon.discountAmount }}</div>
            <div class="coupon-meta">
              <span>{{ coupon.shopName || '平台通用' }}</span>
              <span>{{ formatTime(coupon.endAt) }} 到期</span>
            </div>
            <el-button
              size="small"
              type="danger"
              plain
              :loading="exchangingId === coupon.id"
              :disabled="coupon.exchanged || account.balance < coupon.pointsPrice"
              @click="handleExchange(coupon)"
            >
              {{ coupon.exchanged ? '已兑换' : `${coupon.pointsPrice} 积分兑换` }}
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无可兑换的券" :image-size="70" />
      </div>
    </div>

    <div class="page-card">
      <div class="block-head">
        <span class="block-title">积分明细</span>
      </div>
      <el-table v-if="records.length" :data="records" stripe>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeMeta[row.type]?.type || 'info'" size="small" effect="plain">
              {{ typeMeta[row.type]?.label || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动" width="120">
          <template #default="{ row }">
            <span :class="row.changeAmount > 0 ? 'add' : 'deduct'">
              {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后余额" width="120">
          <template #default="{ row }">{{ row.balanceAfter }}</template>
        </el-table-column>
        <el-table-column label="说明" min-width="180">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="订单" width="180">
          <template #default="{ row }">
            <router-link v-if="row.orderId" :to="`/orders/${row.orderId}`" class="order-link">
              {{ row.orderNo }}
            </router-link>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="还没有积分记录，下单支付后即可获得积分" :image-size="90" />

      <div v-if="total > size" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="handlePage"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}

.balance-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fff7f7, #fff);
}

.balance-label {
  color: #909399;
  font-size: 14px;
}

.balance-value {
  font-size: 40px;
  font-weight: 700;
  color: #c81623;
  line-height: 1.2;
  margin: 4px 0;
}

.balance-side {
  display: flex;
  gap: 40px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: right;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}

.level-card {
  margin-bottom: 16px;
}

.level-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.grid-two {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 16px;
  margin-bottom: 16px;
}

.checkin-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.coupon-card {
  border: 1px dashed #f0c0c5;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #fffafa;
}

.coupon-title {
  font-weight: 600;
}

.coupon-rule {
  color: #c81623;
  font-size: 13px;
}

.coupon-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.block-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
}

.add {
  color: #67c23a;
  font-weight: 600;
}

.deduct {
  color: #c81623;
  font-weight: 600;
}

.order-link {
  color: #c81623;
}

.order-link:hover {
  text-decoration: underline;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .balance-card,
  .grid-two {
    flex-direction: column;
    align-items: flex-start;
  }

  .grid-two {
    display: flex;
  }

  .balance-side {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
