<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addressApi } from '@/api/address'
import { orderApi } from '@/api/order'
import { couponApi } from '@/api/coupon'
import AddressDialog from '@/components/AddressDialog.vue'
import { useCartStore } from '@/stores/cart'
import type { Address, UserCoupon } from '@/types'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()

const addresses = ref<Address[]>([])
const addressId = ref<number>()
const remark = ref('')
const submitting = ref(false)
const addressDialogVisible = ref(false)
const coupons = ref<UserCoupon[]>([])
// 店铺 id -> 选中的券（userCoupon.id），不选表示该店不使用券
const selectedCoupons = reactive<Record<number, number | undefined>>({})
// 平台券：每笔订单限用一张，可与店铺券叠加
const selectedPlatformCouponId = ref<number>()

const ids = computed(() => {
  const raw = route.query.ids
  if (typeof raw !== 'string' || !raw) return []
  return raw.split(',').map(Number).filter((id) => !Number.isNaN(id))
})

const items = computed(() => {
  if (!ids.value.length) return cart.items
  return cart.items.filter((item) => ids.value.includes(item.id))
})

const totalAmount = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)

/** 按店铺分组：店铺券只能抵扣本店商品，因此需要各店原价小计 */
const shopGroups = computed(() => {
  const groups = new Map<number, { shopId: number; shopName: string; amount: number }>()
  for (const item of items.value) {
    const group = groups.get(item.shopId) ?? { shopId: item.shopId, shopName: item.shopName, amount: 0 }
    group.amount += item.price * item.quantity
    groups.set(item.shopId, group)
  }
  return [...groups.values()]
})

function usableCoupons(shopId: number, amount: number) {
  return coupons.value.filter(
    (coupon) => coupon.shopId === shopId && coupon.thresholdAmount <= amount
  )
}

/** 平台券按整单商品金额判断门槛，后端自动抵扣到金额最高的子订单 */
const usablePlatformCoupons = computed(() =>
  coupons.value.filter((coupon) => coupon.platform && coupon.thresholdAmount <= totalAmount.value)
)

const platformDiscount = computed(() => {
  if (!selectedPlatformCouponId.value) return 0
  const coupon = usablePlatformCoupons.value.find((item) => item.id === selectedPlatformCouponId.value)
  return coupon?.discountAmount ?? 0
})

const discountAmount = computed(
  () =>
    shopGroups.value.reduce((sum, group) => {
      const couponId = selectedCoupons[group.shopId]
      if (!couponId) return sum
      const coupon = usableCoupons(group.shopId, group.amount).find((item) => item.id === couponId)
      return sum + (coupon?.discountAmount ?? 0)
    }, 0) + platformDiscount.value
)

const payableAmount = computed(() => Math.max(totalAmount.value - discountAmount.value, 0))

const currentAddress = computed(() => addresses.value.find((item) => item.id === addressId.value))

async function loadAddresses() {
  addresses.value = await addressApi.list()
  const stillExists = addresses.value.some((item) => item.id === addressId.value)
  if (stillExists) return
  const preferred = addresses.value.find((item) => item.defaultAddress) ?? addresses.value[0]
  addressId.value = preferred?.id
}

onMounted(async () => {
  if (!cart.items.length) {
    await cart.load()
  }
  await loadAddresses()
  try {
    coupons.value = await couponApi.mine('UNUSED')
  } catch {
    coupons.value = []
  }
})

async function submitOrder() {
  if (!addressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  if (!items.value.length) {
    ElMessage.warning('没有可结算的商品')
    return
  }

  // 重新核对门槛，避免页面停留期间金额变化导致用券失败
  const invalidCoupon = shopGroups.value.some((group) => {
    const couponId = selectedCoupons[group.shopId]
    if (!couponId) return false
    return !usableCoupons(group.shopId, group.amount).some((coupon) => coupon.id === couponId)
  })
  if (invalidCoupon) {
    ElMessage.warning('有优惠券不满足使用门槛，请重新选择')
    return
  }
  if (
    selectedPlatformCouponId.value &&
    !usablePlatformCoupons.value.some((coupon) => coupon.id === selectedPlatformCouponId.value)
  ) {
    ElMessage.warning('平台券不满足使用门槛，请重新选择')
    return
  }

  const userCouponIds = shopGroups.value
    .map((group) => selectedCoupons[group.shopId])
    .filter((id): id is number => Boolean(id))
  if (selectedPlatformCouponId.value) {
    userCouponIds.push(selectedPlatformCouponId.value)
  }

  submitting.value = true
  try {
    // 每次点击生成新的幂等键，网络重试也不会重复下单
    const requestId = newRequestId()
    const result = await orderApi.seckill({
      requestId,
      addressId: addressId.value,
      remark: remark.value.trim() || undefined,
      cartItemIds: items.value.map((item) => item.id),
      userCouponIds
    })
    if (result.status === 'FAILED') {
      ElMessage.error(result.message || '下单失败，请稍后重试')
      return
    }
    if (result.status === 'SUCCESS' && result.orderId) {
      await finishOrder(result.orderId)
      return
    }
    // 已进入排队队列：轮询等待消费端落库
    await waitForOrderResult(requestId)
  } finally {
    submitting.value = false
  }
}

function newRequestId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return `${Date.now()}${Math.random().toString(16).slice(2)}`
}

/** 轮询排队结果：最长等待约 30 秒，超时后引导用户到订单列表确认 */
async function waitForOrderResult(requestId: string) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    await new Promise((resolve) => setTimeout(resolve, 500))
    const result = await orderApi.seckillStatus(requestId)
    if (result.status === 'SUCCESS' && result.orderId) {
      await finishOrder(result.orderId)
      return
    }
    if (result.status === 'FAILED') {
      ElMessage.error(result.message || '下单失败，请稍后重试')
      return
    }
  }
  ElMessage.warning('下单请求仍在排队，请稍后到订单列表查看结果')
  router.replace('/orders')
}

async function finishOrder(orderId: number) {
  await cart.load()
  ElMessage.success('下单成功，请尽快支付')
  router.replace(`/orders/${orderId}`)
}

async function onAddressSaved() {
  await loadAddresses()
}
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="section-title">确认订单</h2>
      <el-button link @click="router.back()">返回</el-button>
    </div>

    <div class="page-card block">
      <div class="block-head">
        <span class="block-title">收货地址</span>
        <el-button type="primary" link @click="addressDialogVisible = true">+ 新增地址</el-button>
      </div>

      <el-radio-group v-if="addresses.length" v-model="addressId" class="address-list">
        <el-radio v-for="address in addresses" :key="address.id" :value="address.id" border class="address-item">
          <div class="address-main">
            <div class="receiver">
              {{ address.receiverName }}
              <span class="muted">{{ address.phone }}</span>
              <el-tag v-if="address.defaultAddress" size="small" type="danger" effect="plain">默认</el-tag>
            </div>
            <div class="address-text muted">{{ address.fullAddress }}</div>
          </div>
        </el-radio>
      </el-radio-group>
      <el-empty v-else description="还没有收货地址，请先新增" :image-size="80" />
    </div>

    <div class="page-card block">
      <div class="block-head">
        <span class="block-title">商品清单</span>
      </div>
      <div v-for="item in items" :key="item.id" class="order-item">
        <el-image :src="item.image" fit="cover" class="thumb">
          <template #error>
            <div class="img-fallback">无图</div>
          </template>
        </el-image>
        <div class="item-info">
          <div class="item-name">{{ item.productName }}</div>
          <div class="muted">{{ item.skuDesc }}</div>
          <div class="muted">{{ item.shopName }}</div>
        </div>
        <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
        <div class="price item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
      </div>
      <el-empty v-if="!items.length" description="没有待结算商品" :image-size="80" />
    </div>

    <div class="page-card block">
      <div class="block-head">
        <span class="block-title">优惠券</span>
      </div>
      <div v-for="group in shopGroups" :key="group.shopId" class="coupon-row">
        <div class="coupon-shop">
          <div class="item-name">{{ group.shopName }}</div>
          <div class="muted">本店商品 ¥{{ group.amount.toFixed(2) }}</div>
        </div>
        <el-select
          v-model="selectedCoupons[group.shopId]"
          clearable
          placeholder="不使用优惠券"
          class="coupon-select"
          :disabled="!usableCoupons(group.shopId, group.amount).length"
        >
          <el-option
            v-for="coupon in usableCoupons(group.shopId, group.amount)"
            :key="coupon.id"
            :value="coupon.id"
            :label="`${coupon.title}（满${coupon.thresholdAmount}减${coupon.discountAmount}）`"
          />
        </el-select>
      </div>
      <div v-if="usablePlatformCoupons.length" class="coupon-row">
        <div class="coupon-shop">
          <div class="item-name">平台券</div>
          <div class="muted">全场通用，可与店铺券叠加</div>
        </div>
        <el-select
          v-model="selectedPlatformCouponId"
          clearable
          placeholder="不使用平台券"
          class="coupon-select"
        >
          <el-option
            v-for="coupon in usablePlatformCoupons"
            :key="coupon.id"
            :value="coupon.id"
            :label="`${coupon.title}（满${coupon.thresholdAmount}减${coupon.discountAmount}）`"
          />
        </el-select>
      </div>
      <div v-if="!shopGroups.length" class="muted">没有可结算的商品</div>
    </div>

    <div class="page-card block">
      <div class="block-head">
        <span class="block-title">订单备注</span>
      </div>
      <el-input v-model="remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="选填：对本次交易的说明" />
    </div>

    <div class="page-card submit-bar">
      <div class="summary">
        <div class="muted">商品金额：¥{{ totalAmount.toFixed(2) }}</div>
        <div v-if="discountAmount > 0" class="discount-line">优惠：-¥{{ discountAmount.toFixed(2) }}</div>
        <div>
          共 {{ items.length }} 种商品，应付：
          <span class="price total">¥{{ payableAmount.toFixed(2) }}</span>
        </div>
      </div>
      <el-button type="danger" size="large" :loading="submitting" :disabled="!items.length" @click="submitOrder">
        {{ submitting ? '排队处理中…' : '提交订单' }}
      </el-button>
    </div>

    <AddressDialog v-model="addressDialogVisible" @saved="onAddressSaved" />
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

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.address-item {
  width: 100%;
  height: auto;
  margin-right: 0;
  padding: 12px 16px;
  align-items: flex-start;
}

.address-main {
  padding-left: 8px;
}

.receiver {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.address-text {
  margin-top: 4px;
  white-space: normal;
  line-height: 1.5;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}

.order-item:last-child {
  border-bottom: none;
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

.submit-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
  position: sticky;
  bottom: 16px;
}

.summary {
  text-align: right;
  line-height: 1.8;
}

.discount-line {
  color: #c81623;
  font-size: 14px;
}

.coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.coupon-row:last-child {
  border-bottom: none;
}

.coupon-shop {
  min-width: 0;
}

.coupon-select {
  width: 280px;
  flex-shrink: 0;
}

.total {
  font-size: 24px;
}

@media (max-width: 768px) {
  .coupon-row {
    flex-direction: column;
    align-items: stretch;
  }

  .coupon-select {
    width: 100%;
  }
}
</style>
