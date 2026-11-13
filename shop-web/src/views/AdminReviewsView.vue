<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminReviewApi } from '@/api/adminReview'
import type { Review } from '@/types'

const status = ref('')
const keyword = ref('')
const list = ref<Review[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const statusLabels: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

const statusTypes: Record<string, 'warning' | 'success' | 'danger'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger'
}

async function load() {
  loading.value = true
  try {
    const result = await adminReviewApi.list({
      status: status.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    list.value = result.list
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

async function approve(review: Review) {
  await ElMessageBox.confirm('通过后该评价将在商品页展示，确定通过吗？', '审核通过', {
    type: 'info',
    confirmButtonText: '确定通过',
    cancelButtonText: '取消'
  })
  await adminReviewApi.approve(review.id)
  ElMessage.success('已通过')
  load()
}

async function reject(review: Review) {
  const result = await ElMessageBox.prompt('请输入驳回原因（可选，将展示给评价作者）', '驳回评价', {
    inputPlaceholder: '如：含广告引流内容',
    inputValidator: (value) => (value || '').length <= 200 || '不能超过 200 字',
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await adminReviewApi.reject(review.id, result.value || undefined)
  ElMessage.success('已驳回')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">评价审核</h2>

    <div class="page-card toolbar">
      <el-select v-model="status" placeholder="全部状态" class="filter-select" @change="search">
        <el-option label="全部状态" value="" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索评价内容或商品名"
        clearable
        class="keyword-input"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <div class="page-card" v-loading="loading">
      <el-table :data="list" empty-text="没有评价">
        <el-table-column label="商品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.productName }}</div>
            <div class="muted small">{{ row.skuDesc }}</div>
          </template>
        </el-table-column>
        <el-table-column label="星级" width="130">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '—' }}</template>
        </el-table-column>
        <el-table-column label="图片" width="70">
          <template #default="{ row }">{{ row.images?.length || 0 }} 张</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status]" size="small" effect="light">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'APPROVED'" link type="success" @click="approve(row)">
              通过
            </el-button>
            <el-button v-if="row.status !== 'REJECTED'" link type="danger" @click="reject(row)">
              驳回
            </el-button>
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
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 18px;
}

.filter-select {
  width: 160px;
}

.keyword-input {
  max-width: 260px;
}

.small {
  font-size: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
