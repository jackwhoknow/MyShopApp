package com.example.myshopapp.common.config;

import com.example.myshopapp.audit.AdminAuditInterceptor;
import com.example.myshopapp.upload.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 把本地上传目录映射为 /uploads/** 静态资源，浏览器可直接访问上传后的图片。
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UploadProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;
    private final AdminAuditInterceptor adminAuditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 后台写操作审计
        registry.addInterceptor(adminAuditInterceptor).addPathPatterns("/api/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadProperties.dir()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(uploadProperties.publicPrefix() + "/**")
                .addResourceLocations(location);
    }
}
