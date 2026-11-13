-- 评价审核：历史评价默认通过保持原展示效果，新评价由应用按敏感词判定
ALTER TABLE `review`
    ADD COLUMN `status` VARCHAR(16) NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING/APPROVED/REJECTED' AFTER `images`,
    ADD COLUMN `audit_remark` VARCHAR(200) NULL COMMENT '审核备注（驳回原因）' AFTER `status`;

CREATE INDEX `idx_review_product_status` ON `review` (`product_id`, `status`);
