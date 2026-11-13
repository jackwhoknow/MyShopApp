package com.example.myshopapp.audit.repository;

import com.example.myshopapp.audit.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    Page<AdminAuditLog> findAllByOrderByIdDesc(Pageable pageable);

    /** 按操作人、接口路径模糊检索 */
    @Query("""
            select log from AdminAuditLog log
            where log.operatorName like :keyword or log.uri like :keyword
            order by log.id desc
            """)
    Page<AdminAuditLog> search(@Param("keyword") String keyword, Pageable pageable);
}
