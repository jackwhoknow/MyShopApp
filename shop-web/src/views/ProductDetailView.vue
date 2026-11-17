<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { favoriteApi } from '@/api/favorite'
import { historyApi } from '@/api/history'
import { productApi } from '@/api/product'
import { promotionApi } from '@/api/promotion'
import { reviewApi } from '@/api/review'
import { shopFollowApi } from '@/api/shopFollow'
import { supportApi } from '@/api/support'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import ProductCard from '@/components/ProductCard.vue'
import type { FollowState, ProductDetail, ProductListItem, Review, ShopPromotion } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const product = ref<ProductDetail>()
const loading = ref(true)
const adding = ref(false)
const quantity = ref(1)
/** 当前商品是否已收藏，登录后才查询 */
const favorited = ref(false)
const favoritePending = ref(false)
/** 店铺关注状态，游客查询时后端返回未关注 */
const followState = ref<FollowState>({ following: false, followers: 0 })
const followPending = ref(false)
/** 分享二维码弹窗 */
const shareVisible = ref(false)
const shareQr = ref('')
const shareUrl = computed(() => `${location.origin}/products/${product.value?.id ?? ''}`)
/** specName -> 选中的规格值 */
const selected = reactive<Record<string, string>>({})

/** 同分类推荐与店铺满减 */
const related = ref<ProductListItem[]>([])
const promotions = ref<ShopPromotion[]>([])
const contactLoading = ref(false)

/** 商品评价：只展示审核通过的评价，分页加载 */
const reviews = ref<Review[]>([])
const reviewTotal = ref(0)
const reviewPage = ref(0)
const reviewLoading = ref(false)

async function loadReviews() {
  if (!product.value) return
  reviewLoading.value = true
  try {
    const result = await reviewApi.listByProduct(product.value.id, {
      page: reviewPage.value,
      size: 5
    })
    reviews.value = [...reviews.value, ...result.list]
    reviewTotal.value = result.total
  } finally {
    reviewLoading.value = false
  }
}

function loadMoreReviews() {
  reviewPage.value += 1
  loadReviews()
}

const matchedSku = computed(() => {
  if (!product.value) return undefined
  const specs = product.value.specs
  if (specs.some((spec) => !selected[spec.name])) return undefined
  return product.value.skus.find((sku) =>
    sku.specValues.every((value) => selected[value.specName] === value.value)
  )
})

const displayPrice = computed(() => matchedSku.value?.price ?? product.value?.minPrice ?? 0)
const canBuy = computed(() => Boolean(matchedSku.value && matchedSku.value.stock > 0))

watch(matchedSku, (sku) => {
  if (!sku) return
  if (quantity.value > sku.stock) {
    quantity.value = Math.max(1, Math.min(sku.stock, 99))
  }
})

onMounted(async () => {
  try {
    product.value = await productApi.detail(Number(route.params.id))
    if (product.value) {
      shopFollowApi
        .status(product.value.shopId)
        .then((state) => (followState.value = state))
        .catch(() => undefined)
    }
    if (auth.isLoggedIn && product.value) {
      const ids = await favoriteApi.ids([product.value.id]).catch(() => [] as number[])
      favorited.value = ids.includes(product.value.id)
      // 浏览历史属于附加记录，失败不影响商品展示
      historyApi.record(product.value.id).catch(() => undefined)
    }
    if (product.value) {
      productApi.related(product.value.id, 8)
        .then((list) => (related.value = list))
        .catch(() => undefined)
      promotionApi.activeByShop(product.value.shopId)
        .then((list) => (promotions.value = list))
        .catch(() => undefined)
    }
    loadReviews()
  } finally {
    loading.value = false
  }
})

async function toggleFavorite() {
  if (!product.value) return
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoritePending.value = true
  try {
    if (favorited.value) {
      await favoriteApi.remove(product.value.id)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(product.value.id)
      favorited.value = true
      ElMessage.success('已加入收藏')
    }
  } finally {
    favoritePending.value = false
  }
}

function chooseValue(specName: string, value: string) {
  selected[specName] = selected[specName] === value ? '' : value
}

/** 关注/取消关注商品所属店铺 */
async function toggleFollow() {
  if (!product.value) return
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  followPending.value = true
  try {
    followState.value = followState.value.following
      ? await shopFollowApi.unfollow(product.value.shopId)
      : await shopFollowApi.follow(product.value.shopId)
    ElMessage.success(followState.value.following ? '已关注该店铺' : '已取消关注')
  } finally {
    followPending.value = false
  }
}

/** 生成商品分享二维码 */
async function openShare() {
  if (!product.value) return
  shareQr.value = await QRCode.toDataURL(shareUrl.value, { width: 240, margin: 1 })
  shareVisible.value = true
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制地址栏链接')
  }
}

/** 联系店铺客服：登录后自动创建/复用会话并跳转客服页 */
async function contactShop() {
  if (!product.value) return
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  contactLoading.value = true
  try {
    const conversation = await supportApi.start(product.value.shopId)
    router.push({ path: '/support', query: { conversationId: String(conversation.id) } })
  } finally {
    contactLoading.value = false
  }
}

async function addToCart(buyNow = false) {
  if (!matchedSku.value) {
    ElMessage.warning('请先选择完整规格')
    return
  }
  if (matchedSku.value.stock <= 0) {
    ElMessage.warning('该规格已售罄')
    return
  }
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  adding.value = true
  try {
    const item = await cart.add(matchedSku.value.id, quantity.value)
    ElMessage.success('已加入购物车')
    if (buyNow) {
      router.push({ path: '/checkout', query: { ids: String(item.id) } })
    }
  } finally {
    adding.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <div v-if="product" class="page-card detail">
      <div class="gallery">
        <el-image :src="product.mainImage" fit="cover" class="main-image">
          <template #error>
            <div class="img-fallback">暂无图片</div>
          </template>
        </el-image>
      </div>

      <div class="info">
        <h1 class="name">{{ product.name }}</h1>
        <p class="subtitle">{{ product.subtitle }}</p>

        <div class="price-box">
          <span class="price big">¥{{ displayPrice.toFixed(2) }}</span>
          <span class="muted">
            价格区间 ¥{{ product.minPrice.toFixed(2) }} - ¥{{ product.maxPrice.toFixed(2) }}
          </span>
        </div>

        <div class="meta">
          <span>已售 {{ product.sales }}</span>
          <el-divider direction="vertical" />
          <router-link :to="`/shops/${product.shopId}`" class="shop-link">{{ product.shopName }}</router-link>
          <el-divider direction="vertical" />
          <span>{{ product.categoryName }}</span>
        </div>

        <div v-for="spec in product.specs" :key="spec.id" class="spec-row">
          <div class="spec-name">{{ spec.name }}</div>
          <div class="spec-values">
            <button
              v-for="value in spec.values"
              :key="value.id"
              type="button"
              class="spec-btn"
              :class="{ active: selected[spec.name] === value.value }"
              @click="chooseValue(spec.name, value.value)"
            >
              {{ value.value }}
            </button>
          </div>
        </div>

        <div class="spec-row">
          <div class="spec-name">数量</div>
          <div class="spec-values">
            <el-input-number v-model="quantity" :min="1" :max="99" :disabled="!canBuy" />
            <span v-if="matchedSku" class="muted stock">
              {{ matchedSku.stock > 0 ? `库存 ${matchedSku.stock} 件` : '已售罄' }}
            </span>
            <span v-else class="muted stock">请选择完整规格后查看库存</span>
          </div>
        </div>

        <div class="actions">
          <el-button type="primary" plain size="large" :disabled="!canBuy" :loading="adding" @click="addToCart(false)">
            加入购物车
          </el-button>
          <el-button type="danger" size="large" :disabled="!canBuy" :loading="adding" @click="addToCart(true)">
            立即购买
          </el-button>
          <el-button size="large" :loading="favoritePending" @click="toggleFavorite">
            <el-icon class="fav-icon" :class="{ active: favorited }">
              <StarFilled v-if="favorited" />
              <Star v-else />
            </el-icon>
            {{ favorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button size="large" :loading="followPending" @click="toggleFollow">
            {{ followState.following ? '已关注店铺' : '关注店铺' }}（{{ followState.followers }}）
          </el-button>
          <el-button size="large" @click="openShare">分享</el-button>
          <el-button size="large" :loading="contactLoading" @click="contactShop">联系客服</el-button>
        </div>

        <div v-if="promotions.length" class="promo-box">
          <el-tag type="danger" effect="dark" size="small">满减</el-tag>
          <span v-for="promotion in promotions" :key="promotion.id" class="promo-item">
            满 {{ promotion.thresholdAmount }} 减 {{ promotion.reduceAmount }}
          </span>
        </div>
      </div>
    </div>

    <el-dialog v-model="shareVisible" title="分享商品" width="320px">
      <div class="share-body">
        <img v-if="shareQr" :src="shareQr" alt="商品分享二维码" class="share-qr" />
        <div class="muted share-url">{{ shareUrl }}</div>
        <el-button type="primary" plain @click="copyShareLink">复制链接</el-button>
      </div>
    </el-dialog>

    <div v-if="product" class="page-card description">
      <h2 class="section-title">商品详情</h2>
      <p class="desc-text">{{ product.description || '暂无商品描述' }}</p>
    </div>

    <div v-if="product" class="page-card reviews">
      <h2 class="section-title">用户评价（{{ reviewTotal }}）</h2>
      <el-empty v-if="!reviews.length && !reviewLoading" description="暂无评价" />
      <div v-for="review in reviews" :key="review.id" class="review-item">
        <div class="review-head">
          <el-rate :model-value="review.rating" disabled size="small" />
          <span class="muted review-time">{{ review.createdAt?.replace('T', ' ').slice(0, 16) }}</span>
        </div>
        <div v-if="review.skuDesc" class="muted review-sku">{{ review.skuDesc }}</div>
        <p class="review-content">{{ review.content || '用户未填写评价内容' }}</p>
        <div v-if="review.images?.length" class="review-images">
          <el-image
            v-for="image in review.images"
            :key="image"
            :src="image"
            :preview-src-list="review.images"
            preview-teleported
            fit="cover"
            class="review-image"
          />
        </div>
        <div v-if="review.reply" class="review-reply">商家回复：{{ review.reply }}</div>
        <div v-if="review.appendContent || review.appendImages?.length" class="review-append">
          <div class="append-label">追评 · {{ review.appendedAt?.replace('T', ' ').slice(0, 16) }}</div>
          <p v-if="review.appendContent" class="review-content">{{ review.appendContent }}</p>
          <div v-if="review.appendImages?.length" class="review-images">
            <el-image
              v-for="image in review.appendImages"
              :key="image"
              :src="image"
              :preview-src-list="review.appendImages"
              preview-teleported
              fit="cover"
              class="review-image"
            />
          </div>
        </div>
      </div>
      <div v-if="reviews.length < reviewTotal" class="review-more">
        <el-button :loading="reviewLoading" @click="loadMoreReviews">加载更多</el-button>
      </div>
    </div>

    <div v-if="related.length" class="page-card related">
      <h2 class="section-title">相关推荐</h2>
      <div class="product-grid">
        <ProductCard v-for="item in related" :key="item.id" :product="item" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: 440px 1fr;
  gap: 36px;
}

.gallery {
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
}

.name {
  margin: 0 0 8px;
  font-size: 24px;
  line-height: 1.35;
}

.subtitle {
  margin: 0 0 18px;
  color: #909399;
  font-size: 14px;
}

.reviews {
  margin-top: 16px;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-time {
  font-size: 12px;
}

.review-sku {
  margin-top: 4px;
  font-size: 12px;
}

.review-content {
  margin: 8px 0;
  line-height: 1.7;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-image {
  width: 84px;
  height: 84px;
  border-radius: 6px;
}

.review-reply {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #fafafa;
  font-size: 13px;
  color: #606266;
}

.review-append {
  margin-top: 10px;
  padding: 10px 12px;
  border-left: 3px solid #c81623;
  background: #fffafa;
  border-radius: 4px;
}

.append-label {
  font-size: 12px;
  color: #c81623;
  margin-bottom: 4px;
}

.promo-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 12px;
  background: #fff7f7;
  border-radius: 6px;
}

.promo-item {
  font-size: 13px;
  color: #c81623;
}

.related {
  margin-top: 16px;
}

.review-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.shop-link {
  color: #c81623;
  text-decoration: none;
}

.shop-link:hover {
  text-decoration: underline;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding: 16px;
  background: #fdf3f4;
  border-radius: 8px;
}

.price.big {
  font-size: 30px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.spec-row {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  padding: 16px 0;
  border-bottom: 1px dashed #ebeef5;
}

.spec-name {
  width: 60px;
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;
  line-height: 32px;
}

.spec-values {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.spec-btn {
  min-width: 64px;
  padding: 0 14px;
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #303133;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.spec-btn:hover {
  border-color: #c81623;
  color: #c81623;
}

.spec-btn.active {
  border-color: #c81623;
  background: #fdf3f4;
  color: #c81623;
  font-weight: 600;
}

.stock {
  margin-left: 4px;
}

.share-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.share-qr {
  width: 220px;
  height: 220px;
  border-radius: 8px;
}

.share-url {
  word-break: break-all;
  text-align: center;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 16px;
  padding-top: 26px;
}

.fav-icon {
  margin-right: 4px;
  color: #909399;
}

.fav-icon.active {
  color: #e6a23c;
}

.description {
  margin-top: 20px;
}

.desc-text {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #606266;
}

@media (max-width: 900px) {
  .detail {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
