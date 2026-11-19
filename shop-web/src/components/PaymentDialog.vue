<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { paymentApi, type PaymentChannel } from '@/api/payment'

const props = defineProps<{
  modelValue: boolean
  orderId: number
  amount: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  paid: []
}>()

const channel = ref<PaymentChannel>('ALIPAY')
const availableChannels = ref<PaymentChannel[]>([])
const preparing = ref(false)
const stage = ref<'select' | 'qr' | 'waiting'>('select')
const codeUrl = ref('')
const canvasRef = ref<HTMLCanvasElement>()

let pollTimer: number | undefined
let pollCount = 0

function close() {
  stopPolling()
  emit('update:modelValue', false)
}

function reset() {
  stopPolling()
  stage.value = 'select'
  codeUrl.value = ''
  preparing.value = false
  pollCount = 0
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      reset()
      void loadChannels()
    }
  }
)

function isPaymentChannel(value: string): value is PaymentChannel {
  return value === 'ALIPAY' || value === 'WECHAT' || value === 'MOCK'
}

async function loadChannels() {
  const list = await paymentApi.channels()
  availableChannels.value = list.filter(isPaymentChannel)
  if (!availableChannels.value.includes(channel.value)) {
    channel.value = availableChannels.value[0] ?? 'ALIPAY'
  }
}

function channelLabel(value: PaymentChannel) {
  if (value === 'ALIPAY') return '支付宝'
  if (value === 'WECHAT') return '微信支付'
  return '模拟支付（仅本地）'
}

async function startPay() {
  if (preparing.value || availableChannels.value.length === 0) return
  preparing.value = true
  try {
    await preparePayment()
  } finally {
    preparing.value = false
  }
}

async function preparePayment() {
  const result = await paymentApi.prepare(props.orderId, channel.value)
  if (result.paid) return finishPayment()
  if (result.payUrl) return openPayUrl(result.payUrl)
  if (result.codeUrl) return showQr(result.codeUrl)
  ElMessage.warning('支付参数异常，请稍后重试')
}

function finishPayment() {
  ElMessage.success('支付成功')
  emit('paid')
  close()
}

function openPayUrl(url: string) {
  // 支付宝官方收银台：支付密码只在支付宝页面输入
  const opened = window.open(url, '_blank', 'noopener')
  if (!opened) {
    location.href = url
    return
  }
  stage.value = 'waiting'
  startPolling()
}

async function showQr(url: string) {
  codeUrl.value = url
  stage.value = 'qr'
  await nextTick()
  await renderQr()
  startPolling()
}

async function renderQr() {
  if (!canvasRef.value) return
  await QRCode.toCanvas(canvasRef.value, codeUrl.value, { width: 220, margin: 1 })
}

function startPolling() {
  stopPolling()
  pollCount = 0
  // 每 2 秒查询一次，最多 5 分钟
  pollTimer = window.setInterval(checkStatus, 2000)
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

async function checkStatus() {
  pollCount += 1
  if (pollCount > 150) return stopPollingWithWarning()
  const result = await paymentApi.status(props.orderId)
  if (result.paid) return handlePaid()
  if (result.status === 'CANCELLED') handleCancelled()
}

function stopPollingWithWarning() {
  stopPolling()
  ElMessage.warning('支付结果确认超时，请稍后在订单列表查看')
}

function handlePaid() {
  stopPolling()
  finishPayment()
}

function handleCancelled() {
  stopPolling()
  ElMessage.warning('订单已关闭，无法继续支付')
  close()
}

onBeforeUnmount(stopPolling)
</script>

<template>
  <el-dialog
    :model-value="props.modelValue"
    title="收银台"
    width="460px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="reset"
  >
    <div class="amount">
      应付金额：<span class="price">¥{{ props.amount.toFixed(2) }}</span>
    </div>

    <template v-if="stage === 'select'">
      <el-radio-group v-model="channel" class="channels">
        <el-radio v-for="item in availableChannels" :key="item" :value="item" border>
          {{ channelLabel(item) }}
        </el-radio>
      </el-radio-group>
      <el-alert
        v-if="availableChannels.length === 0"
        type="warning"
        :closable="false"
        show-icon
        class="tip"
        title="尚未配置可用的在线支付渠道，请联系客服处理"
      />
      <el-alert
        v-else
        type="info"
        :closable="false"
        show-icon
        class="tip"
        title="支付在支付宝/微信官方页面完成，本站不会收集、传输或保存你的支付密码、银行卡信息。"
      />
    </template>

    <template v-else-if="stage === 'qr'">
      <div class="qr-wrap">
        <canvas ref="canvasRef" class="qr-canvas" />
        <div class="muted">请使用微信扫描二维码完成支付</div>
      </div>
    </template>

    <template v-else>
      <div class="waiting">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <div>已打开支付宝收银台，完成支付后请回到本页面</div>
        <el-button link type="primary" @click="checkStatus">我已完成支付</el-button>
      </div>
    </template>

    <template #footer>
      <el-button @click="close">{{ stage === 'select' ? '取消' : '关闭' }}</el-button>
      <el-button
        v-if="stage === 'select'"
        type="primary"
        :loading="preparing"
        :disabled="availableChannels.length === 0"
        @click="startPay"
      >
        {{ channel === 'MOCK' ? '模拟支付' : '去支付' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.amount {
  margin-bottom: 16px;
  font-size: 15px;
}

.price {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 600;
}

.channels {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.tip {
  line-height: 1.6;
}

.qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 16px;
}

.qr-canvas {
  width: 220px;
  height: 220px;
}

.waiting {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 0;
  color: #606266;
}
</style>
