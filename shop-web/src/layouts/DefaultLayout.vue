<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useNotificationStore } from '@/stores/notification'

const auth = useAuthStore()
const cart = useCartStore()
const notificationStore = useNotificationStore()
const router = useRouter()

onMounted(() => {
  if (auth.isLoggedIn) {
    cart.load().catch(() => undefined)
    notificationStore.refresh()
  }
})

function handleCommand(command: string) {
  if (command === 'logout') {
    // 通知后端把当前 token 加入黑名单，失败也要清本地登录态
    authApi.logout(localStorage.getItem('refresh_token') || undefined).catch(() => undefined)
    auth.logout()
    cart.reset()
    notificationStore.reset()
    router.push('/')
    return
  }
  router.push(`/${command}`)
}
</script>

<template>
  <div>
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">星耀商城</router-link>

        <nav class="header-nav">
          <router-link to="/">首页</router-link>
          <router-link to="/seckill">限时秒杀</router-link>
          <router-link to="/orders">我的订单</router-link>
          <router-link to="/coupons">优惠券</router-link>
          <router-link to="/points">我的积分</router-link>
          <router-link to="/favorites">我的收藏</router-link>
          <router-link to="/followed-shops">我的关注</router-link>
          <router-link to="/addresses">收货地址</router-link>
          <router-link to="/support">在线客服</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/stats">商家看板</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/settlement">商家结算</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/promotions">满减活动</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/support">客服消息</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/coupons">优惠券管理</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/seckill">秒杀活动</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/products">商家商品</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/inventory">库存预警</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/orders">商家订单</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/after-sales">商家售后</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/reviews">商家评价</router-link>
          <router-link v-if="auth.isMerchant" to="/merchant/shop">我的店铺</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/dashboard">平台看板</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/shops">店铺审核</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/products">商品审核</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/orders">订单管理</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/after-sales">售后监管</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/support">平台客服</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/categories">分类管理</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/users">用户管理</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/invoices">发票管理</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/settlements">结算审核</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/reviews">评价审核</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/coupons">平台券</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/audit-logs">操作审计</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/content">内容管理</router-link>
          <router-link v-if="auth.isAdmin" to="/admin/notifications">通知群发</router-link>
        </nav>

        <div class="header-actions">
          <router-link v-if="auth.isLoggedIn" to="/notifications">
            <el-badge
              :value="notificationStore.unreadCount"
              :hidden="notificationStore.unreadCount === 0"
              :max="99"
            >
              <el-button text circle>
                <el-icon :size="22"><Bell /></el-icon>
              </el-button>
            </el-badge>
          </router-link>

          <router-link to="/cart">
            <el-badge :value="cart.count" :hidden="cart.count === 0" :max="99">
              <el-button text circle>
                <el-icon :size="22"><ShoppingCart /></el-icon>
              </el-button>
            </el-badge>
          </router-link>

          <template v-if="auth.isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="header-user">
                <el-icon><User /></el-icon>
                {{ auth.displayName }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="coupons">优惠券</el-dropdown-item>
                  <el-dropdown-item command="points">我的积分</el-dropdown-item>
                  <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="followed-shops">我的关注</el-dropdown-item>
                  <el-dropdown-item command="history">最近浏览</el-dropdown-item>
                  <el-dropdown-item command="notifications">消息通知</el-dropdown-item>
                  <el-dropdown-item command="addresses">收货地址</el-dropdown-item>
                  <el-dropdown-item command="support">在线客服</el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/stats" divided>
                    商家看板
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/settlement">
                    商家结算
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/promotions">
                    满减活动
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/support">
                    客服消息
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/coupons">
                    优惠券管理
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/seckill">
                    秒杀活动
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/products">
                    商家商品
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/inventory">
                    库存预警
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/orders">
                    商家订单
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/after-sales">
                    商家售后
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/reviews">
                    商家评价
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isMerchant" command="merchant/shop">
                    我的店铺
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/dashboard" divided>
                    平台看板
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/shops">
                    店铺审核
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/products">
                    商品审核
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/orders">
                    订单管理
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/after-sales">
                    售后监管
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/support">
                    平台客服
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/categories">
                    分类管理
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/users">
                    用户管理
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/invoices">
                    发票管理
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/settlements">
                    结算审核
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/reviews">
                    评价审核
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/coupons">
                    平台券
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/audit-logs">
                    操作审计
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/content">
                    内容管理
                  </el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin/notifications">
                    通知群发
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login">
              <el-button type="primary" plain size="small">登录 / 注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="container main">
      <router-view />
    </main>

    <footer class="site-footer">
      <span>MyShop 学习项目 · Spring Boot 3 + Vue 3</span>
      <span class="footer-links">
        <router-link to="/privacy">隐私政策</router-link>
        <router-link to="/terms">用户协议</router-link>
      </span>
    </footer>
  </div>
</template>
