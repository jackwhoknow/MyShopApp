<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminInvoiceApi } from '@/api/adminInvoice'
import type { Invoice } from '@/types'

const status = ref('')
const list = ref<Invoice[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

const statusLabels: Record<string, string> = {
  PENDING: '待开票',
  ISSUED: '已开票'
}

async function load() {
  loading.value = true
  try {
    const result = await adminInvoiceApi.list({
      status: status.value || undefined,
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

async function issue(invoice: Invoice) {
  await ElMessageBox.confirm(
    `确定将发票「${invoice.title}」标记为已开具吗？`,
    '开具发票',
    { type: 'info', confirmButtonText: '确定开具', cancelButtonText: '取消' }
  )
  await adminInvoiceApi.issue(invoice.id)
  ElMessage.success('已标记为已开票')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">发票管理</h2>

    <div class="page-card toolbar">
      <el-select v-model="status" placeholder="全部状态" class="filter-select" @change="search">
        <el-option label="全部状态" value="" />
        <el-option label="待开票" value="PENDING" />
        <el-option label="已开票" value="ISSUED" />
      </el-select>
      <el-button @click="search">刷新</el-button>
    </div>

    <div class="page-card" v-loading="loading">
      <el-table :data="list" empty-text="没有发票申请">
        <el-table-column prop="orderId" label="订单 ID" width="100" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.type === 'COMPANY' ? 'warning' : 'info'">
              {{ row.type === 'COMPANY' ? '单位' : '个人' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="发票抬头" min-width="160" show-overflow-tooltip />
        <el-table-column label="税号" min-width="160">
          <template #default="{ row }">{{ row.taxNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="160">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ Number(row.amount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ISSUED' ? 'success' : 'warning'" effect="light" size="small">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'ISSUED'"
              link
              type="primary"
              @click="issue(row)"
            >
              标记已开票
            </el-button>
            <span v-else class="muted">已完成</span>
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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
