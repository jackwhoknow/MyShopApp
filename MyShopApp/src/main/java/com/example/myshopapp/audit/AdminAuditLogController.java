package com.example.myshopapp.audit;

import com.example.myshopapp.audit.entity.AdminAuditLog;
import com.example.myshopapp.audit.repository.AdminAuditLogRepository;
import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员操作审计日志查询。
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminAuditLogRepository repository;

    @GetMapping
    public ApiResponse<PageResult<AdminAuditLog>> list(@RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = StringUtils.hasText(keyword)
                ? repository.search("%" + keyword.trim() + "%", pageable)
                : repository.findAllByOrderByIdDesc(pageable);
        return ApiResponse.ok(PageResult.of(result));
    }
}
