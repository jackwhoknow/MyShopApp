package com.example.myshopapp.points.entity;

import com.example.myshopapp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到记录：每人每天一条，记录当天获得的积分。
 */
@Entity
@Table(name = "user_checkin",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_checkin_day",
                columnNames = {"user_id", "checkin_date"}))
@Getter
@Setter
@NoArgsConstructor
public class UserCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate checkinDate;

    @Column(nullable = false)
    private int points;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
