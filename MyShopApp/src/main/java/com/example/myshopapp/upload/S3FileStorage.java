package com.example.myshopapp.upload;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * S3 兼容对象存储：MinIO、云厂商 S3 网关均可使用。
 * 通过 myshop.upload.storage=s3 切换，多实例部署时图片不再依赖单机磁盘。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "myshop.upload", name = "storage", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("product", "avatar", "content");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final UploadProperties.S3 config;
    private final MinioClient client;
    private volatile boolean bucketReady;

    public S3FileStorage(UploadProperties properties) {
        this.config = properties.s3();
        if (!config.configured()) {
            throw new IllegalStateException(
                    "已选择对象存储，但 myshop.upload.s3 的 endpoint/access-key/secret-key/bucket 未配置完整");
        }
        this.client = MinioClient.builder()
                .endpoint(config.endpoint())
                .credentials(config.accessKey(), config.secretKey())
                .region(config.region())
                .build();
    }

    @Override
    public String storeImage(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过 5MB");
        }
        String safeCategory = StringUtils.hasText(category) ? category.trim() : "product";
        if (!ALLOWED_CATEGORIES.contains(safeCategory)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的图片分类");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传图片失败");
        }
        String extension = ImageSupport.detectExtension(bytes);
        if (extension == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        String objectName = safeCategory + "/" + LocalDate.now().format(MONTH_FORMATTER)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(config.bucket())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(ImageSupport.contentType(extension))
                    .build());
        } catch (Exception e) {
            log.error("上传图片到对象存储失败：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败，请稍后重试");
        }
        return publicUrl(objectName);
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(config.bucket()).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket()).build());
                log.info("对象存储桶不存在，已自动创建：{}", config.bucket());
            }
            bucketReady = true;
        }
    }

    /** 优先返回 CDN/公网前缀，未配置时回退到 endpoint/bucket 访问地址 */
    private String publicUrl(String objectName) {
        if (StringUtils.hasText(config.publicBaseUrl())) {
            String base = config.publicBaseUrl().endsWith("/")
                    ? config.publicBaseUrl().substring(0, config.publicBaseUrl().length() - 1)
                    : config.publicBaseUrl();
            return base + "/" + objectName;
        }
        String endpoint = config.endpoint().endsWith("/")
                ? config.endpoint().substring(0, config.endpoint().length() - 1)
                : config.endpoint();
        return endpoint + "/" + config.bucket() + "/" + objectName;
    }
}
