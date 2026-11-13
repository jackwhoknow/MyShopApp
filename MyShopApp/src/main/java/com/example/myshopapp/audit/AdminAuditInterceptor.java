package com.example.myshopapp.audit;

import com.example.myshopapp.common.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台写操作审计拦截器：只记录 POST/PUT/PATCH/DELETE，避免查询请求淹没日志。
 */
@Component
@RequiredArgsConstructor
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = AdminAuditInterceptor.class.getName() + ".START";

    private final AdminAuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            request.setAttribute(START_ATTR, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (start == null) {
            return;
        }
        long duration = System.currentTimeMillis() - (Long) start;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long operatorId = null;
        String operatorName = "anonymous";
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            operatorId = loginUser.id();
            operatorName = loginUser.username();
        }
        String query = request.getQueryString();
        auditLogService.record(operatorId, operatorName, request.getMethod(),
                request.getRequestURI(), query, clientIp(request), ex == null && response.getStatus() < 400,
                response.getStatus(), duration);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
