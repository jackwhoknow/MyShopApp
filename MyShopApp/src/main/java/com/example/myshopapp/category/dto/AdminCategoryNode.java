package com.example.myshopapp.category.dto;

import java.util.List;

/**
 * 后台分类树节点：与前台 CategoryNode 的区别是包含停用分类与启停状态。
 */
public record AdminCategoryNode(
        Long id,
        Long parentId,
        String name,
        int level,
        int sortOrder,
        boolean enabled,
        List<AdminCategoryNode> children
) {
}
