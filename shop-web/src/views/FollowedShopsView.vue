<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { shopFollowApi } from '@/api/shopFollow'
import type { FollowedShop } from '@/types'

const list = ref<FollowedShop[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await shopFollowApi.mine()
  } finally {
    loading.value = false
  }
}

async function unfollow(shop: FollowedShop) {
  await shopFollowApi.unfollow(shop.shopId)
  ElMessage.success(`已取消关注「${shop.name}」`)
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">我的关注</h2>

    <div v-loading="loading" class="list-wrap">
      <div v-if="list.length" class="shop-list">
        <div v-for="shop in list" :key="shop.shopId" class="shop-card page-card">
          <div class="shop-main">
            <div class="avatar">{{ shop.name.slice(0, 1) }}</div>
            <div class="shop-info">
              <div class="shop-name">
                <router-link :to="`/shops/${shop.shopId}`" class="shop-link">{{ shop.name }}</router-link>
                <el-tag v-if="shop.status !== 'ACTIVE'" type="info" size="small">已关闭</el-tag>
              </div>
              <div class="muted description">{{ shop.description || '这家店铺还没有简介' }}</div>
              <div class="muted">关注于 {{ shop.followedAt?.replace('T', ' ').slice(0, 19) }}</div>
            </div>
          </div>
          <div class="actions">
            <el-button link type="danger" @click="unfollow(shop)">取消关注</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="还没有关注任何店铺" class="empty-block" />
    </div>
  </div>
</template>

<style scoped>
.list-wrap {
  min-height: 280px;
}

.shop-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.shop-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.shop-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #c81623, #ef5b6b);
}

.shop-info {
  min-width: 0;
}

.shop-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.shop-link {
  color: inherit;
  text-decoration: none;
}

.shop-link:hover {
  color: #c81623;
}

.description {
  margin: 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 620px;
}

.actions {
  flex: none;
}

@media (max-width: 768px) {
  .shop-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .description {
    max-width: 100%;
  }
}
</style>
