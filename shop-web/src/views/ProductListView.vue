<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { categoryApi } from '@/api/category'
import { contentApi } from '@/api/content'
import { favoriteApi } from '@/api/favorite'
import { productApi } from '@/api/product'
import ProductCard from '@/components/ProductCard.vue'
import { useAuthStore } from '@/stores/auth'
import type { Announcement, Banner, CategoryNode, HomeContent, ProductListItem } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const keyword = ref('')
const categoryId = ref<number | undefined>()
const sort = ref('default')
const minPrice = ref<number | null>(null)
const maxPrice = ref<number | null>(null)
const categories = ref<CategoryNode[]>([])
const products = ref<ProductListItem[]>([])
const favoriteIds = ref<Set<number>>(new Set())
const total = ref(0)
const page = ref(0)
const size = ref(12)
const loading = ref(false)

const home = ref<HomeContent>({ banners: [], announcements: [] })
const announcementVisible = ref(false)
const activeAnnouncement = ref<Announcement>()

async function load() {
  loading.value = true
  try {
    const result = await productApi.list({
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value,
      minPrice: minPrice.value ?? undefined,
      maxPrice: maxPrice.value ?? undefined,
      sort: sort.value || undefined,
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
  const ids = await favoriteApi.ids(products.value.map((item) => item.id)).catch(() => [] as number[])
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
    ElMessage.success('已加入收藏')
  }
  favoriteIds.value = next
}

function search() {
  if (minPrice.value != null && maxPrice.value != null && minPrice.value > maxPrice.value) {
    ElMessage.warning('最低价不能高于最高价')
    return
  }
  page.value = 0
  load()
}

/** 重置关键词、分类、排序与价格区间 */
function resetFilters() {
  keyword.value = ''
  categoryId.value = undefined
  sort.value = 'default'
  minPrice.value = null
  maxPrice.value = null
  search()
}

function handlePage(current: number) {
  page.value = current
  load()
}

/** 轮播无图片时使用的渐变色，按顺序循环 */
const bannerGradients = [
  'linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%)',
  'linear-gradient(135deg, #0ea5e9 0%, #2563eb 60%, #1e40af 100%)',
  'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
  'linear-gradient(135deg, #10b981 0%, #059669 100%)'
]

function bannerStyle(banner: Banner, index: number) {
  if (banner.imageUrl) {
    return {
      backgroundImage: `url(${banner.imageUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return { backgroundImage: bannerGradients[index % bannerGradients.length] }
}

function openBanner(banner: Banner) {
  if (banner.linkUrl) {
    router.push(banner.linkUrl)
  }
}

function openAnnouncement(announcement: Announcement) {
  activeAnnouncement.value = announcement
  announcementVisible.value = true
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

onMounted(async () => {
  load()
  categories.value = await categoryApi.tree().catch(() => [])
  contentApi.home().then((data) => { home.value = data }).catch(() => undefined)
})
</script>

<template>
  <div>
    <section v-if="home.banners.length || home.announcements.length" class="home-hero">
      <div v-if="home.banners.length" class="banner-wrap">
        <el-carousel height="240px" :interval="4500" arrow="always">
          <el-carousel-item v-for="(banner, index) in home.banners" :key="banner.id">
            <div class="banner-slide" :style="bannerStyle(banner, index)" @click="openBanner(banner)">
              <div class="banner-text">
                <h3>{{ banner.title }}</h3>
                <span v-if="banner.linkUrl" class="banner-link">立即查看 →</span>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <div v-if="home.announcements.length" class="announce-bar page-card">
        <el-icon class="announce-icon"><Bell /></el-icon>
        <span class="announce-label">平台公告</span>
        <div class="announce-items">
          <span
            v-for="item in home.announcements.slice(0, 3)"
            :key="item.id"
            class="announce-item"
            @click="openAnnouncement(item)"
          >
            <el-tag v-if="item.pinned" size="small" type="danger" effect="plain" class="pin-tag">置顶</el-tag>
            {{ item.title }}
          </span>
        </div>
      </div>
    </section>

    <div class="page-card toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        class="search-input"
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button :icon="Search" @click="search">搜索</el-button>
        </template>
      </el-input>

      <el-tree-select
        v-model="categoryId"
        :data="categories"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        check-strictly
        clearable
        placeholder="全部分类"
        class="category-select"
        @change="search"
      />

      <el-select v-model="sort" class="sort-select" @change="search">
        <el-option label="综合排序" value="default" />
        <el-option label="销量优先" value="sales" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
        <el-option label="最新上架" value="newest" />
      </el-select>

      <div class="price-range">
        <el-input-number
          v-model="minPrice"
          :min="0"
          :controls="false"
          placeholder="最低价"
          class="price-input"
        />
        <span class="dash">-</span>
        <el-input-number
          v-model="maxPrice"
          :min="0"
          :controls="false"
          placeholder="最高价"
          class="price-input"
        />
      </div>

      <el-button type="primary" @click="search">筛选</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <div v-loading="loading" class="list-wrap">
      <div v-if="products.length" class="product-grid">
        <ProductCard
          v-for="item in products"
          :key="item.id"
          :product="item"
          :favorited="favoriteIds.has(item.id)"
          @toggle="toggleFavorite"
        />
      </div>
      <el-empty v-else-if="!loading" description="没有找到相关商品" class="empty-block" />

      <div class="pagination">
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

    <el-dialog v-model="announcementVisible" :title="activeAnnouncement?.title || '平台公告'" width="560px">
      <div class="announce-meta">{{ formatDateTime(activeAnnouncement?.publishedAt) }}</div>
      <div class="announce-content">{{ activeAnnouncement?.content }}</div>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 18px;
}

.search-input {
  max-width: 420px;
}

.category-select {
  width: 220px;
}

.sort-select {
  width: 150px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-input {
  width: 110px;
}

.dash {
  color: #909399;
}

.list-wrap {
  min-height: 320px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

.home-hero {
  margin-bottom: 18px;
}

.banner-wrap {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  margin-bottom: 14px;
}

.banner-slide {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 64px;
  cursor: pointer;
}

.banner-text h3 {
  margin: 0 0 10px;
  font-size: 26px;
  color: #ffffff;
  letter-spacing: 1px;
}

.banner-link {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.announce-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
}

.announce-icon {
  color: #e6a23c;
}

.announce-label {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
}

.announce-items {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  min-width: 0;
}

.announce-item {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  white-space: nowrap;
}

.announce-item:hover {
  color: #409eff;
}

.pin-tag {
  margin-right: 6px;
}

.announce-meta {
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
}

.announce-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .category-select,
  .sort-select,
  .price-range {
    max-width: none;
    width: 100%;
  }

  .price-input {
    flex: 1;
    width: auto;
  }

  .banner-slide {
    padding: 0 24px;
  }

  .banner-text h3 {
    font-size: 20px;
  }

  .announce-items {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
