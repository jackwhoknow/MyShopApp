<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '@/api/favorite'
import type { FavoriteProduct } from '@/types'

const list = ref<FavoriteProduct[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(12)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await favoriteApi.mine({ page: page.value, size: size.value })
    list.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handlePage(current: number) {
  page.value = current
  load()
}

async function remove(productId: number) {
  await favoriteApi.remove(productId)
  ElMessage.success('已取消收藏')
  // 当前页删空时退回上一页，避免停在空白页
  if (list.value.length === 1 && page.value > 0) {
    page.value -= 1
  }
  await load()
}

function isOffShelf(item: FavoriteProduct) {
  return item.status !== 'ON_SHELF'
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">我的收藏</h2>

    <div v-loading="loading" class="list-wrap">
      <div v-if="list.length" class="product-grid">
        <div v-for="item in list" :key="item.productId" class="favorite-card">
          <router-link :to="`/products/${item.productId}`" class="cover">
            <el-image :src="item.mainImage" fit="cover" class="cover-img">
              <template #error>
                <div class="img-fallback">暂无图片</div>
              </template>
            </el-image>
            <el-tag v-if="isOffShelf(item)" class="off-shelf" type="info" size="small">
              已下架
            </el-tag>
          </router-link>
          <div class="info">
            <router-link :to="`/products/${item.productId}`" class="name">{{ item.name }}</router-link>
            <div class="subtitle">{{ item.subtitle || '\u00a0' }}</div>
            <div class="bottom">
              <span class="price">
                <template v-if="item.minPrice === item.maxPrice">
                  ¥{{ item.minPrice.toFixed(2) }}
                </template>
                <template v-else>
                  ¥{{ item.minPrice.toFixed(2) }} 起
                </template>
              </span>
              <span class="muted">已售 {{ item.sales }}</span>
            </div>
            <div class="footer">
              <span class="shop muted">{{ item.shopName }}</span>
              <el-button link type="danger" size="small" @click="remove(item.productId)">
                取消收藏
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="还没有收藏任何商品" class="empty-block" />

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
  </div>
</template>

<style scoped>
.list-wrap {
  min-height: 320px;
}

.favorite-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.favorite-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.cover {
  position: relative;
  display: block;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.off-shelf {
  position: absolute;
  top: 8px;
  left: 8px;
}

.info {
  padding: 12px 14px 14px;
}

.name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-primary);
}

.name:hover {
  color: #c81623;
}

.subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom {
  margin-top: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.price {
  color: #c81623;
  font-weight: 700;
  font-size: 17px;
}

.footer {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.shop {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
