package com.example.myshopapp.audit;

import com.example.myshopapp.audit.entity.AdminAuditLog;
import com.example.myshopapp.audit.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志写入。写入失败只记录应用日志，绝不影响业务请求本身。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository repository;

    @Transactional
    public void record(Long operatorId, String operatorName, String method, String uri,
                       String queryString, String ip, boolean success, int status, long durationMs) {
        try {
            AdminAuditLog entry = new AdminAuditLog();
            entry.setOperatorId(operatorId);
            entry.setOperatorName(operatorName);
            entry.setMethod(method);
            entry.setUri(uri.length() > 255 ? uri.substring(0, 255) : uri);
            entry.setQueryString(truncate(queryString, 500));
            entry.setIp(truncate(ip, 64));
            entry.setSuccess(success);
            entry.setStatus(status);
            entry.setDurationMs(durationMs);
            repository.save(entry);
        } catch (Exception e) {
            log.warn("写入管理员审计日志失败：{}", e.getMessage());
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
