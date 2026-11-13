package com.example.myshopapp.common.util;

/**
 * 敏感信息脱敏工具。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** 手机号脱敏：138****1234 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
