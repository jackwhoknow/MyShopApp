package com.example.myshopapp.category;

import com.example.myshopapp.category.dto.CategoryNode;
import com.example.myshopapp.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台分类接口，游客可访问。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public ApiResponse<List<CategoryNode>> tree() {
        return ApiResponse.ok(categoryService.tree());
    }
}
