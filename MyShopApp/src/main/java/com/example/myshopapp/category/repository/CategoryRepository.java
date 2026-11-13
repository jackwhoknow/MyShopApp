package com.example.myshopapp.category.repository;

import com.example.myshopapp.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 一次性取出全部分类，在内存中组装树，避免 N 次查询 */
    List<Category> findAllByOrderByLevelAscSortOrderAscIdAsc();

    boolean existsByParentId(Long parentId);

    /** 查子树：prefix 传 "1/2/" 时匹配 1/2 下所有后代 */
    List<Category> findByPathStartingWith(String prefix);
}
