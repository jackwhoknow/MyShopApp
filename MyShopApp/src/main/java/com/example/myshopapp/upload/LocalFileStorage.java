package com.example.myshopapp.upload;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘存储：按分类/月份分目录，文件名随机生成，扩展名取自文件头而不是原始文件名，
 * 避免路径穿越、伪装扩展名和同名覆盖。生产多实例部署时应切换为对象存储。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "myshop.upload", name = "storage",
        havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("product", "avatar", "content");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final Path root;
    private final String publicPrefix;

    public LocalFileStorage(UploadProperties properties) {
        this.root = Paths.get(properties.dir()).toAbsolutePath().normalize();
        this.publicPrefix = properties.publicPrefix();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("上传目录创建失败: " + root, e);
        }
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
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传图片失败");
        }
        String extension = ImageSupport.detectExtension(bytes);
        if (extension == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        String relativeDir = safeCategory + "/" + LocalDate.now().format(MONTH_FORMATTER);
        Path targetDir = root.resolve(relativeDir).normalize();
        if (!targetDir.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的上传路径");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.createDirectories(targetDir);
            Files.write(targetDir.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            log.error("保存上传图片失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败，请稍后重试");
        }
        return publicPrefix + "/" + relativeDir + "/" + filename;
    }
}
