package com.example.myshopapp.common.config;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + JWT，不需要 CSRF 和 Session
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 常见安全响应头：防点击劫持、防 MIME 嗅探、限制引用来源与浏览器权限
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicy(permissions -> permissions
                            .policy("camera=(), microphone=(), geolocation=()"));
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000));
                })
                .authorizeHttpRequests(auth -> auth
                        // 退出登录必须携带有效登录态，才能知道要吊销哪个 token
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        // 支付平台服务器回调不带登录态，安全性由签名验签保证
                        .requestMatchers("/api/payments/alipay/notify", "/api/payments/wechat/notify").permitAll()
                        // 商品、分类、店铺、首页内容的读接口对外开放，游客也能逛
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**",
                                "/api/shops/**", "/api/content/**", "/uploads/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/merchant/**").hasAnyRole("MERCHANT", "ADMIN")
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) -> writeError(response, 401, "未登录或登录已过期"))
                        .accessDeniedHandler((request, response, ex) -> writeError(response, 403, "没有访问权限")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许跨域的前端地址由配置项控制，生产环境只放行正式域名
        config.setAllowedOriginPatterns(securityProperties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 只把 traceId 暴露给前端，便于用户反馈问题时提供排查 ID
        config.setExposedHeaders(List.of(TraceIdFilter.HEADER));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
