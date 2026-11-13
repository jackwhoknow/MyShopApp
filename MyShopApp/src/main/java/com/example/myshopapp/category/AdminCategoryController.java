package com.example.myshopapp.category;

import com.example.myshopapp.category.dto.AdminCategoryNode;
import com.example.myshopapp.category.dto.CategoryRequest;
import com.example.myshopapp.category.dto.CategoryVO;
import com.example.myshopapp.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台端分类维护接口，需要 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /** 管理端分类树：包含停用分类 */
    @GetMapping("/tree")
    public ApiResponse<List<AdminCategoryNode>> tree() {
        return ApiResponse.ok(categoryService.adminTree());
    }

    @PostMapping
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryVO> update(@PathVariable Long id,
                                          @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
