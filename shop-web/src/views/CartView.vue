<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import type { CartItem } from '@/types'

const router = useRouter()
const cart = useCartStore()

const selected = ref<CartItem[]>([])

const selectedTotal = computed(() =>
  selected.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
)
const selectedCount = computed(() =>
  selected.value.reduce((sum, item) => sum + item.quantity, 0)
)

onMounted(() => {
  cart.load()
})

function handleSelectionChange(rows: CartItem[]) {
  selected.value = rows
}

function selectable(row: CartItem) {
  return row.available
}

async function changeQuantity(item: CartItem, value: number | undefined) {
  if (!value || value < 1) return
  try {
    await cart.updateQuantity(item.id, value)
  } catch {
    await cart.load()
  }
}

async function removeItem(item: CartItem) {
  await ElMessageBox.confirm(`确定从购物车移除「${item.productName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '移除',
    cancelButtonText: '取消'
  })
  await cart.remove(item.id)
  ElMessage.success('已移除')
}

function checkout() {
  if (!selected.value.length) {
    ElMessage.warning('请先勾选要结算的商品')
    return
  }
  const ids = selected.value.map((item) => item.id).join(',')
  router.push({ path: '/checkout', query: { ids } })
}
</script>

<template>
  <div>
    <h2 class="section-title">我的购物车</h2>

    <div v-loading="cart.loading" class="page-card">
      <template v-if="cart.items.length">
        <el-table :data="cart.items" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" :selectable="selectable" />
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div class="product-cell">
                <el-image :src="row.image" fit="cover" class="thumb">
                  <template #error>
                    <div class="img-fallback">无图</div>
                  </template>
                </el-image>
                <div class="product-info">
                  <router-link :to="`/products/${row.productId}`" class="product-name">
                    {{ row.productName }}
                  </router-link>
                  <div class="muted">{{ row.skuDesc }}</div>
                  <div class="muted">{{ row.shopName }}</div>
                  <el-tag v-if="!row.available" type="danger" size="small" effect="plain">库存不足</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ row.price.toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="170">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                :max="Math.min(row.stock, 99)"
                size="small"
                @change="(value: number | undefined) => changeQuantity(row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="130">
            <template #default="{ row }">
              <span class="price">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="cart-footer">
          <div class="summary">
            已选 <strong>{{ selectedCount }}</strong> 件，合计：
            <span class="price total">¥{{ selectedTotal.toFixed(2) }}</span>
          </div>
          <div class="footer-actions">
            <el-button @click="router.push('/')">继续购物</el-button>
            <el-button type="danger" size="large" :disabled="!selected.length" @click="checkout">
              去结算
            </el-button>
          </div>
        </div>
      </template>

      <div v-else class="empty-block">
        <el-empty description="购物车还是空的">
          <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  flex-shrink: 0;
}

.product-info {
  min-width: 0;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.product-name:hover {
  color: #c81623;
}

.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 12px;
}

.summary {
  font-size: 14px;
  color: #606266;
}

.total {
  font-size: 22px;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .cart-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .footer-actions {
    justify-content: flex-end;
  }
}
</style>
