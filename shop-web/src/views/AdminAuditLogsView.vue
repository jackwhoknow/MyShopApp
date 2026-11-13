<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminAuditApi } from '@/api/adminAudit'
import type { AdminAuditLog } from '@/types'

const keyword = ref('')
const list = ref<AdminAuditLog[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await adminAuditApi.list({
      keyword: keyword.value.trim() || undefined,
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

function formatTime(value?: string) {
  return value?.replace('T', ' ').slice(0, 19) || '—'
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">操作审计</h2>

    <div class="page-card toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索操作人、接口或 IP"
        clearable
        class="keyword-input"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <div class="page-card" v-loading="loading">
      <el-table :data="list" empty-text="暂无审计日志">
        <el-table-column label="操作人" width="140">
          <template #default="{ row }">{{ row.operatorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="请求" min-width="260">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" class="method">{{ row.method }}</el-tag>
            <span class="uri">{{ row.uri }}</span>
            <div v-if="row.queryString" class="muted query">?{{ row.queryString }}</div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small" effect="light">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态码" width="90" />
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.durationMs }} ms</template>
        </el-table-column>
        <el-table-column label="IP" width="140">
          <template #default="{ row }">{{ row.ip || '—' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
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

.keyword-input {
  max-width: 320px;
}

.method {
  margin-right: 8px;
}

.uri {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  word-break: break-all;
}

.query {
  margin-top: 2px;
  word-break: break-all;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
