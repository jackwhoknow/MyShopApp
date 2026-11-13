package com.example.myshopapp.category;

import com.example.myshopapp.category.dto.AdminCategoryNode;
import com.example.myshopapp.category.dto.CategoryNode;
import com.example.myshopapp.category.dto.CategoryRequest;
import com.example.myshopapp.category.dto.CategoryVO;
import com.example.myshopapp.category.entity.Category;
import com.example.myshopapp.category.repository.CategoryRepository;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 分类业务：无限级树 + 后台维护。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /** 前台分类树：一次性查询后在内存组装，只保留启用中的分类 */
    @Transactional(readOnly = true)
    public List<CategoryNode> tree() {
        List<Category> all = categoryRepository.findAllByOrderByLevelAscSortOrderAscIdAsc()
                .stream()
                .filter(Category::isEnabled)
                .toList();
        return buildChildren(all, null);
    }

    /** 后台分类树：包含停用分类，便于平台维护 */
    @Transactional(readOnly = true)
    public List<AdminCategoryNode> adminTree() {
        List<Category> all = categoryRepository.findAllByOrderByLevelAscSortOrderAscIdAsc();
        return buildAdminChildren(all, null);
    }

    @Transactional
    public CategoryVO create(CategoryRequest request) {
        Category category = new Category();
        String pathPrefix = "";
        int level = 1;

        if (request.parentId() != null) {
            Category parent = requireCategory(request.parentId());
            level = parent.getLevel() + 1;
            pathPrefix = parent.getPath() + "/";
        }

        category.setParentId(request.parentId());
        category.setName(request.name());
        category.setLevel(level);
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setEnabled(request.enabled() == null || request.enabled());
        // path 依赖自增 id，插入时先占位，拿到 id 后再回填
        category.setPath("");

        categoryRepository.save(category);
        category.setPath(pathPrefix + category.getId());
        return CategoryVO.from(category);
    }

    /** 只允许改名称、排序、启停；不支持移动层级，避免重建整棵子树的 path */
    @Transactional
    public CategoryVO update(Long id, CategoryRequest request) {
        Category category = requireCategory(id);
        category.setName(request.name());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.enabled() != null) {
            category.setEnabled(request.enabled());
        }
        return CategoryVO.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = requireCategory(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分类下还有子分类，不能删除");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该分类下还有商品，不能删除");
        }
        categoryRepository.delete(category);
    }

    private Category requireCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "分类不存在"));
    }

    private List<CategoryNode> buildChildren(List<Category> all, Long parentId) {
        List<CategoryNode> nodes = new ArrayList<>();
        for (Category category : all) {
            if (Objects.equals(category.getParentId(), parentId)) {
                nodes.add(new CategoryNode(
                        category.getId(),
                        category.getName(),
                        category.getLevel(),
                        category.getSortOrder(),
                        buildChildren(all, category.getId())));
            }
        }
        return nodes;
    }

    private List<AdminCategoryNode> buildAdminChildren(List<Category> all, Long parentId) {
        List<AdminCategoryNode> nodes = new ArrayList<>();
        for (Category category : all) {
            if (Objects.equals(category.getParentId(), parentId)) {
                nodes.add(new AdminCategoryNode(
                        category.getId(),
                        category.getParentId(),
                        category.getName(),
                        category.getLevel(),
                        category.getSortOrder(),
                        category.isEnabled(),
                        buildAdminChildren(all, category.getId())));
            }
        }
        return nodes;
    }
}
