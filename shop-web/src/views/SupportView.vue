<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { supportApi } from '@/api/support'
import type { SupportConversation, SupportMessage } from '@/types'

const route = useRoute()

const conversations = ref<SupportConversation[]>([])
const activeId = ref<number>()
const messages = ref<SupportMessage[]>([])
const content = ref('')
const sending = ref(false)
const loading = ref(false)

let timer: number | undefined
let lastId = 0

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(5, 16) : ''
}

async function loadConversations() {
  conversations.value = await supportApi.conversations()
}

async function selectConversation(id: number) {
  if (activeId.value === id) return
  activeId.value = id
  messages.value = []
  lastId = 0
  const conversation = conversations.value.find((item) => item.id === id)
  if (conversation) conversation.unread = 0
  await loadMessages()
}

async function loadMessages() {
  if (!activeId.value) return
  const list = await supportApi.messages(activeId.value, lastId)
  if (!list.length) return
  messages.value = [...messages.value, ...list]
  lastId = list[list.length - 1].id
  await scrollBottom()
}

async function scrollBottom() {
  await nextTick()
  const panel = document.querySelector('.message-panel')
  if (panel) panel.scrollTop = panel.scrollHeight
}

async function send() {
  if (!activeId.value) {
    ElMessage.warning('请先选择会话')
    return
  }
  if (!content.value.trim()) return
  sending.value = true
  try {
    await supportApi.send(activeId.value, content.value.trim())
    content.value = ''
    await loadMessages()
    await loadConversations()
  } finally {
    sending.value = false
  }
}

async function init() {
  loading.value = true
  try {
    await loadConversations()
    const queryId = Number(route.query.conversationId)
    const target = conversations.value.find((item) => item.id === queryId) || conversations.value[0]
    if (target) {
      activeId.value = undefined
      await selectConversation(target.id)
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  init()
  timer = window.setInterval(async () => {
    if (!activeId.value) return
    await loadMessages().catch(() => undefined)
    await loadConversations().catch(() => undefined)
  }, 3000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="page-card support" v-loading="loading">
    <aside class="conversation-list">
      <div class="list-title">我的会话</div>
      <el-empty v-if="!conversations.length" description="还没有会话" :image-size="70" />
      <div
        v-for="item in conversations"
        :key="item.id"
        class="conversation-item"
        :class="{ active: item.id === activeId }"
        @click="selectConversation(item.id)"
      >
        <div class="conversation-head">
          <span class="shop-name">{{ item.shopName }}</span>
          <span class="time">{{ formatTime(item.lastMessageAt) }}</span>
        </div>
        <div class="conversation-preview">
          <span class="preview-text">{{ item.lastMessagePreview || '暂无消息' }}</span>
          <el-badge v-if="item.unread" :value="item.unread" class="unread" />
        </div>
      </div>
    </aside>

    <section class="chat">
      <template v-if="activeId">
        <div class="message-panel">
          <div v-if="!messages.length" class="muted empty-tip">发送第一条消息，向店铺咨询商品问题</div>
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="{ mine: message.senderType === 'USER' }"
          >
            <div class="bubble">
              <div class="bubble-content">{{ message.content }}</div>
              <div class="bubble-time">{{ formatTime(message.createdAt) }}</div>
            </div>
          </div>
        </div>
        <div class="composer">
          <el-input
            v-model="content"
            type="textarea"
            :rows="2"
            maxlength="1000"
            placeholder="输入消息，Enter 发送"
            @keyup.enter.exact.prevent="send"
          />
          <el-button type="primary" :loading="sending" @click="send">发送</el-button>
        </div>
      </template>
      <el-empty v-else description="请选择左侧会话" />
    </section>
  </div>
</template>

<style scoped>
.support {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 0;
  padding: 0;
  overflow: hidden;
  height: 620px;
}

.conversation-list {
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
}

.list-title {
  padding: 16px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}

.conversation-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f7f7f7;
  cursor: pointer;
}

.conversation-item:hover,
.conversation-item.active {
  background: #fff7f7;
}

.conversation-head {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.shop-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 8px;
}

.conversation-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.preview-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-panel {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
}

.empty-tip {
  text-align: center;
  padding-top: 40px;
  font-size: 13px;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.mine {
  justify-content: flex-end;
}

.bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.message-row.mine .bubble {
  background: #c81623;
  color: #ffffff;
}

.bubble-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.bubble-time {
  margin-top: 4px;
  font-size: 11px;
  color: #b0b3b8;
  text-align: right;
}

.message-row.mine .bubble-time {
  color: rgba(255, 255, 255, 0.75);
}

.composer {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-top: 1px solid #ebeef5;
  align-items: flex-end;
}

@media (max-width: 768px) {
  .support {
    grid-template-columns: 1fr;
    height: auto;
  }

  .conversation-list {
    border-right: none;
    border-bottom: 1px solid #ebeef5;
    max-height: 200px;
  }

  .message-panel {
    min-height: 300px;
  }
}
</style>
