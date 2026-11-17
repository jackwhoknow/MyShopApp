package com.example.myshopapp.points.repository;

import com.example.myshopapp.points.entity.PointAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByUserId(Long userId);

    /** 积分变动前加锁，避免并发下单或退款把余额算错 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PointAccount a where a.user.id = :userId")
    Optional<PointAccount> findByUserIdForUpdate(@Param("userId") Long userId);
}
