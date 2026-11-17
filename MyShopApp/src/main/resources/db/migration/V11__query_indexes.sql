-- 高频查询索引治理：为列表、定时任务与推荐查询补齐联合索引。
-- 说明：MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，本迁移只执行一次；
-- 新环境由 Flyway 按序执行，存量环境若已手工建过同名索引需先删除后重跑。

-- 商品：前台按状态筛选 + 销量排序（首页推荐、相关推荐）
CREATE INDEX `idx_product_status_sales` ON `product` (`status`, `sales`);

-- 订单：买家订单列表（user_id + status）与订单超时扫描（status + created_at）
CREATE INDEX `idx_orders_user_status` ON `orders` (`user_id`, `status`);
CREATE INDEX `idx_orders_status_created` ON `orders` (`status`, `created_at`);

-- 子订单：商家订单列表（shop_id + status）与发货超时自动确认（status + shipped_at）
CREATE INDEX `idx_sub_order_shop_status` ON `sub_order` (`shop_id`, `status`);
CREATE INDEX `idx_sub_order_status_shipped` ON `sub_order` (`status`, `shipped_at`);

-- 售后：超时自动关闭扫描（status + created_at）
CREATE INDEX `idx_after_sale_status_created` ON `after_sale` (`status`, `created_at`);

-- 优惠券：买家券包（user_id + status）与满减/积分兑换筛选
CREATE INDEX `idx_user_coupon_user_status` ON `user_coupon` (`user_id`, `status`);
CREATE INDEX `idx_coupon_shop_enabled` ON `coupon` (`shop_id`, `enabled`);
CREATE INDEX `idx_coupon_points_enabled` ON `coupon` (`points_price`, `enabled`);
