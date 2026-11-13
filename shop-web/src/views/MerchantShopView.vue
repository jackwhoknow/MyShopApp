<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantShopApi } from '@/api/merchantShop'
import type { Shop } from '@/types'

const statusMeta: Record<string, { label: string; type: 'warning' | 'success' | 'danger'; tip: string }> = {
  PENDING: { label: '待审核', type: 'warning', tip: '店铺正在等待平台审核，审核通过后即可上架商品。' },
  ACTIVE: { label: '营业中', type: 'success', tip: '店铺已通过审核，可以正常上架商品并接收订单。' },
  REJECTED: { label: '已驳回', type: 'danger', tip: '店铺申请被驳回，可根据驳回原因修改后重新提交。' }
}

const loading = ref(false)
const submitting = ref(false)
const shop = ref<Shop>()
const form = reactive({ name: '', description: '' })

const needApply = () => !shop.value || shop.value.status === 'REJECTED'

async function load() {
  loading.value = true
  try {
    shop.value = await merchantShopApi.mine()
  } catch {
    // 没有申请过店铺时后端返回 404，这里按“无店铺”处理
    shop.value = undefined
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入店铺名称')
    return
  }
  submitting.value = true
  try {
    shop.value = await merchantShopApi.apply({
      name: form.name.trim(),
      description: form.description.trim() || undefined
    })
    form.name = ''
    form.description = ''
    ElMessage.success('开店申请已提交，等待平台审核')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">我的店铺</h2>

    <div v-loading="loading">
      <div v-if="shop" class="page-card shop-card">
        <div class="shop-head">
          <div>
            <div class="shop-name">{{ shop.name }}</div>
            <div class="muted">店主：{{ shop.ownerUsername }} · 申请时间：{{ shop.createdAt?.replace('T', ' ') }}</div>
          </div>
          <el-tag :type="statusMeta[shop.status]?.type || 'info'" effect="light">
            {{ statusMeta[shop.status]?.label || shop.status }}
          </el-tag>
        </div>

        <el-alert
          v-if="shop.status === 'REJECTED'"
          :title="`驳回原因：${shop.rejectReason || '未填写'}`"
          type="error"
          :closable="false"
          show-icon
        />
        <el-alert v-else :title="statusMeta[shop.status]?.tip" type="info" :closable="false" show-icon />

        <div class="shop-desc">
          <div class="desc-label">店铺简介</div>
          <div>{{ shop.description || '暂无简介' }}</div>
        </div>
      </div>

      <div v-else-if="!loading" class="page-card">
        <el-empty description="你还没有店铺，先提交开店申请吧" :image-size="90" />
      </div>

      <div v-if="needApply()" class="page-card">
        <div class="block-title">{{ shop ? '重新申请开店' : '开店申请' }}</div>
        <el-form label-width="90px">
          <el-form-item label="店铺名称" required>
            <el-input v-model="form.name" maxlength="50" placeholder="例如：星辰数码旗舰店" />
          </el-form-item>
          <el-form-item label="店铺简介">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="简单介绍你的店铺"
            />
          </el-form-item>
        </el-form>
        <div class="apply-foot">
          <span class="muted">提交后由平台审核，审核通过即可上架商品。</span>
          <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shop-card {
  margin-bottom: 16px;
}

.shop-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.shop-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.shop-desc {
  margin-top: 16px;
  font-size: 14px;
  color: #606266;
}

.desc-label {
  font-weight: 600;
  margin-bottom: 4px;
  color: #303133;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.apply-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-left: 90px;
}

@media (max-width: 768px) {
  .apply-foot {
    flex-direction: column;
    align-items: stretch;
    padding-left: 0;
  }
}
</style>
