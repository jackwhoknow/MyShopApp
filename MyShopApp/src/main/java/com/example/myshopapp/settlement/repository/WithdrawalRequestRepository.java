package com.example.myshopapp.settlement.repository;

import com.example.myshopapp.settlement.entity.WithdrawalRequest;
import com.example.myshopapp.settlement.entity.WithdrawalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    Page<WithdrawalRequest> findByShopIdOrderByIdDesc(Long shopId, Pageable pageable);

    Page<WithdrawalRequest> findByShopIdAndStatusOrderByIdDesc(Long shopId, WithdrawalStatus status,
                                                              Pageable pageable);

    /** 提现单加锁，避免审核与打款并发重复处理 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WithdrawalRequest w where w.id = :id")
    Optional<WithdrawalRequest> findByIdForUpdate(@Param("id") Long id);

    /** 商家维度提现金额汇总，无数据时返回 null */
    @Query("""
            select sum(w.amount) from WithdrawalRequest w
            where w.shop.id = :shopId
              and w.status in :statuses
            """)
    BigDecimal sumAmountByShopAndStatuses(@Param("shopId") Long shopId,
                                          @Param("statuses") Collection<WithdrawalStatus> statuses);

    long countByShopIdAndStatusIn(Long shopId, Collection<WithdrawalStatus> statuses);

    long countByShopId(Long shopId);

    /** 平台维度提现金额汇总，无数据时返回 null */
    @Query("select sum(w.amount) from WithdrawalRequest w where w.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") WithdrawalStatus status);

    long countByStatus(WithdrawalStatus status);

    /**
     * 平台提现审核列表：支持状态与关键词（提现单号/店铺名/户名/账号）过滤。
     */
    @Query(value = """
            select w from WithdrawalRequest w
            where (:hasStatus = false or w.status = :status)
              and (:hasKeyword = false
                   or w.withdrawalNo like :keywordPattern
                   or w.shop.name like :keywordPattern
                   or w.accountName like :keywordPattern
                   or w.accountNo like :keywordPattern)
            order by w.id desc
            """,
            countQuery = """
            select count(w) from WithdrawalRequest w
            where (:hasStatus = false or w.status = :status)
              and (:hasKeyword = false
                   or w.withdrawalNo like :keywordPattern
                   or w.shop.name like :keywordPattern
                   or w.accountName like :keywordPattern
                   or w.accountNo like :keywordPattern)
            """)
    Page<WithdrawalRequest> searchAdmin(@Param("hasStatus") boolean hasStatus,
                                        @Param("status") WithdrawalStatus status,
                                        @Param("hasKeyword") boolean hasKeyword,
                                        @Param("keywordPattern") String keywordPattern,
                                        Pageable pageable);
}
