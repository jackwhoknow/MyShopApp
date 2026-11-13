<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { UserInfo } from '@/types'

const auth = useAuthStore()

const roleLabels: Record<string, string> = {
  BUYER: '买家',
  MERCHANT: '商家',
  ADMIN: '管理员'
}

const loading = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const profile = ref<UserInfo | null>(auth.user)

const profileForm = reactive({ nickname: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

/** 把最新资料同步到页面展示与全局登录态，顶栏昵称随之刷新 */
function applyUser(user: UserInfo) {
  profile.value = user
  profileForm.nickname = user.nickname || ''
  profileForm.phone = user.phone || ''
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
  if (nickname.length > 50) {
    ElMessage.warning('昵称长度不能超过 50')
    return
  }
  if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  savingProfile.value = true
  try {
    applyUser(
      await userApi.updateProfile({
        nickname: nickname || undefined,
        phone
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
</style>
