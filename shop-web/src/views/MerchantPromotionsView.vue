<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { promotionApi } from '@/api/promotion'
import type { ShopPromotion } from '@/types'

const items = ref<ShopPromotion[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  title: '',
  thresholdAmount: 100,
  reduceAmount: 10,
  startAt: '',
  endAt: '',
  enabled: true
})

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function load() {
  loading.value = true
  try {
    items.value = await promotionApi.merchantList()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    title: '',
    thresholdAmount: 100,
    reduceAmount: 10,
    startAt: '',
    endAt: '',
    enabled: true
  })
  dialogVisible.value = true
}

function openEdit(item: ShopPromotion) {
  editingId.value = item.id
  Object.assign(form, {
    title: item.title,
    thresholdAmount: item.thresholdAmount,
    reduceAmount: item.reduceAmount,
    startAt: item.startAt,
    endAt: item.endAt,
    enabled: item.enabled
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.title || !form.startAt || !form.endAt) {
    ElMessage.warning('请填写活动名称与起止时间')
    return
  }
  if (form.reduceAmount >= form.thresholdAmount) {
    ElMessage.warning('优惠金额必须小于满减门槛')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await promotionApi.update(editingId.value, { ...form })
    } else {
      await promotionApi.create({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(item: ShopPromotion) {
  await ElMessageBox.confirm(`确认删除活动「${item.title}」？`, '提示', { type: 'warning' })
  await promotionApi.remove(item.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="head">
      <div>
        <h2 class="section-title">满减活动</h2>
        <p class="muted">单店商品合计满门槛自动减价，与优惠券可叠加，按优惠力度取最优</p>
      </div>
      <el-button type="primary" @click="openCreate">新建活动</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="title" label="活动名称" min-width="160" />
      <el-table-column label="满减规则" width="160">
        <template #default="{ row }">满 {{ row.thresholdAmount }} 减 {{ row.reduceAmount }}</template>
      </el-table-column>
      <el-table-column label="开始时间" width="160">
        <template #default="{ row }">{{ formatTime(row.startAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="160">
        <template #default="{ row }">{{ formatTime(row.endAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.running" type="success" size="small">进行中</el-tag>
          <el-tag v-else-if="!row.enabled" type="info" size="small">已停用</el-tag>
          <el-tag v-else type="warning" size="small">未开始/已结束</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!items.length && !loading" description="还没有满减活动" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑满减活动' : '新建满减活动'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="活动名称">
          <el-input v-model="form.title" maxlength="50" placeholder="如：暑期满减" />
        </el-form-item>
        <el-form-item label="满减门槛">
          <el-input-number v-model="form.thresholdAmount" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="优惠金额">
          <el-input-number v-model="form.reduceAmount" :min="0.01" :precision="2" :step="5" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择开始时间"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择结束时间"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.muted {
  margin: 4px 0 0;
  font-size: 13px;
}
</style>
