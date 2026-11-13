package com.example.myshopapp.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SKU：真正参与交易的售卖单元，价格和库存都记在这一层。
 * 一个 SKU 由若干“规格值”组合确定，例如 红色 + M 码。
 */
@Entity
@Table(name = "product_sku")
@Getter
@Setter
@NoArgsConstructor
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock = 0;

    @Column(length = 255)
    private String image;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sku_spec_value",
            joinColumns = @JoinColumn(name = "sku_id"),
            inverseJoinColumns = @JoinColumn(name = "spec_value_id"))
    private Set<ProductSpecValue> specValues = new LinkedHashSet<>();
}
