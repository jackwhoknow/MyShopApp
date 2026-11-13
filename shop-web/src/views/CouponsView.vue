<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { couponApi } from '@/api/coupon'
import type { Coupon, UserCoupon } from '@/types'

const activeTab = ref<'available' | 'mine'>('available')
const available = ref<Coupon[]>([])
const mine = ref<UserCoupon[]>([])
const loading = ref(false)
const claimingId = ref<number>()

const stateMeta: Record<string, { label: string; type: 'danger' | 'info' | 'warning' }> = {
  UNUSED: { label: '未使用', type: 'danger' },
  USED: { label: '已使用', type: 'info' },
  EXPIRED: { label: '已过期', type: 'info' }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function loadAll() {
  loading.value = true
  try {
    const [claimable, myCoupons] = await Promise.all([couponApi.available(), couponApi.mine()])
    available.value = claimable
    mine.value = myCoupons
  } catch {
    available.value = []
    mine.value = []
  } finally {
    loading.value = false
  }
}

async function claim(coupon: Coupon) {
  claimingId.value = coupon.id
  try {
    await couponApi.claim(coupon.id)
    ElMessage.success(`已领取「${coupon.title}」，结算时可用`)
    await loadAll()
  } finally {
    claimingId.value = undefined
  }
}

onMounted(loadAll)
</script>

<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2 class="section-title">优惠券</h2>
      <span class="muted">店铺券每店每单限用一张，平台券每单限用一张且可与店铺券叠加</span>
    </div>

    <div class="page-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="`可领取（${available.length}）`" name="available">
          <div v-if="available.length" class="coupon-grid">
            <div v-for="coupon in available" :key="coupon.id" class="coupon-card">
              <div class="coupon-face">
                <div class="coupon-amount">
                  <span class="symbol">¥</span>{{ coupon.discountAmount }}
                </div>
                <div class="coupon-threshold">满 {{ coupon.thresholdAmount }} 元可用</div>
              </div>
              <div class="coupon-body">
                <div class="coupon-title">
                  {{ coupon.title }}
                  <el-tag v-if="coupon.platform" size="small" type="danger" effect="plain">平台</el-tag>
                </div>
                <div class="muted">{{ coupon.platform ? '平台通用券' : coupon.shopName }}</div>
                <div class="muted">有效期至 {{ formatTime(coupon.endAt) }}</div>
              </div>
              <div class="coupon-foot">
                <span class="muted">剩余 {{ coupon.remainingCount }} 张</span>
                <el-button type="danger" size="small" :loading="claimingId === coupon.id" @click="claim(coupon)">
                  立即领取
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无可领取的优惠券，等商家发券后再来看看" :image-size="90" />
        </el-tab-pane>

        <el-tab-pane :label="`我的券（${mine.length}）`" name="mine">
          <div v-if="mine.length" class="coupon-grid">
            <div v-for="coupon in mine" :key="coupon.id" class="coupon-card" :class="{ disabled: coupon.state !== 'UNUSED' }">
              <div class="coupon-face">
                <div class="coupon-amount">
                  <span class="symbol">¥</span>{{ coupon.discountAmount }}
                </div>
                <div class="coupon-threshold">满 {{ coupon.thresholdAmount }} 元可用</div>
              </div>
              <div class="coupon-body">
                <div class="coupon-title">
                  {{ coupon.title }}
                  <el-tag v-if="coupon.platform" size="small" type="danger" effect="plain">平台</el-tag>
                  <el-tag :type="stateMeta[coupon.state]?.type || 'info'" size="small" effect="plain">
                    {{ stateMeta[coupon.state]?.label || coupon.state }}
                  </el-tag>
                </div>
                <div class="muted">{{ coupon.platform ? '平台通用券' : coupon.shopName }}</div>
                <div class="muted">有效期至 {{ formatTime(coupon.endAt) }}</div>
              </div>
              <div class="coupon-foot">
                <span class="muted" v-if="coupon.state === 'USED'">已用于订单</span>
                <router-link v-if="coupon.state === 'UNUSED'" to="/cart" class="use-link">去使用 →</router-link>
                <router-link
                  v-else-if="coupon.state === 'USED' && coupon.orderId"
                  :to="`/orders/${coupon.orderId}`"
                  class="use-link"
                >
                  查看订单 →
                </router-link>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有领过优惠券" :image-size="90" />
        </el-tab-pane>
      </el-tabs>
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

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  padding-top: 8px;
}

.coupon-card {
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid #f3d6d9;
  border-radius: 8px;
  padding: 16px;
  background: linear-gradient(135deg, #fff7f7, #fff);
}

.coupon-card.disabled {
  border-color: #ebeef5;
  background: #fafafa;
  opacity: 0.75;
}

.coupon-face {
  width: 96px;
  flex-shrink: 0;
  text-align: center;
  border-right: 1px dashed #f3d6d9;
  padding-right: 12px;
}

.coupon-amount {
  font-size: 30px;
  font-weight: 700;
  color: #c81623;
  line-height: 1.1;
}

.coupon-amount .symbol {
  font-size: 15px;
  margin-right: 2px;
}

.coupon-threshold {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.coupon-body {
  flex: 1;
  min-width: 0;
}

.coupon-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.coupon-foot {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.use-link {
  color: #c81623;
  font-size: 13px;
}

.use-link:hover {
  text-decoration: underline;
}
</style>
