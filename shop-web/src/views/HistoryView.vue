<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { historyApi } from '@/api/history'
import type { BrowseHistoryItem } from '@/types'

const list = ref<BrowseHistoryItem[]>([])
const loading = ref(false)
const clearing = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await historyApi.recent(20)
  } finally {
    loading.value = false
  }
}

async function clear() {
  await ElMessageBox.confirm('确定清空全部浏览历史吗？', '清空浏览历史', {
    type: 'warning',
    confirmButtonText: '清空',
    cancelButtonText: '取消'
  })
  clearing.value = true
  try {
    await historyApi.clear()
    list.value = []
    ElMessage.success('浏览历史已清空')
  } finally {
    clearing.value = false
  }
}

function formatTime(value: string) {
  return value?.replace('T', ' ').slice(0, 16) || ''
}

function isOffShelf(item: BrowseHistoryItem) {
  return item.status !== 'ON_SHELF'
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="section-title">最近浏览</h2>
      <el-button v-if="list.length" :loading="clearing" @click="clear">清空历史</el-button>
    </div>

    <div v-loading="loading" class="list-wrap">
      <div v-if="list.length" class="product-grid">
        <div v-for="item in list" :key="item.productId" class="history-card">
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
              <span class="muted">{{ formatTime(item.viewedAt) }} 浏览</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="还没有浏览记录，去逛逛商品吧" class="empty-block">
        <router-link to="/">
          <el-button type="primary">去逛首页</el-button>
        </router-link>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.list-wrap {
  min-height: 320px;
}

.history-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.history-card:hover {
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
  font-size: 12px;
}

.shop {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
