package com.example.myshopapp.shop.repository;

import com.example.myshopapp.shop.entity.Shop;
import com.example.myshopapp.shop.entity.ShopStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    /** 提现申请时锁定店铺行，串行化同一店铺的余额扣减 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id = :id")
    Optional<Shop> findByIdForUpdate(@Param("id") Long id);

    /** 按审核状态分页查询 */
    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);

    /** 一个商家可能提交过多次申请，取最新一次 */
    Optional<Shop> findFirstByOwnerIdOrderByIdDesc(Long ownerId);

    /** 是否已存在待审核或已营业的店铺，用于阻止重复申请 */
    boolean existsByOwnerIdAndStatusIn(Long ownerId, Collection<ShopStatus> statuses);

    /** 平台看板：按审核状态统计店铺数量 */
    long countByStatus(ShopStatus status);
}
