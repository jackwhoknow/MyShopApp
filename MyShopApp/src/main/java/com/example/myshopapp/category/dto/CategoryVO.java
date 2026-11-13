package com.example.myshopapp.category.dto;

import com.example.myshopapp.category.entity.Category;

/**
 * 分类平铺信息，用于后台管理和详情展示。
 */
public record CategoryVO(
        Long id,
        Long parentId,
        String name,
        int level,
        String path,
        int sortOrder,
        boolean enabled
) {

    public static CategoryVO from(Category category) {
        return new CategoryVO(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getLevel(),
                category.getPath(),
                category.getSortOrder(),
                category.isEnabled());
    }
}
