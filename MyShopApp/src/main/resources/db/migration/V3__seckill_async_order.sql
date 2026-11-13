-- 秒杀异步下单：请求幂等键，保证 MQ 重复投递不会生成重复订单
ALTER TABLE `orders`
  ADD COLUMN `request_id` varchar(64) DEFAULT NULL AFTER `order_no`,
  ADD UNIQUE KEY `uk_orders_request_id` (`request_id`);
