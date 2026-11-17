-- 商家提现申请：商家从可结算余额发起提现，平台审核后打款
CREATE TABLE IF NOT EXISTS `withdrawal_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `withdrawal_no` varchar(40) NOT NULL,
  `shop_id` bigint NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `account_name` varchar(50) NOT NULL,
  `account_no` varchar(64) NOT NULL,
  `bank_name` varchar(100) NOT NULL,
  `status` varchar(20) NOT NULL,
  `admin_remark` varchar(200) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdrawal_no` (`withdrawal_no`),
  KEY `idx_withdrawal_shop` (`shop_id`),
  KEY `idx_withdrawal_status` (`status`),
  CONSTRAINT `fk_withdrawal_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
