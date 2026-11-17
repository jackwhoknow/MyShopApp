package com.example.myshopapp.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 文件上传配置：本地存储目录、对外访问前缀，以及 S3 兼容对象存储参数。
 */
@ConfigurationProperties(prefix = "myshop.upload")
public record UploadProperties(String dir, String publicPrefix, String storage, S3 s3) {

    public UploadProperties {
        dir = StringUtils.hasText(dir) ? dir : "uploads";
        publicPrefix = StringUtils.hasText(publicPrefix) ? publicPrefix : "/uploads";
        storage = StringUtils.hasText(storage) ? storage.toLowerCase() : "local";
        s3 = s3 == null ? new S3(null, null, null, null, null, null) : s3;
    }

    public record S3(String endpoint, String accessKey, String secretKey, String bucket,
                     String region, String publicBaseUrl) {

        public S3 {
            region = StringUtils.hasText(region) ? region : "us-east-1";
        }

        public boolean configured() {
            return StringUtils.hasText(endpoint) && StringUtils.hasText(accessKey)
                    && StringUtils.hasText(secretKey) && StringUtils.hasText(bucket);
        }
    }
}
