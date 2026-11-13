<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addressApi, type AddressPayload } from '@/api/address'
import AddressDialog from '@/components/AddressDialog.vue'
import type { Address } from '@/types'

const addresses = ref<Address[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<Address | null>(null)

async function load() {
  loading.value = true
  try {
    addresses.value = await addressApi.list()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  dialogVisible.value = true
}

function openEdit(address: Address) {
  editing.value = address
  dialogVisible.value = true
}

function toPayload(address: Address, defaultAddress = address.defaultAddress): AddressPayload {
  return {
    receiverName: address.receiverName,
    phone: address.phone,
    province: address.province,
    city: address.city,
    district: address.district,
    detail: address.detail,
    defaultAddress
  }
}

async function setDefault(address: Address) {
  await addressApi.update(address.id, toPayload(address, true))
  ElMessage.success('已设为默认地址')
  load()
}

async function remove(address: Address) {
  await ElMessageBox.confirm(`确定删除「${address.receiverName}」的收货地址吗？`, '删除地址', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await addressApi.remove(address.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="section-title">收货地址</h2>
      <el-button type="primary" @click="openCreate">新增地址</el-button>
    </div>

    <div v-loading="loading" class="address-grid">
      <div v-for="address in addresses" :key="address.id" class="page-card address-card">
        <div class="card-head">
          <div class="receiver">
            {{ address.receiverName }}
            <span class="muted">{{ address.phone }}</span>
          </div>
          <el-tag v-if="address.defaultAddress" type="danger" size="small" effect="plain">默认</el-tag>
        </div>
        <div class="address-text">{{ address.fullAddress }}</div>
        <div class="card-actions">
          <el-button link type="primary" @click="openEdit(address)">编辑</el-button>
          <el-button v-if="!address.defaultAddress" link type="primary" @click="setDefault(address)">设为默认</el-button>
          <el-button link type="danger" @click="remove(address)">删除</el-button>
        </div>
      </div>

      <div v-if="!loading && !addresses.length" class="page-card empty-block full">
        <el-empty description="还没有收货地址">
          <el-button type="primary" @click="openCreate">新增地址</el-button>
        </el-empty>
      </div>
    </div>

    <AddressDialog v-model="dialogVisible" :address="editing" @saved="load" />
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.address-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.receiver {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.address-text {
  flex: 1;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  border-top: 1px solid #f0f2f5;
  padding-top: 8px;
}

.full {
  grid-column: 1 / -1;
}
</style>
