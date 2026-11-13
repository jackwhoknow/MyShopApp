<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUserApi } from '@/api/adminUser'
import { useAuthStore } from '@/stores/auth'
import type { AdminUser } from '@/types'

const auth = useAuthStore()

const roleLabels: Record<string, string> = {
  BUYER: '买家',
  MERCHANT: '商家',
  ADMIN: '管理员'
}

const roleTypes: Record<string, 'primary' | 'warning' | 'danger'> = {
  BUYER: 'primary',
  MERCHANT: 'warning',
  ADMIN: 'danger'
}

const keyword = ref('')
const role = ref('')
const status = ref('')
const users = ref<AdminUser[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await adminUserApi.list({
      keyword: keyword.value.trim() || undefined,
      role: role.value || undefined,
      status: status.value || undefined,
      page: page.value,
      size: size.value
    })
    users.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 0
  load()
}

function resetFilters() {
  keyword.value = ''
  role.value = ''
  status.value = ''
  search()
}

function handlePage(current: number) {
  page.value = current
  load()
}

/** 禁用/启用账号，禁用后该用户将无法登录 */
async function toggleStatus(user: AdminUser) {
  const next = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = next === 'DISABLED' ? '禁用' : '启用'
  await ElMessageBox.confirm(
    next === 'DISABLED'
      ? `确定禁用账号「${user.username}」吗？禁用后该用户将无法登录。`
      : `确定启用账号「${user.username}」吗？`,
    `${action}账号`,
    {
      type: next === 'DISABLED' ? 'warning' : 'info',
      confirmButtonText: `确定${action}`,
      cancelButtonText: '取消'
    }
  )
  await adminUserApi.updateStatus(user.id, next)
  ElMessage.success(`已${action}账号「${user.username}」`)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">用户管理</h2>

    <div class="page-card toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名或昵称"
        clearable
        class="keyword-input"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="role" placeholder="全部角色" class="filter-select" @change="search">
        <el-option label="全部角色" value="" />
        <el-option label="买家" value="BUYER" />
        <el-option label="商家" value="MERCHANT" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" class="filter-select" @change="search">
        <el-option label="全部状态" value="" />
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已禁用" value="DISABLED" />
      </el-select>
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <div class="page-card" v-loading="loading">
      <el-table :data="users" empty-text="没有符合条件的用户">
        <el-table-column label="用户名" min-width="150">
          <template #default="{ row }">
            <div class="username">
              {{ row.username }}
              <el-tag v-if="row.id === auth.user?.id" size="small" type="info" effect="plain">
                当前账号
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="昵称 / 手机号" min-width="180">
          <template #default="{ row }">
            <div>{{ row.nickname || '未设置昵称' }}</div>
            <div class="muted">{{ row.phone || '未绑定手机号' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag
              v-for="item in row.roles"
              :key="item"
              size="small"
              effect="plain"
              :type="roleTypes[item] || 'info'"
              class="role-tag"
            >
              {{ roleLabels[item] || item }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="light">
              {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 19) || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              link
              type="danger"
              :disabled="row.id === auth.user?.id"
              @click="toggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button v-else link type="success" @click="toggleStatus(row)">启用</el-button>
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

.keyword-input {
  max-width: 280px;
}

.filter-select {
  width: 140px;
}

.username {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.role-tag {
  margin-right: 6px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
