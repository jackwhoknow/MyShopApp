-- 安全加固与上线功能增量脚本

-- 登录失败次数与锁定时间
ALTER TABLE `sys_user`
  ADD COLUMN `failed_login_attempts` int NOT NULL DEFAULT 0 AFTER `status`,
  ADD COLUMN `locked_until` datetime(6) DEFAULT NULL AFTER `failed_login_attempts`;

-- 支付记录增加退款状态
ALTER TABLE `payment_record`
  MODIFY COLUMN `status` enum('FAILED','REFUND_REQUIRED','SUCCESS','REFUNDED') NOT NULL,
  ADD COLUMN `refund_no` varchar(64) DEFAULT NULL,
  ADD COLUMN `refunded_at` datetime(6) DEFAULT NULL;

-- 评价图片（多张图片以英文逗号分隔存储）
ALTER TABLE `review`
  ADD COLUMN `images` varchar(1000) DEFAULT NULL AFTER `content`;

-- 店铺关注
CREATE TABLE IF NOT EXISTS `shop_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_follow` (`user_id`,`shop_id`),
  KEY `idx_shop_follow_shop` (`shop_id`),
  CONSTRAINT `fk_shop_follow_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_shop_follow_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 订单发票
CREATE TABLE IF NOT EXISTS `invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL,
  `title` varchar(100) NOT NULL,
  `tax_no` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `issued_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_order` (`order_id`),
  KEY `idx_invoice_user` (`user_id`),
  KEY `idx_invoice_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 管理员操作审计日志
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  `method` varchar(10) NOT NULL,
  `uri` varchar(255) NOT NULL,
  `query_string` varchar(500) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `success` bit(1) NOT NULL,
  `status` int NOT NULL,
  `duration_ms` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ShedLock 多实例定时任务锁
CREATE TABLE IF NOT EXISTS `shedlock` (
  `name` varchar(64) NOT NULL,
  `lock_until` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
