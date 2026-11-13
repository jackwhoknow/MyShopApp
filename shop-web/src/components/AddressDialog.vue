<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addressApi } from '@/api/address'
import type { Address } from '@/types'

const props = defineProps<{
  modelValue: boolean
  address?: Address | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const title = computed(() => (props.address ? '编辑收货地址' : '新增收货地址'))
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  receiverName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  defaultAddress: false
})

const rules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    Object.assign(form, {
      receiverName: props.address?.receiverName ?? '',
      phone: props.address?.phone ?? '',
      province: props.address?.province ?? '',
      city: props.address?.city ?? '',
      district: props.address?.district ?? '',
      detail: props.address?.detail ?? '',
      defaultAddress: props.address?.defaultAddress ?? false
    })
    formRef.value?.clearValidate()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (props.address) {
      await addressApi.update(props.address.id, { ...form })
    } else {
      await addressApi.create({ ...form })
    }
    ElMessage.success('地址已保存')
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="520px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="收货人" prop="receiverName">
        <el-input v-model="form.receiverName" maxlength="50" placeholder="请输入收货人姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="省份" prop="province">
        <el-input v-model="form.province" maxlength="50" placeholder="如：浙江省" />
      </el-form-item>
      <el-form-item label="城市" prop="city">
        <el-input v-model="form.city" maxlength="50" placeholder="如：杭州市" />
      </el-form-item>
      <el-form-item label="区县" prop="district">
        <el-input v-model="form.district" maxlength="50" placeholder="如：西湖区" />
      </el-form-item>
      <el-form-item label="详细地址" prop="detail">
        <el-input v-model="form.detail" type="textarea" :rows="2" maxlength="200" placeholder="街道、门牌号等" />
      </el-form-item>
      <el-form-item label="默认地址">
        <el-switch v-model="form.defaultAddress" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
