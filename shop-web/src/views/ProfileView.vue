<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { UserInfo } from '@/types'

const auth = useAuthStore()
const router = useRouter()

const roleLabels: Record<string, string> = {
  BUYER: '买家',
  MERCHANT: '商家',
  ADMIN: '管理员'
}

const loading = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const exporting = ref(false)
const deactivating = ref(false)
const profile = ref<UserInfo | null>(auth.user)

const profileForm = reactive({ nickname: '', phone: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

/** 把最新资料同步到页面展示与全局登录态，顶栏昵称随之刷新 */
function applyUser(user: UserInfo) {
  profile.value = user
  profileForm.nickname = user.nickname || ''
  profileForm.phone = user.phone || ''
  profileForm.email = user.email || ''
  auth.updateUser({ nickname: user.nickname, phone: user.phone })
}

async function load() {
  loading.value = true
  try {
    applyUser(await userApi.me())
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  const nickname = profileForm.nickname.trim()
  const phone = profileForm.phone.trim()
  const email = profileForm.email.trim()
  if (nickname.length > 50) {
    ElMessage.warning('昵称长度不能超过 50')
    return
  }
  if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  savingProfile.value = true
  try {
    applyUser(
      await userApi.updateProfile({
        nickname: nickname || undefined,
        phone,
        email
      })
    )
    ElMessage.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码已修改，请牢记新密码')
  } finally {
    savingPassword.value = false
  }
}

/** 导出个人数据为 JSON 文件，便于用户留档或迁移 */
async function exportData() {
  exporting.value = true
  try {
    const data = await userApi.exportData()
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `myshop-data-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('数据已导出')
  } finally {
    exporting.value = false
  }
}

/** 注销账号：二次输入密码确认，成功后清空登录态 */
async function deactivateAccount() {
  let password = ''
  try {
    const result = await ElMessageBox.prompt(
      '注销后账号将无法登录，历史订单会保留但个人资料将被清除，且不可恢复。请输入登录密码确认。',
      '注销账号',
      {
        inputType: 'password',
        inputPlaceholder: '请输入登录密码',
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    password = result.value || ''
  } catch {
    return
  }
  if (!password) {
    ElMessage.warning('请输入登录密码')
    return
  }
  deactivating.value = true
  try {
    await userApi.deactivate(password)
    ElMessage.success('账号已注销')
    auth.logout()
    router.push('/login')
  } finally {
    deactivating.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="section-title">个人中心</h2>

    <div v-loading="loading" class="profile-layout">
      <div class="page-card profile-summary">
        <div class="avatar">
          <el-icon :size="34"><User /></el-icon>
        </div>
        <div class="summary-info">
          <div class="summary-name">{{ auth.displayName }}</div>
          <div class="muted">用户名：{{ profile?.username }}</div>
          <div class="role-tags">
            <el-tag
              v-for="role in profile?.roles || []"
              :key="role"
              size="small"
              effect="plain"
              :type="role === 'ADMIN' ? 'danger' : role === 'MERCHANT' ? 'warning' : 'primary'"
            >
              {{ roleLabels[role] || role }}
            </el-tag>
          </div>
        </div>
        <div class="summary-meta">
          <div class="meta-label">昵称</div>
          <div>{{ profile?.nickname || '未设置' }}</div>
          <div class="meta-label">手机号</div>
          <div>{{ profile?.phone || '未绑定' }}</div>
          <div class="meta-label">邮箱</div>
          <div>{{ profile?.email || '未绑定' }}</div>
        </div>
      </div>

      <div class="profile-columns">
        <div class="page-card">
          <div class="block-title">修改资料</div>
          <el-form label-width="80px" @submit.prevent>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" maxlength="50" show-word-limit placeholder="留空则保持原昵称" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" maxlength="20" placeholder="留空表示解绑手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" maxlength="100" placeholder="用于找回密码，留空表示解绑" />
            </el-form-item>
          </el-form>
          <div class="form-foot">
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
          </div>
        </div>

        <div class="page-card">
          <div class="block-title">修改密码</div>
          <el-form label-width="80px" @submit.prevent>
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                maxlength="50"
                placeholder="至少 6 位"
              />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                maxlength="50"
                placeholder="再次输入新密码"
              />
            </el-form-item>
          </el-form>
          <div class="form-foot">
            <el-button type="primary" :loading="savingPassword" @click="changePassword">修改密码</el-button>
          </div>
        </div>

        <div class="page-card">
          <div class="block-title">数据与账号</div>
          <p class="muted data-tip">
            你可以随时导出在本平台产生的资料、地址、订单、评价、优惠券与积分记录。
            注销账号后个人资料将被清除且无法恢复，历史订单按法规要求保留。
          </p>
          <div class="data-actions">
            <el-button :loading="exporting" @click="exportData">导出我的数据</el-button>
            <el-button type="danger" plain :loading="deactivating" @click="deactivateAccount">
              注销账号
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.summary-info {
  flex: 1;
  min-width: 200px;
}

.summary-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.role-tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.summary-meta {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 12px;
  font-size: 14px;
  color: #606266;
}

.meta-label {
  color: #909399;
}

.profile-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.form-foot {
  display: flex;
  justify-content: flex-end;
}

.data-tip {
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 16px;
}

.data-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
