package com.example.myshopapp.auth.mail;

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
 * 未配置 spring.mail.host 或发件人时发送会直接报错，避免验证码静默丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "myshop.mail", name = "enabled", havingValue = "true")
public class SmtpMailSender implements MailSender {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final MailProperties properties;

    @Override
    public void send(String to, String subject, String content) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("已开启邮件发送，但未配置 spring.mail.host");
        }
        if (!StringUtils.hasText(properties.from())) {
            throw new IllegalStateException("已开启邮件发送，但未配置 myshop.mail.from");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.fromName() + " <" + properties.from() + ">");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        sender.send(message);
        log.info("邮件已发送：to={} subject={}", to, subject);
    }
}
