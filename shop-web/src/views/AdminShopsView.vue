<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminShopApi } from '@/api/adminShop'
import type { Shop } from '@/types'

const statusTabs = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'ACTIVE' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '全部', value: '' }
]

const statusLabels: Record<string, string> = {
  PENDING: '待审核',
  ACTIVE: '营业中',
  REJECTED: '已驳回'
}

const statusTypes: Record<string, 'warning' | 'success' | 'danger'> = {
  PENDING: 'warning',
  ACTIVE: 'success',
  REJECTED: 'danger'
}

const activeStatus = ref('PENDING')
const shops = ref<Shop[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const rejectVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectTarget = ref<Shop>()
const rejectReason = ref('')

const isEmpty = computed(() => !loading.value && shops.value.length === 0)

async function load() {
  loading.value = true
  try {
    const result = await adminShopApi.list({
      status: activeStatus.value || undefined,
      page: page.value,
      size: size.value
    })
    shops.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  page.value = 0
  load()
}

function handlePage(current: number) {
  page.value = current
  load()
}

async function approve(shop: Shop) {
  await ElMessageBox.confirm(`确定通过「${shop.name}」的开店申请吗？`, '通过审核', {
    type: 'success',
    confirmButtonText: '确定通过',
    cancelButtonText: '取消'
  })
  await adminShopApi.approve(shop.id)
  ElMessage.success('已通过该店铺的申请')
  load()
}

function openReject(shop: Shop) {
  rejectTarget.value = shop
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectTarget.value) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  rejectSubmitting.value = true
  try {
    await adminShopApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    ElMessage.success('已驳回该店铺的申请')
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
    <h2 class="section-title">店铺审核</h2>

    <el-tabs
      :model-value="activeStatus"
      class="status-tabs"
      @tab-change="(name: string) => { activeStatus = name; handleTabChange() }"
    >
      <el-tab-pane v-for="tab in statusTabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div class="page-card" v-loading="loading">
      <el-table :data="shops" empty-text="暂无店铺申请">
        <el-table-column label="店铺名称" min-width="180">
          <template #default="{ row }">
            <div class="shop-name">{{ row.name }}</div>
            <div v-if="row.description" class="muted">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="ownerUsername" label="店主" width="140" />
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ row.createdAt?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status] || 'info'" effect="light">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="180">
          <template #default="{ row }">
            <span class="muted">{{ row.rejectReason || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isEmpty" class="empty-block">
        <el-empty description="暂无店铺申请" :image-size="80" />
      </div>

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

    <el-dialog v-model="rejectVisible" title="驳回店铺申请" width="460px">
      <div class="reject-target">
        店铺：<strong>{{ rejectTarget?.name }}</strong>（{{ rejectTarget?.ownerUsername }}）
      </div>
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="请输入驳回原因，商家会在“我的店铺”里看到"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.status-tabs {
  margin-bottom: 8px;
}

.shop-name {
  font-weight: 600;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.reject-target {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}
</style>
