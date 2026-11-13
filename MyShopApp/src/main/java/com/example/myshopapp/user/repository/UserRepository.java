package com.example.myshopapp.user.repository;

import com.example.myshopapp.user.entity.User;
import com.example.myshopapp.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsername(String username);

    /** 平台看板：未删除用户总数 */
    long countByDeletedFalse();

    /** 后台用户列表：支持用户名/昵称关键词、角色与状态筛选，排除已删除用户 */
    @Query(value = """
            select distinct u from User u join u.roles r
            where u.deleted = false
              and (:keyword = '' or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')))
              and (:role = '' or r.code = :role)
              and (:status is null or u.status = :status)
            """,
            countQuery = """
            select count(distinct u) from User u join u.roles r
            where u.deleted = false
              and (:keyword = '' or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')))
              and (:role = '' or r.code = :role)
              and (:status is null or u.status = :status)
            """)
    Page<User> searchAdmin(@Param("keyword") String keyword,
                           @Param("role") String role,
                           @Param("status") UserStatus status,
                           Pageable pageable);

    /** 通知群发接收人：正常状态用户，role 为空表示全部角色 */
    @Query("""
            select distinct u from User u join u.roles r
            where u.deleted = false
              and u.status = :status
              and (:role = '' or r.code = :role)
            """)
    List<User> findActiveRecipients(@Param("status") UserStatus status,
                                    @Param("role") String role);

    /** 通知群发前的人数预估：正常状态用户，role 为空表示全部角色 */
    @Query("""
            select count(distinct u) from User u join u.roles r
            where u.deleted = false
              and u.status = :status
              and (:role = '' or r.code = :role)
            """)
    long countActiveRecipients(@Param("status") UserStatus status,
                               @Param("role") String role);
}
