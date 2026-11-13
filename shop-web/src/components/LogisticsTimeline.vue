<script setup lang="ts">
import { computed } from 'vue'
import type { SubOrderLogistics } from '@/types'

const props = defineProps<{ logistics: SubOrderLogistics }>()

const statusLabels: Record<string, string> = {
  WAIT_SHIP: '待发货',
  SHIPPED: '运输中',
  COMPLETED: '已签收',
  REFUNDING: '售后处理中',
  CANCELLED: '已取消'
}

const statusText = computed(() => statusLabels[props.logistics.status] || props.logistics.status)
const reached = computed(() => props.logistics.traces.length > 0)

function formatTime(value?: string) {
  return value?.replace('T', ' ').slice(0, 16) || ''
}
</script>

<template>
  <div>
    <div class="summary">
      <div>
        <div class="company">{{ logistics.shipCompany || '模拟物流' }}</div>
        <div class="muted">运单号：{{ logistics.trackingNo || '-' }}</div>
        <div v-if="logistics.shippedAt" class="muted">
          发货时间：{{ formatTime(logistics.shippedAt) }}
        </div>
      </div>
      <el-tag size="small" :type="reached ? 'primary' : 'info'" effect="plain">{{ statusText }}</el-tag>
    </div>

    <el-divider />

    <el-empty
      v-if="!reached"
      description="商家发货后可查看物流轨迹"
      :image-size="80"
    />
    <el-timeline v-else class="timeline">
      <el-timeline-item
        v-for="(trace, index) in logistics.traces"
        :key="index"
        :timestamp="formatTime(trace.occurredAt)"
        placement="top"
        :type="index === 0 ? 'primary' : 'info'"
        :hollow="index !== 0"
      >
        <div class="trace-title" :class="{ current: index === 0 }">
          {{ trace.title }}
          <el-tag v-if="index === 0" size="small" type="primary" effect="dark" class="latest-tag">
            最新
          </el-tag>
        </div>
        <div class="trace-desc">{{ trace.description }}</div>
        <div class="muted trace-loc">{{ trace.location }}</div>
      </el-timeline-item>
    </el-timeline>

    <div class="muted tip">物流轨迹为模拟数据，仅用于功能演示</div>
  </div>
</template>

<style scoped>
.summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.company {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.muted {
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
}

.timeline {
  padding-left: 4px;
}

.trace-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}

.trace-title.current {
  color: #c81623;
}

.latest-tag {
  transform: scale(0.9);
}

.trace-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
}

.trace-loc {
  margin-top: 2px;
}

.tip {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}
</style>
