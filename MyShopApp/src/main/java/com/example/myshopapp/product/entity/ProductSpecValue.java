package com.example.myshopapp.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 规格值，如“红色”“M 码”。
 */
@Entity
@Table(name = "product_spec_value")
@Getter
@Setter
@NoArgsConstructor
public class ProductSpecValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spec_id", nullable = false)
    private ProductSpec spec;

    @Column(nullable = false, length = 50)
    private String value;

    @Column(nullable = false)
    private int sortOrder = 0;
}
