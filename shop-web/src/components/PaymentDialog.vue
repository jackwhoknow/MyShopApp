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
// 模拟支付仅本地开发构建可见，生产环境由后端 mock-enabled=false 双重拦截
const mockEnabled = import.meta.env.DEV
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
    if (visible) reset()
  }
)

async function startPay() {
  if (preparing.value) return
  preparing.value = true
  try {
    const result = await paymentApi.prepare(props.orderId, channel.value)
    if (result.paid) {
      ElMessage.success('支付成功')
      emit('paid')
      close()
      return
    }
    if (result.payUrl) {
      // 支付宝官方收银台：支付密码只在支付宝页面输入
      const opened = window.open(result.payUrl, '_blank', 'noopener')
      if (!opened) {
        location.href = result.payUrl
        return
      }
      stage.value = 'waiting'
      startPolling()
      return
    }
    if (result.codeUrl) {
      codeUrl.value = result.codeUrl
      stage.value = 'qr'
      await nextTick()
      await renderQr()
      startPolling()
      return
    }
    ElMessage.warning('支付参数异常，请稍后重试')
  } finally {
    preparing.value = false
  }
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
  if (pollCount > 150) {
    stopPolling()
    ElMessage.warning('支付结果确认超时，请稍后在订单列表查看')
    return
  }
  const result = await paymentApi.status(props.orderId)
  if (result.paid) {
    stopPolling()
    ElMessage.success('支付成功')
    emit('paid')
    close()
    return
  }
  if (result.status === 'CANCELLED') {
    stopPolling()
    ElMessage.warning('订单已关闭，无法继续支付')
    close()
  }
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
        <el-radio value="ALIPAY" border>支付宝</el-radio>
        <el-radio value="WECHAT" border>微信支付</el-radio>
        <el-radio v-if="mockEnabled" value="MOCK" border>模拟支付（仅本地）</el-radio>
      </el-radio-group>
      <el-alert
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
      <el-button v-if="stage === 'select'" type="primary" :loading="preparing" @click="startPay">
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
