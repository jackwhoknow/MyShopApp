package com.example.myshopapp.address.entity;

import com.example.myshopapp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 收货地址簿：下单时会把这里的收货信息快照进订单，后续改地址不影响历史订单。
 */
@Entity
@Table(name = "user_address", indexes = @Index(name = "idx_address_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String province;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(nullable = false, length = 200)
    private String detail;

    /** 数据库列名用 is_default，避免 default 关键字冲突 */
    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 省市县 + 详细地址，下单时直接快照这一串 */
    public String fullAddress() {
        return province + city + district + detail;
    }
}
