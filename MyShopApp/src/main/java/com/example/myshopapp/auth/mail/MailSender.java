package com.example.myshopapp.auth.mail;

/**
 * 邮件发送抽象：本地开发默认把邮件内容打到日志，
 * 生产环境接入 spring-boot-starter-mail 后提供真实实现即可。
 */
public interface MailSender {

    void send(String to, String subject, String content);
}
