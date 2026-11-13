package com.example.myshopapp.product.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 规格定义，如“颜色”“尺寸”，一个商品可有多组规格。
 */
@Entity
@Table(name = "product_spec")
@Getter
@Setter
@NoArgsConstructor
public class ProductSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int sortOrder = 0;

    @OneToMany(mappedBy = "spec", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpecValue> values = new ArrayList<>();
}
