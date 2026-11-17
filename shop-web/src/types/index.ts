/** 与后端 ApiResponse / PageResult 结构一一对应 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  phone?: string
  /** 找回密码使用的邮箱 */
  email?: string
  roles: string[]
}

export interface LoginResult {
  token: string
  /** 一次性 refresh token，access token 过期后用它续期 */
  refreshToken: string
  expiresIn: number
  refreshExpiresIn: number
  user: UserInfo
}

/** 后台用户管理列表项 */
export interface AdminUser {
  id: number
  username: string
  nickname?: string
  phone?: string
  roles: string[]
  status: 'ACTIVE' | 'DISABLED'
  createdAt?: string
}

export interface CategoryNode {
  id: number
  name: string
  level: number
  sortOrder: number
  children?: CategoryNode[]
}

export interface ProductListItem {
  id: number
  name: string
  subtitle?: string
  mainImage?: string
  minPrice: number
  maxPrice: number
  sales: number
  status: string
  shopId: number
  shopName: string
  categoryId: number
  /** 审核驳回理由，未驳回时为空 */
  rejectReason?: string
}

export interface ProductSpecValue {
  id: number
  value: string
}

export interface ProductSpec {
  id: number
  name: string
  values: ProductSpecValue[]
}

export interface SkuSpec {
  specValueId: number
  specName: string
  value: string
}

export interface ProductSku {
  id: number
  price: number
  stock: number
  image?: string
  specValues: SkuSpec[]
}

export interface ProductDetail {
  id: number
  name: string
  subtitle?: string
  description?: string
  mainImage?: string
  minPrice: number
  maxPrice: number
  sales: number
  /** 每人限购数量，0 表示不限购 */
  purchaseLimit?: number
  status: string
  shopId: number
  shopName: string
  categoryId: number
  categoryName: string
  /** 审核驳回理由，未驳回时为空 */
  rejectReason?: string
  specs: ProductSpec[]
  skus: ProductSku[]
}

export interface FavoriteProduct {
  productId: number
  name: string
  subtitle?: string
  mainImage?: string
  minPrice: number
  maxPrice: number
  sales: number
  status: string
  shopId: number
  shopName: string
  favoritedAt: string
}

/** 最近浏览的商品，viewedAt 为最近一次浏览时间 */
export interface BrowseHistoryItem {
  productId: number
  name: string
  subtitle?: string
  mainImage?: string
  minPrice: number
  maxPrice: number
  sales: number
  status: string
  shopId: number
  shopName: string
  viewedAt: string
}

export interface CartItem {
  id: number
  skuId: number
  productId: number
  productName: string
  skuDesc: string
  image?: string
  price: number
  /** SKU 原价，命中秒杀活动时与 price 不同 */
  originalPrice?: number
  quantity: number
  stock: number
  available: boolean
  /** 是否按秒杀活动价展示 */
  seckill?: boolean
  shopId: number
  shopName: string
}

/** 秒杀活动：state 为 NOT_STARTED / RUNNING / SOLD_OUT / EXPIRED / DISABLED */
export interface SeckillActivity {
  id: number
  shopId: number
  shopName: string
  productId: number
  productName: string
  skuId: number
  skuDesc: string
  image?: string
  originalPrice: number
  activityPrice: number
  totalStock: number
  soldCount: number
  remainingStock: number
  perUserLimit: number
  startAt: string
  endAt: string
  enabled: boolean
  state: string
}

export interface Address {
  id: number
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  fullAddress: string
  defaultAddress: boolean
}

export interface OrderItem {
  id: number
  productId: number
  skuId: number
  productName: string
  skuDesc: string
  image?: string
  price: number
  quantity: number
  totalPrice: number
}

export interface SubOrder {
  id: number
  subOrderNo: string
  shopId: number
  shopName: string
  totalAmount: number
  discountAmount: number
  couponTitle?: string
  /** 满减活动优惠金额 */
  promotionDiscount?: number
  /** 命中的满减活动名称 */
  promotionTitle?: string
  status: string
  shipCompany?: string
  trackingNo?: string
  shippedAt?: string
  completedAt?: string
  items: OrderItem[]
}

/** 物流轨迹节点（模拟数据），最新节点由后端排在列表最前 */
export interface LogisticsTrace {
  title: string
  description: string
  location: string
  occurredAt: string
}

/** 子订单物流轨迹 */
export interface SubOrderLogistics {
  subOrderId: number
  subOrderNo: string
  status: string
  shipCompany?: string
  trackingNo?: string
  shippedAt?: string
  completedAt?: string
  traces: LogisticsTrace[]
}

/** 商家履约视角的子订单，额外带买家收货信息与主订单信息 */
export interface MerchantSubOrder {
  id: number
  subOrderNo: string
  shopId: number
  shopName: string
  totalAmount: number
  discountAmount: number
  couponTitle?: string
  status: string
  createdAt: string
  shippedAt?: string
  completedAt?: string
  shipCompany?: string
  trackingNo?: string
  orderId: number
  orderNo: string
  orderStatus: string
  paidAt?: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  merchantRemark?: string
  items: OrderItem[]
}

export interface BatchShipFailure {
  subOrderId: number
  subOrderNo?: string
  reason: string
}

export interface BatchShipResult {
  total: number
  shipped: number
  failures: BatchShipFailure[]
}

/** 商品评价：买家评价，商家可回复一次 */
export interface Review {
  id: number
  orderItemId: number
  orderNo: string
  subOrderNo: string
  productId: number
  productName: string
  skuDesc?: string
  image?: string
  rating: number
  content?: string
  /** 评价图片地址列表，最多 6 张 */
  images?: string[]
  /** 追评内容与图片 */
  appendContent?: string
  appendImages?: string[]
  appendedAt?: string
  reply?: string
  repliedAt?: string
  /** 审核状态：PENDING 待审核、APPROVED 通过、REJECTED 驳回 */
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditRemark?: string
  createdAt: string
}

export interface Order {
  id: number
  orderNo: string
  totalAmount: number
  /** 下单时使用的积分 */
  pointsUsed: number
  /** 积分抵扣的金额 */
  pointsDiscount: number
  /** 支付成功返还的积分 */
  pointsEarned: number
  status: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  createdAt: string
  paidAt?: string
  subOrders: SubOrder[]
}

/** 积分账户概览 */
export interface PointAccount {
  balance: number
  totalEarned: number
  totalSpent: number
  /** 会员等级编码：BRONZE / SILVER / GOLD / PLATINUM / DIAMOND */
  level: string
  levelName: string
  nextLevelName?: string | null
  pointsToNext?: number | null
}

/** 每日签到状态 */
export interface CheckinStatus {
  checkedInToday: boolean
  consecutiveDays: number
  todayPoints: number
  nextPoints: number
}

/** 积分兑换券 */
export interface PointCoupon {
  id: number
  shopId?: number | null
  shopName?: string | null
  platform: boolean
  title: string
  thresholdAmount: number
  discountAmount: number
  pointsPrice: number
  remainingCount: number
  endAt: string
  exchanged: boolean
}

/** 积分流水，changeAmount 正数为获得、负数为扣减 */
export interface PointTransaction {
  id: number
  changeAmount: number
  balanceAfter: number
  /** ORDER_DEDUCT / ORDER_EARN / ORDER_CANCEL_RETURN / ORDER_REFUND */
  type: string
  orderId?: number
  orderNo?: string
  remark?: string
  createdAt: string
}

/** 结算页积分抵扣试算 */
export interface PointQuote {
  balance: number
  maxDeductiblePoints: number
  maxDeductibleAmount: number
}

export interface ReorderSkippedItem {
  productName: string
  skuDesc?: string
  reason: string
}

export interface ReorderResult {
  addedCount: number
  skippedCount: number
  skippedItems: ReorderSkippedItem[]
}

/** 售后单，买家与商家共用；未发货为仅退款，发货后退货退款 */
export interface AfterSale {
  id: number
  afterSaleNo: string
  subOrderId: number
  subOrderNo: string
  orderId: number
  orderNo: string
  shopId: number
  shopName: string
  buyerUsername: string
  amount: number
  reason: string
  /** 买家上传的凭证图片 */
  evidenceImages?: string[]
  type: string
  status: string
  merchantRemark?: string
  adminRemark?: string
  returnCompany?: string
  returnTrackingNo?: string
  returnedAt?: string
  receivedAt?: string
  createdAt: string
  handledAt?: string
  items: OrderItem[]
}

export interface Shop {
  id: number
  ownerId: number
  ownerUsername: string
  name: string
  description?: string
  status: string
  rejectReason?: string
  createdAt: string
}

/** 店铺主页公开信息 */
export interface ShopDetail {
  id: number
  name: string
  description?: string
  productCount: number
  followers: number
  createdAt: string
}

/** 站内通知：readFlag 为 false 时前端展示未读点 */
export interface NotificationMessage {
  id: number
  type: string
  title: string
  content: string
  link?: string
  readFlag: boolean
  createdAt: string
}

export interface AdminCategoryNode {
  id: number
  parentId?: number
  name: string
  level: number
  sortOrder: number
  enabled: boolean
  children: AdminCategoryNode[]
}

/** 商家看板趋势点：某天的销售额与订单数 */
export interface MerchantStatsTrendPoint {
  date: string
  orderCount: number
  salesAmount: number
}

/** 商家看板热销商品 */
export interface MerchantStatsTopProduct {
  productId: number
  productName: string
  quantity: number
  salesAmount: number
}

export interface MerchantStats {
  todayOrderCount: number
  todaySalesAmount: number
  pendingShipCount: number
  refundingCount: number
  totalSalesAmount: number
  days: number
  trend: MerchantStatsTrendPoint[]
  topProducts: MerchantStatsTopProduct[]
}

/** 领券中心的可领取券：platform 为 true 表示平台通用券，shopId / shopName 为空 */
export interface Coupon {
  id: number
  shopId?: number | null
  shopName?: string | null
  platform: boolean
  title: string
  thresholdAmount: number
  discountAmount: number
  remainingCount: number
  startAt: string
  endAt: string
}

/** 我的券：state 为 UNUSED / USED / EXPIRED */
export interface UserCoupon {
  id: number
  couponId: number
  shopId?: number | null
  shopName?: string | null
  platform: boolean
  title: string
  thresholdAmount: number
  discountAmount: number
  state: string
  orderId?: number
  claimedAt: string
  usedAt?: string
  endAt: string
}

/** 券管理视图（商家券 / 平台券共用） */
export interface MerchantCoupon {
  id: number
  shopId?: number | null
  shopName?: string | null
  platform: boolean
  title: string
  thresholdAmount: number
  discountAmount: number
  totalCount: number
  claimedCount: number
  /** 积分兑换所需积分，0 表示不参与积分兑换 */
  pointsPrice: number
  usedCount: number
  remainingCount: number
  startAt: string
  endAt: string
  enabled: boolean
  state: string
  createdAt: string
}

/** 首页轮播图，imageUrl 为空时前台用渐变色占位 */
export interface Banner {
  id: number
  title: string
  imageUrl?: string
  linkUrl?: string
  sortOrder: number
  enabled: boolean
  createdAt?: string
}

/** 平台公告，pinned 为 true 时置顶 */
export interface Announcement {
  id: number
  title: string
  content: string
  pinned: boolean
  enabled: boolean
  publishedAt?: string
  createdAt?: string
}

/** 首页内容聚合 */
export interface HomeContent {
  banners: Banner[]
  announcements: Announcement[]
}

/** 平台看板趋势点：某天的订单数与销售额 */
export interface AdminDashboardTrendPoint {
  date: string
  orderCount: number
  salesAmount: number
}

/** 平台看板热销商品 */
export interface AdminDashboardTopProduct {
  productId: number
  productName: string
  quantity: number
  salesAmount: number
}

/** 平台经营看板 */
export interface AdminDashboard {
  totalUsers: number
  activeShops: number
  pendingShops: number
  onShelfProducts: number
  totalOrders: number
  totalSalesAmount: number
  todayOrderCount: number
  todaySalesAmount: number
  refundingCount: number
  days: number
  trend: AdminDashboardTrendPoint[]
  topProducts: AdminDashboardTopProduct[]
}

/** 平台订单列表中的子订单摘要 */
export interface AdminOrderSubOrderBrief {
  id: number
  subOrderNo: string
  shopId: number
  shopName: string
  status: string
  totalAmount: number
  itemCount: number
  createdAt: string
  shipCompany?: string
  trackingNo?: string
  shippedAt?: string
  afterSaleStatus?: string | null
}

/** 平台订单列表视图 */
export interface AdminOrder {
  id: number
  orderNo: string
  totalAmount: number
  status: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  createdAt: string
  paidAt?: string
  buyerId: number
  buyerUsername: string
  buyerNickname?: string
  subOrders: AdminOrderSubOrderBrief[]
}

/** 子订单最新售后单摘要，无售后时为 null */
export interface AdminAfterSaleBrief {
  id: number
  afterSaleNo: string
  type: string
  status: string
  amount: number
  reason: string
  createdAt: string
  handledAt?: string
}

/** 平台订单详情中的子订单，含商品明细与售后 */
export interface AdminOrderSubOrderDetail {
  id: number
  subOrderNo: string
  shopId: number
  shopName: string
  totalAmount: number
  discountAmount: number
  couponTitle?: string
  status: string
  createdAt: string
  shippedAt?: string
  completedAt?: string
  shipCompany?: string
  trackingNo?: string
  merchantRemark?: string
  items: OrderItem[]
  afterSale?: AdminAfterSaleBrief | null
}

/** 平台订单详情视图 */
export interface AdminOrderDetail {
  id: number
  orderNo: string
  totalAmount: number
  status: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  createdAt: string
  paidAt?: string
  buyerId: number
  buyerUsername: string
  buyerNickname?: string
  buyerPhone?: string
  subOrders: AdminOrderSubOrderDetail[]
}

/** 管理员群发通知的目标人群 */
export type NotificationAudience = 'ALL' | 'BUYER' | 'MERCHANT'

/** 管理员群发记录 */
export interface AdminBroadcast {
  id: number
  title: string
  content: string
  link?: string
  audience: NotificationAudience
  recipientCount: number
  senderName: string
  createdAt: string
}

/** 各发送范围的接收人数预估 */
export interface AudienceCounts {
  all: number
  buyer: number
  merchant: number
}

/** 商家库存预警条目 */
export interface StockAlert {
  skuId: number
  productId: number
  productName: string
  image?: string | null
  specDesc: string
  price: number
  stock: number
  productStatus: string
  sales: number
}

/** 库存预警概览 */
export interface StockAlertSummary {
  outOfStockCount: number
  lowStockCount: number
  totalSkuCount: number
  threshold: number
}

/** 店铺关注状态与粉丝数 */
export interface FollowState {
  following: boolean
  followers: number
}

/** 我关注的店铺 */
export interface FollowedShop {
  shopId: number
  name: string
  description?: string
  status: string
  followedAt: string
}

/** 订单发票，状态 PENDING（待开票）/ ISSUED（已开票） */
export interface Invoice {
  id: number
  orderId: number
  type: 'PERSONAL' | 'COMPANY'
  title: string
  taxNo?: string
  email?: string
  amount: number
  status: string
  createdAt: string
  issuedAt?: string
}

/** 管理员操作审计日志 */
export interface AdminAuditLog {
  id: number
  operatorId?: number
  operatorName?: string
  method: string
  uri: string
  queryString?: string
  ip?: string
  success: boolean
  status: number
  durationMs: number
  createdAt: string
}

/** 商家结算概览 */
export interface SettlementSummary {
  totalIncome: number
  refundedAmount: number
  withdrawnAmount: number
  frozenAmount: number
  availableAmount: number
  pendingWithdrawalCount: number
  totalWithdrawalCount: number
}

/** 结算账单明细，settleable 为 true 表示计入可结算收入 */
export interface SettlementBill {
  subOrderId: number
  subOrderNo: string
  orderNo: string
  amount: number
  status: string
  paidAt?: string
  completedAt?: string
  settleable: boolean
}

/** 提现申请，状态 PENDING / APPROVED / REJECTED / PAID */
export interface WithdrawalRequest {
  id: number
  withdrawalNo: string
  shopId: number
  shopName: string
  amount: number
  accountName: string
  accountNo: string
  bankName: string
  status: string
  adminRemark?: string
  createdAt: string
  reviewedAt?: string
  paidAt?: string
}

/** 平台提现审核概览 */
export interface AdminSettlementSummary {
  pendingCount: number
  pendingAmount: number
  approvedCount: number
  approvedAmount: number
  paidCount: number
  paidAmount: number
}

/** 店铺满减活动 */
export interface ShopPromotion {
  id: number
  shopId: number
  shopName: string
  title: string
  thresholdAmount: number
  reduceAmount: number
  startAt: string
  endAt: string
  enabled: boolean
  running: boolean
}

/** 客服会话 */
export interface SupportConversation {
  id: number
  userId: number
  userName: string
  shopId: number
  shopName: string
  orderId?: number
  status: string
  lastMessagePreview?: string
  lastMessageAt?: string
  unread: number
}

/** 客服消息 */
export interface SupportMessage {
  id: number
  conversationId: number
  senderType: 'USER' | 'SHOP' | 'PLATFORM'
  senderId: number
  content: string
  readFlag: boolean
  createdAt: string
}
