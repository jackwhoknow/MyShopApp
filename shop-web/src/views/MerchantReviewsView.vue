<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantReviewApi } from '@/api/review'
import type { Review } from '@/types'

const reviews = ref<Review[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(5)
const loading = ref(false)

const isEmpty = computed(() => !loading.value && reviews.value.length === 0)

const replyDialogVisible = ref(false)
const replySubmitting = ref(false)
const replyTarget = ref<Review>()
const replyForm = reactive({ reply: '' })

async function load() {
  loading.value = true
  try {
    const result = await merchantReviewApi.list({ page: page.value, size: size.value })
    reviews.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handlePage(current: number) {
  page.value = current
  load()
}

function openReply(item: Review) {
  replyTarget.value = item
  replyForm.reply = ''
  replyDialogVisible.value = true
}

async function submitReply() {
  if (!replyTarget.value) return
  if (!replyForm.reply.trim()) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replySubmitting.value = true
  try {
    await merchantReviewApi.reply(replyTarget.value.id, replyForm.reply.trim())
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    load()
  } finally {
    replySubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">商家评价</h2>

    <div v-loading="loading">
      <div v-for="item in reviews" :key="item.id" class="page-card review-card">
        <div class="card-head">
          <div class="meta">
            <span class="strong">{{ item.productName }}</span>
            <span class="muted">{{ item.skuDesc }}</span>
            <span class="muted">主订单号：{{ item.orderNo }}</span>
            <span class="muted">评价时间：{{ item.createdAt?.replace('T', ' ') }}</span>
          </div>
          <el-tag :type="item.reply ? 'success' : 'warning'" size="small" effect="light">
            {{ item.reply ? '已回复' : '待回复' }}
          </el-tag>
        </div>

        <div class="rating-line">
          <el-rate :model-value="item.rating" disabled size="small" />
          <span class="muted">{{ item.rating }} 分</span>
        </div>

        <div class="content-line">{{ item.content || '买家未填写评价内容' }}</div>

        <div v-if="item.images?.length" class="review-images">
          <el-image
            v-for="img in item.images"
            :key="img"
            :src="img"
            :preview-src-list="item.images"
            fit="cover"
            class="review-image"
          />
        </div>

        <div v-if="item.reply" class="reply-block">
          <div class="reply-title">
            商家回复
            <span class="muted">{{ item.repliedAt?.replace('T', ' ') }}</span>
          </div>
          <div>{{ item.reply }}</div>
        </div>

        <div class="card-foot">
          <span class="muted">子订单号：{{ item.subOrderNo }}</span>
          <el-button v-if="!item.reply" type="primary" size="small" @click="openReply(item)">
            回复评价
          </el-button>
        </div>
      </div>

      <div v-if="isEmpty" class="page-card empty-block">
        <el-empty description="暂无评价" />
      </div>

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

    <el-dialog v-model="replyDialogVisible" title="回复评价" width="460px">
      <div v-if="replyTarget" class="dialog-summary">
        {{ replyTarget.productName }}
        <div class="muted">买家评价：{{ replyTarget.content || '未填写内容' }}</div>
      </div>
      <el-form label-width="80px">
        <el-form-item label="回复内容" required>
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="回复会展示在买家的评价下方，且只能回复一次"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replySubmitting" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-card {
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
}

.strong {
  font-weight: 600;
}

.rating-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.content-line {
  margin-top: 8px;
  font-size: 14px;
  color: #303133;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.review-image {
  width: 64px;
  height: 64px;
  border-radius: 4px;
}

.reply-block {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f5f7fa;
  font-size: 13px;
  color: #606266;
}

.reply-title {
  display: flex;
  gap: 10px;
  margin-bottom: 4px;
  font-weight: 600;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 13px;
}

.empty-block {
  margin-bottom: 16px;
}

.dialog-summary {
  margin-bottom: 12px;
  font-size: 13px;
  color: #303133;
}

.pagination {
  display: flex;
  justify-content: center;
  margin: 8px 0 16px;
}
</style>
