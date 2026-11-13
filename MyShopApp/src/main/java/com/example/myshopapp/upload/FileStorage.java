package com.example.myshopapp.upload;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象：当前用本地磁盘，后续可无缝替换为对象存储。
 */
public interface FileStorage {

    /**
     * 保存图片，返回可直接访问的相对地址（如 /uploads/product/202611/xxx.jpg）。
     */
    String storeImage(MultipartFile file, String category);
}
