<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantSeckillApi } from '@/api/seckill'
import { merchantProductApi } from '@/api/merchantProduct'
import type { ProductDetail, ProductListItem, SeckillActivity } from '@/types'

const stateMeta: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  RUNNING: { label: '抢购中', type: 'danger' },
  NOT_STARTED: { label: '未开始', type: 'warning' },
  SOLD_OUT: { label: '已售罄', type: 'info' },
  EXPIRED: { label: '已结束', type: 'info' },
  DISABLED: { label: '已停用', type: 'info' }
}

const loading = ref(false)
const activities = ref<SeckillActivity[]>([])
const products = ref<ProductListItem[]>([])
const productDetail = ref<ProductDetail>()
const detailLoading = ref(false)
const createVisible = ref(false)
const submitting = ref(false)
const togglingId = ref<number>()

const form = reactive({
  productId: undefined as number | undefined,
  skuId: undefined as number | undefined,
  activityPrice: 1,
  totalStock: 1,
  perUserLimit: 1,
  dateRange: [] as string[]
})

const selectedSku = computed(() =>
  productDetail.value?.skus.find((sku) => sku.id === form.skuId)
)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatSkuDesc(activity: SeckillActivity) {
  return activity.skuDesc || '默认规格'
}

async function load() {
  loading.value = true
  try {
    activities.value = await merchantSeckillApi.list()
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

async function loadProducts() {
  try {
    const page = await merchantProductApi.list({ page: 0, size: 100 })
    products.value = page.list.filter((product) => product.status === 'ON_SHELF')
  } catch {
    products.value = []
  }
}

async function onProductChange(productId?: number) {
  form.skuId = undefined
  productDetail.value = undefined
  if (!productId) return
  detailLoading.value = true
  try {
    productDetail.value = await merchantProductApi.detail(productId)
  } finally {
    detailLoading.value = false
  }
}

function onSkuChange(skuId?: number) {
  const sku = productDetail.value?.skus.find((item) => item.id === skuId)
  if (!sku) return
  form.activityPrice = Number((sku.price * 0.8).toFixed(2))
  form.totalStock = Math.max(sku.stock, 1)
  form.perUserLimit = 1
}

async function openCreate() {
  form.productId = undefined
  form.skuId = undefined
  form.activityPrice = 1
  form.totalStock = 1
  form.perUserLimit = 1
  form.dateRange = []
  productDetail.value = undefined
  if (!products.value.length) {
    await loadProducts()
  }
  createVisible.value = true
}

async function submitCreate() {
  if (!form.skuId) {
    ElMessage.warning('请选择秒杀商品规格')
    return
  }
  if (!form.activityPrice || form.activityPrice <= 0) {
    ElMessage.warning('请填写活动价')
    return
  }
  if (selectedSku.value && form.activityPrice >= selectedSku.value.price) {
    ElMessage.warning('活动价必须低于商品原价')
    return
  }
  if (!form.totalStock || form.totalStock < 1) {
    ElMessage.warning('活动库存至少为 1')
    return
  }
  if (selectedSku.value && form.totalStock > selectedSku.value.stock) {
    ElMessage.warning(`活动库存不能超过当前库存（${selectedSku.value.stock}）`)
    return
  }
  if (!form.perUserLimit || form.perUserLimit < 1) {
    ElMessage.warning('每人限购至少为 1')
    return
  }
  if (form.perUserLimit > form.totalStock) {
    ElMessage.warning('每人限购不能超过活动库存')
    return
  }
  if (form.dateRange.length !== 2) {
    ElMessage.warning('请选择活动时间')
    return
  }

  submitting.value = true
  try {
    await merchantSeckillApi.create({
      skuId: form.skuId,
      activityPrice: form.activityPrice,
      totalStock: form.totalStock,
      perUserLimit: form.perUserLimit,
      startAt: form.dateRange[0],
      endAt: form.dateRange[1]
    })
    createVisible.value = false
    ElMessage.success('秒杀活动已创建，买家可在秒杀专区抢购')
    await load()
  } finally {
    submitting.value = false
  }
}

async function toggle(activity: SeckillActivity) {
  togglingId.value = activity.id
  try {
    await merchantSeckillApi.updateStatus(activity.id, !activity.enabled)
    ElMessage.success(activity.enabled ? '活动已停用' : '活动已启用')
    await load()
  } finally {
    togglingId.value = undefined
  }
}

onMounted(async () => {
  await Promise.all([load(), loadProducts()])
})
</script>

<template>
  <div v-loading="loading">
    <div class="page-head">
      <h2 class="section-title">秒杀活动</h2>
      <el-button type="primary" @click="openCreate">新建活动</el-button>
    </div>

    <div class="page-card">
      <el-table :data="activities">
        <el-table-column label="商品" min-width="180">
          <template #default="{ row }">
            <div class="product-name">{{ row.productName }}</div>
            <div class="muted">{{ formatSkuDesc(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="160">
          <template #default="{ row }">
            <div class="seckill-price">¥{{ row.activityPrice }}</div>
            <div class="muted origin">原价 ¥{{ row.originalPrice }}</div>
          </template>
        </el-table-column>
        <el-table-column label="活动库存" width="150">
          <template #default="{ row }">
            <div>已售 {{ row.soldCount }} / {{ row.totalStock }}</div>
            <div class="muted">剩余 {{ row.remainingStock }} 件</div>
          </template>
        </el-table-column>
        <el-table-column label="每人限购" width="100" align="center">
          <template #default="{ row }">{{ row.perUserLimit }} 件</template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="180">
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
          <el-empty description="还没有秒杀活动，创建一场试试" :image-size="80" />
        </template>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="新建秒杀活动" width="520px">
      <el-form label-width="100px">
        <el-form-item label="商品" required>
          <el-select
            v-model="form.productId"
            filterable
            placeholder="选择在售商品"
            style="width: 100%"
            @change="onProductChange"
          >
            <el-option v-for="product in products" :key="product.id" :value="product.id" :label="product.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格" required>
          <el-select
            v-model="form.skuId"
            :disabled="!productDetail"
            :loading="detailLoading"
            placeholder="选择商品规格"
            style="width: 100%"
            @change="onSkuChange"
          >
            <el-option
              v-for="sku in productDetail?.skus || []"
              :key="sku.id"
              :value="sku.id"
              :label="`${sku.specValues.map((item) => item.value).join(' / ') || '默认规格'}（¥${sku.price}，库存 ${sku.stock}）`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动价" required>
          <el-input-number v-model="form.activityPrice" :min="0.01" :precision="2" :step="1" />
          <span class="unit">元</span>
          <span v-if="selectedSku" class="unit">原价 ¥{{ selectedSku.price }}</span>
        </el-form-item>
        <el-form-item label="活动库存" required>
          <el-input-number v-model="form.totalStock" :min="1" :step="1" />
          <span class="unit">件</span>
        </el-form-item>
        <el-form-item label="每人限购" required>
          <el-input-number v-model="form.perUserLimit" :min="1" :step="1" />
          <span class="unit">件</span>
        </el-form-item>
        <el-form-item label="活动时间" required>
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

.product-name {
  font-weight: 600;
}

.seckill-price {
  color: #c81623;
  font-weight: 600;
}

.origin {
  text-decoration: line-through;
}

.unit {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
