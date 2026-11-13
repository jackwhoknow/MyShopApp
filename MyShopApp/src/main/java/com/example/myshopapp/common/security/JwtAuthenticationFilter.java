package com.example.myshopapp.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 从 Authorization: Bearer xxx 中解析 JWT，把用户写入 SecurityContext。
 * 解析失败不在这里报错，交给后面的 AuthenticationEntryPoint 统一返回 401。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenRevocationService tokenRevocationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtUtil.ParsedToken parsed = jwtUtil.parse(token);
                LoginUser loginUser = parsed.loginUser();
                // 已注销或被改密吊销的 token 一律拒绝
                if (tokenRevocationService.isRevoked(token, loginUser.id(), parsed.issuedAt())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                List<SimpleGrantedAuthority> authorities = loginUser.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, token, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // 无效 token 当作未登录处理
            }
        }
        filterChain.doFilter(request, response);
    }
}
