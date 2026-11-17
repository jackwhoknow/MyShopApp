package com.example.myshopapp.auth;

import com.example.myshopapp.auth.mail.MailSender;
import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 邮箱验证码：用于忘记密码场景。
 * 验证码只存 Redis（10 分钟有效）并通过 MailSender 发送，不落数据库；
 * 同一邮箱 60 秒内只能请求一次，防止短信/邮件轰炸。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final String CODE_PREFIX = "myshop:emailcode:reset:";
    private static final String COOLDOWN_PREFIX = "myshop:emailcode:cooldown:";
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final MailSender mailSender;
    private final UserRepository userRepository;

    /**
     * 发送找回密码验证码：邮箱未注册时静默返回，避免账号探测。
     */
    public void sendResetCode(String email) {
        String normalized = normalize(email);
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(COOLDOWN_PREFIX + normalized, "1", COOLDOWN_TTL);
        if (Boolean.FALSE.equals(first)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "验证码发送过于频繁，请 1 分钟后再试");
        }
        redisTemplate.expire(COOLDOWN_PREFIX + normalized, COOLDOWN_TTL);

        if (userRepository.findByEmailAndDeletedFalse(normalized).isEmpty()) {
            log.info("找回密码请求的邮箱未注册：{}", normalized);
            return;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_PREFIX + normalized, code, CODE_TTL);
        mailSender.send(normalized, "【商城】找回密码验证码",
                "你的找回密码验证码为 " + code + "，10 分钟内有效。如非本人操作请忽略本邮件。");
    }

    /** 校验并立即作废验证码 */
    public void verifyResetCode(String email, String code) {
        String normalized = normalize(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入邮箱验证码");
        }
        String stored = redisTemplate.opsForValue().getAndDelete(CODE_PREFIX + normalized);
        if (stored == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }
        if (!stored.equals(code.trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不正确");
        }
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入邮箱");
        }
        return email.trim().toLowerCase();
    }
}
