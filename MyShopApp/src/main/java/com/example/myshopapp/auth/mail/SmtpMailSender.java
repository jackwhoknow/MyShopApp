package com.example.myshopapp.auth.mail;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SMTP 邮件实现：MAIL_ENABLED=true 时启用。
 * 启动时校验发件配置，缺项直接报错，避免验证码发送时才发现配置不完整。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "myshop.mail", name = "enabled", havingValue = "true")
public class SmtpMailSender implements MailSender {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final org.springframework.boot.autoconfigure.mail.MailProperties springMailProperties;
    private final MailProperties properties;

    @PostConstruct
    public void validateConfiguration() {
        require(StringUtils.hasText(springMailProperties.getHost()), "未配置 spring.mail.host");
        require(StringUtils.hasText(properties.from()), "未配置 myshop.mail.from");
        if (smtpAuthEnabled()) {
            require(StringUtils.hasText(springMailProperties.getUsername()), "未配置 spring.mail.username");
            require(StringUtils.hasText(springMailProperties.getPassword()), "未配置 spring.mail.password");
        }
    }

    @Override
    public void send(String to, String subject, String content) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("未找到 JavaMailSender Bean");
        }
        sender.send(buildMessage(to, subject, content));
        log.info("邮件已发送：to={} subject={}", to, subject);
    }

    private SimpleMailMessage buildMessage(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromName() + " <" + properties.from() + ">");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        return message;
    }

    private boolean smtpAuthEnabled() {
        Object auth = springMailProperties.getProperties().get("mail.smtp.auth");
        return auth == null || Boolean.parseBoolean(auth.toString());
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalStateException("邮件配置错误：" + message);
        }
    }
}
