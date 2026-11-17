package com.example.myshopapp.points.repository;

import com.example.myshopapp.points.entity.UserCheckin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserCheckinRepository extends JpaRepository<UserCheckin, Long> {

    boolean existsByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);

    Optional<UserCheckin> findByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);

    /** 最近签到记录，按日期倒序，用于计算连续天数 */
    List<UserCheckin> findByUserIdOrderByCheckinDateDesc(Long userId, Pageable pageable);
}
