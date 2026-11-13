-- 基线结构：与项目初始版本的表结构保持一致
-- 已存在的旧库会由 Flyway baseline 跳过本脚本，只执行后续增量脚本

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted` bit(1) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `password` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` enum('ACTIVE','DISABLED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(100) NOT NULL,
  `name` varchar(50) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `path` varchar(200) DEFAULT NULL,
  `type` enum('API','BUTTON','MENU') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_sys_user_role_role` (`role_id`),
  CONSTRAINT `fk_sys_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_sys_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `idx_sys_role_permission_permission` (`permission_id`),
  CONSTRAINT `fk_sys_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_sys_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_default` bit(1) NOT NULL,
  `detail` varchar(200) NOT NULL,
  `district` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `province` varchar(50) NOT NULL,
  `receiver_name` varchar(50) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_address_user` (`user_id`),
  CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `shop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audited_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `reject_reason` varchar(200) DEFAULT NULL,
  `status` enum('ACTIVE','CLOSED','PENDING','REJECTED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `low_stock_threshold` int NOT NULL DEFAULT '5',
  PRIMARY KEY (`id`),
  KEY `idx_shop_owner` (`owner_id`),
  CONSTRAINT `fk_shop_owner` FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `level` int NOT NULL,
  `name` varchar(50) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `path` varchar(200) NOT NULL,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category_parent` (`parent_id`),
  KEY `idx_category_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `main_image` varchar(255) DEFAULT NULL,
  `max_price` decimal(10,2) DEFAULT NULL,
  `min_price` decimal(10,2) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `sales` int NOT NULL,
  `status` enum('DRAFT','OFF_SHELF','ON_SHELF','PENDING_REVIEW') NOT NULL,
  `subtitle` varchar(200) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `category_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `reject_reason` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product_shop` (`shop_id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_status` (`status`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
  CONSTRAINT `fk_product_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock` int NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product_sku_product` (`product_id`),
  CONSTRAINT `fk_product_sku_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `sort_order` int NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product_spec_product` (`product_id`),
  CONSTRAINT `fk_product_spec_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product_spec_value` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sort_order` int NOT NULL,
  `value` varchar(50) NOT NULL,
  `spec_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_spec_value_spec` (`spec_id`),
  CONSTRAINT `fk_spec_value_spec` FOREIGN KEY (`spec_id`) REFERENCES `product_spec` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sku_spec_value` (
  `sku_id` bigint NOT NULL,
  `spec_value_id` bigint NOT NULL,
  PRIMARY KEY (`sku_id`,`spec_value_id`),
  KEY `idx_sku_spec_value_value` (`spec_value_id`),
  CONSTRAINT `fk_sku_spec_value_value` FOREIGN KEY (`spec_value_id`) REFERENCES `product_spec_value` (`id`),
  CONSTRAINT `fk_sku_spec_value_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `cart_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `quantity` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `sku_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_user_sku` (`user_id`,`sku_id`),
  KEY `idx_cart_user` (`user_id`),
  KEY `idx_cart_sku` (`sku_id`),
  CONSTRAINT `fk_cart_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`id`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `order_no` varchar(32) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `receiver_address` varchar(255) NOT NULL,
  `receiver_name` varchar(50) NOT NULL,
  `receiver_phone` varchar(20) NOT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','PAID','PENDING_PAYMENT') NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_orders_user` (`user_id`),
  KEY `idx_orders_status` (`status`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sub_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','REFUNDING','SHIPPED','WAIT_SHIP') NOT NULL,
  `sub_order_no` varchar(40) NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `order_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `ship_company` varchar(50) DEFAULT NULL,
  `shipped_at` datetime(6) DEFAULT NULL,
  `tracking_no` varchar(50) DEFAULT NULL,
  `merchant_remark` varchar(200) DEFAULT NULL,
  `coupon_title` varchar(50) DEFAULT NULL,
  `discount_amount` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sub_order_order` (`order_id`),
  KEY `idx_sub_order_shop` (`shop_id`),
  CONSTRAINT `fk_sub_order_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_sub_order_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(100) NOT NULL,
  `quantity` int NOT NULL,
  `sku_desc` varchar(100) DEFAULT NULL,
  `sku_id` bigint NOT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `sub_order_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_item_sub` (`sub_order_id`),
  CONSTRAINT `fk_order_item_sub_order` FOREIGN KEY (`sub_order_id`) REFERENCES `sub_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `payment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(10,2) NOT NULL,
  `channel` varchar(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `pay_no` varchar(32) NOT NULL,
  `status` enum('FAILED','REFUND_REQUIRED','SUCCESS') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no` (`pay_no`),
  KEY `idx_pay_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `after_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `after_sale_no` varchar(40) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `handled_at` datetime(6) DEFAULT NULL,
  `merchant_remark` varchar(200) DEFAULT NULL,
  `reason` varchar(200) NOT NULL,
  `shop_id` bigint NOT NULL,
  `status` enum('CLOSED','PENDING','REFUNDED','REJECTED','RETURNING') NOT NULL,
  `user_id` bigint NOT NULL,
  `sub_order_id` bigint NOT NULL,
  `received_at` datetime(6) DEFAULT NULL,
  `return_company` varchar(50) DEFAULT NULL,
  `return_tracking_no` varchar(50) DEFAULT NULL,
  `returned_at` datetime(6) DEFAULT NULL,
  `type` enum('REFUND_ONLY','RETURN_REFUND') DEFAULT NULL,
  `admin_remark` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_after_sale_sub_order` (`sub_order_id`),
  KEY `idx_after_sale_user` (`user_id`),
  KEY `idx_after_sale_shop` (`shop_id`),
  CONSTRAINT `fk_after_sale_sub_order` FOREIGN KEY (`sub_order_id`) REFERENCES `sub_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `claimed_count` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `discount_amount` decimal(10,2) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `start_at` datetime(6) NOT NULL,
  `threshold_amount` decimal(10,2) NOT NULL,
  `title` varchar(50) NOT NULL,
  `total_count` int NOT NULL,
  `shop_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_shop` (`shop_id`),
  CONSTRAINT `fk_coupon_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `claimed_at` datetime(6) DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `status` enum('UNUSED','USED') NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `coupon_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon` (`coupon_id`,`user_id`),
  KEY `idx_user_coupon_user` (`user_id`),
  CONSTRAINT `fk_user_coupon_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`),
  CONSTRAINT `fk_user_coupon_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `replied_at` datetime(6) DEFAULT NULL,
  `reply` varchar(500) DEFAULT NULL,
  `shop_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `order_item_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_order_item` (`order_item_id`),
  KEY `idx_review_shop` (`shop_id`),
  KEY `idx_review_user` (`user_id`),
  CONSTRAINT `fk_review_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `product_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_favorite` (`product_id`,`user_id`),
  KEY `idx_product_favorite_user` (`user_id`),
  CONSTRAINT `fk_product_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_product_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `browse_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `viewed_at` datetime(6) NOT NULL,
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_browse_history` (`product_id`,`user_id`),
  KEY `idx_browse_history_user` (`user_id`),
  CONSTRAINT `fk_browse_history_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_browse_history_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `link` varchar(200) DEFAULT NULL,
  `read_flag` bit(1) NOT NULL,
  `title` varchar(100) NOT NULL,
  `type` enum('ADMIN_BROADCAST','AFTER_SALE_APPLIED','AFTER_SALE_APPROVED','AFTER_SALE_PLATFORM_INTERVENED','AFTER_SALE_REFUNDED','AFTER_SALE_REJECTED','AFTER_SALE_RETURN_SHIPPED','NEW_ORDER','ORDER_CANCELLED','ORDER_COMPLETED','ORDER_PAID','ORDER_SHIPPED') NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user` (`user_id`),
  KEY `idx_notification_user_read` (`user_id`,`read_flag`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `notification_broadcast` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `audience` enum('ALL','BUYER','MERCHANT') NOT NULL,
  `content` varchar(500) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `link` varchar(200) DEFAULT NULL,
  `recipient_count` int NOT NULL,
  `sender_name` varchar(50) NOT NULL,
  `title` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(2000) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `pinned` bit(1) NOT NULL,
  `published_at` datetime(6) NOT NULL,
  `title` varchar(100) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `link_url` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `title` varchar(100) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
