package com.example.myshopapp.content.repository;

import com.example.myshopapp.content.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    /** 前台展示：只看启用中的，按排序值升序 */
    List<Banner> findByEnabledTrueOrderBySortOrderAscIdAsc();

    /** 后台列表：包含停用项 */
    List<Banner> findAllByOrderBySortOrderAscIdAsc();
}
