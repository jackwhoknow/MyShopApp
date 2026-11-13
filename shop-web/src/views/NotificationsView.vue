<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationMessage } from '@/types'

const router = useRouter()
const notificationStore = useNotificationStore()

const list = ref<NotificationMessage[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)

/** 按类型给出标签文案与颜色，便于快速区分订单与售后消息 */
const TYPE_LABELS: Record<string, { text: string; type: 'primary' | 'success' | 'info' | 'warning' | 'danger' }> = {
  ORDER_PAID: { text: '支付', type: 'success' },
  NEW_ORDER: { text: '新订单', type: 'warning' },
  ORDER_SHIPPED: { text: '发货', type: 'primary' },
  ORDER_COMPLETED: { text: '交易完成', type: 'success' },
  ORDER_CANCELLED: { text: '订单取消', type: 'info' },
  AFTER_SALE_APPLIED: { text: '售后申请', type: 'warning' },
  AFTER_SALE_APPROVED: { text: '售后同意', type: 'primary' },
  AFTER_SALE_REJECTED: { text: '售后驳回', type: 'danger' },
  AFTER_SALE_RETURN_SHIPPED: { text: '退货寄回', type: 'primary' },
  AFTER_SALE_REFUNDED: { text: '退款成功', type: 'success' },
  AFTER_SALE_PLATFORM_INTERVENED: { text: '平台介入', type: 'danger' },
  ADMIN_BROADCAST: { text: '平台公告', type: 'primary' }
}

function typeLabel(type: string) {
  return TYPE_LABELS[type] ?? { text: '通知', type: 'info' as const }
}

function formatTime(value: string) {
  return value ? value.slice(0, 16).replace('T', ' ') : ''
}

async function load() {
  loading.value = true
  try {
    const result = await notificationApi.mine({ page: page.value, size: size.value })
    list.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handlePage(current: number) {
  page.value = current
  load()
}

/** 点击通知：先标记已读，有跳转地址就进入对应页面 */
async function open(item: NotificationMessage) {
  if (!item.readFlag) {
    await notificationApi.markRead(item.id)
    item.readFlag = true
    await notificationStore.refresh()
  }
  if (item.link) {
    router.push(item.link)
  }
}

async function readAll() {
  await notificationApi.markAllRead()
  ElMessage.success('已全部标记为已读')
  list.value.forEach((item) => {
    item.readFlag = true
  })
  await notificationStore.refresh()
}

onMounted(async () => {
  await Promise.all([load(), notificationStore.refresh()])
})
</script>

<template>
  <div>
    <div class="head">
      <h2 class="section-title">消息通知</h2>
      <el-button
        :disabled="notificationStore.unreadCount === 0"
        size="small"
        @click="readAll"
      >
        全部已读
      </el-button>
    </div>

    <div v-loading="loading" class="list-wrap">
      <ul v-if="list.length" class="notice-list">
        <li
          v-for="item in list"
          :key="item.id"
          class="notice-item"
          :class="{ unread: !item.readFlag, clickable: Boolean(item.link) }"
          @click="open(item)"
        >
          <span class="dot" :class="{ show: !item.readFlag }" />
          <div class="main">
            <div class="line">
              <el-tag :type="typeLabel(item.type).type" size="small" effect="plain">
                {{ typeLabel(item.type).text }}
              </el-tag>
              <span class="title">{{ item.title }}</span>
              <span class="time muted">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="content">{{ item.content }}</div>
          </div>
        </li>
      </ul>
      <el-empty v-else-if="!loading" description="暂时没有消息" class="empty-block" />

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
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-wrap {
  min-height: 320px;
}

.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.notice-item {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f2f3f5;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item.unread {
  background: #fff8f8;
}

.notice-item.clickable {
  cursor: pointer;
}

.notice-item.clickable:hover {
  background: #fafafa;
}

.dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: transparent;
  flex: none;
}

.dot.show {
  background: #c81623;
}

.main {
  flex: 1;
  min-width: 0;
}

.line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title {
  font-weight: 600;
  font-size: 14px;
}

.time {
  margin-left: auto;
  font-size: 12px;
}

.content {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
