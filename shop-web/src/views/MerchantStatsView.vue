<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { merchantStatsApi } from '@/api/merchantStats'
import { reportApi } from '@/api/report'
import type { MerchantStats } from '@/types'

const days = ref(7)
const loading = ref(false)
const stats = ref<MerchantStats>()
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function formatMoney(value?: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

async function load() {
  loading.value = true
  try {
    stats.value = await merchantStatsApi.overview(days.value)
    await nextTick()
    renderChart()
  } catch {
    // 没有店铺时后端返回 404，页面保持空态
    stats.value = undefined
  } finally {
    loading.value = false
  }
}

function renderChart() {
  const container = chartRef.value
  const data = stats.value
  if (!container || !data) return

  if (!chart) {
    chart = echarts.init(container)
    window.addEventListener('resize', resizeChart)
  }

  const dates = data.trend.map((point) => point.date.slice(5))
  const sales = data.trend.map((point) => Number(point.salesAmount))
  const orders = data.trend.map((point) => point.orderCount)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: { data: ['销售额', '订单数'], top: 0 },
    grid: { left: 16, right: 16, top: 48, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: { alignWithLabel: true },
      axisLabel: { color: '#909399' }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额（元）',
        nameTextStyle: { color: '#909399' },
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { color: '#f0f2f5' } }
      },
      {
        type: 'value',
        name: '订单数',
        minInterval: 1,
        nameTextStyle: { color: '#909399' },
        axisLabel: { color: '#909399' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: { color: '#c81623', borderRadius: [4, 4, 0, 0] },
        data: sales
      },
      {
        name: '订单数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 6,
        itemStyle: { color: '#409eff' },
        lineStyle: { width: 2 },
        data: orders
      }
    ]
  })
  chart.resize()
}

function resizeChart() {
  chart?.resize()
}

onMounted(load)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div v-loading="loading">
    <div class="board-head">
      <h2 class="section-title">商家经营看板</h2>
      <div class="board-actions">
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="14">近 14 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        <el-button size="small" @click="reportApi.exportOrders()">导出订单</el-button>
        <el-button size="small" @click="reportApi.exportSettlements()">导出结算</el-button>
      </div>
    </div>

    <template v-if="stats">
      <div class="metric-grid">
        <div class="page-card metric-card">
          <div class="metric-label">今日订单</div>
          <div class="metric-value">{{ stats.todayOrderCount }}</div>
          <div class="muted">不含已取消子订单</div>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">今日销售额</div>
          <div class="metric-value price">{{ formatMoney(stats.todaySalesAmount) }}</div>
          <div class="muted">已发货 + 已完成</div>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">待发货</div>
          <div class="metric-value warning">{{ stats.pendingShipCount }}</div>
          <router-link to="/merchant/orders" class="metric-link">去发货 →</router-link>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">退款中</div>
          <div class="metric-value warning">{{ stats.refundingCount }}</div>
          <router-link to="/merchant/after-sales" class="metric-link">处理售后 →</router-link>
        </div>
        <div class="page-card metric-card">
          <div class="metric-label">累计销售额</div>
          <div class="metric-value price">{{ formatMoney(stats.totalSalesAmount) }}</div>
          <div class="muted">全部已发货 + 已完成</div>
        </div>
      </div>

      <div class="page-card chart-card">
        <div class="block-title">近 {{ stats.days }} 天销售趋势</div>
        <div ref="chartRef" class="chart"></div>
      </div>

      <div class="page-card">
        <div class="block-title">热销商品 Top5</div>
        <el-table :data="stats.topProducts" size="small">
          <el-table-column label="排名" width="70" align="center">
            <template #default="{ $index }">
              <span class="rank" :class="{ top: $index === 0 }">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品" min-width="200" show-overflow-tooltip />
          <el-table-column prop="quantity" label="销量" width="100" align="right" />
          <el-table-column label="销售额" width="140" align="right">
            <template #default="{ row }">{{ formatMoney(row.salesAmount) }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="还没有销售数据，发货后这里会出现热销榜" :image-size="70" />
          </template>
        </el-table>
      </div>
    </template>

    <div v-else-if="!loading" class="page-card">
      <el-empty description="还没有店铺或暂无统计数据" :image-size="90" />
    </div>
  </div>
</template>

<style scoped>
.board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.board-head .section-title {
  margin: 0;
}

.board-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  padding: 18px 20px;
}

.metric-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 8px;
}

.metric-value.warning {
  color: #e6a23c;
}

.metric-link {
  font-size: 13px;
  color: #c81623;
}

.metric-link:hover {
  text-decoration: underline;
}

.chart-card {
  margin-bottom: 16px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.chart {
  width: 100%;
  height: 340px;
}

.rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 50%;
  background: #f0f2f5;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
}

.rank.top {
  background: #c81623;
  color: #fff;
}

@media (max-width: 768px) {
  .board-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .chart {
    height: 280px;
  }
}
</style>
