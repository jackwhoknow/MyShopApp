package com.example.myshopapp.auth.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 日志邮件实现：不依赖外部邮件服务，验证码直接打印到应用日志，
 * 便于本地联调；生产环境开启 myshop.mail.enabled 后由 SMTP 实现接管。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "myshop.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String content) {
        log.warn("[EMAIL] to={} subject={} content={}", to, subject, content);
    }
}
