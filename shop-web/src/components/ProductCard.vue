<script setup lang="ts">
import type { ProductListItem } from '@/types'

defineProps<{ product: ProductListItem; favorited?: boolean }>()
const emit = defineEmits<{ toggle: [product: ProductListItem] }>()
</script>

<template>
  <router-link :to="`/products/${product.id}`" class="product-card">
    <div class="cover">
      <el-image :src="product.mainImage" fit="cover" lazy class="cover-img">
        <template #error>
          <div class="img-fallback">暂无图片</div>
        </template>
      </el-image>
      <button
        type="button"
        class="favorite-btn"
        :class="{ active: favorited }"
        :title="favorited ? '取消收藏' : '收藏'"
        @click.stop.prevent="emit('toggle', product)"
      >
        <el-icon :size="18">
          <StarFilled v-if="favorited" />
          <Star v-else />
        </el-icon>
      </button>
    </div>
    <div class="info">
      <div class="name">{{ product.name }}</div>
      <div class="subtitle">{{ product.subtitle || '\u00a0' }}</div>
      <div class="bottom">
        <span class="price">
          <template v-if="product.minPrice === product.maxPrice">
            ¥{{ product.minPrice.toFixed(2) }}
          </template>
          <template v-else>
            ¥{{ product.minPrice.toFixed(2) }} 起
          </template>
        </span>
        <span class="muted">已售 {{ product.sales }}</span>
      </div>
      <div class="shop muted">{{ product.shopName }}</div>
    </div>
  </router-link>
</template>

<style scoped>
.product-card {
  display: block;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #ebeef5;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.cover {
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
  position: relative;
}

.favorite-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #909399;
  cursor: pointer;
  transition: color 0.2s, transform 0.2s;
}

.favorite-btn:hover {
  color: #e6a23c;
  transform: scale(1.08);
}

.favorite-btn.active {
  color: #e6a23c;
}

.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}

.info {
  padding: 12px 14px 14px;
}

.name {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.shop {
  margin-top: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
