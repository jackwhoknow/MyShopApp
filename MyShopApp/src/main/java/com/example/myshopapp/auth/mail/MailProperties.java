package com.example.myshopapp.auth.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 邮件发送配置：未配置 SMTP 时保持关闭，验证码退化为日志输出。
 */
@ConfigurationProperties(prefix = "myshop.mail")
public record MailProperties(boolean enabled, String from, String fromName) {

    public MailProperties {
        from = StringUtils.hasText(from) ? from : null;
        fromName = StringUtils.hasText(fromName) ? fromName : "星耀商城";
    }
}
