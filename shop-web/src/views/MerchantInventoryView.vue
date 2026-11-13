<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantInventoryApi } from '@/api/merchantInventory'
import type { StockAlert, StockAlertSummary } from '@/types'

const productStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  ON_SHELF: '已上架',
  OFF_SHELF: '已下架'
}

const productStatusTypes: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  ON_SHELF: 'success',
  OFF_SHELF: 'info'
}

const summary = ref<StockAlertSummary>({
  outOfStockCount: 0,
  lowStockCount: 0,
  totalSkuCount: 0,
  threshold: 5
})
const thresholdInput = ref(5)
const thresholdSaving = ref(false)

const keyword = ref('')
const alerts = ref<StockAlert[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const restockVisible = ref(false)
const restockTarget = ref<StockAlert>()
const restockValue = ref(0)
const restocking = ref(false)

async function loadSummary() {
  summary.value = await merchantInventoryApi.summary()
  thresholdInput.value = summary.value.threshold
}

async function loadAlerts() {
  loading.value = true
  try {
    const result = await merchantInventoryApi.alerts({
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: size.value
    })
    alerts.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 0
  loadAlerts()
}

function handlePage(current: number) {
  page.value = current
  loadAlerts()
}

async function saveThreshold() {
  thresholdSaving.value = true
  try {
    summary.value = await merchantInventoryApi.updateThreshold(thresholdInput.value)
    ElMessage.success('预警阈值已更新')
    page.value = 0
    await loadAlerts()
  } finally {
    thresholdSaving.value = false
  }
}

function openRestock(row: StockAlert) {
  restockTarget.value = row
  restockValue.value = Math.max(row.stock + 10, summary.value.threshold + 1)
  restockVisible.value = true
}

async function saveRestock() {
  if (!restockTarget.value) return
  restocking.value = true
  try {
    await merchantInventoryApi.restock(restockTarget.value.skuId, restockValue.value)
    ElMessage.success('补货成功，库存已更新')
    restockVisible.value = false
    await Promise.all([loadSummary(), loadAlerts()])
  } finally {
    restocking.value = false
  }
}

function stockTagType(stock: number) {
  return stock === 0 ? 'danger' : 'warning'
}

onMounted(() => {
  loadSummary()
  loadAlerts()
})
</script>

<template>
  <div>
    <div class="head">
      <h2 class="section-title">库存预警</h2>
      <div class="threshold-box">
        <span class="muted">预警阈值（库存不高于）</span>
        <el-input-number v-model="thresholdInput" :min="0" :max="9999" size="small" controls-position="right" />
        <el-button size="small" type="primary" :loading="thresholdSaving" @click="saveThreshold">保存</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card danger">
        <div class="summary-label">缺货 SKU</div>
        <div class="summary-value">{{ summary.outOfStockCount }}</div>
      </div>
      <div class="summary-card warning">
        <div class="summary-label">低库存 SKU</div>
        <div class="summary-value">{{ summary.lowStockCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">SKU 总数</div>
        <div class="summary-value">{{ summary.totalSkuCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">当前预警阈值</div>
        <div class="summary-value">{{ summary.threshold }}</div>
      </div>
    </div>

    <div class="page-card search-bar">
      <el-input
        v-model="keyword"
        class="search-keyword"
        clearable
        placeholder="搜索商品名称"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <div class="page-card table-card" v-loading="loading">
      <el-table :data="alerts" empty-text="暂无库存预警，库存状态良好">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image :src="row.image" fit="cover" class="thumb">
                <template #error>
                  <div class="img-fallback">无图</div>
                </template>
              </el-image>
              <div class="product-info">
                <div class="product-name">{{ row.productName }}</div>
                <div class="muted">{{ row.specDesc }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="productStatusTypes[row.productStatus] || 'info'" effect="plain" size="small">
              {{ productStatusLabels[row.productStatus] || row.productStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计销量" width="100">
          <template #default="{ row }">
            <span class="muted">{{ row.sales }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前库存" width="120">
          <template #default="{ row }">
            <el-tag :type="stockTagType(row.stock)" effect="light">
              {{ row.stock === 0 ? '缺货' : row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRestock(row)">补货</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="restockVisible" title="快速补货" width="420px">
      <template v-if="restockTarget">
        <div class="restock-product">
          <div class="product-name">{{ restockTarget.productName }}</div>
          <div class="muted">{{ restockTarget.specDesc }} · 当前库存 {{ restockTarget.stock }}</div>
        </div>
        <el-form label-width="90px">
          <el-form-item label="新库存">
            <el-input-number v-model="restockValue" :min="0" :max="999999" controls-position="right" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="restockVisible = false">取消</el-button>
        <el-button type="primary" :loading="restocking" @click="saveRestock">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.threshold-box {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  padding: 16px 20px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.summary-card.danger .summary-value {
  color: #f56c6c;
}

.summary-card.warning .summary-value {
  color: #e6a23c;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 22px;
}

.search-keyword {
  flex: 1;
  max-width: 360px;
}

.table-card {
  padding: 14px 22px 20px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  flex-shrink: 0;
}

.product-info {
  min-width: 0;
}

.product-name {
  font-weight: 600;
  color: #303133;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.restock-product {
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-keyword {
    max-width: none;
  }
}
</style>
