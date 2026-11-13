<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { uploadApi, type UploadCategory } from '@/api/upload'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    category?: UploadCategory
    tip?: string
    size?: number
  }>(),
  { modelValue: '', category: 'product', tip: '', size: 96 }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const uploading = ref(false)
const inputRef = ref<HTMLInputElement>()

function pick() {
  inputRef.value?.click()
}

async function onChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WEBP 图片')
    return
  }
  uploading.value = true
  try {
    const result = await uploadApi.image(file, props.category)
    emit('update:modelValue', result.url)
    ElMessage.success('图片上传成功')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="image-uploader">
    <div class="preview" :style="{ width: `${props.size}px`, height: `${props.size}px` }" @click="pick">
      <el-image v-if="props.modelValue" :src="props.modelValue" fit="cover" class="img">
        <template #error>
          <div class="placeholder">加载失败</div>
        </template>
      </el-image>
      <div v-else class="placeholder">点击上传</div>
      <div v-if="uploading" class="mask">
        <el-icon class="is-loading" :size="20"><Loading /></el-icon>
      </div>
    </div>
    <div class="side">
      <div class="actions">
        <el-button size="small" :loading="uploading" @click="pick">选择图片</el-button>
        <el-button v-if="props.modelValue" size="small" link type="danger" @click="emit('update:modelValue', '')">
          移除
        </el-button>
      </div>
      <div v-if="props.tip" class="muted tip">{{ props.tip }}</div>
    </div>
    <input
      ref="inputRef"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="hidden-input"
      @change="onChange"
    />
  </div>
</template>

<style scoped>
.image-uploader {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.preview {
  position: relative;
  flex-shrink: 0;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;
}

.img {
  width: 100%;
  height: 100%;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 0 4px;
}

.mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.75);
}

.side {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip {
  font-size: 12px;
  line-height: 1.4;
}

.hidden-input {
  display: none;
}
</style>
