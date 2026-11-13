package com.example.myshopapp.category.dto;

import java.util.List;

/**
 * 分类树节点，children 为空表示叶子分类。
 */
public record CategoryNode(
        Long id,
        String name,
        int level,
        int sortOrder,
        List<CategoryNode> children
) {
}
