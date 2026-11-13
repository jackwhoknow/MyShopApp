<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminProductApi, type AdminProductStatus } from '@/api/adminProduct'
import type { ProductListItem } from '@/types'

const statusLabels: Record<string, string> = {
  PENDING_REVIEW: '待审核',
  ON_SHELF: '已上架',
  OFF_SHELF: '已下架',
  DRAFT: '草稿'
}

const statusTypes: Record<string, 'info' | 'success' | 'warning'> = {
  PENDING_REVIEW: 'warning',
  ON_SHELF: 'success',
  OFF_SHELF: 'info',
  DRAFT: 'info'
}

const statusFilter = ref<AdminProductStatus | ''>('PENDING_REVIEW')
const keyword = ref('')
const products = ref<ProductListItem[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const rejectVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectProduct = ref<ProductListItem | null>(null)
const rejectReason = ref('')

async function load() {
  loading.value = true
  try {
    const result = await adminProductApi.list({
      status: statusFilter.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    products.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 0
  load()
}

function handlePage(current: number) {
  page.value = current
  load()
}

async function approve(item: ProductListItem) {
  await ElMessageBox.confirm(`确定审核通过「${item.name}」吗？通过后将立即上架。`, '审核通过', {
    type: 'warning',
    confirmButtonText: '确定通过',
    cancelButtonText: '取消'
  })
  await adminProductApi.approve(item.id)
  ElMessage.success('审核通过，商品已上架')
  load()
}

function openReject(item: ProductListItem) {
  rejectProduct.value = item
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  if (!rejectProduct.value) return
  rejectSubmitting.value = true
  try {
    await adminProductApi.reject(rejectProduct.value.id, rejectReason.value.trim())
    ElMessage.success('已驳回，商家可查看驳回理由')
    rejectVisible.value = false
    load()
  } finally {
    rejectSubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">商品审核</h2>

    <div class="page-card">
      <el-select v-model="statusFilter" class="status-select" @change="search">
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已上架" value="ON_SHELF" />
        <el-option label="已下架" value="OFF_SHELF" />
        <el-option label="全部状态" value="" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        class="search-input"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button @click="search">搜索</el-button>
    </div>

    <div class="page-card table-card" v-loading="loading">
      <el-table :data="products" empty-text="暂无商品">
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="prod-cell">
              <el-image :src="row.mainImage" fit="cover" class="thumb">
                <template #error>
                  <div class="img-fallback">无图</div>
                </template>
              </el-image>
              <div class="prod-info">
                <div class="prod-name">{{ row.name }}</div>
                <div class="muted">{{ row.subtitle || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="店铺" width="150" />
        <el-table-column label="价格" width="170">
          <template #default="{ row }">
            ¥{{ row.minPrice.toFixed(2) }} ~ ¥{{ row.maxPrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="170">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status] || 'info'" effect="light">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
            <div v-if="row.rejectReason" class="reject-reason">驳回：{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <span v-else class="muted">—</span>
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

    <el-dialog v-model="rejectVisible" title="驳回商品" width="480px">
      <p class="reject-target">商品：{{ rejectProduct?.name }}</p>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="填写驳回理由，商家可在商品列表看到"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.status-select {
  width: 150px;
  margin-right: 10px;
}

.search-input {
  width: 260px;
  margin-right: 10px;
}

.table-card {
  margin-top: 16px;
}

.prod-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex-shrink: 0;
}

.prod-info {
  min-width: 0;
}

.prod-name {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reject-reason {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-color-danger);
  word-break: break-all;
}

.reject-target {
  margin: 0 0 12px;
  color: var(--el-text-color-regular);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
