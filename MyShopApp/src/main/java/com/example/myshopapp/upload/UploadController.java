package com.example.myshopapp.upload;

import com.example.myshopapp.common.api.ApiResponse;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.common.security.LoginUser;
import com.example.myshopapp.upload.dto.UploadResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传接口：商品图与首页内容图仅商家/管理员可用，头像允许所有登录用户上传。
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorage fileStorage;

    @PostMapping("/image")
    public ApiResponse<UploadResultVO> uploadImage(@AuthenticationPrincipal LoginUser loginUser,
                                                   @RequestParam("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "product") String category) {
        if (("product".equals(category) || "content".equals(category)) && !isMerchantOrAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有商家或管理员可以上传商品图片");
        }
        return ApiResponse.ok(new UploadResultVO(fileStorage.storeImage(file, category)));
    }

    private boolean isMerchantOrAdmin(LoginUser loginUser) {
        return loginUser.roles().contains("MERCHANT") || loginUser.roles().contains("ADMIN");
    }
}
