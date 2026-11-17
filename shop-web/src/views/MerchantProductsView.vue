<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'
import { merchantProductApi } from '@/api/merchantProduct'
import ImageUploader from '@/components/ImageUploader.vue'
import type { CategoryNode, ProductDetail, ProductListItem } from '@/types'

interface SpecDraft {
  name: string
  /** 用逗号分隔的多个取值 */
  valuesText: string
}

interface EditSkuRow {
  id?: number
  signature: string
  values: Record<string, string>
  price: number
  stock: number
  image: string
}

interface PriceRow {
  skuId: number
  desc: string
  price: number
  stock: number
}

const router = useRouter()

const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  ON_SHELF: '已上架',
  OFF_SHELF: '已下架'
}

const statusTypes: Record<string, 'info' | 'success' | 'warning'> = {
  DRAFT: 'info',
  PENDING_REVIEW: 'warning',
  ON_SHELF: 'success',
  OFF_SHELF: 'warning'
}

const keyword = ref('')
const products = ref<ProductListItem[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const selection = ref<ProductListItem[]>([])
const tableRef = ref()

const categories = ref<CategoryNode[]>([])

const editVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive({
  id: 0,
  categoryId: undefined as number | undefined,
  name: '',
  subtitle: '',
  mainImage: '',
  purchaseLimit: 0,
  description: ''
})
const editSpecs = ref<SpecDraft[]>([])
const editSkuRows = ref<EditSkuRow[]>([])

const priceVisible = ref(false)
const priceSubmitting = ref(false)
const priceProductId = ref(0)
const priceRows = ref<PriceRow[]>([])

/** 把“逗号分隔文本”解析成后端需要的规格结构 */
const parsedEditSpecs = computed(() =>
  editSpecs.value
    .map((spec) => ({
      name: spec.name.trim(),
      values: spec.valuesText
        .split(/[,，]/)
        .map((value) => value.trim())
        .filter(Boolean)
    }))
    .filter((spec) => spec.name && spec.values.length)
)

/** 规格变化后重新生成 SKU 组合，这里用字符串做依赖便于 watch */
const editSpecKey = computed(() =>
  parsedEditSpecs.value.map((spec) => `${spec.name}:${spec.values.join(',')}`).join('|')
)

function buildCombos(specList: { name: string; values: string[] }[]) {
  if (specList.length === 0) return []
  let combos: Record<string, string>[] = [{}]
  for (const spec of specList) {
    const next: Record<string, string>[] = []
    for (const combo of combos) {
      for (const value of spec.values) {
        next.push({ ...combo, [spec.name]: value })
      }
    }
    combos = next
  }
  return combos
}

function signatureOf(values: Record<string, string>) {
  return Object.entries(values)
    .map(([name, value]) => `${name}=${value}`)
    .join('|')
}

// 规格一变，SKU 组合就重算；已填过的 id、价格、库存按组合签名保留
watch(
  editSpecKey,
  () => {
    const existing = new Map(editSkuRows.value.map((row) => [row.signature, row]))
    editSkuRows.value = buildCombos(parsedEditSpecs.value).map((values) => {
      const signature = signatureOf(values)
      const old = existing.get(signature)
      return old
        ? { ...old, values }
        : { id: undefined, signature, values, price: 0, stock: 0, image: '' }
    })
  },
  { immediate: true }
)

function skuDesc(values: Record<string, string>) {
  return Object.values(values).join(' / ')
}

async function load() {
  loading.value = true
  try {
    const result = await merchantProductApi.list({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    products.value = result.list
    total.value = result.total
    selection.value = []
    await nextTick()
    tableRef.value?.clearSelection()
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

function handleSelectionChange(rows: ProductListItem[]) {
  selection.value = rows
}

async function changeStatus(item: ProductListItem, target: 'ON_SHELF' | 'OFF_SHELF') {
  const action = target === 'ON_SHELF' ? '提交审核' : '下架'
  await ElMessageBox.confirm(`确定${action}「${item.name}」吗？`, `${action}商品`, {
    type: 'warning',
    confirmButtonText: `确定${action}`,
    cancelButtonText: '取消'
  })
  await merchantProductApi.changeStatus(item.id, target)
  ElMessage.success(
    target === 'ON_SHELF' ? '已提交审核，请等待管理员审核' : '下架成功'
  )
  load()
}

async function batchChangeStatus(target: 'ON_SHELF' | 'OFF_SHELF') {
  if (selection.value.length === 0) {
    ElMessage.warning('请先选择商品')
    return
  }
  const action = target === 'ON_SHELF' ? '提交审核' : '下架'
  await ElMessageBox.confirm(`确定${action}选中的 ${selection.value.length} 个商品吗？`, `批量${action}`, {
    type: 'warning',
    confirmButtonText: `确定${action}`,
    cancelButtonText: '取消'
  })
  const result = await merchantProductApi.batchChangeStatus(
    selection.value.map((item) => item.id),
    target
  )
  if (result.failures.length > 0) {
    const detail = result.failures
      .slice(0, 3)
      .map((failure) => `${failure.productName || `ID ${failure.productId}`}：${failure.reason}`)
      .join('；')
    ElMessage.warning(`成功 ${result.updated}/${result.total} 个；${detail}`)
  } else {
    ElMessage.success(
      target === 'ON_SHELF'
        ? `已提交 ${result.updated} 个商品审核`
        : `批量下架成功，共 ${result.updated} 个商品`
    )
  }
  load()
}

async function openEdit(item: ProductListItem) {
  const detail: ProductDetail = await merchantProductApi.detail(item.id)
  editForm.id = detail.id
  editForm.categoryId = detail.categoryId
  editForm.name = detail.name
  editForm.subtitle = detail.subtitle ?? ''
  editForm.mainImage = detail.mainImage ?? ''
  editForm.purchaseLimit = detail.purchaseLimit ?? 0
  editForm.description = detail.description ?? ''
  editSpecs.value = detail.specs.map((spec) => ({
    name: spec.name,
    valuesText: spec.values.map((value) => value.value).join(',')
  }))

  // 按规格组合签名匹配已有 SKU，保留 id 与价格库存
  const existing = new Map<string, EditSkuRow>()
  for (const sku of detail.skus) {
    const values: Record<string, string> = {}
    for (const spec of detail.specs) {
      const matched = sku.specValues.find((value) => value.specName === spec.name)
      values[spec.name] = matched?.value ?? ''
    }
    const signature = signatureOf(values)
    existing.set(signature, {
      id: sku.id,
      signature,
      values,
      price: sku.price,
      stock: sku.stock,
      image: sku.image ?? ''
    })
  }
  editSkuRows.value = buildCombos(parsedEditSpecs.value).map((values) => {
    const signature = signatureOf(values)
    const old = existing.get(signature)
    return old
      ? { ...old, values }
      : { id: undefined, signature, values, price: 0, stock: 0, image: '' }
  })

  editVisible.value = true
}

function addEditSpec() {
  editSpecs.value.push({ name: '', valuesText: '' })
}

function removeEditSpec(index: number) {
  editSpecs.value.splice(index, 1)
}

async function submitEdit() {
  if (!editForm.name.trim()) {
    ElMessage.warning('商品名称不能为空')
    return
  }
  if (editSpecs.value.some((spec) => spec.name.trim() && !spec.valuesText.trim())) {
    ElMessage.warning('规格值不能为空')
    return
  }
  if (parsedEditSpecs.value.length === 0) {
    ElMessage.warning('至少定义一个规格及取值')
    return
  }
  if (editSkuRows.value.length === 0) {
    ElMessage.warning('请先定义规格以生成 SKU')
    return
  }
  const invalidSku = editSkuRows.value.find((row) => !row.price || row.price <= 0)
  if (invalidSku) {
    ElMessage.warning(`SKU「${skuDesc(invalidSku.values)}」的价格必须大于 0`)
    return
  }

  editSubmitting.value = true
  try {
    await merchantProductApi.update(editForm.id, {
      categoryId: editForm.categoryId,
      name: editForm.name.trim(),
      subtitle: editForm.subtitle.trim() || undefined,
      description: editForm.description || undefined,
      mainImage: editForm.mainImage.trim() || undefined,
      purchaseLimit: Number(editForm.purchaseLimit) || 0,
      specs: parsedEditSpecs.value,
      skus: editSkuRows.value.map((row) => ({
        id: row.id,
        price: row.price,
        stock: Number(row.stock) || 0,
        image: row.image.trim() || undefined,
        specValues: row.values
      }))
    })
    ElMessage.success('保存成功，已提交审核')
    editVisible.value = false
    load()
  } finally {
    editSubmitting.value = false
  }
}

async function openPriceStock(item: ProductListItem) {
  const detail: ProductDetail = await merchantProductApi.detail(item.id)
  priceProductId.value = detail.id
  priceRows.value = detail.skus.map((sku) => ({
    skuId: sku.id,
    desc: sku.specValues.map((value) => value.value).join(' / '),
    price: sku.price,
    stock: sku.stock
  }))
  priceVisible.value = true
}

async function submitPriceStock() {
  const invalid = priceRows.value.find((row) => !row.price || row.price <= 0)
  if (invalid) {
    ElMessage.warning(`SKU「${invalid.desc || '默认'}」的价格必须大于 0`)
    return
  }
  priceSubmitting.value = true
  try {
    await merchantProductApi.updateSkus(
      priceProductId.value,
      priceRows.value.map((row) => ({
        skuId: row.skuId,
        price: row.price,
        stock: Number(row.stock) || 0
      }))
    )
    ElMessage.success('价格与库存已更新')
    priceVisible.value = false
    load()
  } finally {
    priceSubmitting.value = false
  }
}

async function removeProduct(item: ProductListItem) {
  await ElMessageBox.confirm(
    `确定删除「${item.name}」吗？删除后不可恢复。`,
    '删除商品',
    {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    }
  )
  await merchantProductApi.remove(item.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  await load()
  categories.value = await categoryApi.tree()
})
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="section-title">商家商品管理</h2>
      <el-button type="primary" @click="router.push('/merchant/products/new')">新建商品</el-button>
    </div>

    <div class="page-card">
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
      <div class="batch-bar">
        <el-button size="small" :disabled="selection.length === 0" @click="batchChangeStatus('ON_SHELF')">
          批量提交审核
        </el-button>
        <el-button size="small" :disabled="selection.length === 0" @click="batchChangeStatus('OFF_SHELF')">
          批量下架
        </el-button>
        <span v-if="selection.length" class="muted">已选 {{ selection.length }} 个商品</span>
      </div>

      <el-table
        ref="tableRef"
        :data="products"
        empty-text="暂无商品"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" />
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
        <el-table-column label="价格" width="170">
          <template #default="{ row }">
            ¥{{ row.minPrice.toFixed(2) }} ~ ¥{{ row.maxPrice.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="90" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status] || 'info'" effect="light">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
            <div v-if="row.rejectReason" class="reject-reason">驳回：{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openPriceStock(row)">价格库存</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'OFF_SHELF'"
              link
              type="success"
              @click="changeStatus(row, 'ON_SHELF')"
            >
              提交审核
            </el-button>
            <el-button v-else-if="row.status === 'ON_SHELF'" link type="warning" @click="changeStatus(row, 'OFF_SHELF')">
              下架
            </el-button>
            <el-button v-if="row.status === 'ON_SHELF'" link @click="router.push(`/products/${row.id}`)">
              查看
            </el-button>
            <el-button link type="danger" @click="removeProduct(row)">删除</el-button>
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

    <el-dialog v-model="editVisible" title="编辑商品" width="760px">
      <el-form label-width="90px">
        <el-form-item label="商品分类">
          <el-tree-select
            v-model="editForm.categoryId"
            :data="categories"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            clearable
            placeholder="选择分类"
            class="full"
          />
        </el-form-item>
        <el-form-item label="商品名称" required>
          <el-input v-model="editForm.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="editForm.subtitle" maxlength="200" />
        </el-form-item>
        <el-form-item label="商品主图">
          <ImageUploader
            v-model="editForm.mainImage"
            category="product"
            tip="支持 JPG/PNG/GIF/WEBP，单张不超过 5MB"
          />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="每人限购">
          <el-input-number v-model="editForm.purchaseLimit" :min="0" :max="9999" />
          <span class="muted">0 表示不限购</span>
        </el-form-item>
      </el-form>

      <div class="edit-block">
        <div class="edit-block-head">
          <span class="edit-block-title">规格与 SKU</span>
          <el-button size="small" @click="addEditSpec">添加规格</el-button>
        </div>
        <div v-for="(spec, index) in editSpecs" :key="index" class="spec-row">
          <el-input v-model="spec.name" placeholder="规格名，如 颜色" class="spec-name" maxlength="50" />
          <el-input
            v-model="spec.valuesText"
            placeholder="规格值，用逗号分隔，如 曜石黑,珍珠白"
            class="spec-values"
          />
          <el-button link type="danger" @click="removeEditSpec(index)">删除</el-button>
        </div>
        <el-table :data="editSkuRows" size="small" empty-text="先填写规格与取值，SKU 会自动生成">
          <el-table-column label="规格组合" min-width="140">
            <template #default="{ row }">{{ skuDesc(row.values) }}</template>
          </el-table-column>
          <el-table-column label="价格" width="170">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="SKU 图片（可选）" min-width="200">
            <template #default="{ row }">
              <ImageUploader v-model="row.image" category="product" :size="44" />
            </template>
          </el-table-column>
        </el-table>
        <div class="muted hint">
          已有 SKU 会按规格组合自动保留；删除被订单引用的 SKU 时保存会失败并提示。
        </div>
      </div>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="priceVisible" title="价格与库存" width="520px">
      <el-table :data="priceRows" size="small" empty-text="没有 SKU">
        <el-table-column label="规格组合" min-width="140">
          <template #default="{ row }">{{ row.desc || '默认' }}</template>
        </el-table-column>
        <el-table-column label="价格" width="170">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" size="small" controls-position="right" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" :loading="priceSubmitting" @click="submitPriceStock">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-input {
  width: 260px;
  margin-right: 10px;
}

.table-card {
  margin-top: 16px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
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

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.full {
  width: 100%;
}

.edit-block {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 14px;
}

.edit-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.edit-block-title {
  font-size: 15px;
  font-weight: 600;
}

.spec-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.spec-name {
  width: 180px;
  flex-shrink: 0;
}

.spec-values {
  flex: 1;
}

.hint {
  margin-top: 10px;
}

.reject-reason {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-color-danger);
  word-break: break-all;
}
</style>
