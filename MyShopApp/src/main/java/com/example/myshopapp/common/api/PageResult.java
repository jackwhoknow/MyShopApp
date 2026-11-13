package com.example.myshopapp.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 统一分页结果，避免把 Spring 的 Page 结构直接暴露给前端。
 */
public record PageResult<T>(List<T> list, long total, int page, int size) {

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(),
                page.getNumber(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }
}
