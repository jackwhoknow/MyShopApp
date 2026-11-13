package com.example.myshopapp.order.repository;

import com.example.myshopapp.order.entity.PaymentRecord;
import com.example.myshopapp.order.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    /** 第三方交易号是否已入账，用于回调幂等 */
    boolean existsByPayNo(String payNo);

    /** 订单最近一笔支付记录，退款时按渠道原路退回 */
    Optional<PaymentRecord> findFirstByOrderIdOrderByIdDesc(Long orderId);

    /** 对账扫描：指定状态且创建时间晚于扫描窗口的记录 */
    List<PaymentRecord> findTop50ByStatusAndCreatedAtAfterOrderByIdAsc(PaymentStatus status,
                                                                     LocalDateTime after);
}
