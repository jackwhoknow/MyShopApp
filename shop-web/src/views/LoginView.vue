<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const loginRef = ref<FormInstance>()
const registerRef = ref<FormInstance>()

const loginForm = reactive({ username: '', password: '', captchaCode: '' })
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: ''
})

/** 忘记密码：邮箱验证码重置 */
const resetVisible = ref(false)
const resetLoading = ref(false)
const codeSending = ref(false)
const resetForm = reactive({ email: '', code: '', newPassword: '' })

/** 图形验证码，登录页进入时获取，登录失败或手动点击时刷新 */
const captchaId = ref('')
const captchaImage = ref('')

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '长度需在 3-30 之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d\W_]{8,30}$/,
      message: '8-30 位，需包含大小写字母和数字',
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function refreshCaptcha() {
  loginForm.captchaCode = ''
  try {
    const data = await authApi.captcha()
    captchaId.value = data.captchaId
    captchaImage.value = data.image
  } catch {
    captchaId.value = ''
    captchaImage.value = ''
  }
}

function redirectAfterLogin() {
  const redirect = route.query.redirect as string | undefined
  if (redirect) {
    router.replace(redirect)
    return
  }
  // 平台管理员进分类管理，商家进履约工作台
  if (auth.isAdmin) {
    router.replace('/admin/categories')
    return
  }
  router.replace(auth.isMerchant ? '/merchant/orders' : '/')
}

async function submitLogin() {
  const valid = await loginRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await authApi.login({
      username: loginForm.username,
      password: loginForm.password,
      captchaId: captchaId.value,
      captchaCode: loginForm.captchaCode
    })
    auth.setLogin(result)
    await cart.load().catch(() => undefined)
    ElMessage.success('登录成功')
    redirectAfterLogin()
  } catch {
    // 验证码用后即焚，失败后必须刷新，否则再提交一定报验证码错误
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const valid = await registerRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.register({ ...registerForm, merchant: false })
    ElMessage.success('注册成功，请使用新账号登录')
    // 注册接口不再自动登录（登录需要验证码），切回登录页并预填用户名
    loginForm.username = registerForm.username
    loginForm.password = ''
    mode.value = 'login'
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)

/** 发送找回密码验证码 */
async function sendResetCode() {
  if (!resetForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  codeSending.value = true
  try {
    await authApi.sendResetCode(resetForm.email)
    ElMessage.success('验证码已发送，请查收邮箱（本地环境请查看后端日志）')
  } finally {
    codeSending.value = false
  }
}

/** 提交重置密码 */
async function submitReset() {
  if (!resetForm.email || !resetForm.code || !resetForm.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  resetLoading.value = true
  try {
    await authApi.resetPassword({ ...resetForm })
    ElMessage.success('密码已重置，请使用新密码登录')
    loginForm.username = ''
    resetVisible.value = false
    resetForm.code = ''
    resetForm.newPassword = ''
  } finally {
    resetLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">星耀商城</div>
      <p class="slogan">学一个完整的电商交易闭环</p>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="submitLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="验证码" prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model="loginForm.captchaCode" placeholder="请输入验证码" maxlength="8" />
                <img
                  v-if="captchaImage"
                  class="captcha-img"
                  :src="captchaImage"
                  alt="验证码"
                  title="点击刷新"
                  @click="refreshCaptcha"
                />
                <span v-else class="captcha-placeholder" @click="refreshCaptcha">点击获取</span>
              </div>
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="submitLogin">登录</el-button>
            <p class="forgot">
              <el-link type="info" :underline="false" @click="resetVisible = true">忘记密码？</el-link>
            </p>
            <p class="hint">买家：buyer02 / buyer123 &nbsp;·&nbsp; 商家：merchant01 / merchant123</p>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3-30 位字母或数字" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="8-30 位，含大小写字母和数字" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="选填" maxlength="50" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="选填" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="registerForm.email" placeholder="选填，用于找回密码" maxlength="100" />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="submitRegister">注册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <router-link to="/" class="back">先去逛逛 →</router-link>
    </div>

    <el-dialog v-model="resetVisible" title="找回密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="注册邮箱">
          <el-input v-model="resetForm.email" placeholder="请输入注册时填写的邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="captcha-row">
            <el-input v-model="resetForm.code" placeholder="6 位验证码" maxlength="10" />
            <el-button :loading="codeSending" @click="sendResetCode">获取验证码</el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            show-password
            placeholder="8-30 位，需包含字母和数字"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #c81623 0%, #ef5b6b 45%, #f7d1d5 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 36px 32px 28px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 18px 45px rgba(91, 10, 18, 0.28);
}

.brand {
  font-size: 26px;
  font-weight: 700;
  color: #c81623;
  text-align: center;
}

.slogan {
  margin: 6px 0 20px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  height: 40px;
  width: 120px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  flex: none;
}

.captcha-placeholder {
  height: 40px;
  width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  flex: none;
}

.submit {
  width: 100%;
  margin-top: 6px;
}

.forgot {
  margin: 8px 0 0;
  text-align: center;
}

.hint {
  margin: 12px 0 0;
  text-align: center;
  font-size: 12px;
  color: #b1b3b8;
}

.back {
  display: block;
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.back:hover {
  color: #c81623;
}
</style>
