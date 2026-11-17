<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { seckillApi } from '@/api/seckill'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { SeckillActivity } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const activities = ref<SeckillActivity[]>([])
const loading = ref(false)
const buyingId = ref<number>()
const now = ref(Date.now())
let timer: number | undefined

const stateMeta: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  RUNNING: { label: '抢购中', type: 'danger' },
  NOT_STARTED: { label: '即将开始', type: 'warning' },
  SOLD_OUT: { label: '已售罄', type: 'info' },
  EXPIRED: { label: '已结束', type: 'info' },
  DISABLED: { label: '已停用', type: 'info' }
}

const runningCount = computed(() => activities.value.filter((item) => item.state === 'RUNNING').length)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatDuration(target: string) {
  const diff = new Date(target).getTime() - now.value
  if (diff <= 0) return '00:00:00'
  const totalSeconds = Math.floor(diff / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

function countdownText(activity: SeckillActivity) {
  if (activity.state === 'NOT_STARTED') return `距开始 ${formatDuration(activity.startAt)}`
  if (activity.state === 'RUNNING') return `距结束 ${formatDuration(activity.endAt)}`
  return ''
}

function progressPercent(activity: SeckillActivity) {
  if (!activity.totalStock) return 0
  return Math.min(Math.round((activity.soldCount / activity.totalStock) * 100), 100)
}

async function load() {
  loading.value = true
  try {
    activities.value = await seckillApi.list()
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

async function buy(activity: SeckillActivity) {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: '/seckill' } })
    return
  }
  buyingId.value = activity.id
  try {
    const item = await cart.add(activity.skuId, 1)
    router.push({ path: '/checkout', query: { ids: String(item.id) } })
  } finally {
    buyingId.value = undefined
  }
}

onMounted(async () => {
  await load()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-head">
      <div>
        <h2 class="section-title">限时秒杀</h2>
        <div class="muted">每天精选好物，活动价限时限量，每人限购</div>
      </div>
      <el-tag type="danger" effect="plain">正在抢购 {{ runningCount }} 场</el-tag>
    </div>

    <div v-if="activities.length" class="seckill-grid">
      <div v-for="activity in activities" :key="activity.id" class="seckill-card">
        <div class="card-cover">
          <el-image :src="activity.image" fit="cover" class="cover-img">
            <template #error>
              <div class="img-fallback">无图</div>
            </template>
          </el-image>
          <el-tag class="state-tag" :type="stateMeta[activity.state]?.type || 'info'" effect="dark">
            {{ stateMeta[activity.state]?.label || activity.state }}
          </el-tag>
        </div>

        <div class="card-body">
          <router-link :to="`/products/${activity.productId}`" class="product-name">
            {{ activity.productName }}
          </router-link>
          <div class="muted sku-desc">{{ activity.skuDesc || '默认规格' }}</div>
          <div class="shop-line muted">{{ activity.shopName }}</div>

          <div class="price-line">
            <span class="seckill-price"><span class="symbol">¥</span>{{ activity.activityPrice }}</span>
            <span class="origin-price">¥{{ activity.originalPrice }}</span>
          </div>

          <el-progress
            :percentage="progressPercent(activity)"
            :show-text="false"
            :stroke-width="8"
            color="#c81623"
            class="progress"
          />
          <div class="stock-line">
            <span class="muted">已抢 {{ activity.soldCount }} 件</span>
            <span class="muted">剩余 {{ activity.remainingStock }} 件</span>
          </div>

          <div class="meta-line">
            <span v-if="countdownText(activity)" class="countdown">{{ countdownText(activity) }}</span>
            <span class="muted">每人限购 {{ activity.perUserLimit }} 件</span>
          </div>

          <el-button
            class="buy-btn"
            type="danger"
            :disabled="activity.state !== 'RUNNING' || activity.remainingStock <= 0"
            :loading="buyingId === activity.id"
            @click="buy(activity)"
          >
            {{ activity.state === 'NOT_STARTED' ? '即将开始' : activity.state === 'RUNNING' ? '立即抢购' : '已结束' }}
          </el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无秒杀活动，商家发活动后再来看看" :image-size="100" />

    <div v-if="activities.length" class="activity-tip muted">
      活动时间以页面展示为准，下单时将自动按活动价结算并校验每人限购。
      最近结束：{{ formatTime(activities[activities.length - 1]?.endAt) }}
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head .section-title {
  margin: 0 0 4px;
}

.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.seckill-card {
  background: #fff;
  border: 1px solid #f0e2e4;
  border-radius: 10px;
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
}

.seckill-card:hover {
  box-shadow: 0 8px 24px rgba(200, 22, 35, 0.12);
  transform: translateY(-2px);
}

.card-cover {
  position: relative;
  height: 180px;
  background: #f7f8fa;
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 13px;
}

.state-tag {
  position: absolute;
  top: 10px;
  left: 10px;
}

.card-body {
  padding: 14px;
}

.product-name {
  display: block;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-name:hover {
  color: #c81623;
}

.sku-desc,
.shop-line {
  font-size: 12px;
  margin-top: 4px;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 10px 0 6px;
}

.seckill-price {
  color: #c81623;
  font-size: 24px;
  font-weight: 700;
}

.seckill-price .symbol {
  font-size: 14px;
  margin-right: 2px;
}

.origin-price {
  color: #a8abb2;
  font-size: 13px;
  text-decoration: line-through;
}

.progress {
  margin: 6px 0;
}

.stock-line,
.meta-line {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.meta-line {
  margin: 8px 0 12px;
}

.countdown {
  color: #c81623;
  font-weight: 600;
}

.buy-btn {
  width: 100%;
}

.activity-tip {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
}
</style>
