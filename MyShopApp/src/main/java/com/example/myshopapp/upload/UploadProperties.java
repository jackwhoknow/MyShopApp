package com.example.myshopapp.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 文件上传配置：本地存储目录与对外访问前缀。
 */
@ConfigurationProperties(prefix = "myshop.upload")
public record UploadProperties(String dir, String publicPrefix) {

    public UploadProperties {
        dir = StringUtils.hasText(dir) ? dir : "uploads";
        publicPrefix = StringUtils.hasText(publicPrefix) ? publicPrefix : "/uploads";
    }
}
