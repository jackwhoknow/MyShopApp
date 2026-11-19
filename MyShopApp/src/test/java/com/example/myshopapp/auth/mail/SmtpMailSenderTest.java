package com.example.myshopapp.auth.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SMTP 通道：配置校验与邮件内容组装。 */
class SmtpMailSenderTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);

    private org.springframework.boot.autoconfigure.mail.MailProperties springProperties;
    private MailProperties properties;

    @BeforeEach
    void setUp() {
        springProperties = new org.springframework.boot.autoconfigure.mail.MailProperties();
        springProperties.setHost("smtp.test");
        springProperties.setUsername("noreply@test.com");
        springProperties.setPassword("secret");
        properties = new MailProperties(true, "noreply@test.com", "测试商城");
    }

    @Test
    void sendsConfiguredMessage() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        SmtpMailSender sender = sender();
        sender.validateConfiguration();
        sender.send("user@test.com", "验证码", "123456");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("测试商城 <noreply@test.com>");
        assertThat(captor.getValue().getTo()).containsExactly("user@test.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("验证码");
        assertThat(captor.getValue().getText()).isEqualTo("123456");
    }

    @Test
    void missingHostFailsValidation() {
        springProperties.setHost("");
        assertThatThrownBy(() -> sender().validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("host");
    }

    @Test
    void missingFromFailsValidation() {
        properties = new MailProperties(true, "", "测试商城");
        assertThatThrownBy(() -> sender().validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("from");
    }

    private SmtpMailSender sender() {
        return new SmtpMailSender(provider, springProperties, properties);
    }
}
