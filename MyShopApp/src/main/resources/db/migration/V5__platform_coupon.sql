-- 平台券：shop_id 允许为空，为空即代表平台通用券（可与店铺券叠加使用）
ALTER TABLE `coupon`
  MODIFY COLUMN `shop_id` bigint DEFAULT NULL;
