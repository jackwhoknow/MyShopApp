<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminDashboardApi } from '@/api/adminDashboard'
import type { AdminDashboard } from '@/types'

const days = ref(7)
const loading = ref(false)
const dashboard = ref<AdminDashboard>()

const maxSales = computed(() => {
  const values = dashboard.value?.trend.map((item) => Number(item.salesAmount)) ?? [0]
  return Math.max(...values, 1)
})

function barHeight(amount: number) {
  return `${Math.max(4, (Number(amount) / maxSales.value) * 100)}%`
}

function formatMoney(value?: number) {
  return Number(value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

async function load() {
  loading.value = true
  try {
    dashboard.value = await adminDashboardApi.overview(days.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="dashboard-header">
      <h2 class="section-title">平台经营看板</h2>
      <el-select v-model="days" class="days-select" @change="load">
        <el-option label="近 7 天" :value="7" />
        <el-option label="近 14 天" :value="14" />
        <el-option label="近 30 天" :value="30" />
      </el-select>
    </div>

    <div v-loading="loading" class="dashboard-body">
      <div v-if="dashboard" class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">用户总数</div>
          <div class="stat-value">{{ dashboard.totalUsers }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">营业店铺</div>
          <div class="stat-value">{{ dashboard.activeShops }}</div>
          <div class="stat-sub">待审核 {{ dashboard.pendingShops }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">在售商品</div>
          <div class="stat-value">{{ dashboard.onShelfProducts }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">有效订单</div>
          <div class="stat-value">{{ dashboard.totalOrders }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">累计成交额</div>
          <div class="stat-value money">¥{{ formatMoney(dashboard.totalSalesAmount) }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日成交额</div>
          <div class="stat-value money">¥{{ formatMoney(dashboard.todaySalesAmount) }}</div>
          <div class="stat-sub">今日订单 {{ dashboard.todayOrderCount }}</div>
        </div>
        <div class="stat-card warning">
          <div class="stat-label">退款中</div>
          <div class="stat-value">{{ dashboard.refundingCount }}</div>
        </div>
      </div>

      <div v-if="dashboard" class="page-card chart-card">
        <div class="card-title">近 {{ dashboard.days }} 天成交趋势</div>
        <div class="trend-chart">
          <el-tooltip
            v-for="point in dashboard.trend"
            :key="point.date"
            placement="top"
            :content="`${point.date} · 订单 ${point.orderCount} 单 · 成交 ¥${formatMoney(point.salesAmount)}`"
          >
            <div class="trend-col">
              <div class="trend-value">{{ point.orderCount }}</div>
              <div class="trend-bar-wrap">
                <div class="trend-bar" :style="{ height: barHeight(point.salesAmount) }"></div>
              </div>
              <div class="trend-label">{{ point.date.slice(5) }}</div>
            </div>
          </el-tooltip>
        </div>
        <el-empty v-if="!dashboard.trend.length" description="暂无成交数据" :image-size="70" />
      </div>

      <div v-if="dashboard" class="page-card">
        <div class="card-title">平台热销商品</div>
        <el-table :data="dashboard.topProducts" empty-text="暂无销售数据">
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column prop="quantity" label="销量" width="120" />
          <el-table-column label="销售额" width="170">
            <template #default="{ row }">¥{{ formatMoney(row.salesAmount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.days-select {
  width: 130px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #eef0f4;
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.stat-value.money {
  font-size: 22px;
  color: #e6a23c;
}

.stat-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.stat-card.warning .stat-value {
  color: #f56c6c;
}

.chart-card {
  margin-bottom: 18px;
}

.card-title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 600;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 200px;
  padding: 8px 4px 0;
}

.trend-col {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-width: 0;
}

.trend-value {
  height: 18px;
  font-size: 12px;
  color: #606266;
}

.trend-bar-wrap {
  display: flex;
  flex: 1;
  align-items: flex-end;
  justify-content: center;
  width: 100%;
}

.trend-bar {
  width: 60%;
  max-width: 36px;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, #409eff, #79bbff);
  transition: height 0.3s;
}

.trend-label {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
</style>
