<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantCouponApi } from '@/api/merchantCoupon'
import type { MerchantCoupon } from '@/types'

const stateMeta: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  RUNNING: { label: '进行中', type: 'success' },
  NOT_STARTED: { label: '未开始', type: 'warning' },
  EXPIRED: { label: '已结束', type: 'info' },
  DISABLED: { label: '已停用', type: 'info' }
}

const loading = ref(false)
const coupons = ref<MerchantCoupon[]>([])
const createVisible = ref(false)
const submitting = ref(false)
const togglingId = ref<number>()

const form = reactive({
  title: '',
  thresholdAmount: 100,
  discountAmount: 10,
  totalCount: 100,
  dateRange: [] as string[]
})

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function load() {
  loading.value = true
  try {
    coupons.value = await merchantCouponApi.list()
  } catch {
    coupons.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.title = ''
  form.thresholdAmount = 100
  form.discountAmount = 10
  form.totalCount = 100
  form.dateRange = []
  createVisible.value = true
}

async function submitCreate() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写券名称')
    return
  }
  if (!form.thresholdAmount || !form.discountAmount) {
    ElMessage.warning('请填写使用门槛和减免金额')
    return
  }
  if (form.discountAmount >= form.thresholdAmount) {
    ElMessage.warning('减免金额必须小于使用门槛')
    return
  }
  if (!form.totalCount || form.totalCount < 1) {
    ElMessage.warning('发放数量至少为 1')
    return
  }
  if (form.dateRange.length !== 2) {
    ElMessage.warning('请选择有效期')
    return
  }

  submitting.value = true
  try {
    await merchantCouponApi.create({
      title: form.title.trim(),
      thresholdAmount: form.thresholdAmount,
      discountAmount: form.discountAmount,
      totalCount: form.totalCount,
      startAt: form.dateRange[0],
      endAt: form.dateRange[1]
    })
    createVisible.value = false
    ElMessage.success('优惠券已创建，买家可在领券中心领取')
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggle(coupon: MerchantCoupon) {
  togglingId.value = coupon.id
  try {
    await merchantCouponApi.updateStatus(coupon.id, !coupon.enabled)
    ElMessage.success(coupon.enabled ? '优惠券已停用，不再发放' : '优惠券已启用')
    await load()
  } finally {
    togglingId.value = undefined
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2 class="section-title">优惠券管理</h2>
      <el-button type="primary" @click="openCreate">新建优惠券</el-button>
    </div>

    <div class="page-card">
      <el-table :data="coupons" size="default">
        <el-table-column prop="title" label="券名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="优惠" width="160">
          <template #default="{ row }">满 {{ row.thresholdAmount }} 减 {{ row.discountAmount }}</template>
        </el-table-column>
        <el-table-column label="发放情况" width="160">
          <template #default="{ row }">
            <div>已领 {{ row.claimedCount }} / {{ row.totalCount }}</div>
            <div class="muted">剩余 {{ row.remainingCount }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="usedCount" label="已使用" width="90" align="right" />
        <el-table-column label="有效期" min-width="180">
          <template #default="{ row }">
            <div class="muted">{{ formatTime(row.startAt) }}</div>
            <div class="muted">至 {{ formatTime(row.endAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="stateMeta[row.state]?.type || 'info'" effect="light">
              {{ stateMeta[row.state]?.label || row.state }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              link
              :type="row.enabled ? 'danger' : 'primary'"
              :loading="togglingId === row.id"
              @click="toggle(row)"
            >
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有优惠券，创建一张试试" :image-size="80" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="新建优惠券" width="460px">
      <el-form label-width="90px">
        <el-form-item label="券名称" required>
          <el-input v-model="form.title" maxlength="50" placeholder="例如：新客满100减10" />
        </el-form-item>
        <el-form-item label="使用门槛" required>
          <el-input-number v-model="form.thresholdAmount" :min="0.01" :precision="2" :step="10" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="减免金额" required>
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" :step="1" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="发放数量" required>
          <el-input-number v-model="form.totalCount" :min="1" :step="10" />
          <span class="unit">张</span>
        </el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker
            v-model="form.dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head .section-title {
  margin: 0;
}

.unit {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
