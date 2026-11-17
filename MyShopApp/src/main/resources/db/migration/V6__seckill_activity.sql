-- 秒杀活动：活动时间、活动价、活动库存与每人限购
CREATE TABLE IF NOT EXISTS `seckill_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `activity_price` decimal(10,2) NOT NULL,
  `total_stock` int NOT NULL,
  `sold_count` int NOT NULL DEFAULT 0,
  `per_user_limit` int NOT NULL DEFAULT 1,
  `start_at` datetime(6) NOT NULL,
  `end_at` datetime(6) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_seckill_shop` (`shop_id`),
  KEY `idx_seckill_product` (`product_id`),
  KEY `idx_seckill_sku` (`sku_id`),
  KEY `idx_seckill_window` (`start_at`,`end_at`),
  CONSTRAINT `fk_seckill_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`),
  CONSTRAINT `fk_seckill_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `fk_seckill_sku` FOREIGN KEY (`sku_id`) REFERENCES `product_sku` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 订单项记录秒杀活动，用于每人限购统计与活动销量回补
ALTER TABLE `order_item`
  ADD COLUMN `seckill_activity_id` bigint DEFAULT NULL AFTER `sku_id`,
  ADD KEY `idx_order_item_activity` (`seckill_activity_id`);
