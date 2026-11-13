<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi } from '@/api/category'
import { merchantProductApi } from '@/api/merchantProduct'
import ImageUploader from '@/components/ImageUploader.vue'
import type { CategoryNode } from '@/types'

interface SpecDraft {
  name: string
  /** 用逗号分隔的多个取值 */
  valuesText: string
}

interface SkuRow {
  signature: string
  values: Record<string, string>
  price: number
  stock: number
  image: string
}

const router = useRouter()

const categories = ref<CategoryNode[]>([])
const submitting = ref(false)

const form = reactive({
  categoryId: undefined as number | undefined,
  name: '',
  subtitle: '',
  mainImage: '',
  description: ''
})

const specs = ref<SpecDraft[]>([{ name: '颜色', valuesText: '' }])

/** 把“逗号分隔文本”解析成后端需要的规格结构 */
const parsedSpecs = computed(() =>
  specs.value
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
const specKey = computed(() =>
  parsedSpecs.value.map((spec) => `${spec.name}:${spec.values.join(',')}`).join('|')
)

const skuRows = ref<SkuRow[]>([])

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

// 规格一变，SKU 组合就重算；已填过的价格/库存按组合签名保留
watch(
  specKey,
  () => {
    const existing = new Map(skuRows.value.map((row) => [row.signature, row]))
    skuRows.value = buildCombos(parsedSpecs.value).map((values) => {
      const signature = signatureOf(values)
      const old = existing.get(signature)
      return old
        ? { ...old, values }
        : { signature, values, price: 0, stock: 0, image: '' }
    })
  },
  { immediate: true }
)

function addSpec() {
  specs.value.push({ name: '', valuesText: '' })
}

function removeSpec(index: number) {
  specs.value.splice(index, 1)
}

function skuDesc(values: Record<string, string>) {
  return Object.values(values).join(' / ')
}

async function submit() {
  if (!form.categoryId) {
    ElMessage.warning('请选择商品分类')
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请输入商品名称')
    return
  }
  if (!form.mainImage.trim()) {
    ElMessage.warning('请上传商品主图')
    return
  }
  if (specs.value.some((spec) => spec.name.trim() && !spec.valuesText.trim())) {
    ElMessage.warning('规格值不能为空')
    return
  }
  if (parsedSpecs.value.length === 0) {
    ElMessage.warning('至少定义一个规格及取值')
    return
  }
  if (skuRows.value.length === 0) {
    ElMessage.warning('请先定义规格以生成 SKU')
    return
  }
  const invalidSku = skuRows.value.find((row) => !row.price || row.price <= 0)
  if (invalidSku) {
    ElMessage.warning(`SKU「${skuDesc(invalidSku.values)}」的价格必须大于 0`)
    return
  }

  submitting.value = true
  try {
    await merchantProductApi.create({
      categoryId: form.categoryId,
      name: form.name.trim(),
      subtitle: form.subtitle.trim() || undefined,
      description: form.description || undefined,
      mainImage: form.mainImage.trim(),
      specs: parsedSpecs.value,
      skus: skuRows.value.map((row) => ({
        price: row.price,
        stock: Number(row.stock) || 0,
        image: row.image.trim() || undefined,
        specValues: row.values
      }))
    })
    ElMessage.success('商品已提交审核，管理员通过后将自动上架')
    router.push('/merchant/products')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  categories.value = await categoryApi.tree()
})
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="section-title">新建商品</h2>
      <el-button link @click="router.push('/merchant/products')">返回商品列表</el-button>
    </div>

    <div class="page-card block">
      <div class="block-title">基本信息</div>
      <el-form label-width="90px">
        <el-form-item label="商品分类" required>
          <el-tree-select
            v-model="form.categoryId"
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
          <el-input v-model="form.name" maxlength="100" placeholder="例如：星耀 X1 手机" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="200" placeholder="一句话卖点" />
        </el-form-item>
        <el-form-item label="商品主图" required>
          <ImageUploader
            v-model="form.mainImage"
            category="product"
            tip="支持 JPG/PNG/GIF/WEBP，单张不超过 5MB"
          />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card block">
      <div class="block-head">
        <div class="block-title">规格定义</div>
        <el-button size="small" @click="addSpec">添加规格</el-button>
      </div>

      <div v-for="(spec, index) in specs" :key="index" class="spec-row">
        <el-input v-model="spec.name" placeholder="规格名，如 颜色" class="spec-name" maxlength="50" />
        <el-input
          v-model="spec.valuesText"
          placeholder="规格值，用逗号分隔，如 曜石黑,珍珠白"
          class="spec-values"
        />
        <el-button link type="danger" @click="removeSpec(index)">删除</el-button>
      </div>
      <div class="muted">
        SKU 由所有规格的取值自动组合生成，例如 颜色 × 版本 会生成 2 × 2 = 4 个 SKU。
      </div>
    </div>

    <div class="page-card block">
      <div class="block-title">SKU 列表</div>
      <el-table :data="skuRows" empty-text="先填写规格与取值，SKU 会自动生成">
        <el-table-column label="规格组合" min-width="180">
          <template #default="{ row }">{{ skuDesc(row.values) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="170">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0" :precision="2" :step="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.stock" :min="0" :step="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="SKU 图片（可选）" min-width="240">
          <template #default="{ row }">
            <ImageUploader v-model="row.image" category="product" :size="52" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card submit-bar">
      <span class="muted">创建后商品进入待审核状态，管理员审核通过后买家才能看到。</span>
      <div class="actions">
        <el-button @click="router.push('/merchant/products')">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">创建商品</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.block {
  margin-bottom: 16px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.block-head .block-title {
  margin-bottom: 0;
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

.full {
  width: 100%;
}

.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .spec-row {
    flex-wrap: wrap;
  }

  .spec-name {
    width: 100%;
  }

  .submit-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
