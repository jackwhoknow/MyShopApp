-- 积分账户：每个买家一个账户，余额允许为负（整单退款时扣回已赠送积分）
CREATE TABLE IF NOT EXISTS `user_point_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `balance` int NOT NULL DEFAULT 0,
  `total_earned` int NOT NULL DEFAULT 0,
  `total_spent` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_point_account_user` (`user_id`),
  CONSTRAINT `fk_point_account_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 积分流水：每笔变动记录变动后余额，便于对账
CREATE TABLE IF NOT EXISTS `point_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `change_amount` int NOT NULL,
  `balance_after` int NOT NULL,
  `type` varchar(20) NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `order_no` varchar(40) DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_point_tx_user` (`user_id`, `id`),
  CONSTRAINT `fk_point_tx_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 订单积分：下单抵扣、支付赠送金额
ALTER TABLE `orders`
  ADD COLUMN `points_used` int NOT NULL DEFAULT 0 AFTER `total_amount`,
  ADD COLUMN `points_discount` decimal(10,2) NOT NULL DEFAULT 0.00 AFTER `points_used`,
  ADD COLUMN `points_earned` int NOT NULL DEFAULT 0 AFTER `points_discount`;
