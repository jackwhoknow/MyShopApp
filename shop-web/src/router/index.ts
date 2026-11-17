import { createRouter, createWebHistory } from 'vue-router'

/**
 * 需要登录的页面放到 meta.requiresAuth，路由守卫统一拦截。
 */
const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/ProductListView.vue')
        },
        {
          path: 'products/:id',
          name: 'product-detail',
          component: () => import('@/views/ProductDetailView.vue')
        },
        {
          path: 'shops/:id',
          name: 'shop-home',
          component: () => import('@/views/ShopView.vue')
        },
        {
          path: 'seckill',
          name: 'seckill',
          component: () => import('@/views/SeckillView.vue')
        },
        {
          path: 'cart',
          name: 'cart',
          component: () => import('@/views/CartView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'checkout',
          name: 'checkout',
          component: () => import('@/views/CheckoutView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrderListView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('@/views/OrderDetailView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'addresses',
          name: 'addresses',
          component: () => import('@/views/AddressListView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('@/views/CouponsView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'points',
          name: 'points',
          component: () => import('@/views/PointsView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'support',
          name: 'support',
          component: () => import('@/views/SupportView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('@/views/FavoritesView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'followed-shops',
          name: 'followed-shops',
          component: () => import('@/views/FollowedShopsView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'privacy',
          name: 'privacy',
          component: () => import('@/views/AgreementView.vue'),
          meta: { doc: 'privacy' }
        },
        {
          path: 'terms',
          name: 'terms',
          component: () => import('@/views/AgreementView.vue'),
          meta: { doc: 'terms' }
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/NotificationsView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'history',
          name: 'history',
          component: () => import('@/views/HistoryView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'merchant/stats',
          name: 'merchant-stats',
          component: () => import('@/views/MerchantStatsView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/settlement',
          name: 'merchant-settlement',
          component: () => import('@/views/MerchantSettlementView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/promotions',
          name: 'merchant-promotions',
          component: () => import('@/views/MerchantPromotionsView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/support',
          name: 'merchant-support',
          component: () => import('@/views/MerchantSupportView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/coupons',
          name: 'merchant-coupons',
          component: () => import('@/views/MerchantCouponsView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/seckill',
          name: 'merchant-seckill',
          component: () => import('@/views/MerchantSeckillView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/inventory',
          name: 'merchant-inventory',
          component: () => import('@/views/MerchantInventoryView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/orders',
          name: 'merchant-orders',
          component: () => import('@/views/MerchantOrdersView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/after-sales',
          name: 'merchant-after-sales',
          component: () => import('@/views/MerchantAfterSalesView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/reviews',
          name: 'merchant-reviews',
          component: () => import('@/views/MerchantReviewsView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/products',
          name: 'merchant-products',
          component: () => import('@/views/MerchantProductsView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/products/new',
          name: 'merchant-product-new',
          component: () => import('@/views/MerchantProductCreateView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'merchant/shop',
          name: 'merchant-shop',
          component: () => import('@/views/MerchantShopView.vue'),
          meta: { requiresAuth: true, roles: ['MERCHANT', 'ADMIN'] }
        },
        {
          path: 'admin/support',
          name: 'admin-support',
          component: () => import('@/views/AdminSupportView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/orders',
          name: 'admin-orders',
          component: () => import('@/views/AdminOrdersView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/after-sales',
          name: 'admin-after-sales',
          component: () => import('@/views/AdminAfterSalesView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/notifications',
          name: 'admin-notifications',
          component: () => import('@/views/AdminNotificationsView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/categories',
          name: 'admin-categories',
          component: () => import('@/views/AdminCategoriesView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/shops',
          name: 'admin-shops',
          component: () => import('@/views/AdminShopsView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/products',
          name: 'admin-products',
          component: () => import('@/views/AdminProductAuditView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/users',
          name: 'admin-users',
          component: () => import('@/views/AdminUsersView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/invoices',
          name: 'admin-invoices',
          component: () => import('@/views/AdminInvoicesView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/settlements',
          name: 'admin-settlements',
          component: () => import('@/views/AdminSettlementsView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/reviews',
          name: 'admin-reviews',
          component: () => import('@/views/AdminReviewsView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/coupons',
          name: 'admin-coupons',
          component: () => import('@/views/AdminCouponsView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/audit-logs',
          name: 'admin-audit-logs',
          component: () => import('@/views/AdminAuditLogsView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/content',
          name: 'admin-content',
          component: () => import('@/views/AdminContentView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        },
        {
          path: 'admin/dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/AdminDashboardView.vue'),
          meta: { requiresAuth: true, roles: ['ADMIN'] }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue')
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles) {
    let roles: string[] = []
    try {
      roles = JSON.parse(localStorage.getItem('user') || '{}').roles ?? []
    } catch {
      roles = []
    }
    if (!roles.some((role) => requiredRoles.includes(role))) {
      return { path: '/' }
    }
  }
  return true
})

export default router
