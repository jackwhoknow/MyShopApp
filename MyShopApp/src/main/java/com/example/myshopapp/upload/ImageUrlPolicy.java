package com.example.myshopapp.upload;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片地址白名单：只允许本站上传接口返回的地址入库（本地 /uploads 或配置的对象存储前缀），
 * 避免把外部链接写进评价、售后凭证。
 */
@Component
public class ImageUrlPolicy {

    private static final int MAX_URL_LENGTH = 255;

    private final List<String> allowedPrefixes;

    public ImageUrlPolicy(UploadProperties properties) {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(properties.publicPrefix() + "/");

        UploadProperties.S3 s3 = properties.s3();
        if (StringUtils.hasText(s3.publicBaseUrl())) {
            prefixes.add(stripTrailingSlash(s3.publicBaseUrl()) + "/");
        }
        if (StringUtils.hasText(s3.endpoint()) && StringUtils.hasText(s3.bucket())) {
            prefixes.add(stripTrailingSlash(s3.endpoint()) + "/" + s3.bucket() + "/");
        }
        this.allowedPrefixes = List.copyOf(prefixes);
    }

    public boolean allowed(String url) {
        return url != null && url.length() <= MAX_URL_LENGTH
                && allowedPrefixes.stream().anyMatch(url::startsWith);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
