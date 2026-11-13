<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminCategoryApi } from '@/api/adminCategory'
import type { AdminCategoryNode } from '@/types'

const loading = ref(false)
const rows = ref<AdminCategoryNode[]>([])

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number>()
const form = reactive({
  parentId: undefined as number | undefined,
  parentName: '顶级分类',
  name: '',
  sortOrder: 0,
  enabled: true
})

async function load() {
  loading.value = true
  try {
    rows.value = await adminCategoryApi.tree()
  } finally {
    loading.value = false
  }
}

function openCreate(parent?: AdminCategoryNode) {
  editingId.value = undefined
  form.parentId = parent?.id
  form.parentName = parent ? parent.name : '顶级分类'
  form.name = ''
  form.sortOrder = 0
  form.enabled = true
  dialogVisible.value = true
}

function openEdit(node: AdminCategoryNode) {
  editingId.value = node.id
  form.parentId = node.parentId
  form.parentName = '—'
  form.name = node.name
  form.sortOrder = node.sortOrder
  form.enabled = node.enabled
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  submitting.value = true
  try {
    const payload = {
      parentId: editingId.value ? undefined : form.parentId,
      name: form.name.trim(),
      sortOrder: form.sortOrder,
      enabled: form.enabled
    }
    if (editingId.value) {
      await adminCategoryApi.update(editingId.value, payload)
      ElMessage.success('分类已更新')
    } else {
      await adminCategoryApi.create(payload)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function remove(node: AdminCategoryNode) {
  await ElMessageBox.confirm(
    `确定删除分类「${node.name}」吗？分类下有子分类或商品时无法删除。`,
    '删除分类',
    { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  )
  await adminCategoryApi.remove(node.id)
  ElMessage.success('分类已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="section-title">分类管理</h2>
      <el-button type="primary" @click="openCreate()">新增一级分类</el-button>
    </div>

    <div class="page-card" v-loading="loading">
      <el-table
        :data="rows"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        empty-text="暂无分类"
      >
        <el-table-column prop="name" label="分类名称" min-width="240" />
        <el-table-column prop="level" label="层级" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子分类</el-button>
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="460px"
    >
      <el-form label-width="90px">
        <el-form-item label="上级分类">
          <el-input :model-value="form.parentName" disabled />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
          <span class="muted switch-tip">停用后前台分类树不再展示</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.switch-tip {
  margin-left: 10px;
  font-size: 12px;
}
</style>
