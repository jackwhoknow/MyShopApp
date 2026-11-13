<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '@/api/favorite'
import { shopApi } from '@/api/shop'
import { shopFollowApi } from '@/api/shopFollow'
import ProductCard from '@/components/ProductCard.vue'
import { useAuthStore } from '@/stores/auth'
import type { FollowState, ProductListItem, ShopDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const shopId = Number(route.params.id)
const shop = ref<ShopDetail>()
const notFound = ref(false)

const products = ref<ProductListItem[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(12)
const keyword = ref('')
const sort = ref('default')
const loading = ref(false)

const followState = ref<FollowState>({ following: false, followers: 0 })
const followPending = ref(false)
const favoriteIds = ref<Set<number>>(new Set())

async function loadShop() {
  try {
    shop.value = await shopApi.detail(shopId)
    followState.value.followers = shop.value.followers
  } catch {
    notFound.value = true
  }
}

async function loadProducts() {
  loading.value = true
  try {
    const result = await shopApi.products(shopId, {
      keyword: keyword.value || undefined,
      sort: sort.value === 'default' ? undefined : sort.value,
      page: page.value,
      size: size.value
    })
    products.value = result.list
    total.value = result.total
    await loadFavorites()
  } finally {
    loading.value = false
  }
}

/** 登录后才批量查询收藏状态，游客浏览不触发请求 */
async function loadFavorites() {
  if (!auth.isLoggedIn || !products.value.length) {
    favoriteIds.value = new Set()
    return
  }
  const ids = await favoriteApi
    .ids(products.value.map((item) => item.id))
    .catch(() => [] as number[])
  favoriteIds.value = new Set(ids)
}

async function toggleFavorite(product: ProductListItem) {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const next = new Set(favoriteIds.value)
  if (next.has(product.id)) {
    await favoriteApi.remove(product.id)
    next.delete(product.id)
    ElMessage.success('已取消收藏')
  } else {
    await favoriteApi.add(product.id)
    next.add(product.id)
    ElMessage.success('已收藏')
  }
  favoriteIds.value = next
}

async function toggleFollow() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  followPending.value = true
  try {
    followState.value = followState.value.following
      ? await shopFollowApi.unfollow(shopId)
      : await shopFollowApi.follow(shopId)
    if (shop.value) {
      shop.value.followers = followState.value.followers
    }
    ElMessage.success(followState.value.following ? '已关注该店铺' : '已取消关注')
  } finally {
    followPending.value = false
  }
}

async function loadFollowState() {
  followState.value = await shopFollowApi.status(shopId).catch(() => followState.value)
}

function search() {
  page.value = 0
  loadProducts()
}

function handlePage(current: number) {
  page.value = current
  loadProducts()
}

onMounted(async () => {
  await loadShop()
  if (notFound.value) {
    return
  }
  await Promise.all([loadProducts(), loadFollowState()])
})
</script>

<template>
  <div class="shop-page">
    <el-empty v-if="notFound" description="店铺不存在或已停止营业" class="empty-block">
      <el-button type="primary" @click="router.push('/')">去首页逛逛</el-button>
    </el-empty>

    <template v-else>
      <div class="shop-header page-card">
        <div class="avatar">{{ (shop?.name || '店').slice(0, 1) }}</div>
        <div class="shop-info">
          <div class="shop-name">{{ shop?.name }}</div>
          <div class="muted desc">{{ shop?.description || '这家店铺还没有简介' }}</div>
          <div class="meta">
            <span>在售商品 {{ shop?.productCount ?? 0 }}</span>
            <el-divider direction="vertical" />
            <span>关注 {{ followState.followers }}</span>
            <el-divider direction="vertical" />
            <span>开店于 {{ (shop?.createdAt || '').slice(0, 10) }}</span>
          </div>
        </div>
        <el-button
          class="follow-btn"
          :type="followState.following ? 'default' : 'danger'"
          :loading="followPending"
          @click="toggleFollow"
        >
          {{ followState.following ? '已关注' : '关注店铺' }}
        </el-button>
      </div>

      <div class="page-card toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索店内商品"
          clearable
          class="search-input"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="sort" class="sort-select" @change="search">
          <el-option label="综合排序" value="default" />
          <el-option label="销量优先" value="sales" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="最新上架" value="newest" />
        </el-select>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>

      <div v-loading="loading" class="product-wrap">
        <div v-if="products.length" class="product-grid">
          <ProductCard
            v-for="item in products"
            :key="item.id"
            :product="item"
            :favorited="favoriteIds.has(item.id)"
            @toggle="toggleFavorite"
          />
        </div>
        <el-empty v-else-if="!loading" description="该店铺暂无在售商品" class="empty-block" />

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
    </template>
  </div>
</template>

<style scoped>
.shop-header {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #c81623, #ef5b6b);
}

.shop-info {
  flex: 1;
  min-width: 0;
}

.shop-name {
  font-size: 20px;
  font-weight: 700;
}

.desc {
  margin: 6px 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  font-size: 13px;
  color: #909399;
}

.follow-btn {
  flex: none;
}

.toolbar {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  padding: 14px 16px;
}

.search-input {
  max-width: 280px;
}

.sort-select {
  width: 150px;
}

.product-wrap {
  margin-top: 16px;
  min-height: 260px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .shop-header {
    flex-wrap: wrap;
  }

  .follow-btn {
    width: 100%;
  }

  .toolbar {
    flex-wrap: wrap;
  }

  .search-input {
    max-width: 100%;
    flex: 1;
  }
}
</style>
