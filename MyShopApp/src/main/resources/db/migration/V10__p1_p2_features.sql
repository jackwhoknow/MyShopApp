-- 用户邮箱：忘记密码通过邮箱验证码重置（验证码只写 Redis 并打印日志，不落库）
ALTER TABLE `sys_user`
  ADD COLUMN `email` varchar(100) DEFAULT NULL AFTER `phone`,
  ADD UNIQUE KEY `uk_sys_user_email` (`email`);

-- 评价追评：买家可以对已评价商品追加一次追评
ALTER TABLE `review`
  ADD COLUMN `append_content` varchar(500) DEFAULT NULL AFTER `reply`,
  ADD COLUMN `append_images` varchar(1000) DEFAULT NULL AFTER `append_content`,
  ADD COLUMN `appended_at` datetime(6) DEFAULT NULL AFTER `append_images`;

-- 售后凭证与超时自动关闭时间
ALTER TABLE `after_sale`
  ADD COLUMN `evidence_images` varchar(1000) DEFAULT NULL AFTER `reason`,
  ADD COLUMN `closed_at` datetime(6) DEFAULT NULL AFTER `handled_at`;

-- 商品每人限购数量，0 表示不限购
ALTER TABLE `product`
  ADD COLUMN `purchase_limit` int NOT NULL DEFAULT 0 AFTER `sales`;

-- 优惠券积分兑换价格，0 表示不可用积分兑换
ALTER TABLE `coupon`
  ADD COLUMN `points_price` int NOT NULL DEFAULT 0 AFTER `total_count`;

-- 子订单满减优惠：优惠在优惠券之前计算
ALTER TABLE `sub_order`
  ADD COLUMN `promotion_discount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `discount_amount`,
  ADD COLUMN `promotion_title` varchar(50) DEFAULT NULL AFTER `promotion_discount`;

-- 店铺满减活动
CREATE TABLE IF NOT EXISTS `shop_promotion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL,
  `title` varchar(50) NOT NULL,
  `threshold_amount` decimal(10,2) NOT NULL,
  `reduce_amount` decimal(10,2) NOT NULL,
  `start_at` datetime(6) NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_shop_promotion_shop` (`shop_id`),
  CONSTRAINT `fk_shop_promotion_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 每日签到：每人每天一条，记录当天获得的积分
CREATE TABLE IF NOT EXISTS `user_checkin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `checkin_date` date NOT NULL,
  `points` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_checkin_day` (`user_id`, `checkin_date`),
  CONSTRAINT `fk_user_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 在线客服会话：买家与店铺一对一，最后一次消息用于会话列表展示
CREATE TABLE IF NOT EXISTS `support_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `last_message_at` datetime(6) DEFAULT NULL,
  `last_message_preview` varchar(200) DEFAULT NULL,
  `user_unread` int NOT NULL DEFAULT 0,
  `shop_unread` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_support_conversation` (`user_id`, `shop_id`),
  KEY `idx_support_conversation_shop` (`shop_id`),
  CONSTRAINT `fk_support_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_support_conversation_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 客服消息：sender_type 区分买家与店铺
CREATE TABLE IF NOT EXISTS `support_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `sender_type` varchar(10) NOT NULL,
  `sender_id` bigint NOT NULL,
  `content` varchar(1000) NOT NULL,
  `read_flag` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_support_message_conversation` (`conversation_id`, `id`),
  CONSTRAINT `fk_support_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `support_conversation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 通知发件箱：定时类通知按 dedupe_key 去重，避免重复提醒
CREATE TABLE IF NOT EXISTS `notification_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dedupe_key` varchar(120) NOT NULL,
  `user_id` bigint NOT NULL,
  `type` varchar(40) NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` varchar(500) NOT NULL,
  `link` varchar(200) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_outbox_dedupe` (`dedupe_key`),
  KEY `idx_notification_outbox_status` (`status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 通知类型扩展：自动确认收货、优惠券到期、会员升级、签到、客服消息、售后超时关闭
ALTER TABLE `notification`
  MODIFY COLUMN `type` enum(
    'ADMIN_BROADCAST',
    'AFTER_SALE_APPLIED',
    'AFTER_SALE_APPROVED',
    'AFTER_SALE_AUTO_CLOSED',
    'AFTER_SALE_PLATFORM_INTERVENED',
    'AFTER_SALE_REFUNDED',
    'AFTER_SALE_REJECTED',
    'AFTER_SALE_RETURN_SHIPPED',
    'CHECKIN_REWARD',
    'COUPON_EXPIRING',
    'MEMBER_LEVEL_UP',
    'NEW_ORDER',
    'ORDER_AUTO_COMPLETED',
    'ORDER_CANCELLED',
    'ORDER_COMPLETED',
    'ORDER_PAID',
    'ORDER_SHIPPED',
    'SUPPORT_MESSAGE',
    'WITHDRAWAL_APPROVED',
    'WITHDRAWAL_PAID',
    'WITHDRAWAL_REJECTED'
  ) NOT NULL;
