<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminNotificationApi } from '@/api/adminNotification'
import type { AdminBroadcast, AudienceCounts, NotificationAudience } from '@/types'

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<{
  title: string
  content: string
  audience: NotificationAudience
  link: string
}>({
  title: '',
  content: '',
  audience: 'ALL',
  link: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请填写通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写通知内容', trigger: 'blur' }],
  audience: [{ required: true, message: '请选择发送范围', trigger: 'change' }]
}

const counts = ref<AudienceCounts>({ all: 0, buyer: 0, merchant: 0 })

const audienceOptions = computed(() => [
  { value: 'ALL' as NotificationAudience, label: '全部用户', count: counts.value.all },
  { value: 'BUYER' as NotificationAudience, label: '仅买家', count: counts.value.buyer },
  { value: 'MERCHANT' as NotificationAudience, label: '仅商家', count: counts.value.merchant }
])

const audienceLabels: Record<NotificationAudience, string> = {
  ALL: '全部用户',
  BUYER: '仅买家',
  MERCHANT: '仅商家'
}

const audienceTagTypes: Record<NotificationAudience, 'primary' | 'success' | 'warning'> = {
  ALL: 'primary',
  BUYER: 'success',
  MERCHANT: 'warning'
}

function audienceLabel(value: string) {
  return audienceLabels[value as NotificationAudience] || value
}

function audienceTagType(value: string): 'primary' | 'success' | 'warning' | 'info' {
  return audienceTagTypes[value as NotificationAudience] || 'info'
}

function currentCount() {
  if (form.audience === 'BUYER') return counts.value.buyer
  if (form.audience === 'MERCHANT') return counts.value.merchant
  return counts.value.all
}

const history = ref<AdminBroadcast[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const historyLoading = ref(false)

async function loadCounts() {
  counts.value = await adminNotificationApi.audienceCounts()
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const result = await adminNotificationApi.history({ page: page.value, size: size.value })
    history.value = result.list
    total.value = result.total
  } finally {
    historyLoading.value = false
  }
}

function handlePage(current: number) {
  page.value = current
  loadHistory()
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const count = currentCount()
  if (count === 0) {
    ElMessage.warning('当前范围内没有可接收通知的用户')
    return
  }

  try {
    await ElMessageBox.confirm(
      `将向 ${count} 位「${audienceLabels[form.audience]}」发送通知，确认发送？`,
      '确认群发',
      { type: 'warning', confirmButtonText: '发送', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const record = await adminNotificationApi.broadcast({
      title: form.title.trim(),
      content: form.content.trim(),
      audience: form.audience,
      link: form.link.trim() || undefined
    })
    ElMessage.success(`已发送给 ${record.recipientCount} 位用户`)
    form.title = ''
    form.content = ''
    form.link = ''
    page.value = 0
    await Promise.all([loadCounts(), loadHistory()])
  } finally {
    submitting.value = false
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}

onMounted(() => {
  loadCounts()
  loadHistory()
})
</script>

<template>
  <div>
    <h2 class="section-title">通知群发</h2>

    <div class="page-card form-card">
      <div class="card-title">发送通知</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="通知标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="例如：双十一大促活动提醒"
          />
        </el-form-item>

        <el-form-item label="发送范围" prop="audience">
          <div class="audience-list">
            <div
              v-for="option in audienceOptions"
              :key="option.value"
              class="audience-item"
              :class="{ active: form.audience === option.value }"
              @click="form.audience = option.value"
            >
              <div class="audience-label">{{ option.label }}</div>
              <div class="audience-count">{{ option.count }} 人</div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="通知内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="填写通知正文，将展示在用户的站内消息中"
          />
        </el-form-item>

        <el-form-item label="跳转地址（可选）" prop="link">
          <el-input v-model="form.link" maxlength="200" placeholder="例如 /admin/orders，需以 / 开头" />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" :loading="submitting" @click="submit">发送通知</el-button>
          <span class="muted">发送后立即写入选中用户的站内消息，不可撤回</span>
        </div>
      </el-form>
    </div>

    <div class="page-card history-card" v-loading="historyLoading">
      <div class="card-title">发送记录</div>
      <el-table :data="history" empty-text="暂无发送记录">
        <el-table-column label="通知标题" min-width="180">
          <template #default="{ row }">
            <div class="broadcast-title">{{ row.title }}</div>
            <div v-if="row.link" class="muted">跳转：{{ row.link }}</div>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="240">
          <template #default="{ row }">
            <span class="broadcast-content">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发送范围" width="120">
          <template #default="{ row }">
            <el-tag :type="audienceTagType(row.audience)" effect="plain" size="small">
              {{ audienceLabel(row.audience) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收人数" width="100">
          <template #default="{ row }">{{ row.recipientCount }} 人</template>
        </el-table-column>
        <el-table-column label="发送人" width="110">
          <template #default="{ row }">{{ row.senderName }}</template>
        </el-table-column>
        <el-table-column label="发送时间" width="170">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createdAt) }}</span>
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
.form-card {
  margin-bottom: 16px;
  padding: 16px 22px 20px;
}

.card-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.audience-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.audience-item {
  min-width: 140px;
  padding: 10px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.audience-item:hover {
  border-color: #409eff;
}

.audience-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.audience-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.audience-count {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.history-card {
  padding: 16px 22px 20px;
}

.broadcast-title {
  font-weight: 600;
  color: #303133;
}

.broadcast-content {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #606266;
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}
</style>
